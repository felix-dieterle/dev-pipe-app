package com.devpipe.app.data.repository

import com.devpipe.app.data.api.DevPipeApi
import com.devpipe.app.data.api.PhpDiscoveryApi
import com.devpipe.app.data.model.CreateSessionRequest
import com.devpipe.app.data.model.DiscoveryStatusResponse
import com.devpipe.app.data.model.HealthResponse
import com.devpipe.app.data.model.IpResponse
import com.devpipe.app.data.model.Job
import com.devpipe.app.data.model.LanIpResponse
import com.devpipe.app.data.model.Session
import com.devpipe.app.data.model.SessionActionRequest
import com.devpipe.app.data.model.StatusResponse
import com.devpipe.app.data.model.UrlResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DevPipeRepository @Inject constructor(
    private val api: DevPipeApi,
    private val phpApi: PhpDiscoveryApi
) {

    suspend fun getHealth(): Result<HealthResponse> = runCatching { api.getHealth() }

    suspend fun getStatus(): Result<StatusResponse> = runCatching { api.getStatus() }

    suspend fun getSessions(): Result<List<Session>> = runCatching { api.getSessions() }

    suspend fun createSession(request: CreateSessionRequest): Result<Session> =
        runCatching { api.createSession(request) }

    suspend fun getSession(id: String): Result<Session> = runCatching { api.getSession(id) }

    suspend fun sessionAction(id: String, action: String, reason: String? = null): Result<Session> =
        runCatching { api.sessionAction(id, SessionActionRequest(action, reason)) }

    suspend fun getJobs(): Result<List<Job>> = runCatching { api.getJobs() }

    suspend fun getJob(id: String): Result<Job> = runCatching { api.getJob(id) }

    suspend fun discoverUrl(phpUrl: String, token: String): Result<UrlResponse> =
        runCatching { phpApi.getUrl(phpUrl, token) }

    suspend fun discoverIp(phpUrl: String, token: String): Result<IpResponse> =
        runCatching { phpApi.getIp(phpUrl, token) }

    suspend fun discoverLanIp(phpUrl: String, token: String): Result<LanIpResponse> =
        runCatching { phpApi.getLanIp(phpUrl, token) }

    suspend fun getDiscoveryStatus(phpUrl: String, token: String): Result<DiscoveryStatusResponse> =
        runCatching { phpApi.getDiscoveryStatus(phpUrl, token) }
}
