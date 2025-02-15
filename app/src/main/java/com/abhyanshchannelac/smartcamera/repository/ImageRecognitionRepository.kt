package com.abhyanshchannelac.smartcamera.repository

import com.abhyanshchannelac.smartcamera.api.ImaggaService
import com.abhyanshchannelac.smartcamera.api.Tag
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Base64

class ImageRecognitionRepository {
    private val imaggaService: ImaggaService

    init {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.imagga.com")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        imaggaService = retrofit.create(ImaggaService::class.java)
    }

    suspend fun recognizeImage(imageUrl: String): Result<List<Tag>> {
        return try {
            val auth = "Basic " + Base64.getEncoder().encodeToString(
                "acc_a8ac440abdffb59:c9ca2b253d135d45eb2562cb5661a1d7".toByteArray()
            )
            
            val response = imaggaService.getTags(auth, imageUrl)
            if (response.isSuccessful) {
                val tags = response.body()?.result?.tags ?: emptyList()
                Result.success(tags)
            } else {
                Result.failure(Exception("Failed to recognize image: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
