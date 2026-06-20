package com.example.data.api

import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class Part(
    @Json(name = "text") val text: String
)

@JsonClass(generateAdapter = true)
data class Content(
    @Json(name = "parts") val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    @Json(name = "temperature") val temperature: Float? = null,
    @Json(name = "maxOutputTokens") val maxOutputTokens: Int? = null
)

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    @Json(name = "contents") val contents: List<Content>,
    @Json(name = "generationConfig") val generationConfig: GenerationConfig? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    @Json(name = "content") val content: Content?
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    @Json(name = "candidates") val candidates: List<Candidate>?
)

interface GeminiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val service: GeminiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiService::class.java)
    }

    suspend fun getStudyTip(
        moduleName: String,
        progressPercent: Int,
        todayTopic: String,
        todayDescription: String
    ): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return "Coaching Goal: Work through '${todayTopic}'. Consistency is your super power, keep taking small steps every day!"
        }

        val prompt = """
            You are an expert, encouraging study coach AI assistant.
            The user is studying a software development curriculum with modern concepts.
            Here is their status:
            - Current Module: "$moduleName"
            - Progress completed in current module: $progressPercent%
            - Today's Topic: "$todayTopic"
            - Description of topic: "$todayDescription"

            Generate a custom, highly encouraging, concise Study Tip or a specific, actionable daily Goal (max 2-3 sentences) tailored to their module progress and today's topic. Advise them exactly on how they can maximize their focus, retention, or coding practice for this specific theme. Keep it professional, inspirational, and highly engaging. Do not use markdown format other than simple bolding if needed, and do not prefix the answer with "Study Tip:" or anything similar. Just give the coaching text directly.
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(
                Content(parts = listOf(Part(text = prompt)))
            ),
            generationConfig = GenerationConfig(temperature = 0.7f)
        )

        return try {
            val response = service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
                ?: "Tip of the Day: Keep going with $todayTopic. Spend 15 minutes drafting practical scenarios and code snippets to solidify your learning."
        } catch (e: Exception) {
            e.printStackTrace()
            "Coaching Goal: Review '$todayTopic' with extreme focus today. Real mastery comes from day-to-day discipline and small victories!"
        }
    }
}
