package com.aiclient.model;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single chunk in a streaming response.
 * Structure is similar to ChatResponse but without usage info.
 */
public class StreamChunk {
    private String id = "";
    
    @SerializedName("object")
    private String objectType = "";
    
    private long created = 0;
    private String model = "";
    private List<Choice> choices = new ArrayList<>();
    
    // Default constructor for Gson
    public StreamChunk() {}
    
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
}
