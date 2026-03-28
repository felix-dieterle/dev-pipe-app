package com.devpipe.app.data.model

import com.google.gson.annotations.SerializedName

data class ComponentStatus(
    @SerializedName("name") val name: String,
    @SerializedName("status") val status: String
)

data class HealthResponse(
    @SerializedName("components") val components: List<ComponentStatus> = emptyList()
)
