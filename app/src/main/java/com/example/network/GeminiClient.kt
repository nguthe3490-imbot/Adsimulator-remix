package com.example.network

import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@JsonClass(generateAdapter = true)
data class GeminiPart(
    @Json(name = "text") val text: String
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    @Json(name = "parts") val parts: List<GeminiPart>,
    @Json(name = "role") val role: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    @Json(name = "contents") val contents: List<GeminiContent>,
    @Json(name = "systemInstruction") val systemInstruction: GeminiContent? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    @Json(name = "content") val content: GeminiContent?
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    @Json(name = "candidates") val candidates: List<GeminiCandidate>?
)

object GeminiClient {
    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val requestAdapter = moshi.adapter(GeminiRequest::class.java)
    private val responseAdapter = moshi.adapter(GeminiResponse::class.java)

    /**
     * Generates a response from Gemini using specified parameters.
     * Returns the text, or null if an error occurred / API key is missing.
     */
    suspend fun generateTeasingResponse(
        systemPrompt: String,
        conversationHistory: List<GeminiContent>
    ): String? = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext null
        }

        val requestUrl = "$BASE_URL?key=$apiKey"
        val requestBodyData = GeminiRequest(
            contents = conversationHistory,
            systemInstruction = GeminiContent(parts = listOf(GeminiPart(systemPrompt)))
        )

        // Clean conversations by mapping null roles or ensuring valid system prompt format
        val jsonString = try {
            requestAdapter.toJson(requestBodyData)
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonString.toRequestBody(mediaType)

        val request = Request.Builder()
            .url(requestUrl)
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string()
                    System.err.println("Gemini API Error: Code ${response.code}, Body: $errBody")
                    return@withContext null
                }
                val bodyString = response.body?.string() ?: return@withContext null
                val r = responseAdapter.fromJson(bodyString)
                val textResponse = r?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                return@withContext textResponse
            }
        } catch (e: IOException) {
            e.printStackTrace()
            return@withContext null
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

    /**
     * Generates an image using Gemini (Imagen tool equivalent model: gemini-2.5-flash-image)
     * Returns the base64-encoded image string, or null if an error occurred.
     */
    suspend fun generateAdImage(
        prompt: String
    ): String? = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext null
        }

        // Using gemini-2.5-flash-image
        val requestUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-image:generateContent?key=$apiKey"
        
        val requestBodyData = GeminiImageRequest(
            contents = listOf(
                GeminiImageContent(
                    parts = listOf(GeminiImagePart(text = prompt))
                )
            ),
            generationConfig = GeminiImageGenerationConfig(
                imageConfig = GeminiImageConfig(aspectRatio = "1:1", imageSize = "1K"),
                responseModalities = listOf("TEXT", "IMAGE")
            )
        )

        val jsonString = try {
            moshi.adapter(GeminiImageRequest::class.java).toJson(requestBodyData)
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonString.toRequestBody(mediaType)

        val request = Request.Builder()
            .url(requestUrl)
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string()
                    System.err.println("Gemini Image API Error: Code ${response.code}, Body: $errBody")
                    return@withContext null
                }
                val bodyString = response.body?.string() ?: return@withContext null
                val r = moshi.adapter(GeminiImageResponse::class.java).fromJson(bodyString)
                
                // Find part with inlineData (contains generated image in base64 format)
                val inlinePart = r?.candidates?.firstOrNull()?.content?.parts?.firstOrNull { it.inlineData != null }
                return@withContext inlinePart?.inlineData?.data
            }
        } catch (e: IOException) {
            e.printStackTrace()
            return@withContext null
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }
}

@JsonClass(generateAdapter = true)
data class GeminiImagePart(
    @Json(name = "text") val text: String? = null,
    @Json(name = "inlineData") val inlineData: GeminiImageInlineData? = null
)

@JsonClass(generateAdapter = true)
data class GeminiImageInlineData(
    @Json(name = "mimeType") val mimeType: String,
    @Json(name = "data") val data: String
)

@JsonClass(generateAdapter = true)
data class GeminiImageContent(
    @Json(name = "parts") val parts: List<GeminiImagePart>
)

@JsonClass(generateAdapter = true)
data class GeminiImageConfig(
    @Json(name = "aspectRatio") val aspectRatio: String,
    @Json(name = "imageSize") val imageSize: String
)

@JsonClass(generateAdapter = true)
data class GeminiImageGenerationConfig(
    @Json(name = "imageConfig") val imageConfig: GeminiImageConfig,
    @Json(name = "responseModalities") val responseModalities: List<String>
)

@JsonClass(generateAdapter = true)
data class GeminiImageRequest(
    @Json(name = "contents") val contents: List<GeminiImageContent>,
    @Json(name = "generationConfig") val generationConfig: GeminiImageGenerationConfig
)

@JsonClass(generateAdapter = true)
data class GeminiImageCandidate(
    @Json(name = "content") val content: GeminiImageContent?
)

@JsonClass(generateAdapter = true)
data class GeminiImageResponse(
    @Json(name = "candidates") val candidates: List<GeminiImageCandidate>?
)
