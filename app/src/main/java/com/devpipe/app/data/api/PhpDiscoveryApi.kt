package com.devpipe.app.data.api

import com.devpipe.app.data.model.UrlResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface PhpDiscoveryApi {

    @GET("api.php")
    suspend fun getUrl(
        @Query("token") token: String,
        @Query("action") action: String = "get_url"
    ): UrlResponse
}
