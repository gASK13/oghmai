package net.gask13.oghmai.network

import android.util.Log
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import java.io.IOException
import net.gask13.oghmai.BuildConfig
import net.gask13.oghmai.auth.AuthManager
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {

    private const val BASE_URL = BuildConfig.API_ENDPOINT
    private const val TAG = "RetrofitInstance"

    // Reference to AuthManager, will be set during initialization
    private lateinit var authManager: AuthManager

    // Initialize with AuthManager
    fun initialize(authManager: AuthManager) {
        this.authManager = authManager
        Log.d(TAG, "RetrofitInstance initialized with AuthManager")
    }

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

    // Create OkHttpClient with interceptors
    private val client by lazy {
        if (!::authManager.isInitialized) {
            Log.e(TAG, "AuthManager not initialized. API calls will not be authenticated.")
        }

        OkHttpClient.Builder().apply {
            // Add auth interceptor if AuthManager is initialized
            if (::authManager.isInitialized) {
                addInterceptor(AuthInterceptor(authManager))
                Log.d(TAG, "Added AuthInterceptor to OkHttpClient")
            }

            // Add error handling interceptor
            addInterceptor(errorHandlingInterceptor)
        }.build()
    }

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
