package com.aiclient.model;

/**
 * Represents incremental content in a streaming response.
 * Contains partial role and content updates.
 */
public class Delta {
    /** Role of the message (may be null after first chunk) */
    private String role;
    
    /** Incremental content piece */
    private String content;
    
    // Default constructor for Gson
    public Delta() {}
    
    // Getters and setters
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
