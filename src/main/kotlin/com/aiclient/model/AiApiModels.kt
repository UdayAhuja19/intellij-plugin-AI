package com.aiclient.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a single message in a chat conversation.
 */
@Serializable
data class ChatMessage(
    val role: String,
    val content: String
)

/**
 * Request body for the chat completions API.
 */
@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double = 0.7,
    @SerialName("max_tokens")
    val maxTokens: Int = 2048,
    val stream: Boolean = false
)

/**
 * Response from the chat completions API.
 */
@Serializable
data class ChatResponse(
    val id: String = "",
    val `object`: String = "",
    val created: Long = 0,
    val model: String = "",
    val choices: List<Choice> = emptyList(),
    val usage: Usage? = null
)

@Serializable
data class Choice(
    val index: Int = 0,
    val message: ChatMessage? = null,
    val delta: Delta? = null,
    @SerialName("finish_reason")
    val finishReason: String? = null
)

@Serializable
data class Delta(
    val role: String? = null,
    val content: String? = null
)

@Serializable
data class Usage(
    @SerialName("prompt_tokens")
    val promptTokens: Int = 0,
    @SerialName("completion_tokens")
    val completionTokens: Int = 0,
    @SerialName("total_tokens")
    val totalTokens: Int = 0
)

/**
 * Streaming chunk from the chat completions API.
 */
@Serializable
data class StreamChunk(
    val id: String = "",
    val `object`: String = "",
    val created: Long = 0,
    val model: String = "",
    val choices: List<Choice> = emptyList()
)

/**
 * Represents an AI provider configuration.
 */
enum class AiProvider(val displayName: String, val defaultEndpoint: String, val defaultModel: String) {
    OPENAI("OpenAI", "https://api.openai.com/v1", "gpt-4o"),
    ANTHROPIC("Anthropic", "https://api.anthropic.com/v1", "claude-3-opus-20240229"),
    CUSTOM("Custom", "http://localhost:11434/v1", "llama2")
}

/**
 * Error response from the API.
 */
@Serializable
data class ApiError(
    val error: ErrorDetail? = null
)

@Serializable
data class ErrorDetail(
    val message: String = "",
    val type: String = "",
    val code: String? = null
)
