package com.abhyanshchannelac.smartcamera.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface ImaggaService {
    @GET("/v2/tags")
    suspend fun getTags(
        @Header("Authorization") auth: String,
        @Query("image_url") imageUrl: String
    ): Response<TagResponse>
}
