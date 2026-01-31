package com.aiclient.model;

import java.util.Objects;

/**
 * Represents a single message in a chat conversation.
 * Each message has a role (system, user, or assistant) and content.
 */
public class ChatMessage {
    /** The role of the message sender: "system", "user", or "assistant" */
    private String role;
    
    /** The content/text of the message */
    private String content;
    
    /**
     * Creates a new ChatMessage.
     * @param role The role (system, user, assistant)
     * @param content The message content
     */
    public ChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }
    
    // Getters and setters
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChatMessage that = (ChatMessage) o;
        return Objects.equals(role, that.role) && Objects.equals(content, that.content);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(role, content);
    }
}
