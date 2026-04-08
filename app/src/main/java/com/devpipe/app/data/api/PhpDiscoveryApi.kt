package com.devpipe.app.data.api

import com.devpipe.app.data.model.DiscoveryStatusResponse
import com.devpipe.app.data.model.IpResponse
import com.devpipe.app.data.model.UrlResponse
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

interface PhpDiscoveryApi {

    /**
     * Fetches the current Dev-Pipe backend URL from the PHP discovery endpoint.
     *
     * @param url   Full URL to the api.php endpoint (e.g. "https://example.com/api.php").
     *              Using [@Url] lets Retrofit override the base URL at call time so that the
     *              user-configured discovery URL is always used.
     * @param token The API token that must match DEV_PIPE_TOKEN on the PHP side.
     * @param action The action to perform (default: "get_url").
     */
    @GET
    suspend fun getUrl(
        @Url url: String,
        @Query("token") token: String,
        @Query("action") action: String = "get_url"
    ): UrlResponse

    /** Fetches the cached public IP address together with its last-updated timestamp. */
    @GET
    suspend fun getIp(
        @Url url: String,
        @Query("token") token: String,
        @Query("action") action: String = "get_ip"
    ): IpResponse

    /** Checks whether the Dev-Pipe server is reachable and returns its status. */
    @GET
    suspend fun getDiscoveryStatus(
        @Url url: String,
        @Query("token") token: String,
        @Query("action") action: String = "status"
    ): DiscoveryStatusResponse
}
