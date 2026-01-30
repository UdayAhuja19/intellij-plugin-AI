package com.aiclient.settings

import com.aiclient.model.AiProvider
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import javax.swing.*

/**
 * Settings UI panel for AI Client configuration.
 * Accessible via Settings > Tools > AI Client
 */
class AiClientSettingsConfigurable : Configurable {
    
    private var mainPanel: JPanel? = null
    private var providerCombo: ComboBox<AiProvider>? = null
    private var apiKeyField: JBPasswordField? = null
    private var endpointField: JBTextField? = null
    private var modelField: JBTextField? = null
    private var temperatureSpinner: JSpinner? = null
    private var maxTokensSpinner: JSpinner? = null
    private var systemPromptArea: JBTextArea? = null
    private var streamingCheckbox: JCheckBox? = null
    
    private val settings get() = AiClientSettings.getInstance()
    
    override fun getDisplayName(): String = "AI Client"
    
    override fun createComponent(): JComponent {
        // Provider selection
        providerCombo = ComboBox(AiProvider.entries.toTypedArray()).apply {
            renderer = object : DefaultListCellRenderer() {
                override fun getListCellRendererComponent(
                    list: JList<*>?, value: Any?, index: Int, 
                    isSelected: Boolean, cellHasFocus: Boolean
                ): java.awt.Component {
                    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
                    text = (value as? AiProvider)?.displayName ?: value.toString()
                    return this
                }
            }
            addActionListener {
                val selected = selectedItem as? AiProvider ?: return@addActionListener
                endpointField?.text = selected.defaultEndpoint
                modelField?.text = selected.defaultModel
            }
        }
        
        // API Key (password field)
        apiKeyField = JBPasswordField().apply {
            columns = 40
        }
        
        // Endpoint URL
        endpointField = JBTextField().apply {
            columns = 40
        }
        
        // Model name
        modelField = JBTextField().apply {
            columns = 30
        }
        
        // Temperature slider (0.0 - 2.0)
        temperatureSpinner = JSpinner(SpinnerNumberModel(0.7, 0.0, 2.0, 0.1))
        
        // Max tokens
        maxTokensSpinner = JSpinner(SpinnerNumberModel(2048, 100, 32000, 100))
        
        // System prompt
        systemPromptArea = JBTextArea(4, 40).apply {
            lineWrap = true
            wrapStyleWord = true
            border = JBUI.Borders.empty(5)
        }
        val systemPromptScroll = JScrollPane(systemPromptArea).apply {
            border = JBUI.Borders.customLine(JBUI.CurrentTheme.CustomFrameDecorations.separatorForeground())
        }
        
        // Streaming checkbox
        streamingCheckbox = JCheckBox("Enable streaming responses")
        
        // Build form
        mainPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("Provider:"), providerCombo!!)
            .addLabeledComponent(JBLabel("API Key:"), apiKeyField!!)
            .addLabeledComponent(JBLabel("API Endpoint:"), endpointField!!)
            .addLabeledComponent(JBLabel("Model:"), modelField!!)
            .addLabeledComponent(JBLabel("Temperature:"), temperatureSpinner!!)
            .addLabeledComponent(JBLabel("Max Tokens:"), maxTokensSpinner!!)
            .addSeparator()
            .addLabeledComponent(JBLabel("System Prompt:"), systemPromptScroll)
            .addComponent(streamingCheckbox!!)
            .addComponentFillVertically(JPanel(), 0)
            .panel.apply {
                border = JBUI.Borders.empty(10)
            }
        
        return mainPanel!!
    }
    
    override fun isModified(): Boolean {
        val state = settings.state
        return providerCombo?.selectedItem != state.provider ||
                String(apiKeyField?.password ?: charArrayOf()) != state.apiKey ||
                endpointField?.text != state.apiEndpoint ||
                modelField?.text != state.model ||
                (temperatureSpinner?.value as? Double) != state.temperature ||
                (maxTokensSpinner?.value as? Int) != state.maxTokens ||
                systemPromptArea?.text != state.systemPrompt ||
                streamingCheckbox?.isSelected != state.streamingEnabled
    }
    
    override fun apply() {
        val state = settings.state
        state.provider = providerCombo?.selectedItem as? AiProvider ?: AiProvider.OPENAI
        state.apiKey = String(apiKeyField?.password ?: charArrayOf())
        state.apiEndpoint = endpointField?.text ?: AiProvider.OPENAI.defaultEndpoint
        state.model = modelField?.text ?: AiProvider.OPENAI.defaultModel
        state.temperature = (temperatureSpinner?.value as? Double) ?: 0.7
        state.maxTokens = (maxTokensSpinner?.value as? Int) ?: 2048
        state.systemPrompt = systemPromptArea?.text ?: ""
        state.streamingEnabled = streamingCheckbox?.isSelected ?: true
    }
    
    override fun reset() {
        val state = settings.state
        providerCombo?.selectedItem = state.provider
        apiKeyField?.text = state.apiKey
        endpointField?.text = state.apiEndpoint
        modelField?.text = state.model
        temperatureSpinner?.value = state.temperature
        maxTokensSpinner?.value = state.maxTokens
        systemPromptArea?.text = state.systemPrompt
        streamingCheckbox?.isSelected = state.streamingEnabled
    }
    
    override fun disposeUIResources() {
        mainPanel = null
        providerCombo = null
        apiKeyField = null
        endpointField = null
        modelField = null
        temperatureSpinner = null
        maxTokensSpinner = null
        systemPromptArea = null
        streamingCheckbox = null
    }
}
