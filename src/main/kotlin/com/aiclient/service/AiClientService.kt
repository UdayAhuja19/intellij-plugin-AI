package com.aiclient.service

import com.aiclient.model.*
import com.aiclient.settings.AiClientSettings
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json

/**
 * Service for communicating with AI APIs.
 * Supports OpenAI, Anthropic, and custom OpenAI-compatible endpoints.
 */
@Service(Service.Level.PROJECT)
class AiClientService(private val project: Project) {
    
    private val logger = Logger.getInstance(AiClientService::class.java)
    private val settings get() = service<AiClientSettings>()
    
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }
    
    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(json)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 120_000
            connectTimeoutMillis = 30_000
        }
        expectSuccess = false
    }
    
    private val conversationHistory = mutableListOf<ChatMessage>()
    
    /**
     * Sends a chat message and returns the response with retry logic for rate limits.
     */
    suspend fun sendMessage(userMessage: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val apiKey = settings.state.apiKey
            if (apiKey.isBlank()) {
                return@withContext Result.failure(Exception("API key not configured. Please set it in Settings > Tools > AI Client"))
            }
            
            conversationHistory.add(ChatMessage("user", userMessage))
            
            val request = ChatRequest(
                model = settings.state.model,
                messages = buildMessages(),
                temperature = settings.state.temperature,
                maxTokens = settings.state.maxTokens,
                stream = false
            )
            
            // Retry logic for rate limits (429)
            var lastError: Exception? = null
            val maxRetries = 3
            val baseDelayMs = 2000L
            
            for (attempt in 0 until maxRetries) {
                val response = httpClient.post("${settings.state.apiEndpoint}/chat/completions") {
                    contentType(ContentType.Application.Json)
                    header("Authorization", "Bearer $apiKey")
                    setBody(request)
                }
                
                when {
                    response.status.isSuccess() -> {
                        val chatResponse: ChatResponse = response.body()
                        val assistantMessage = chatResponse.choices.firstOrNull()?.message?.content ?: ""
                        
                        if (assistantMessage.isNotEmpty()) {
                            conversationHistory.add(ChatMessage("assistant", assistantMessage))
                        }
                        
                        return@withContext Result.success(assistantMessage)
                    }
                    response.status.value == 429 -> {
                        // Rate limited - wait and retry
                        val delayMs = baseDelayMs * (attempt + 1)
                        logger.info("Rate limited (429), retrying in ${delayMs}ms (attempt ${attempt + 1}/$maxRetries)")
                        delay(delayMs)
                        lastError = Exception("Rate limited (429). Please wait a moment or check your API quota at your provider's dashboard.")
                    }
                    else -> {
                        val errorBody = response.bodyAsText()
                        logger.warn("API error: ${response.status} - $errorBody")
                        return@withContext Result.failure(Exception("API error: ${response.status}. Check your API key and billing status."))
                    }
                }
            }
            
            Result.failure(lastError ?: Exception("Max retries exceeded"))
        } catch (e: Exception) {
            logger.error("Error sending message", e)
            Result.failure(e)
        }
    }
    
    /**
     * Sends a chat message and returns streaming responses.
     */
    fun sendMessageStreaming(userMessage: String): Flow<String> = flow {
        val apiKey = settings.state.apiKey
        if (apiKey.isBlank()) {
            emit("[Error: API key not configured. Please set it in Settings > Tools > AI Client]")
            return@flow
        }
        
        conversationHistory.add(ChatMessage("user", userMessage))
        
        val request = ChatRequest(
            model = settings.state.model,
            messages = buildMessages(),
            temperature = settings.state.temperature,
            maxTokens = settings.state.maxTokens,
            stream = true
        )
        
        try {
            val response = httpClient.preparePost("${settings.state.apiEndpoint}/chat/completions") {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $apiKey")
                setBody(request)
            }.execute { httpResponse ->
                if (!httpResponse.status.isSuccess()) {
                    emit("[Error: API returned ${httpResponse.status}]")
                    return@execute
                }
                
                val channel: ByteReadChannel = httpResponse.bodyAsChannel()
                val fullResponse = StringBuilder()
                
                while (!channel.isClosedForRead) {
                    val line = channel.readUTF8Line() ?: break
                    
                    if (line.startsWith("data: ")) {
                        val data = line.removePrefix("data: ").trim()
                        if (data == "[DONE]") break
                        
                        try {
                            val chunk = json.decodeFromString<StreamChunk>(data)
                            val content = chunk.choices.firstOrNull()?.delta?.content
                            if (content != null) {
                                fullResponse.append(content)
                                emit(content)
                            }
                        } catch (e: Exception) {
                            // Skip malformed chunks
                        }
                    }
                }
                
                if (fullResponse.isNotEmpty()) {
                    conversationHistory.add(ChatMessage("assistant", fullResponse.toString()))
                }
            }
        } catch (e: Exception) {
            logger.error("Error in streaming", e)
            emit("[Error: ${e.message}]")
        }
    }
    
    /**
     * Sends a single prompt without conversation history (for code actions).
     */
    suspend fun askAboutCode(code: String, prompt: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val apiKey = settings.state.apiKey
            if (apiKey.isBlank()) {
                return@withContext Result.failure(Exception("API key not configured"))
            }
            
            val systemMessage = ChatMessage(
                "system",
                "You are an expert programming assistant. Analyze the provided code and respond helpfully. Format code blocks with appropriate language tags."
            )
            
            val userMessage = ChatMessage(
                "user",
                "$prompt\n\n```\n$code\n```"
            )
            
            val request = ChatRequest(
                model = settings.state.model,
                messages = listOf(systemMessage, userMessage),
                temperature = 0.3,
                maxTokens = settings.state.maxTokens,
                stream = false
            )
            
            val response = httpClient.post("${settings.state.apiEndpoint}/chat/completions") {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $apiKey")
                setBody(request)
            }
            
            if (!response.status.isSuccess()) {
                return@withContext Result.failure(Exception("API error: ${response.status}"))
            }
            
            val chatResponse: ChatResponse = response.body()
            val assistantMessage = chatResponse.choices.firstOrNull()?.message?.content ?: ""
            
            Result.success(assistantMessage)
        } catch (e: Exception) {
            logger.error("Error asking about code", e)
            Result.failure(e)
        }
    }
    
    /**
     * Clears the conversation history.
     */
    fun clearHistory() {
        conversationHistory.clear()
    }
    
    private fun buildMessages(): List<ChatMessage> {
        val systemMessage = ChatMessage(
            "system",
            settings.state.systemPrompt.ifBlank {
                "You are a helpful AI programming assistant. You help developers understand, write, and improve code. Be concise but thorough."
            }
        )
        return listOf(systemMessage) + conversationHistory.takeLast(20) // Keep last 20 messages
    }
    
    companion object {
        fun getInstance(project: Project): AiClientService = project.service()
    }
}
