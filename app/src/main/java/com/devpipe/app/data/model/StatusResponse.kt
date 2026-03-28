package com.devpipe.app.data.model

import com.google.gson.annotations.SerializedName

data class StatusResponse(
    @SerializedName("session_counts") val sessionCounts: Map<String, Int> = emptyMap(),
    @SerializedName("job_counts") val jobCounts: Map<String, Int> = emptyMap()
)
