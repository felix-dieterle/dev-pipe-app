package com.devpipe.app.di

import com.devpipe.app.data.storage.PreferencesManager
import android.util.Log
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Rewrites the base URL of every request using the value stored in
 * [PreferencesManager]. Reading from a DataStore [Flow] with [runBlocking] is
 * acceptable here because OkHttp dispatch threads are never the main thread.
 */
@Singleton
class DynamicUrlInterceptor @Inject constructor(
    private val preferencesManager: PreferencesManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val baseUrl = runBlocking { preferencesManager.backendUrl.first() }
        val originalRequest = chain.request()
        return try {
            val newBaseUrl = baseUrl.toHttpUrl()
            val newUrl = originalRequest.url.newBuilder()
                .scheme(newBaseUrl.scheme)
                .host(newBaseUrl.host)
                .port(newBaseUrl.port)
                .build()
            val newRequest = originalRequest.newBuilder().url(newUrl).build()
            chain.proceed(newRequest)
        } catch (e: Exception) {
            Log.e("DynamicUrlInterceptor", "Invalid base URL '$baseUrl', using original", e)
            chain.proceed(originalRequest)
        }
    }
}
