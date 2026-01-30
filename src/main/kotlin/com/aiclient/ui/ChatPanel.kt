package com.aiclient.ui

import com.aiclient.service.AiClientService
import com.aiclient.settings.AiClientSettings
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import java.awt.*
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.*
import javax.swing.text.*

/**
 * Main chat panel UI component.
 * Displays conversation history and handles user input.
 * Supports code context from active editor.
 */
class ChatPanel(private val project: Project) : JPanel(BorderLayout()) {
    
    private val aiService = AiClientService.getInstance(project)
    private val settings get() = AiClientSettings.getInstance()
    
    private val chatArea: JTextPane
    private val inputArea: JBTextArea
    private val sendButton: JButton
    private val clearButton: JButton
    private val statusLabel: JLabel
    private val includeFileCheckbox: JCheckBox
    private val includeSelectionCheckbox: JCheckBox
    private val contextLabel: JLabel
    
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isProcessing = false
    
    init {
        border = JBUI.Borders.empty()
        
        // Chat history area
        chatArea = JTextPane().apply {
            isEditable = false
            background = UIUtil.getPanelBackground()
            border = JBUI.Borders.empty(10)
            contentType = "text/plain"
        }
        
        val chatScroll = JBScrollPane(chatArea).apply {
            border = JBUI.Borders.empty()
            verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
        }
        
        // Input area
        inputArea = JBTextArea(3, 40).apply {
            lineWrap = true
            wrapStyleWord = true
            border = JBUI.Borders.empty(8)
            font = UIUtil.getLabelFont()
            addKeyListener(object : KeyAdapter() {
                override fun keyPressed(e: KeyEvent) {
                    // Support both Ctrl+Enter and Cmd+Enter (macOS)
                    if (e.keyCode == KeyEvent.VK_ENTER && (e.isControlDown || e.isMetaDown)) {
                        e.consume()
                        sendMessage()
                    }
                }
            })
        }
        
        val inputScroll = JBScrollPane(inputArea).apply {
            border = JBUI.Borders.customLine(JBColor.border())
            preferredSize = Dimension(0, 80)
        }
        
        // Context options
        includeFileCheckbox = JCheckBox("Include current file").apply {
            isSelected = false
            font = font.deriveFont(11f)
            toolTipText = "Send the entire current file as context to the AI"
            addActionListener { updateContextLabel() }
        }
        
        includeSelectionCheckbox = JCheckBox("Include selection").apply {
            isSelected = true
            font = font.deriveFont(11f)
            toolTipText = "Send selected code as context to the AI"
            addActionListener { updateContextLabel() }
        }
        
        contextLabel = JLabel().apply {
            font = font.deriveFont(10f)
            foreground = JBColor.GRAY
        }
        
        // Context panel
        val contextPanel = JPanel(FlowLayout(FlowLayout.LEFT, 5, 0)).apply {
            add(includeFileCheckbox)
            add(includeSelectionCheckbox)
            add(contextLabel)
            border = JBUI.Borders.empty(2, 5)
        }
        
        // Buttons
        sendButton = JButton("Send", AllIcons.Actions.Execute).apply {
            addActionListener { sendMessage() }
        }
        
        clearButton = JButton("Clear", AllIcons.Actions.GC).apply {
            addActionListener { clearChat() }
        }
        
        statusLabel = JLabel("Ready").apply {
            foreground = JBColor.GRAY
            font = font.deriveFont(11f)
        }
        
        // Button panel
        val buttonPanel = JPanel(FlowLayout(FlowLayout.RIGHT)).apply {
            add(statusLabel)
            add(Box.createHorizontalStrut(10))
            add(clearButton)
            add(sendButton)
            border = JBUI.Borders.empty(5)
        }
        
        // Input container with context options
        val inputContainer = JPanel(BorderLayout()).apply {
            add(contextPanel, BorderLayout.NORTH)
            add(inputScroll, BorderLayout.CENTER)
            add(buttonPanel, BorderLayout.SOUTH)
            border = JBUI.Borders.empty(5)
        }
        
        // Header bar
        val headerPanel = createHeaderPanel()
        
        // Layout
        add(headerPanel, BorderLayout.NORTH)
        add(chatScroll, BorderLayout.CENTER)
        add(inputContainer, BorderLayout.SOUTH)
        
        // Welcome message
        appendMessage("AI Assistant", """Hello! I'm your AI coding assistant. How can I help you today?

💡 Tips:
• Press Cmd+Enter (or Ctrl+Enter) to send a message quickly
• Check "Include current file" to give me context about your code
• Select code in the editor and check "Include selection" for specific questions""", isUser = false)
        
        // Initial context update
        updateContextLabel()
    }
    
    private fun createHeaderPanel(): JPanel {
        return JPanel(BorderLayout()).apply {
            background = JBColor(Color(60, 63, 65), Color(60, 63, 65))
            border = JBUI.Borders.empty(8, 12)
            
            val titleLabel = JLabel("AI Chat").apply {
                font = font.deriveFont(Font.BOLD, 14f)
                foreground = JBColor.WHITE
                icon = AllIcons.Actions.Lightning
            }
            
            val modelLabel = JLabel(settings.state.model).apply {
                font = font.deriveFont(11f)
                foreground = JBColor(Color(150, 150, 150), Color(150, 150, 150))
            }
            
            add(titleLabel, BorderLayout.WEST)
            add(modelLabel, BorderLayout.EAST)
        }
    }
    
    private fun updateContextLabel() {
        val parts = mutableListOf<String>()
        
        if (includeFileCheckbox.isSelected) {
            val fileName = getCurrentFileName()
            if (fileName != null) {
                parts.add("📄 $fileName")
            }
        }
        
        if (includeSelectionCheckbox.isSelected) {
            val selectionLength = getSelectedTextLength()
            if (selectionLength > 0) {
                parts.add("✂️ ${selectionLength} chars selected")
            }
        }
        
        contextLabel.text = if (parts.isNotEmpty()) parts.joinToString(" | ") else ""
    }
    
    private fun getCurrentFileName(): String? {
        return ReadAction.compute<String?, Throwable> {
            val editor = FileEditorManager.getInstance(project).selectedTextEditor
            val file = editor?.virtualFile
            file?.name
        }
    }
    
    private fun getSelectedTextLength(): Int {
        return ReadAction.compute<Int, Throwable> {
            val editor = FileEditorManager.getInstance(project).selectedTextEditor
            editor?.selectionModel?.selectedText?.length ?: 0
        }
    }
    
    private fun getCurrentFileContent(): String? {
        return ReadAction.compute<String?, Throwable> {
            val editor = FileEditorManager.getInstance(project).selectedTextEditor
            val file = editor?.virtualFile
            val document = editor?.document
            
            if (file != null && document != null) {
                val fileName = file.name
                val content = document.text
                "File: $fileName\n```\n$content\n```"
            } else {
                null
            }
        }
    }
    
    private fun getSelectedCode(): String? {
        return ReadAction.compute<String?, Throwable> {
            val editor = FileEditorManager.getInstance(project).selectedTextEditor
            val selectedText = editor?.selectionModel?.selectedText
            val file = editor?.virtualFile
            
            if (!selectedText.isNullOrEmpty()) {
                val fileName = file?.name ?: "unknown"
                "Selected code from $fileName:\n```\n$selectedText\n```"
            } else {
                null
            }
        }
    }
    
    private fun buildContextMessage(): String? {
        val contextParts = mutableListOf<String>()
        
        if (includeFileCheckbox.isSelected) {
            getCurrentFileContent()?.let { contextParts.add(it) }
        }
        
        if (includeSelectionCheckbox.isSelected) {
            getSelectedCode()?.let { contextParts.add(it) }
        }
        
        return if (contextParts.isNotEmpty()) {
            "Here is the relevant code context:\n\n${contextParts.joinToString("\n\n")}"
        } else {
            null
        }
    }
    
    private fun sendMessage() {
        val message = inputArea.text.trim()
        if (message.isEmpty() || isProcessing) return
        
        inputArea.text = ""
        
        // Build the full message with context
        val contextMessage = buildContextMessage()
        val fullMessage = if (contextMessage != null) {
            "$contextMessage\n\nUser question: $message"
        } else {
            message
        }
        
        // Show user message (without context for readability)
        val displayMessage = if (contextMessage != null) {
            "📎 [With code context]\n$message"
        } else {
            message
        }
        appendMessage("You", displayMessage, isUser = true)
        
        isProcessing = true
        updateUI(processing = true)
        
        coroutineScope.launch {
            try {
                if (settings.state.streamingEnabled) {
                    // Streaming mode
                    ApplicationManager.getApplication().invokeLater {
                        appendMessage("AI Assistant", "", isUser = false)
                    }
                    var fullResponse = ""
                    aiService.sendMessageStreaming(fullMessage).collect { chunk ->
                        fullResponse += chunk
                        ApplicationManager.getApplication().invokeLater {
                            updateLastMessage(fullResponse)
                        }
                    }
                } else {
                    // Non-streaming mode
                    val result = aiService.sendMessage(fullMessage)
                    ApplicationManager.getApplication().invokeLater {
                        result.fold(
                            onSuccess = { response ->
                                appendMessage("AI Assistant", response, isUser = false)
                            },
                            onFailure = { error ->
                                appendMessage("Error", error.message ?: "Unknown error occurred", isUser = false, isError = true)
                            }
                        )
                    }
                }
            } catch (e: Exception) {
                ApplicationManager.getApplication().invokeLater {
                    appendMessage("Error", e.message ?: "Unknown error occurred", isUser = false, isError = true)
                }
            } finally {
                isProcessing = false
                updateUI(processing = false)
            }
        }
    }
    
    private fun appendMessage(sender: String, message: String, isUser: Boolean, isError: Boolean = false) {
        ApplicationManager.getApplication().invokeLater {
            val doc = chatArea.styledDocument
            
            // Sender style
            val senderStyle = SimpleAttributeSet().apply {
                StyleConstants.setBold(this, true)
                StyleConstants.setForeground(this, when {
                    isError -> JBColor.RED
                    isUser -> JBColor(Color(100, 180, 100), Color(100, 180, 100))
                    else -> JBColor(Color(100, 150, 220), Color(100, 150, 220))
                })
            }
            
            // Message style
            val messageStyle = SimpleAttributeSet().apply {
                StyleConstants.setForeground(this, if (isError) JBColor.RED else UIUtil.getLabelForeground())
            }
            
            try {
                doc.insertString(doc.length, "\n$sender:\n", senderStyle)
                doc.insertString(doc.length, "$message\n", messageStyle)
                chatArea.caretPosition = doc.length
            } catch (e: BadLocationException) {
                // Ignore
            }
        }
    }
    
    private fun updateLastMessage(content: String) {
        ApplicationManager.getApplication().invokeLater {
            val doc = chatArea.styledDocument
            val text = doc.getText(0, doc.length)
            
            // Find the last message start
            val lastSenderIndex = text.lastIndexOf("\nAI Assistant:\n")
            if (lastSenderIndex >= 0) {
                val messageStart = lastSenderIndex + "\nAI Assistant:\n".length
                try {
                    doc.remove(messageStart, doc.length - messageStart)
                    val messageStyle = SimpleAttributeSet().apply {
                        StyleConstants.setForeground(this, UIUtil.getLabelForeground())
                    }
                    doc.insertString(doc.length, "$content\n", messageStyle)
                    chatArea.caretPosition = doc.length
                } catch (e: BadLocationException) {
                    // Ignore
                }
            }
        }
    }
    
    private fun clearChat() {
        aiService.clearHistory()
        chatArea.text = ""
        appendMessage("AI Assistant", "Chat cleared. How can I help you?", isUser = false)
    }
    
    private fun updateUI(processing: Boolean) {
        ApplicationManager.getApplication().invokeLater {
            sendButton.isEnabled = !processing
            inputArea.isEnabled = !processing
            statusLabel.text = if (processing) "Processing..." else "Ready"
            statusLabel.foreground = if (processing) JBColor(Color(255, 180, 0), Color(255, 180, 0)) else JBColor.GRAY
            
            // Update context label when not processing
            if (!processing) {
                updateContextLabel()
            }
        }
    }
    
    fun dispose() {
        coroutineScope.cancel()
    }
}
