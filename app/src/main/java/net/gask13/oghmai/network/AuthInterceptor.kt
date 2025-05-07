package net.gask13.oghmai.network

import android.util.Log
import net.gask13.oghmai.auth.AuthManager
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Interceptor that adds the authentication token to requests
 */
class AuthInterceptor() : Interceptor {
    
    private val tag = "AuthInterceptor"
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        // Get the auth token
        val token = AuthManager.getAuthToken()
        
        // If we have a token, add it to the request
        return if (token != null) {
            Log.d(tag, "Adding auth token to request")
            
            // Create a new request with the token in the Authorization header
            val newRequest = originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
                
            chain.proceed(newRequest)
        } else {
            Log.d(tag, "No auth token available, proceeding without authentication")
            chain.proceed(originalRequest)
        }
    }
}