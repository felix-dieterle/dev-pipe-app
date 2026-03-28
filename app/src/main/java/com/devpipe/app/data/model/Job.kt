package com.devpipe.app.data.model

import com.google.gson.annotations.SerializedName

data class Job(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("status") val status: String,
    @SerializedName("steps") val steps: List<Step> = emptyList()
)

data class Step(
    @SerializedName("name") val name: String,
    @SerializedName("status") val status: String
)
