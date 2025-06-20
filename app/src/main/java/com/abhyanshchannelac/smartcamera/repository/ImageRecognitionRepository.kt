package com.abhyanshchannelac.smartcamera.repository

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Base64
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import com.google.gson.JsonParser
import com.google.gson.JsonElement

class ImageRecognitionRepository {
    private val clarifaiApiKey = "972ba2518e864b7e8aeba5881b2b5275"
    private val clarifaiWorkflowId = "SmartCamFlow"
    private val clarifaiUserId = "clarifai"
    private val clarifaiAppId = "main"

    suspend fun getClarifaiTags(imageFile: File): List<String> = withContext(Dispatchers.IO) {
        val bytes = imageFile.readBytes()
        val base64Image = Base64.getEncoder().encodeToString(bytes)

        val url = URL("https://api.clarifai.com/v2/users/$clarifaiUserId/apps/$clarifaiAppId/workflows/$clarifaiWorkflowId/results")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Authorization", "Key $clarifaiApiKey")
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true

        val jsonInputString = """
            {
                "inputs": [
                    {
                        "data": {
                            "image": {
                            "base64": "$base64Image"
                            }
                        }
                    }
                ]
            }
        """

        connection.outputStream.use { os ->
            val input = jsonInputString.toByteArray()
            os.write(input, 0, input.size)
        }

        val responseCode = connection.responseCode
        if (responseCode == HttpURLConnection.HTTP_OK) {
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val concepts = JsonParser().parse(response)
                .asJsonObject["results"].asJsonArray[0]
                .asJsonObject["outputs"].asJsonArray[0]
                .asJsonObject["data"].asJsonObject["concepts"].asJsonArray

            return@withContext concepts.map { concept: JsonElement ->
                val name = concept.asJsonObject["name"].asString
                val value = concept.asJsonObject["value"].asFloat * 100
                "$name (${String.format("%.1f", value)}%)"
            }
        } else {
            throw Exception("Clarifai API error: $responseCode")
        }
    }
}
