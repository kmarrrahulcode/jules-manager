package com.example.julesmanager.api

data class SessionListResponse(
    val sessions: List<Session>?
)

data class Session(
    val name: String,
    val title: String?,
    val state: String?,
    val sourceContext: SourceContext?
    // Add createTime/updateTime if available in JSON for sorting
)

data class SourceListResponse(
    val sources: List<Source>?
)

data class Source(
    val name: String,
    val githubRepo: GitHubRepo?
)

data class GitHubRepo(
    val owner: String,
    val repo: String,
    val defaultBranch: BranchInfo?
)

data class BranchInfo(
    val displayName: String?
)

data class ActivityListResponse(
    val activities: List<ActivityItem>?
)

data class ActivityItem(
    val name: String,
    val createTime: String?,
    val userMessaged: UserMessaged?,
    val agentMessaged: AgentMessaged?,
    val planGenerated: PlanGenerated?
)

data class PlanGenerated(
    val plan: Plan?
)

data class Plan(
    val steps: List<PlanStep>?
)

data class PlanStep(
    val title: String?,
    val description: String?
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
    val source: String,
    val branch: String? = null // Optimistic addition based on user request
)

data class SendMessageRequest(
    val message: String
)
