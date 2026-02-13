package com.example.droidscrape.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

interface ApiService {
    @POST
    suspend fun ingest(
        @Url url: String,
        @Body payload: IngestPayload
    ): Response<IngestResponse>
}
