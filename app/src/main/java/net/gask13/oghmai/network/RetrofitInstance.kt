package net.gask13.oghmai.network

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import java.io.IOException
import net.gask13.oghmai.BuildConfig
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {

    private const val BASE_URL = BuildConfig.API_ENDPOINT

    // Custom interceptor for error handling
    private val errorHandlingInterceptor = Interceptor { chain ->
        try {
            val response = chain.proceed(chain.request())
            if (!response.isSuccessful) {
                // Log or handle HTTP errors (e.g., 4xx, 5xx)
                throw IOException("HTTP error: ${response.code} on call to ${chain.request().method} - ${chain.request().url}")
            }
            response
        } catch (e: Exception) {
            // Log or handle unexpected errors
            throw IOException("Unexpected error: ${e.message} on call to ${chain.request().method} - ${chain.request().url}", e)
        }
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(ApiKeyInterceptor(BuildConfig.API_KEY))
        .addInterceptor(errorHandlingInterceptor) // Add the error handling interceptor
        .build()

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
