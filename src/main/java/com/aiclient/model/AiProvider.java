package com.aiclient.model;

/**
 * Represents the available AI provider configurations.
 * Each provider has a display name, default API endpoint, and default model.
 * 
 * Supported providers:
 * - OPENAI: OpenAI's GPT models (gpt-4o, gpt-3.5-turbo, etc.)
 * - ANTHROPIC: Anthropic's Claude models
 * - CUSTOM: Any OpenAI-compatible API (Ollama, LM Studio, etc.)
 */
public enum AiProvider {
    
    /**
     * OpenAI - GPT-4, GPT-3.5 Turbo, and other OpenAI models.
     * Requires an API key from platform.openai.com
     */
    OPENAI("OpenAI", "https://api.openai.com/v1", "gpt-4o"),
    
    /**
     * Anthropic - Claude 3 Opus, Sonnet, and Haiku models.
     * Requires an API key from console.anthropic.com
     */
    ANTHROPIC("Anthropic", "https://api.anthropic.com/v1", "claude-3-opus-20240229"),
    
    /**
     * Custom - Any OpenAI-compatible API endpoint.
     * Default is configured for local Ollama instance.
     */
    CUSTOM("Custom", "http://localhost:11434/v1", "llama2");
    
    /** Human-readable name for the provider */
    private final String displayName;
    
    /** Default API endpoint URL */
    private final String defaultEndpoint;
    
    /** Default model identifier */
    private final String defaultModel;
    
    /**
     * Creates an AI provider configuration.
     * 
     * @param displayName Human-readable name
     * @param defaultEndpoint Default API endpoint URL
     * @param defaultModel Default model identifier
     */
    AiProvider(String displayName, String defaultEndpoint, String defaultModel) {
        this.displayName = displayName;
        this.defaultEndpoint = defaultEndpoint;
        this.defaultModel = defaultModel;
    }
    
    /**
     * Gets the human-readable display name.
     * @return The display name (e.g., "OpenAI")
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Gets the default API endpoint URL.
     * @return The endpoint URL (e.g., "https://api.openai.com/v1")
     */
    public String getDefaultEndpoint() {
        return defaultEndpoint;
    }
    
    /**
     * Gets the default model identifier.
     * @return The model ID (e.g., "gpt-4o")
     */
    public String getDefaultModel() {
        return defaultModel;
    }
}
