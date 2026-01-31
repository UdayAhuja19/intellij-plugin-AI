package com.aiclient.settings;

import com.aiclient.model.AiProvider;
import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.CredentialAttributesKt;
import com.intellij.credentialStore.Credentials;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Persistent settings for the AI Client plugin.
 * 
 * This service manages all plugin configuration including:
 * - API provider selection (OpenAI, Anthropic, Custom)
 * - API endpoint URL
 * - Model selection
 * - Generation parameters (temperature, max tokens)
 * - System prompt customization
 * - Streaming preference
 * 
 * API keys are stored securely using IntelliJ's credential store,
 * NOT in plain text in the settings XML file.
 * 
 * Usage:
 * <pre>
 * AiClientSettings settings = AiClientSettings.getInstance();
 * String apiKey = settings.getState().getApiKey();
 * </pre>
 */
@Service(Service.Level.APP)
@State(
    name = "AiClientSettings",
    storages = @Storage("AiClientSettings.xml")
)
public final class AiClientSettings implements PersistentStateComponent<AiClientSettings.State> {
    
    // Credential store key for the API key
    private static final String CREDENTIAL_KEY = "AiClientApiKey";
    
    // Current settings state
    private State myState = new State();
    
    // ========================================================================
    // State Class - Holds all settings values
    // ========================================================================
    
    /**
     * Settings state class containing all configuration values.
     * This is what gets serialized to AiClientSettings.xml.
     * 
     * Note: API key is NOT stored here - it uses secure credential storage.
     */
    public static class State {
        /** Selected AI provider */
        public AiProvider provider = AiProvider.OPENAI;
        
        /** API endpoint URL */
        public String apiEndpoint = AiProvider.OPENAI.getDefaultEndpoint();
        
        /** Model identifier */
        public String model = AiProvider.OPENAI.getDefaultModel();
        
        /** Temperature for generation (0.0 - 2.0) */
        public double temperature = 0.7;
        
        /** Maximum tokens to generate */
        public int maxTokens = 2048;
        
        /** Custom system prompt */
        public String systemPrompt = "";
        
        /** Whether to use streaming responses */
        public boolean streamingEnabled = true;
        
        /**
         * Gets the API key from secure storage.
         * @return The API key, or empty string if not set
         */
        public String getApiKey() {
            return getStoredApiKey();
        }
        
        /**
         * Stores the API key in secure storage.
         * @param apiKey The API key to store
         */
        public void setApiKey(String apiKey) {
            storeApiKey(apiKey);
        }
    }
    
    // ========================================================================
    // PersistentStateComponent Implementation
    // ========================================================================
    
    /**
     * Gets the current state for persistence.
     * @return The current settings state
     */
    @Override
    public @NotNull State getState() {
        return myState;
    }
    
    /**
     * Loads state from persistence.
     * Called by IntelliJ when loading saved settings.
     * @param state The state to load
     */
    @Override
    public void loadState(@NotNull State state) {
        myState = state;
    }
    
    // ========================================================================
    // Provider Management
    // ========================================================================
    
    /**
     * Updates the selected provider and resets endpoint/model to defaults.
     * Call this when the user switches providers in the settings UI.
     * 
     * @param provider The new provider to use
     */
    public void updateProvider(AiProvider provider) {
        myState.provider = provider;
        myState.apiEndpoint = provider.getDefaultEndpoint();
        myState.model = provider.getDefaultModel();
    }
    
    // ========================================================================
    // Secure Credential Storage
    // ========================================================================
    
    /**
     * Creates credential attributes for the API key.
     * Uses IntelliJ's credential store for secure storage.
     */
    private static CredentialAttributes createCredentialAttributes() {
        return new CredentialAttributes(
            CredentialAttributesKt.generateServiceName("AiClient", CREDENTIAL_KEY)
        );
    }
    
    /**
     * Retrieves the API key from secure storage.
     * @return The stored API key, or empty string if not set
     */
    private static String getStoredApiKey() {
        CredentialAttributes attributes = createCredentialAttributes();
        Credentials credentials = PasswordSafe.getInstance().get(attributes);
        if (credentials != null) {
            String password = credentials.getPasswordAsString();
            return password != null ? password : "";
        }
        return "";
    }
    
    /**
     * Stores the API key in secure storage.
     * @param apiKey The API key to store
     */
    private static void storeApiKey(String apiKey) {
        CredentialAttributes attributes = createCredentialAttributes();
        Credentials credentials = new Credentials(CREDENTIAL_KEY, apiKey);
        PasswordSafe.getInstance().set(attributes, credentials);
    }
    
    // ========================================================================
    // Service Instance Access
    // ========================================================================
    
    /**
     * Gets the singleton instance of AiClientSettings.
     * 
     * @return The settings instance
     */
    public static AiClientSettings getInstance() {
        return ApplicationManager.getApplication().getService(AiClientSettings.class);
    }
}
