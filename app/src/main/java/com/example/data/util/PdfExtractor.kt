package com.example.data.util

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

object PdfExtractor {
    private var isInitialized = false

    fun init(context: Context) {
        if (!isInitialized) {
            PDFBoxResourceLoader.init(context.applicationContext)
            isInitialized = true
        }
    }

    suspend fun extractText(
        context: Context,
        uri: Uri,
        fileName: String,
        startPage: Int? = null,
        endPage: Int? = null
    ): String = withContext(Dispatchers.IO) {
        val lowercaseName = fileName.lowercase()
        val mimeType = context.contentResolver.getType(uri)?.lowercase() ?: ""
        
        when {
            lowercaseName.endsWith(".txt") || mimeType == "text/plain" -> {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                        reader.readText()
                    }
                } ?: throw Exception("Could not open input stream for text file")
            }
            lowercaseName.endsWith(".pdf") || mimeType == "application/pdf" -> {
                extractTextFromPdf(context, uri, startPage, endPage)
            }
            else -> {
                // Try reading as plain text with robust guard against binary formats
                try {
                    val content = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                            reader.readText()
                        }
                    } ?: throw Exception("Could not open input stream")
                    
                    val controlCount = content.count { it.isISOControl() && it != '\n' && it != '\r' && it != '\t' }
                    if (controlCount > content.length * 0.05) {
                        throw Exception("Binary content detected")
                    }
                    content
                } catch (e: Exception) {
                    throw Exception("Unsupported file format: '$fileName'. Please upload a PDF (.pdf) or a text (.txt) file.")
                }
            }
        }
    }

    suspend fun extractTextFromPdf(
        context: Context,
        uri: Uri,
        startPage: Int? = null,
        endPage: Int? = null
    ): String = withContext(Dispatchers.IO) {
        init(context)
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val document = PDDocument.load(inputStream)
            val stripper = PDFTextStripper()
            if (startPage != null) {
                stripper.startPage = startPage
            }
            if (endPage != null) {
                stripper.endPage = endPage
            }
            val text = stripper.getText(document)
            document.close()
            text ?: ""
        } ?: throw Exception("Could not open input stream for PDF")
    }

    private fun findTelegramAttachedDocumentUrl(html: String): String? {
        val docWrapRegex = """class="[^"]*tgme_widget_message_document_wrap[^"]*"[^>]*href="([^"]+)"""".toRegex()
        docWrapRegex.find(html)?.let { return it.groupValues[1] }

        val hrefFirstRegex = """href="([^"]+)"[^>]*class="[^"]*tgme_widget_message_document_wrap[^"]*"""".toRegex()
        hrefFirstRegex.find(html)?.let { return it.groupValues[1] }

        val genericPdfLinkRegex = """href="([^"]+\.pdf[^"]*)"""".toRegex(RegexOption.IGNORE_CASE)
        genericPdfLinkRegex.find(html)?.let { return it.groupValues[1] }

        return null
    }

    private fun downloadPdfBodyAndExtract(
        context: Context,
        body: okhttp3.ResponseBody,
        fileName: String,
        startPage: Int?,
        endPage: Int?
    ): Pair<String, String> {
        val tempFile = File(context.cacheDir, "temp_downloaded_${System.currentTimeMillis()}.pdf")
        tempFile.outputStream().use { fos ->
            body.byteStream().copyTo(fos)
        }
        return try {
            init(context)
            val document = PDDocument.load(tempFile)
            val stripper = PDFTextStripper()
            if (startPage != null) stripper.startPage = startPage
            if (endPage != null) stripper.endPage = endPage
            val text = stripper.getText(document)
            document.close()
            Pair(text ?: "", fileName)
        } finally {
            tempFile.delete()
        }
    }

    suspend fun downloadAndExtract(
        context: Context,
        urlString: String,
        startPage: Int? = null,
        endPage: Int? = null
    ): Pair<String, String> = withContext(Dispatchers.IO) {
        val isTelegramPostLink = urlString.contains("t.me/")
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()

        val request = Request.Builder()
            .url(urlString)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36")
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("Failed to fetch link: HTTP ${response.code}")
        }

        val body = response.body ?: throw Exception("Response body was empty")
        val contentType = body.contentType()?.toString()?.lowercase() ?: ""
        
        // Guess file name from content-disposition or URL path
        val contentDisposition = response.header("Content-Disposition") ?: ""
        var fileName = ""
        if (contentDisposition.contains("filename=")) {
            val parts = contentDisposition.split("filename=")
            if (parts.size > 1) {
                fileName = parts[1].trim().removeSurrounding("\"", "\"").removeSurrounding("'", "'")
            }
        }
        if (fileName.isBlank()) {
            val path = Uri.parse(urlString).path ?: ""
            fileName = path.substringAfterLast('/')
        }
        if (fileName.isBlank()) {
            fileName = "downloaded_document"
        }

        val lowercaseName = fileName.lowercase()

        when {
            // PDF
            contentType.contains("application/pdf") || lowercaseName.endsWith(".pdf") -> {
                if (!fileName.lowercase().endsWith(".pdf")) {
                    fileName += ".pdf"
                }
                downloadPdfBodyAndExtract(context, body, fileName, startPage, endPage)
            }
            // TXT
            contentType.contains("text/plain") || lowercaseName.endsWith(".txt") -> {
                if (!fileName.lowercase().endsWith(".txt")) {
                    fileName += ".txt"
                }
                val text = body.string()
                Pair(text, fileName)
            }
            // HTML / Telegram
            contentType.contains("text/html") || lowercaseName.endsWith(".html") || lowercaseName.endsWith(".htm") || isTelegramPostLink -> {
                val htmlContent = body.string()

                if (isTelegramPostLink) {
                    val attachedDocUrl = findTelegramAttachedDocumentUrl(htmlContent)
                    if (attachedDocUrl != null) {
                        val docLowercase = attachedDocUrl.lowercase()
                        if (docLowercase.endsWith(".pdf") || docLowercase.contains(".pdf?")) {
                            val docRequest = Request.Builder()
                                .url(attachedDocUrl)
                                .header("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36")
                                .build()
                            val docResponse = client.newCall(docRequest).execute()
                            if (docResponse.isSuccessful) {
                                val docBody = docResponse.body
                                if (docBody != null) {
                                    var docFileName = Uri.parse(attachedDocUrl).lastPathSegment ?: "telegram_document.pdf"
                                    if (!docFileName.lowercase().endsWith(".pdf")) {
                                        docFileName += ".pdf"
                                    }
                                    return@withContext downloadPdfBodyAndExtract(context, docBody, docFileName, startPage, endPage)
                                }
                            }
                        }
                    }
                }

                val extractedText = extractTextFromHtml(htmlContent, urlString)
                val displayTitle = if (isTelegramPostLink) {
                    // Try to extract channel name if possible, e.g. from t.me/channel/123
                    val uri = Uri.parse(urlString)
                    val segments = uri.pathSegments
                    if (segments.isNotEmpty()) "Telegram: @${segments[0]}" else "Telegram Post"
                } else {
                    "Web Link Content"
                }
                Pair(extractedText, displayTitle)
            }
            // Default plain text fallbacks
            else -> {
                val text = body.string()
                val controlCount = text.count { it.isISOControl() && it != '\n' && it != '\r' && it != '\t' }
                if (controlCount > text.length * 0.05) {
                    throw Exception("The downloaded content appears to be binary. Please provide a direct link to a PDF, TXT file, or a public Telegram post link.")
                }
                Pair(text, fileName)
            }
        }
    }

    private fun extractTextFromHtml(html: String, url: String): String {
        if (url.contains("t.me/")) {
            // Look for Telegram message widget content
            val tgTextRegex = """class="[^"]*tgme_widget_message_text[^"]*"[^>]*>(.*?)</div>""".toRegex(RegexOption.DOT_MATCHES_ALL)
            val matches = tgTextRegex.findAll(html)
            if (matches.any()) {
                val combinedText = matches.map { match ->
                    val rawHtml = match.groupValues[1]
                    stripHtmlTags(rawHtml)
                }.joinToString("\n\n")
                if (combinedText.isNotBlank()) {
                    return combinedText
                }
            }
            
            // Look for og:description fallback
            val descRegex = """meta property="og:description" content="([^"]*)"""".toRegex()
            val descMatch = descRegex.find(html)
            if (descMatch != null) {
                val desc = descMatch.groupValues[1]
                if (desc.isNotBlank() && !desc.contains("Telegram")) {
                    return stripHtmlTags(desc)
                }
            }
        }

        return stripHtmlTags(html)
    }

    private fun stripHtmlTags(html: String): String {
        var text = html
        text = text.replace("""(?s)<style.*?>.*?</style>""".toRegex(), "")
        text = text.replace("""(?s)<script.*?>.*?</script>""".toRegex(), "")
        text = text.replace("""(?i)<br\s*/?>""".toRegex(), "\n")
        text = text.replace("""(?i)</p\s*>""".toRegex(), "\n\n")
        text = text.replace("""(?i)</div>""".toRegex(), "\n")
        text = text.replace("""<[^>]*>""".toRegex(), "")
        
        text = text.replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&rsquo;", "'")
            .replace("&lsquo;", "'")
            .replace("&ldquo;", "\"")
            .replace("&rdquo;", "\"")
            .replace("&ndash;", "-")
            .replace("&mdash;", "—")
        
        return text.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .joinToString("\n")
    }
}
