package com.devpipe.app.data.api

import com.devpipe.app.data.model.CreateSessionRequest
import com.devpipe.app.data.model.HealthResponse
import com.devpipe.app.data.model.Job
import com.devpipe.app.data.model.Session
import com.devpipe.app.data.model.SessionActionRequest
import com.devpipe.app.data.model.StatusResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface DevPipeApi {

    @GET("api/health")
    suspend fun getHealth(): HealthResponse

    @GET("api/status")
    suspend fun getStatus(): StatusResponse

    @GET("api/sessions")
    suspend fun getSessions(): List<Session>

    @POST("api/sessions")
    suspend fun createSession(@Body request: CreateSessionRequest): Session

    @GET("api/sessions/{id}")
    suspend fun getSession(@Path("id") id: String): Session

    @POST("api/sessions/{id}")
    suspend fun sessionAction(
        @Path("id") id: String,
        @Body request: SessionActionRequest
    ): Session

    @GET("api/jobs")
    suspend fun getJobs(): List<Job>

    @GET("api/jobs/{id}")
    suspend fun getJob(@Path("id") id: String): Job
}
