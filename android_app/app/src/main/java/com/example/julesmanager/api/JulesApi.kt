package com.example.julesmanager.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface JulesApi {
    @GET("v1alpha/sessions")
    suspend fun listSessions(): SessionListResponse

    @GET("v1alpha/{session_name}")
    suspend fun getSession(@Path("session_name", encoded = true) sessionName: String): Session

    @GET("v1alpha/{session_name}/activities")
    suspend fun listActivities(@Path("session_name", encoded = true) sessionName: String): ActivityListResponse

    @POST("v1alpha/{session_name}:sendMessage")
    suspend fun sendMessage(
        @Path("session_name", encoded = true) sessionName: String,
        @Body body: SendMessageRequest
    ): Any // Response format varies, ignoring for now or map to Map<String, Any>
}
