package com.aiclient.model;

import com.google.gson.annotations.SerializedName;

/**
 * Represents a single choice/response from the AI.
 * Non-streaming responses have a 'message', streaming has 'delta'.
 */
public class Choice {
    /** Index of this choice (usually 0 for single response) */
    private int index = 0;
    
    /** The complete message (for non-streaming responses) */
    private ChatMessage message;
    
    /** Incremental content (for streaming responses) */
    private Delta delta;
    
    /** Reason the generation stopped (e.g., "stop", "length") */
    @SerializedName("finish_reason")
    private String finishReason;
    
    // Default constructor for Gson
    public Choice() {}
    
    // Getters and setters
    public int getIndex() { return index; }
    public void setIndex(int index) { this.index = index; }
    public ChatMessage getMessage() { return message; }
    public void setMessage(ChatMessage message) { this.message = message; }
    public Delta getDelta() { return delta; }
    public void setDelta(Delta delta) { this.delta = delta; }
    public String getFinishReason() { return finishReason; }
    public void setFinishReason(String finishReason) { this.finishReason = finishReason; }
}
