package com.devpipe.app.data.model

import com.google.gson.annotations.SerializedName

data class UrlResponse(
    @SerializedName("url") val url: String,
    @SerializedName("updated") val updated: String? = null
)

data class IpResponse(
    @SerializedName("ip") val ip: String,
    @SerializedName("updated") val updated: String? = null
)

data class LanIpResponse(
    @SerializedName("ip") val ip: String,
    @SerializedName("type") val type: String? = null
)

data class DiscoveryStatusResponse(
    @SerializedName("status") val status: String,
    @SerializedName("url") val url: String? = null,
    @SerializedName("error") val error: String? = null
) {
    val isOnline: Boolean get() = status == STATUS_ONLINE

    companion object {
        const val STATUS_ONLINE = "online"
    }
}

data class ApiTokenResponse(
    @SerializedName("token") val token: String
)
