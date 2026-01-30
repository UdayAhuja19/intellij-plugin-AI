package com.aiclient.settings

import com.aiclient.model.AiProvider
import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.*

/**
 * Persistent settings for the AI Client plugin.
 * API keys are stored securely using IntelliJ's credential store.
 */
@Service(Service.Level.APP)
@State(
    name = "AiClientSettings",
    storages = [Storage("AiClientSettings.xml")]
)
class AiClientSettings : PersistentStateComponent<AiClientSettings.State> {
    
    private var myState = State()
    
    data class State(
        var provider: AiProvider = AiProvider.OPENAI,
        var apiEndpoint: String = AiProvider.OPENAI.defaultEndpoint,
        var model: String = AiProvider.OPENAI.defaultModel,
        var temperature: Double = 0.7,
        var maxTokens: Int = 2048,
        var systemPrompt: String = "",
        var streamingEnabled: Boolean = true
    ) {
        // API key is stored separately in credential store
        var apiKey: String
            get() = getStoredApiKey()
            set(value) = storeApiKey(value)
    }
    
    override fun getState(): State = myState
    
    override fun loadState(state: State) {
        myState = state
    }
    
    fun updateProvider(provider: AiProvider) {
        myState.provider = provider
        myState.apiEndpoint = provider.defaultEndpoint
        myState.model = provider.defaultModel
    }
    
    companion object {
        private const val CREDENTIAL_KEY = "AiClientApiKey"
        
        private fun createCredentialAttributes(): CredentialAttributes {
            return CredentialAttributes(
                generateServiceName("AiClient", CREDENTIAL_KEY)
            )
        }
        
        private fun getStoredApiKey(): String {
            val attributes = createCredentialAttributes()
            val credentials = PasswordSafe.instance.get(attributes)
            return credentials?.getPasswordAsString() ?: ""
        }
        
        private fun storeApiKey(apiKey: String) {
            val attributes = createCredentialAttributes()
            val credentials = Credentials(CREDENTIAL_KEY, apiKey)
            PasswordSafe.instance.set(attributes, credentials)
        }
        
        fun getInstance(): AiClientSettings {
            return ApplicationManager.getApplication().getService(AiClientSettings::class.java)
        }
    }
}
