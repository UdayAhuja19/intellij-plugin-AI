package com.aiclient.service;

import com.aiclient.model.*;
import com.aiclient.settings.AiClientSettings;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import okhttp3.*;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Service for communicating with AI APIs.
 * 
 * This service handles all HTTP communication with AI providers including:
 * - OpenAI (GPT-4, GPT-3.5-turbo, etc.)
 * - Anthropic (Claude models)
 * - Custom OpenAI-compatible endpoints (Ollama, LM Studio, etc.)
 * 
 * Features:
 * - Asynchronous message sending using CompletableFuture
 * - Streaming response support via Server-Sent Events (SSE)
 * - Automatic retry logic for rate limits (429 errors)
 * - Conversation history management
 * 
 * Usage:
 * <pre>
 * AiClientService service = AiClientService.getInstance(project);
 * CompletableFuture<String> future = service.sendMessage("Hello!");
 * future.thenAccept(response -> System.out.println(response));
 * </pre>
 */
@Service(Service.Level.PROJECT)
public final class AiClientService {
    
    // ========================================================================
    // Constants and Fields
    // ========================================================================
    
    private static final Logger LOG = Logger.getInstance(AiClientService.class);
    
    /** Maximum number of retry attempts for rate-limited requests */
    private static final int MAX_RETRIES = 3;
    
    /** Base delay in milliseconds for retry backoff */
    private static final long BASE_DELAY_MS = 2000L;
    
    /** Maximum number of messages to keep in conversation history */
    private static final int MAX_HISTORY_SIZE = 20;
    
    /** The project this service is associated with */
    private final Project project;
    
    /** HTTP client with configured timeouts */
    private final OkHttpClient httpClient;
    
    /** Gson instance for JSON serialization/deserialization */
    private final Gson gson;
    
    /** Conversation history for multi-turn chat */
    private final List<ChatMessage> conversationHistory = new ArrayList<>();
    
    // ========================================================================
    // Constructor
    // ========================================================================
    
    /**
     * Creates a new AI client service for the given project.
     * 
     * @param project The IntelliJ project instance
     */
    public AiClientService(Project project) {
        this.project = project;
        
        // Configure HTTP client with appropriate timeouts
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();
        
        // Configure Gson for lenient parsing
        this.gson = new GsonBuilder()
            .setLenient()
            .create();
    }
    
    // ========================================================================
    // Public API
    // ========================================================================
    
    /**
     * Sends a chat message and returns the response asynchronously.
     * Includes automatic retry logic for rate-limited requests.
     * 
     * @param userMessage The user's message to send
     * @return CompletableFuture containing the AI's response or error message
     */
    public CompletableFuture<String> sendMessage(String userMessage) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return sendMessageSync(userMessage);
            } catch (Exception e) {
                LOG.error("Error sending message", e);
                return "[Error: " + e.getMessage() + "]";
            }
        });
    }
    
    /**
     * Sends a chat message with streaming response.
     * The callback is invoked for each chunk of the response.
     * 
     * @param userMessage The user's message to send
     * @param onChunk Callback invoked for each response chunk
     * @param onComplete Callback invoked when streaming completes
     * @param onError Callback invoked on error
     */
    public void sendMessageStreaming(
            String userMessage,
            Consumer<String> onChunk,
            Runnable onComplete,
            Consumer<String> onError) {
        
        // Run on background thread to avoid EDT access to PasswordSafe
        CompletableFuture.runAsync(() -> {
            try {
                AiClientSettings.State settings = getSettings().getState();
                String apiKey = settings.getApiKey();
                
                if (apiKey == null || apiKey.isBlank()) {
                    onError.accept("API key not configured. Please set it in Settings > Tools > AI Client");
                    return;
                }
                
                // Add user message to history
                conversationHistory.add(new ChatMessage("user", userMessage));
                
                // Build request
                ChatRequest chatRequest = new ChatRequest(
                    settings.model,
                    buildMessages(),
                    settings.temperature,
                    settings.maxTokens,
                    true  // streaming = true
                );
                
                String requestBody = gson.toJson(chatRequest);
                String url = settings.apiEndpoint + "/chat/completions";
                
                Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Accept", "text/event-stream")
                    .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
                    .build();
                
                // Use SSE for streaming
                StringBuilder fullResponse = new StringBuilder();
                
                EventSourceListener listener = new EventSourceListener() {
                    @Override
                    public void onEvent(@NotNull EventSource eventSource, @Nullable String id, 
                                       @Nullable String type, @NotNull String data) {
                        if ("[DONE]".equals(data)) {
                            // Streaming complete
                            if (fullResponse.length() > 0) {
                                conversationHistory.add(new ChatMessage("assistant", fullResponse.toString()));
                            }
                            onComplete.run();
                            return;
                        }
                        
                        try {
                            StreamChunk chunk = gson.fromJson(data, StreamChunk.class);
                            if (chunk.getChoices() != null && !chunk.getChoices().isEmpty()) {
                                Delta delta = chunk.getChoices().get(0).getDelta();
                                if (delta != null && delta.getContent() != null) {
                                    fullResponse.append(delta.getContent());
                                    onChunk.accept(delta.getContent());
                                }
                            }
                        } catch (Exception e) {
                            // Skip malformed chunks
                            LOG.debug("Failed to parse streaming chunk: " + data);
                        }
                    }
                    
                    @Override
                    public void onFailure(@NotNull EventSource eventSource, @Nullable Throwable t, 
                                         @Nullable Response response) {
                        String errorMsg = "Streaming failed";
                        if (t != null) {
                            errorMsg = t.getMessage();
                        } else if (response != null) {
                            errorMsg = "HTTP " + response.code();
                        }
                        onError.accept(errorMsg);
                    }
                };
                
                // Create and start event source
                EventSources.createFactory(httpClient).newEventSource(request, listener);
            } catch (Exception e) {
                onError.accept("Error: " + e.getMessage());
            }
        });
    }
    
    /**
     * Sends a single prompt about code without affecting conversation history.
     * Useful for one-off code analysis actions.
     * 
     * @param code The code to analyze
     * @param prompt The question or instruction about the code
     * @return CompletableFuture containing the AI's response
     */
    public CompletableFuture<String> askAboutCode(String code, String prompt) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                AiClientSettings.State settings = getSettings().getState();
                String apiKey = settings.getApiKey();
                
                if (apiKey == null || apiKey.isBlank()) {
                    return "[Error: API key not configured]";
                }
                
                // Build request with system prompt for code analysis
                List<ChatMessage> messages = new ArrayList<>();
                messages.add(new ChatMessage("system", 
                    "You are an expert programming assistant. Analyze the provided code " +
                    "and respond helpfully. Format code blocks with appropriate language tags."));
                messages.add(new ChatMessage("user", 
                    prompt + "\n\n```\n" + code + "\n```"));
                
                ChatRequest chatRequest = new ChatRequest(
                    settings.model,
                    messages,
                    0.3,  // Lower temperature for code analysis
                    settings.maxTokens,
                    false
                );
                
                return executeRequest(chatRequest, apiKey);
            } catch (Exception e) {
                LOG.error("Error asking about code", e);
                return "[Error: " + e.getMessage() + "]";
            }
        });
    }
    
    /**
     * Clears the conversation history.
     * Call this to start a fresh conversation.
     */
    public void clearHistory() {
        conversationHistory.clear();
    }
    
    // ========================================================================
    // Private Helper Methods
    // ========================================================================
    
    /**
     * Sends a message synchronously with retry logic.
     */
    private String sendMessageSync(String userMessage) throws Exception {
        AiClientSettings.State settings = getSettings().getState();
        String apiKey = settings.getApiKey();
        
        if (apiKey == null || apiKey.isBlank()) {
            throw new Exception("API key not configured. Please set it in Settings > Tools > AI Client");
        }
        
        // Add user message to history
        conversationHistory.add(new ChatMessage("user", userMessage));
        
        // Build request
        ChatRequest chatRequest = new ChatRequest(
            settings.model,
            buildMessages(),
            settings.temperature,
            settings.maxTokens,
            false
        );
        
        // Execute with retry logic
        String response = executeWithRetry(chatRequest, apiKey);
        
        // Add assistant response to history
        if (response != null && !response.startsWith("[Error:")) {
            conversationHistory.add(new ChatMessage("assistant", response));
        }
        
        return response;
    }
    
    /**
     * Executes a request with automatic retry for rate limits.
     */
    private String executeWithRetry(ChatRequest chatRequest, String apiKey) throws Exception {
        Exception lastError = null;
        
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                String response = executeRequest(chatRequest, apiKey);
                return response;
            } catch (RateLimitException e) {
                // Rate limited - wait and retry
                long delayMs = BASE_DELAY_MS * (attempt + 1);
                LOG.info("Rate limited (429), retrying in " + delayMs + "ms (attempt " + 
                        (attempt + 1) + "/" + MAX_RETRIES + ")");
                Thread.sleep(delayMs);
                lastError = e;
            }
        }
        
        throw lastError != null ? lastError : new Exception("Max retries exceeded");
    }
    
    /**
     * Executes a single API request.
     */
    private String executeRequest(ChatRequest chatRequest, String apiKey) throws Exception {
        AiClientSettings.State settings = getSettings().getState();
        String url = settings.apiEndpoint + "/chat/completions";
        String requestBody = gson.toJson(chatRequest);
        
        Request request = new Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer " + apiKey)
            .addHeader("Content-Type", "application/json")
            .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
            .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (response.code() == 429) {
                throw new RateLimitException("Rate limited (429)");
            }
            
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "";
                throw new Exception("API error: " + response.code() + ". " + errorBody);
            }
            
            String responseBody = response.body() != null ? response.body().string() : "";
            ChatResponse chatResponse = gson.fromJson(responseBody, ChatResponse.class);
            
            if (chatResponse.getChoices() != null && !chatResponse.getChoices().isEmpty()) {
                ChatMessage message = chatResponse.getChoices().get(0).getMessage();
                if (message != null) {
                    return message.getContent();
                }
            }
            
            return "";
        }
    }
    
    /**
     * Builds the messages list including system prompt and conversation history.
     */
    private List<ChatMessage> buildMessages() {
        List<ChatMessage> messages = new ArrayList<>();
        
        // Add system message
        AiClientSettings.State settings = getSettings().getState();
        String systemPrompt = settings.systemPrompt;
        if (systemPrompt == null || systemPrompt.isBlank()) {
            systemPrompt = "You are a helpful AI programming assistant. You help developers " +
                          "understand, write, and improve code. Be concise but thorough.";
        }
        messages.add(new ChatMessage("system", systemPrompt));
        
        // Add recent conversation history (limit to prevent token overflow)
        int startIndex = Math.max(0, conversationHistory.size() - MAX_HISTORY_SIZE);
        messages.addAll(conversationHistory.subList(startIndex, conversationHistory.size()));
        
        return messages;
    }
    
    /**
     * Gets the settings instance.
     */
    private AiClientSettings getSettings() {
        return AiClientSettings.getInstance();
    }
    
    // ========================================================================
    // Service Instance Access
    // ========================================================================
    
    /**
     * Gets the AI client service instance for the given project.
     * 
     * @param project The IntelliJ project
     * @return The service instance
     */
    public static AiClientService getInstance(Project project) {
        return project.getService(AiClientService.class);
    }
    
    // ========================================================================
    // Exception Classes
    // ========================================================================
    
    /**
     * Exception thrown when API returns 429 (rate limited).
     */
    private static class RateLimitException extends Exception {
        public RateLimitException(String message) {
            super(message);
        }
    }
}
