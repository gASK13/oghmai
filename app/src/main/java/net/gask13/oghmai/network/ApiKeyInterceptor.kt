package net.gask13.oghmai.network

import okhttp3.Interceptor
import okhttp3.Response

class ApiKeyInterceptor(private val apiKey: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Adding the x-api-key header
        val newRequest = originalRequest.newBuilder()
            .addHeader("x-api-key", apiKey)  // Add your API key here
            .build()

        return chain.proceed(newRequest)
    }
}
