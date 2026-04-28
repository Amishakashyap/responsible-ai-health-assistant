package com.example.foodtracker.data.network

import com.example.foodtracker.data.model.NewsResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface NewsApiService {
    @GET("v2/top-headlines")
    suspend fun getLatestNews(
        @Query("category") category: String = "health",
        @Query("language") language: String = "en",
        @Query("apiKey") apiKey: String = "abd1970078a24beba6025e82edb3d9f9"
    ): Response<NewsResponse>
}