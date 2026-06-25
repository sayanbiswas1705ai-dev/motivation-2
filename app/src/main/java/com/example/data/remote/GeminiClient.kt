package com.example.data.remote

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class Part(val text: String)

@JsonClass(generateAdapter = true)
data class Content(val parts: List<Part>)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val responseMimeType: String? = null,
    val temperature: Float? = null
)

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(val content: Content)

@JsonClass(generateAdapter = true)
data class GeminiResponse(val candidates: List<Candidate>?)

interface GeminiApiService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

@JsonClass(generateAdapter = true)
data class RawQuizQuestion(
    val word: String,
    val optionA: String,
    val optionB: String,
    val optionC: String,
    val optionD: String,
    val correctOptionIndex: Int,
    val explanation: String
)

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val service: GeminiApiService = retrofit.create(GeminiApiService::class.java)

    suspend fun generateVocabularyQuiz(apiKey: String, textContent: String): List<RawQuizQuestion> {
        val prompt = """
            You are an expert tutor creating a vocabulary multiple-choice quiz.
            Analyze the following text extracted from a vocabulary document and identify key vocabulary words and their meanings present in the document.
            Generate a multiple-choice quiz from them.
            Each question must show one vocabulary word and ask the user to pick its correct meaning from 4 options:
            - Exactly one option must be the correct meaning.
            - The other three options must be plausible incorrect options also derived from or related to the document/context.
            - Provide a brief, simple explanation of the word's meaning.
            - The options must be represented as optionA, optionB, optionC, and optionD.
            - The correctOptionIndex must be an integer from 0 to 3 (0 = optionA, 1 = optionB, 2 = optionC, 3 = optionD).

            Return the response in a strict JSON array matching the following schema:
            [
              {
                "word": "word",
                "optionA": "definition A",
                "optionB": "definition B",
                "optionC": "definition C",
                "optionD": "definition D",
                "correctOptionIndex": 0,
                "explanation": "explanation of word meaning"
              }
            ]

            Do not wrap in markdown or anything else except raw JSON array. Here is the source text content:
            
            $textContent
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(Content(parts = listOf(Part(prompt)))),
            generationConfig = GenerationConfig(responseMimeType = "application/json", temperature = 0.2f)
        )

        val modelsToTry = listOf("gemini-3.5-flash", "gemini-flash-latest", "gemini-3.1-flash-lite-preview")
        var textResponse: String? = null
        var lastException: Exception? = null

        for (model in modelsToTry) {
            try {
                android.util.Log.d("GeminiClient", "Attempting vocabulary quiz generation with model: $model")
                val response = service.generateContent(model, apiKey, request)
                val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!text.isNullOrBlank()) {
                    textResponse = text
                    android.util.Log.d("GeminiClient", "Successfully received vocabulary quiz response from model: $model")
                    break
                }
            } catch (e: Exception) {
                android.util.Log.w("GeminiClient", "Model $model failed with exception: ${e.message}", e)
                lastException = e
            }
        }

        if (textResponse == null) {
            val errorMessage = lastException?.message ?: "Unknown API response"
            when {
                errorMessage.contains("503") -> {
                    throw Exception("The Gemini API is temporarily busy (HTTP 503). This usually happens when the model is under high demand. Please wait a few seconds and try again.")
                }
                errorMessage.contains("429") -> {
                    throw Exception("Rate limit exceeded (HTTP 429). Please wait a moment and try again.")
                }
                else -> {
                    throw Exception("Failed to generate quiz: $errorMessage")
                }
            }
        }

        // Parse JSON list
        val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, RawQuizQuestion::class.java)
        val adapter = moshi.adapter<List<RawQuizQuestion>>(type)
        return adapter.fromJson(textResponse) ?: emptyList()
    }
}
