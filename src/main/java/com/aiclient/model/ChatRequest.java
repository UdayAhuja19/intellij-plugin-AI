package com.aiclient.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Request body for the chat completions API.
 * Contains the model name, messages, and generation parameters.
 */
public class ChatRequest {
    /** The model identifier (e.g., "gpt-4o", "claude-3-opus") */
    private String model;
    
    /** List of messages in the conversation */
    private List<ChatMessage> messages;
    
    /** Temperature controls randomness (0.0 = deterministic, 2.0 = very random) */
    private double temperature;
    
    /** Maximum number of tokens to generate */
    @SerializedName("max_tokens")
    private int maxTokens;
    
    /** Whether to stream the response */
    private boolean stream;
    
    /**
     * Creates a new ChatRequest with all parameters.
     */
    public ChatRequest(String model, List<ChatMessage> messages, double temperature, 
                       int maxTokens, boolean stream) {
        this.model = model;
        this.messages = messages;
        this.temperature = temperature;
        this.maxTokens = maxTokens;
        this.stream = stream;
    }
    
    /**
     * Creates a new ChatRequest with default parameters.
     */
    public ChatRequest(String model, List<ChatMessage> messages) {
        this(model, messages, 0.7, 2048, false);
    }
    
    // Getters and setters
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public List<ChatMessage> getMessages() { return messages; }
    public void setMessages(List<ChatMessage> messages) { this.messages = messages; }
    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }
    public int getMaxTokens() { return maxTokens; }
    public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
    public boolean isStream() { return stream; }
    public void setStream(boolean stream) { this.stream = stream; }
}
