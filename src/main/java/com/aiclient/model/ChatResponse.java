package com.aiclient.model;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

/**
 * Response from the chat completions API.
 * Contains the generated choices and usage information.
 */
public class ChatResponse {
    /** Unique identifier for the response */
    private String id = "";
    
    /** Object type (usually "chat.completion") */
    @SerializedName("object")
    private String objectType = "";
    
    /** Unix timestamp when the response was created */
    private long created = 0;
    
    /** The model used for generation */
    private String model = "";
    
    /** List of generated choices/responses */
    private List<Choice> choices = new ArrayList<>();
    
    /** Token usage statistics */
    private Usage usage;
    
    // Default constructor for Gson
    public ChatResponse() {}
    
    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getObjectType() { return objectType; }
    public void setObjectType(String objectType) { this.objectType = objectType; }
    public long getCreated() { return created; }
    public void setCreated(long created) { this.created = created; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public List<Choice> getChoices() { return choices; }
    public void setChoices(List<Choice> choices) { this.choices = choices; }
    public Usage getUsage() { return usage; }
    public void setUsage(Usage usage) { this.usage = usage; }
}
