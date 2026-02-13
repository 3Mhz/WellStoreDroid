package com.example.droidscrape.data.remote

import com.example.droidscrape.data.remote.dto.UploadRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.HEAD
import retrofit2.http.POST
import retrofit2.http.Url

interface UploadService {
    @POST
    suspend fun upload(@Url url: String, @Body request: UploadRequest): Response<Unit>

    @HEAD
    suspend fun test(@Url url: String): Response<Unit>
}
