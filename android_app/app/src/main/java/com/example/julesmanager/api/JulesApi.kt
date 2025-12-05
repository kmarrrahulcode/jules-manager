package com.example.julesmanager.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface JulesApi {
    @GET("v1alpha/sessions")
    suspend fun listSessions(): SessionListResponse

    @GET("v1alpha/sources")
    suspend fun listSources(): SourceListResponse

    @POST("v1alpha/sessions")
    suspend fun createSession(@Body body: CreateSessionRequest): Session

    @GET("v1alpha/{session_name}")
    suspend fun getSession(@Path("session_name", encoded = true) sessionName: String): Session

    @GET("v1alpha/{session_name}/activities")
    suspend fun listActivities(@Path("session_name", encoded = true) sessionName: String): ActivityListResponse

    @POST("v1alpha/{session_name}:sendMessage")
    suspend fun sendMessage(
        @Path("session_name", encoded = true) sessionName: String,
        @Body body: SendMessageRequest
    ): Any

    @POST("v1alpha/{session_name}:approvePlan")
    suspend fun approvePlan(
        @Path("session_name", encoded = true) sessionName: String,
        @Body body: Map<String, String> = emptyMap()
    ): Any
}
