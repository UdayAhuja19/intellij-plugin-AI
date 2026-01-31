package com.aiclient.model;

import com.google.gson.annotations.SerializedName;

/**
 * Token usage statistics from the API response.
 * Useful for tracking costs and limits.
 */
public class Usage {
    /** Number of tokens in the prompt */
    @SerializedName("prompt_tokens")
    private int promptTokens = 0;
    
    /** Number of tokens in the completion */
    @SerializedName("completion_tokens")
    private int completionTokens = 0;
    
    /** Total tokens used (prompt + completion) */
    @SerializedName("total_tokens")
    private int totalTokens = 0;
    
    // Default constructor for Gson
    public Usage() {}
    
    // Getters and setters
    public int getPromptTokens() { return promptTokens; }
    public void setPromptTokens(int promptTokens) { this.promptTokens = promptTokens; }
    public int getCompletionTokens() { return completionTokens; }
    public void setCompletionTokens(int completionTokens) { this.completionTokens = completionTokens; }
    public int getTotalTokens() { return totalTokens; }
    public void setTotalTokens(int totalTokens) { this.totalTokens = totalTokens; }
}
