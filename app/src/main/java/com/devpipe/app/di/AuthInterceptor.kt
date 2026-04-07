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
        val response = try {
            chain.proceed(request)
        } catch (e: java.io.IOException) {
            logManager.error(
                "AuthInterceptor",
                "Connection error for ${safeUrl(request.url)} (token: ${maskedToken(token)}): ${e.message}"
            )
            throw e
        }
        if (response.code == 401) {
            logManager.error(
                "AuthInterceptor",
                "401 Unauthorized for ${safeUrl(request.url)} (token: ${maskedToken(token)}) – check that the API token is correct"
            )
        } else if (response.code >= 400) {
            logManager.warn(
                "AuthInterceptor",
                "HTTP ${response.code} received for ${safeUrl(request.url)} (token: ${maskedToken(token)})"
            )
        }
        return response
    }

    /** Returns only scheme, host, port and path – drops query parameters to avoid leaking sensitive data. */
    private fun safeUrl(url: okhttp3.HttpUrl): String =
        "${url.scheme}://${url.host}:${url.port}${url.encodedPath}"

    /**
     * Returns the token with only the first and last character visible and the
     * remaining characters replaced by asterisks, e.g. "a***z".
     * Returns "<none>" if the token is null or blank.
     */
    private fun maskedToken(token: String?): String {
        if (token.isNullOrBlank()) return "<none>"
        if (token.length == 1) return token
        if (token.length == 2) return "${token.first()}*"
        return "${token.first()}${"*".repeat(token.length - 2)}${token.last()}"
    }
}
