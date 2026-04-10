package com.devpipe.app.data.model

import com.google.gson.annotations.SerializedName

data class Session(
    @SerializedName(value = "session_id", alternate = ["id"]) val sessionId: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("repo") val repo: Repo?,
    @SerializedName("pr_url") val prUrl: String?,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("approved_at") val approvedAt: String?
)

data class Repo(
    @SerializedName("owner") val owner: String,
    @SerializedName("name") val name: String
)

data class CreateSessionRequest(
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String,
    @SerializedName("repo_owner") val repoOwner: String,
    @SerializedName("repo_name") val repoName: String
)

data class SessionActionRequest(
    @SerializedName("action") val action: String,
    @SerializedName("reason") val reason: String? = null
)
