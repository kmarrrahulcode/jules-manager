package com.example.julesmanager.api

data class SessionListResponse(
    val sessions: List<Session>?
)

data class Session(
    val name: String,
    val title: String?,
    val state: String?
)

data class ActivityListResponse(
    val activities: List<ActivityItem>?
)

data class ActivityItem(
    val name: String,
    val createTime: String?,
    val userMessaged: UserMessaged?,
    val agentMessaged: AgentMessaged?
    // Add other types if needed (planGenerated, etc.)
)

data class UserMessaged(
    val userMessage: String
)

data class AgentMessaged(
    val agentMessage: String
)

data class CreateSessionRequest(
    val prompt: String,
    val sourceContext: SourceContext
)

data class SourceContext(
    val source: String
)

data class SendMessageRequest(
    val message: String
)
