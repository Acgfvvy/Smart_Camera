package com.abhyanshchannelac.smartcamera.repository

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.Base64
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import timber.log.Timber

// Data classes for Clarifai API
private data class ClarifaiImage(val base64: String)

private data class ClarifaiInput(val data: ClarifaiInputData)

private data class ClarifaiInputData(val image: ClarifaiImage)

private data class ClarifaiRequest(val inputs: List<ClarifaiInput>)

// Retrofit service interface
private interface ClarifaiService {
    @POST("v2/users/clarifai/apps/main/workflows/SmartCamFlow/results")
    suspend fun analyzeImage(
        @Header("Authorization") auth: String,
        @Header("Content-Type") contentType: String = "application/json",
        @Body request: ClarifaiRequest
    ): Response<ClarifaiResponse>
}

// Response data classes
private data class ClarifaiResponse(
    val status: ClarifaiStatus,
    val outputs: List<ClarifaiOutput>
)

private data class ClarifaiStatus(
    val code: Int,
    val description: String
)

private data class ClarifaiOutput(
    val id: String,
    val status: ClarifaiStatus,
    val created_at: String,
    val model: ClarifaiModel,
    val data: ClarifaiOutputData
)

private data class ClarifaiModel(
    val name: String,
    val id: String,
    val model_version: ClarifaiModelVersion
)

private data class ClarifaiModelVersion(
    val id: String
)

private data class ClarifaiOutputData(
    val concepts: List<Concept>
)

private data class Concept(
    val id: String,
    val name: String,
    val value: Float,
    val app_id: String
)

class ImageRecognitionRepository {
    private val clarifaiApiKey = "Key 972ba2518e864b7e8aeba5881b2b5275"
    
    private val clarifaiService: ClarifaiService by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
            
        Retrofit.Builder()
            .baseUrl("https://api.clarifai.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ClarifaiService::class.java)
    }

    suspend fun getClarifaiTags(imageFile: File): List<String> = withContext(Dispatchers.IO) {
        try {
            val bytes = imageFile.readBytes()
            val base64Image = Base64.getEncoder().encodeToString(bytes)
            
            val request = ClarifaiRequest(
                inputs = listOf(
                    ClarifaiInput(
                        data = ClarifaiInputData(
                            image = ClarifaiImage(base64 = base64Image)
                        )
                    )
                )
            )
            
            val response = clarifaiService.analyzeImage(
                auth = clarifaiApiKey,
                request = request
            )
            
            if (response.isSuccessful) {
                response.body()?.let { clarifaiResponse ->
                    if (clarifaiResponse.status.code == 10000) { // 10000 is the success code from Clarifai
                        return@withContext clarifaiResponse.outputs.firstOrNull()?.data?.concepts
                            ?.sortedByDescending { it.value }
                            ?.take(5) // Limit to top 5 results
                            ?.map { 
                                "${it.name} (${String.format("%.1f", it.value * 100)}%)" 
                            } ?: emptyList()
                    } else {
                        throw Exception("Clarifai API error: ${clarifaiResponse.status.description}")
                    }
                } ?: throw Exception("Empty response from Clarifai API")
            } else {
                throw Exception("API call failed with code: ${response.code()}")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting Clarifai tags")
            throw e
        }
    }
}
