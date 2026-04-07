package com.devpipe.app.di

import com.devpipe.app.data.logging.LogManager
import com.devpipe.app.data.storage.TokenManager
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager,
    private val logManager: LogManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenManager.getToken()
        val request = if (!token.isNullOrBlank()) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            val safeUrl = safeUrl(chain.request().url)
            logManager.warn("AuthInterceptor", "No API token configured – request sent without Authorization header: $safeUrl")
            chain.request()
        }
        val response = chain.proceed(request)
        if (response.code == 401) {
            logManager.error(
                "AuthInterceptor",
                "401 Unauthorized for ${safeUrl(request.url)} – check that the API token is correct"
            )
        } else if (response.code >= 400) {
            logManager.warn(
                "AuthInterceptor",
                "HTTP ${response.code} received for ${safeUrl(request.url)}"
            )
        }
        return response
    }

    /** Returns only scheme, host, port and path – drops query parameters to avoid leaking sensitive data. */
    private fun safeUrl(url: okhttp3.HttpUrl): String =
        "${url.scheme}://${url.host}:${url.port}${url.encodedPath}"
}
