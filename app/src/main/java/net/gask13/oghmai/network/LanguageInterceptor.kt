package net.gask13.oghmai.network

import net.gask13.oghmai.preferences.PreferencesManager
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Interceptor that automatically appends the current language query parameter
 * to outgoing API requests if not already specified.
 */
class LanguageInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val originalUrl = originalRequest.url

        val currentLanguageCode = PreferencesManager.getCurrentLanguageCode()

        val newUrl = if (originalUrl.queryParameter("language") == null) {
            originalUrl.newBuilder()
                .addQueryParameter("language", currentLanguageCode)
                .build()
        } else {
            originalUrl
        }

        val newRequest = originalRequest.newBuilder()
            .url(newUrl)
            .build()

        return chain.proceed(newRequest)
    }
}
