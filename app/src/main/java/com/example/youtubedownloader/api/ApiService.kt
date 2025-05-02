package com.example.youtubedownloader.api

import com.example.youtubedownloader.model.VideoMetadata
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Streaming

interface ApiService {
    @POST("metadata")
    suspend fun getVideoMetadata(@Body request: MetadataRequest): Response<MetadataResponse>

    @POST("download")
    @Streaming
    suspend fun downloadVideo(@Body request: DownloadRequest): Response<ResponseBody>
}

data class MetadataRequest(
    val url: String
)

data class MetadataResponse(
    val success: Boolean,
    val data: VideoMetadata?,
    val error: String?
)

data class DownloadRequest(
    val url: String,
    val formatId: String
)

object ApiClient {
    private const val BASE_URL = "http://your-backend-url:5000/"  // Replace with your actual backend URL
    
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    val service: ApiService = retrofit.create(ApiService::class.java)
}

sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val message: String) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

suspend fun <T> safeApiCall(call: suspend () -> Response<T>): Result<T> {
    return try {
        val response = call()
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null) {
                Result.Success(body)
            } else {
                Result.Error("Response body is empty")
            }
        } else {
            Result.Error("Error: ${response.code()} ${response.message()}")
        }
    } catch (e: Exception) {
        Result.Error(e.message ?: "Unknown error occurred")
    }
}
