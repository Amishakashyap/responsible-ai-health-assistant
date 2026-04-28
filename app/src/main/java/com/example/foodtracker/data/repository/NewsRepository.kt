package com.example.foodtracker.data.repository

import com.example.foodtracker.data.model.NewsResponse
import com.example.foodtracker.data.network.NewsApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.Response
import java.util.concurrent.TimeUnit

class NewsRepository {
    private val newsApiService: NewsApiService

    init {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Accept", "application/json")
                    .build()
                chain.proceed(request)
            }
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://newsapi.org/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        newsApiService = retrofit.create(NewsApiService::class.java)
    }

    suspend fun getLatestNews(): Result<NewsResponse> = withContext(Dispatchers.IO) {
        try {
            val response = newsApiService.getLatestNews()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = "HTTP ${response.code()}: ${response.message()}\n$errorBody"
                android.util.Log.e("NewsRepository", errorMessage)
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            val errorMessage = "Network error: ${e.javaClass.simpleName} - ${e.localizedMessage}"
            android.util.Log.e("NewsRepository", errorMessage, e)
            Result.failure(Exception(errorMessage))
        }
    }
}