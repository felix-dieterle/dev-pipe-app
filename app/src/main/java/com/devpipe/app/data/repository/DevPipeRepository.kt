package com.devpipe.app.data.repository

import com.devpipe.app.data.api.DevPipeApi
import com.devpipe.app.data.api.PhpDiscoveryApi
import com.devpipe.app.data.logging.LogManager
import com.devpipe.app.data.model.ApiTokenResponse
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
    private val phpApi: PhpDiscoveryApi,
    private val logManager: LogManager
) {

    suspend fun getHealth(): Result<HealthResponse> =
        runCatching { HealthResponse(api.getHealth()) }.also { result ->
            result.fold(
                onSuccess = { logManager.info("API", "getHealth → OK (${it.components.size} components)") },
                onFailure = { logManager.error("API", "getHealth → ERROR: ${it.message}") }
            )
        }

    suspend fun getStatus(): Result<StatusResponse> =
        runCatching { api.getStatus() }.also { result ->
            result.fold(
                onSuccess = { logManager.info("API", "getStatus → OK") },
                onFailure = { logManager.error("API", "getStatus → ERROR: ${it.message}") }
            )
        }

    suspend fun getSessions(): Result<List<Session>> =
        runCatching { api.getSessions() }.also { result ->
            result.fold(
                onSuccess = { logManager.info("API", "getSessions → OK (${it.size} sessions)") },
                onFailure = { logManager.error("API", "getSessions → ERROR: ${it.message}") }
            )
        }

    suspend fun createSession(request: CreateSessionRequest): Result<Session> =
        runCatching { api.createSession(request) }.also { result ->
            result.fold(
                onSuccess = { logManager.info("API", "createSession → OK (id=${it.sessionId})") },
                onFailure = { logManager.error("API", "createSession → ERROR: ${it.message}") }
            )
        }

    suspend fun getSession(id: String): Result<Session> =
        runCatching { api.getSession(id) }.also { result ->
            result.fold(
                onSuccess = { logManager.info("API", "getSession($id) → OK (status=${it.status})") },
                onFailure = { logManager.error("API", "getSession($id) → ERROR: ${it.message}") }
            )
        }

    suspend fun sessionAction(id: String, action: String, reason: String? = null): Result<Session> =
        runCatching { api.sessionAction(id, SessionActionRequest(action, reason)) }.also { result ->
            result.fold(
                onSuccess = { logManager.info("API", "sessionAction($id, $action) → OK (status=${it.status})") },
                onFailure = { logManager.error("API", "sessionAction($id, $action) → ERROR: ${it.message}") }
            )
        }

    suspend fun getJobs(): Result<List<Job>> =
        runCatching { api.getJobs() }.also { result ->
            result.fold(
                onSuccess = { logManager.info("API", "getJobs → OK (${it.size} jobs)") },
                onFailure = { logManager.error("API", "getJobs → ERROR: ${it.message}") }
            )
        }

    suspend fun getJob(id: String): Result<Job> =
        runCatching { api.getJob(id) }.also { result ->
            result.fold(
                onSuccess = { logManager.info("API", "getJob($id) → OK (status=${it.status})") },
                onFailure = { logManager.error("API", "getJob($id) → ERROR: ${it.message}") }
            )
        }

    suspend fun discoverUrl(phpUrl: String, token: String): Result<UrlResponse> =
        runCatching { phpApi.getUrl(phpUrl, token) }.also { result ->
            result.fold(
                onSuccess = { logManager.info("API", "discoverUrl → OK") },
                onFailure = { logManager.error("API", "discoverUrl → ERROR: ${it.message}") }
            )
        }

    suspend fun discoverIp(phpUrl: String, token: String): Result<IpResponse> =
        runCatching { phpApi.getIp(phpUrl, token) }.also { result ->
            result.fold(
                onSuccess = { logManager.info("API", "discoverIp → OK") },
                onFailure = { logManager.error("API", "discoverIp → ERROR: ${it.message}") }
            )
        }

    suspend fun discoverLanIp(phpUrl: String, token: String): Result<LanIpResponse> =
        runCatching { phpApi.getLanIp(phpUrl, token) }.also { result ->
            result.fold(
                onSuccess = { logManager.info("API", "discoverLanIp → OK") },
                onFailure = { logManager.error("API", "discoverLanIp → ERROR: ${it.message}") }
            )
        }

    suspend fun getDiscoveryStatus(phpUrl: String, token: String): Result<DiscoveryStatusResponse> =
        runCatching { phpApi.getDiscoveryStatus(phpUrl, token) }.also { result ->
            result.fold(
                onSuccess = { logManager.info("API", "getDiscoveryStatus → OK (status=${it.status})") },
                onFailure = { logManager.error("API", "getDiscoveryStatus → ERROR: ${it.message}") }
            )
        }

    suspend fun fetchApiToken(phpUrl: String, discoveryToken: String): Result<ApiTokenResponse> =
        runCatching { phpApi.getApiToken(phpUrl, discoveryToken) }.also { result ->
            result.fold(
                onSuccess = { logManager.info("API", "fetchApiToken → OK") },
                onFailure = { logManager.error("API", "fetchApiToken → ERROR: ${it.message}") }
            )
        }
}
