package com.aiclient.settings

import com.aiclient.model.AiProvider
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.JBColor
import com.intellij.ui.TitledSeparator
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.*
import javax.swing.*
import javax.swing.border.AbstractBorder

/**
 * Modern settings UI panel for AI Client configuration.
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
    
    // Modern colors
    private val sectionBg = JBColor(Color(248, 249, 252), Color(48, 50, 55))
    private val borderColor = JBColor(Color(225, 228, 235), Color(60, 63, 68))
    private val accentColor = JBColor(Color(64, 120, 242), Color(74, 136, 255))
    
    override fun getDisplayName(): String = "AI Client"
    
    override fun createComponent(): JComponent {
        // Provider selection with modern styling
        providerCombo = ComboBox(AiProvider.entries.toTypedArray()).apply {
            renderer = object : DefaultListCellRenderer() {
                override fun getListCellRendererComponent(
                    list: JList<*>?, value: Any?, index: Int, 
                    isSelected: Boolean, cellHasFocus: Boolean
                ): java.awt.Component {
                    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
                    text = (value as? AiProvider)?.displayName ?: value.toString()
                    border = JBUI.Borders.empty(4, 8)
                    return this
                }
            }
            addActionListener {
                val selected = selectedItem as? AiProvider ?: return@addActionListener
                endpointField?.text = selected.defaultEndpoint
                modelField?.text = selected.defaultModel
            }
        }
        
        // API Key with modern password field
        apiKeyField = JBPasswordField().apply {
            columns = 40
            border = createFieldBorder()
        }
        
        // Endpoint URL
        endpointField = JBTextField().apply {
            columns = 40
            border = createFieldBorder()
        }
        
        // Model name
        modelField = JBTextField().apply {
            columns = 30
            border = createFieldBorder()
        }
        
        // Temperature slider
        temperatureSpinner = JSpinner(SpinnerNumberModel(0.7, 0.0, 2.0, 0.1)).apply {
            preferredSize = Dimension(100, 30)
        }
        
        // Max tokens
        maxTokensSpinner = JSpinner(SpinnerNumberModel(2048, 100, 32000, 100)).apply {
            preferredSize = Dimension(120, 30)
        }
        
        // System prompt with modern styling
        systemPromptArea = JBTextArea(4, 40).apply {
            lineWrap = true
            wrapStyleWord = true
            border = JBUI.Borders.empty(10)
            font = UIUtil.getLabelFont().deriveFont(12f)
        }
        val systemPromptScroll = JScrollPane(systemPromptArea).apply {
            border = RoundedBorder(8, borderColor)
        }
        
        // Modern streaming checkbox
        streamingCheckbox = JCheckBox("Enable streaming responses").apply {
            font = font.deriveFont(Font.PLAIN, 12f)
            isOpaque = false
            toolTipText = "Show AI responses in real-time as they're generated"
        }
        
        // Build modern sectioned layout
        mainPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = JBUI.Borders.empty(16)
            background = UIUtil.getPanelBackground()
            
            // Connection Section
            add(createSection("Connection", listOf(
                createFormRow("Provider", providerCombo!!, "Select your AI provider"),
                createFormRow("API Key", apiKeyField!!, "Your API key (stored securely)"),
                createFormRow("API Endpoint", endpointField!!, "Provider API endpoint URL")
            )))
            
            add(Box.createVerticalStrut(16))
            
            // Model Settings Section
            add(createSection("Model Settings", listOf(
                createFormRow("Model", modelField!!, "Model identifier (e.g., gpt-4, claude-3)"),
                createFormRow("Temperature", temperatureSpinner!!, "Controls randomness (0.0-2.0)"),
                createFormRow("Max Tokens", maxTokensSpinner!!, "Maximum response length")
            )))
            
            add(Box.createVerticalStrut(16))
            
            // Advanced Section
            add(createSection("Advanced", listOf(
                createFormRow("System Prompt", systemPromptScroll, "Custom instructions for the AI"),
                createCheckboxRow(streamingCheckbox!!)
            )))
            
            add(Box.createVerticalGlue())
        }
        
        return JScrollPane(mainPanel).apply {
            border = JBUI.Borders.empty()
            background = UIUtil.getPanelBackground()
            viewport.background = UIUtil.getPanelBackground()
        }
    }
    
    private fun createSection(title: String, rows: List<JPanel>): JPanel {
        return JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = CompoundBorder(
                RoundedBorder(12, borderColor),
                JBUI.Borders.empty(16)
            )
            background = sectionBg
            alignmentX = Component.LEFT_ALIGNMENT
            
            // Section header
            val header = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)).apply {
                isOpaque = false
                alignmentX = Component.LEFT_ALIGNMENT
                add(JLabel(title).apply {
                    font = font.deriveFont(Font.BOLD, 14f)
                    foreground = accentColor
                })
            }
            add(header)
            add(Box.createVerticalStrut(12))
            
            // Add rows
            rows.forEachIndexed { index, row ->
                row.alignmentX = Component.LEFT_ALIGNMENT
                add(row)
                if (index < rows.size - 1) {
                    add(Box.createVerticalStrut(12))
                }
            }
        }
    }
    
    private fun createFormRow(label: String, component: JComponent, tooltip: String): JPanel {
        return JPanel(BorderLayout(12, 0)).apply {
            isOpaque = false
            alignmentX = Component.LEFT_ALIGNMENT
            maximumSize = Dimension(Int.MAX_VALUE, 36)
            
            val labelComp = JBLabel("$label:").apply {
                preferredSize = Dimension(120, 24)
                font = font.deriveFont(12f)
                toolTipText = tooltip
            }
            
            add(labelComp, BorderLayout.WEST)
            add(component, BorderLayout.CENTER)
        }
    }
    
    private fun createCheckboxRow(checkbox: JCheckBox): JPanel {
        return JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)).apply {
            isOpaque = false
            alignmentX = Component.LEFT_ALIGNMENT
            add(Box.createHorizontalStrut(132)) // Align with form fields
            add(checkbox)
        }
    }
    
    private fun createFieldBorder(): javax.swing.border.Border {
        return javax.swing.border.CompoundBorder(
            RoundedBorder(6, borderColor),
            JBUI.Borders.empty(6, 10)
        )
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
    
    /**
     * Custom rounded border for modern cards
     */
    private class RoundedBorder(private val radius: Int, private val color: Color) : AbstractBorder() {
        override fun paintBorder(c: Component, g: Graphics, x: Int, y: Int, width: Int, height: Int) {
            val g2d = g as Graphics2D
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g2d.color = color
            g2d.drawRoundRect(x, y, width - 1, height - 1, radius, radius)
        }
        
        override fun getBorderInsets(c: Component) = Insets(1, 1, 1, 1)
    }
    
    private class CompoundBorder(
        private val outside: javax.swing.border.Border,
        private val inside: javax.swing.border.Border
    ) : AbstractBorder() {
        override fun paintBorder(c: Component, g: Graphics, x: Int, y: Int, width: Int, height: Int) {
            outside.paintBorder(c, g, x, y, width, height)
        }
        
        override fun getBorderInsets(c: Component): Insets {
            val o = outside.getBorderInsets(c)
            val i = inside.getBorderInsets(c)
            return Insets(o.top + i.top, o.left + i.left, o.bottom + i.bottom, o.right + i.right)
        }
    }
}
