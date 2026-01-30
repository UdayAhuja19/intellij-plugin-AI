package com.aiclient.ui

import com.aiclient.service.AiClientService
import com.aiclient.settings.AiClientSettings
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
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
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.swing.*
import javax.swing.border.AbstractBorder

/**
 * Context mode enum for mutually exclusive options
 */
enum class ContextMode {
    NONE,
    CURRENT_FILE,
    SELECTION,
    ENTIRE_PROJECT
}

/**
 * Modern chat panel UI component with bubble-style messages.
 * Displays conversation history with improved visual design.
 */
class ChatPanel(private val project: Project) : JPanel(BorderLayout()) {
    
    private val aiService = AiClientService.getInstance(project)
    private val settings get() = AiClientSettings.getInstance()
    
    private val messagesPanel: JPanel
    private val messagesScroll: JBScrollPane
    private val inputArea: JBTextArea
    private val sendButton: JButton
    private val clearButton: JButton
    private val statusIndicator: JLabel
    private val statusLabel: JLabel
    private val contextLabel: JLabel
    
    // Radio buttons for context mode
    private val contextButtonGroup: ButtonGroup
    private val noContextRadio: JRadioButton
    private val currentFileRadio: JRadioButton
    private val selectionRadio: JRadioButton
    private val entireProjectRadio: JRadioButton
    
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isProcessing = false
    
    // Modern color palette
    private val accentColor = JBColor(Color(64, 120, 242), Color(74, 136, 255))
    private val userBubbleColor = JBColor(Color(64, 120, 242), Color(55, 105, 210))
    private val aiBubbleColor = JBColor(Color(240, 242, 245), Color(55, 57, 62))
    private val headerGradientStart = JBColor(Color(45, 50, 60), Color(35, 38, 45))
    private val headerGradientEnd = JBColor(Color(60, 70, 85), Color(50, 55, 65))
    private val inputBgColor = JBColor(Color(255, 255, 255), Color(50, 52, 58))
    private val inputBorderColor = JBColor(Color(64, 120, 242), Color(74, 136, 255))
    private val successGreen = JBColor(Color(46, 184, 92), Color(50, 190, 100))
    private val warningOrange = JBColor(Color(255, 170, 0), Color(255, 180, 50))
    
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    
    private var currentContextMode = ContextMode.NONE
    
    init {
        border = JBUI.Borders.empty()
        background = UIUtil.getPanelBackground()
        
        // Messages container
        messagesPanel = object : JPanel() {
            init {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                background = UIUtil.getPanelBackground()
                border = JBUI.Borders.empty(12)
            }
        }
        
        messagesScroll = JBScrollPane(messagesPanel).apply {
            border = JBUI.Borders.empty()
            verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
            horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
            background = UIUtil.getPanelBackground()
            viewport.background = UIUtil.getPanelBackground()
        }
        
        // Modern input area
        inputArea = JBTextArea(4, 40).apply {
            lineWrap = true
            wrapStyleWord = true
            border = JBUI.Borders.empty(12)
            font = UIUtil.getLabelFont().deriveFont(14f)
            background = inputBgColor
            addKeyListener(object : KeyAdapter() {
                override fun keyPressed(e: KeyEvent) {
                    if (e.keyCode == KeyEvent.VK_ENTER && (e.isControlDown || e.isMetaDown)) {
                        e.consume()
                        sendMessage()
                    }
                }
            })
        }
        
        // Prominent input scroll with accent border
        val inputScroll = JBScrollPane(inputArea).apply {
            border = AccentBorder(12, inputBorderColor, 2)
            minimumSize = Dimension(100, 100)
            preferredSize = Dimension(0, 110)
            background = inputBgColor
            viewport.background = inputBgColor
        }
        
        // Context mode radio buttons - mutually exclusive
        contextButtonGroup = ButtonGroup()
        
        noContextRadio = JRadioButton("No context").apply {
            isSelected = true
            font = font.deriveFont(Font.PLAIN, 11f)
            isOpaque = false
            addActionListener { 
                currentContextMode = ContextMode.NONE
                updateContextLabel() 
            }
        }
        
        currentFileRadio = JRadioButton("📄 Current File").apply {
            isSelected = false
            font = font.deriveFont(Font.PLAIN, 11f)
            isOpaque = false
            toolTipText = "Send the entire current file as context"
            addActionListener { 
                currentContextMode = ContextMode.CURRENT_FILE
                updateContextLabel() 
            }
        }
        
        selectionRadio = JRadioButton("✂️ Selection").apply {
            isSelected = false
            font = font.deriveFont(Font.PLAIN, 11f)
            isOpaque = false
            toolTipText = "Send selected code as context"
            addActionListener { 
                currentContextMode = ContextMode.SELECTION
                updateContextLabel() 
            }
        }
        
        entireProjectRadio = JRadioButton("📁 Entire Project").apply {
            isSelected = false
            font = font.deriveFont(Font.PLAIN, 11f)
            isOpaque = false
            toolTipText = "Send project structure and key files as context"
            addActionListener { 
                currentContextMode = ContextMode.ENTIRE_PROJECT
                updateContextLabel() 
            }
        }
        
        // Add to button group for mutual exclusivity
        contextButtonGroup.add(noContextRadio)
        contextButtonGroup.add(currentFileRadio)
        contextButtonGroup.add(selectionRadio)
        contextButtonGroup.add(entireProjectRadio)
        
        contextLabel = JLabel().apply {
            font = font.deriveFont(Font.ITALIC, 10f)
            foreground = JBColor.GRAY
        }
        
        // Context panel with wrap layout
        val contextPanel = JPanel(WrapLayout(FlowLayout.LEFT, 6, 4)).apply {
            add(JLabel("Context:").apply { 
                font = font.deriveFont(Font.BOLD, 11f) 
                foreground = accentColor
            })
            add(noContextRadio)
            add(currentFileRadio)
            add(selectionRadio)
            add(entireProjectRadio)
            add(contextLabel)
            isOpaque = false
            border = JBUI.Borders.empty(4, 4)
        }
        
        // Modern buttons
        sendButton = createModernButton("Send", AllIcons.Actions.Execute, accentColor).apply {
            addActionListener { sendMessage() }
        }
        
        clearButton = createModernButton("Clear", AllIcons.Actions.GC, null).apply {
            addActionListener { clearChat() }
        }
        
        // Status indicator
        statusIndicator = JLabel("●").apply {
            foreground = successGreen
            font = font.deriveFont(10f)
        }
        
        statusLabel = JLabel("Ready").apply {
            foreground = JBColor.GRAY
            font = font.deriveFont(Font.PLAIN, 11f)
        }
        
        val statusPanel = JPanel(FlowLayout(FlowLayout.LEFT, 4, 0)).apply {
            isOpaque = false
            add(statusIndicator)
            add(statusLabel)
        }
        
        // Button panel
        val buttonPanel = JPanel(BorderLayout()).apply {
            isOpaque = false
            add(statusPanel, BorderLayout.WEST)
            val buttonsRight = JPanel(FlowLayout(FlowLayout.RIGHT, 8, 0)).apply {
                isOpaque = false
                add(clearButton)
                add(sendButton)
            }
            add(buttonsRight, BorderLayout.EAST)
            border = JBUI.Borders.empty(8, 4)
        }
        
        // Input container
        val inputContainer = JPanel(BorderLayout()).apply {
            isOpaque = false
            border = JBUI.Borders.empty(8, 12, 12, 12)
            add(contextPanel, BorderLayout.NORTH)
            add(inputScroll, BorderLayout.CENTER)
            add(buttonPanel, BorderLayout.SOUTH)
        }
        
        // Header bar
        val headerPanel = createModernHeader()
        
        // Layout
        add(headerPanel, BorderLayout.NORTH)
        add(messagesScroll, BorderLayout.CENTER)
        add(inputContainer, BorderLayout.SOUTH)
        
        // Welcome message
        addMessageBubble(
            "AI Assistant",
            """Hello! I'm your AI coding assistant. How can I help you today?

💡 Tips:
• Press Cmd+Enter (or Ctrl+Enter) to send quickly
• Select a context mode to share code with me""",
            isUser = false
        )
        
        updateContextLabel()
    }
    
    private fun createModernHeader(): JPanel {
        return object : JPanel(BorderLayout()) {
            override fun paintComponent(g: Graphics) {
                val g2d = g as Graphics2D
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
                val gradient = GradientPaint(
                    0f, 0f, headerGradientStart,
                    width.toFloat(), height.toFloat(), headerGradientEnd
                )
                g2d.paint = gradient
                g2d.fillRect(0, 0, width, height)
            }
        }.apply {
            border = JBUI.Borders.empty(12, 16)
            preferredSize = Dimension(0, 48)
            
            val titlePanel = JPanel(FlowLayout(FlowLayout.LEFT, 8, 0)).apply {
                isOpaque = false
                val iconLabel = JLabel(AllIcons.Actions.Lightning)
                val titleLabel = JLabel("AI Chat").apply {
                    font = font.deriveFont(Font.BOLD, 15f)
                    foreground = Color.WHITE
                }
                add(iconLabel)
                add(titleLabel)
            }
            
            val modelBadge = JPanel().apply {
                isOpaque = false
                layout = FlowLayout(FlowLayout.RIGHT, 0, 0)
                val badge = object : JLabel(settings.state.model) {
                    override fun paintComponent(g: Graphics) {
                        val g2d = g as Graphics2D
                        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                        g2d.color = Color(255, 255, 255, 30)
                        g2d.fillRoundRect(0, 0, width, height, 12, 12)
                        super.paintComponent(g)
                    }
                }.apply {
                    font = font.deriveFont(Font.PLAIN, 11f)
                    foreground = Color(200, 205, 215)
                    border = JBUI.Borders.empty(4, 10)
                }
                add(badge)
            }
            
            add(titlePanel, BorderLayout.WEST)
            add(modelBadge, BorderLayout.EAST)
        }
    }
    
    private fun createModernButton(text: String, icon: Icon?, accentBg: Color?): JButton {
        return object : JButton(text, icon) {
            override fun paintComponent(g: Graphics) {
                val g2d = g as Graphics2D
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                
                if (accentBg != null) {
                    g2d.color = if (model.isPressed) accentBg.darker() else accentBg
                    g2d.fillRoundRect(0, 0, width, height, 10, 10)
                    foreground = Color.WHITE
                } else {
                    g2d.color = if (model.isRollover) JBColor(Color(230, 232, 236), Color(70, 72, 77)) 
                               else JBColor(Color(245, 246, 248), Color(55, 57, 62))
                    g2d.fillRoundRect(0, 0, width, height, 10, 10)
                    foreground = UIUtil.getLabelForeground()
                }
                
                val fm = g2d.fontMetrics
                val iconWidth = icon?.iconWidth ?: 0
                val gap = if (icon != null) 6 else 0
                val totalWidth = iconWidth + gap + fm.stringWidth(text)
                var x = (width - totalWidth) / 2
                
                icon?.paintIcon(this, g2d, x, (height - (icon.iconHeight)) / 2)
                x += iconWidth + gap
                
                g2d.color = foreground
                g2d.drawString(text, x, (height + fm.ascent - fm.descent) / 2)
            }
        }.apply {
            isContentAreaFilled = false
            isBorderPainted = false
            isFocusPainted = false
            font = font.deriveFont(Font.BOLD, 12f)
            preferredSize = Dimension(if (accentBg != null) 85 else 75, 32)
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        }
    }
    
    private fun addMessageBubble(sender: String, message: String, isUser: Boolean, isError: Boolean = false) {
        ApplicationManager.getApplication().invokeLater {
            val bubblePanel = createBubblePanel(sender, message, isUser, isError)
            messagesPanel.add(bubblePanel)
            messagesPanel.add(Box.createVerticalStrut(12))
            messagesPanel.revalidate()
            messagesPanel.repaint()
            
            SwingUtilities.invokeLater {
                messagesScroll.verticalScrollBar.value = messagesScroll.verticalScrollBar.maximum
            }
        }
    }
    
    private fun createBubblePanel(sender: String, message: String, isUser: Boolean, isError: Boolean): JPanel {
        val maxBubbleWidth = 0.75
        
        return JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            isOpaque = false
            alignmentX = Component.LEFT_ALIGNMENT
            
            if (isUser) {
                add(Box.createHorizontalGlue())
            }
            
            val bubble = object : JPanel(BorderLayout()) {
                override fun paintComponent(g: Graphics) {
                    val g2d = g as Graphics2D
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                    
                    g2d.color = JBColor(Color(0, 0, 0, 20), Color(0, 0, 0, 50))
                    g2d.fillRoundRect(2, 2, width - 2, height - 2, 16, 16)
                    
                    g2d.color = when {
                        isError -> JBColor(Color(255, 230, 230), Color(80, 40, 40))
                        isUser -> userBubbleColor
                        else -> aiBubbleColor
                    }
                    g2d.fillRoundRect(0, 0, width - 2, height - 2, 16, 16)
                }
                
                override fun getMaximumSize(): Dimension {
                    val parentWidth = parent?.width ?: 400
                    val maxWidth = (parentWidth * maxBubbleWidth).toInt().coerceAtLeast(200)
                    return Dimension(maxWidth, super.getPreferredSize().height)
                }
                
                override fun getPreferredSize(): Dimension {
                    val pref = super.getPreferredSize()
                    val parentWidth = parent?.width ?: 400
                    val maxWidth = (parentWidth * maxBubbleWidth).toInt().coerceAtLeast(200)
                    return Dimension(pref.width.coerceAtMost(maxWidth), pref.height)
                }
            }.apply {
                isOpaque = false
                border = JBUI.Borders.empty(12, 16)
                
                val senderPanel = JPanel(FlowLayout(FlowLayout.LEFT, 4, 0)).apply {
                    isOpaque = false
                    val icon = if (isUser) AllIcons.General.User else AllIcons.Actions.Lightning
                    add(JLabel(icon))
                    add(JLabel(sender).apply {
                        font = font.deriveFont(Font.BOLD, 11f)
                        foreground = when {
                            isError -> JBColor.RED
                            isUser -> Color.WHITE
                            else -> accentColor
                        }
                    })
                    add(JLabel("· ${LocalTime.now().format(timeFormatter)}").apply {
                        font = font.deriveFont(Font.PLAIN, 10f)
                        foreground = if (isUser) Color(255, 255, 255, 180) else JBColor.GRAY
                    })
                }
                
                val messageLabel = createFormattedTextPane(message, isUser, isError).apply {
                    border = JBUI.Borders.empty(6, 0, 0, 0)
                }
                
                add(senderPanel, BorderLayout.NORTH)
                add(messageLabel, BorderLayout.CENTER)
            }
            
            add(bubble)
            
            if (!isUser) {
                add(Box.createHorizontalGlue())
            }
        }
    }
        /**
     * Creates a formatted text component that displays cleaned markdown text.
     */
    private fun createFormattedTextPane(text: String, isUser: Boolean, isError: Boolean): JTextArea {
        // Clean markdown to readable plain text
        val cleanedText = cleanMarkdown(text)
        
        val textColor = when {
            isError -> JBColor.RED
            isUser -> Color.WHITE
            else -> UIUtil.getLabelForeground()
        }
        
        return JTextArea(cleanedText).apply {
            isEditable = false
            lineWrap = true
            wrapStyleWord = true
            font = UIUtil.getLabelFont().deriveFont(13f)
            isOpaque = false
            foreground = textColor
            background = Color(0, 0, 0, 0)
        }
    }
    
    /**
     * Clean markdown symbols for readable display.
     */
    private fun cleanMarkdown(text: String): String {
        val lines = text.split("\n")
        val result = StringBuilder()
        var inCodeBlock = false
        
        for (line in lines) {
            val trimmed = line.trim()
            
            // Handle code blocks
            if (trimmed.startsWith("```")) {
                inCodeBlock = !inCodeBlock
                if (!inCodeBlock) result.append("\n")
                continue
            }
            
            // Inside code block - keep as-is with indent
            if (inCodeBlock) {
                result.append("    $line\n")
                continue
            }
            
            var cleanLine = line
            
            // Remove heading markers, make uppercase for emphasis
            if (trimmed.startsWith("#")) {
                cleanLine = trimmed.replace(Regex("^#+\\s*"), "").uppercase()
                result.append("\n$cleanLine\n")
                continue
            }
            
            // Remove bold markers
            cleanLine = cleanLine.replace("**", "")
            cleanLine = cleanLine.replace("__", "")
            
            // Remove italic markers (single)
            cleanLine = cleanLine.replace(Regex("(?<![\\w])\\*([^*]+)\\*(?![\\w])"), "$1")
            cleanLine = cleanLine.replace(Regex("(?<![\\w])_([^_]+)_(?![\\w])"), "$1")
            
            // Remove inline code backticks
            cleanLine = cleanLine.replace("`", "")
            
            // Convert bullet markers to clean bullets
            if (trimmed.matches(Regex("^[*-]\\s+.*"))) {
                cleanLine = "  • " + trimmed.substring(2).trim()
                    .replace("**", "")
                    .replace("`", "")
            }
            
            // Keep numbered lists with indentation
            if (trimmed.matches(Regex("^\\d+\\.\\s+.*"))) {
                cleanLine = "  $trimmed"
                    .replace("**", "")
                    .replace("`", "")
            }
            
            result.append("$cleanLine\n")
        }
        
        return result.toString().trim()
    }
    
    
    private fun updateContextLabel() {
        contextLabel.text = when (currentContextMode) {
            ContextMode.NONE -> ""
            ContextMode.CURRENT_FILE -> {
                val fileName = getCurrentFileName()
                if (fileName != null) "→ $fileName" else ""
            }
            ContextMode.SELECTION -> {
                val len = getSelectedTextLength()
                if (len > 0) "→ $len chars selected" else "→ No selection"
            }
            ContextMode.ENTIRE_PROJECT -> "→ ${project.name}"
        }
    }
    
    private fun getCurrentFileName(): String? {
        return ReadAction.compute<String?, Throwable> {
            val editor = FileEditorManager.getInstance(project).selectedTextEditor
            editor?.virtualFile?.name
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
                "File: ${file.name}\n```\n${document.text}\n```"
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
                "Selected code from ${file?.name ?: "unknown"}:\n```\n$selectedText\n```"
            } else {
                null
            }
        }
    }
    
    private fun getProjectContext(): String? {
        return ReadAction.compute<String?, Throwable> {
            val projectRoot = ProjectRootManager.getInstance(project).contentRoots.firstOrNull()
            if (projectRoot == null) return@compute null
            
            val sb = StringBuilder()
            sb.appendLine("Project: ${project.name}")
            sb.appendLine("Root: ${projectRoot.path}")
            sb.appendLine()
            
            // Supported text file extensions
            val textExtensions = setOf(
                "kt", "java", "xml", "json", "yaml", "yml", "properties", "gradle", "kts",
                "md", "txt", "html", "css", "js", "ts", "tsx", "jsx", "py", "rb", "go",
                "rs", "c", "cpp", "h", "hpp", "swift", "sql", "sh", "bash", "toml"
            )
            
            // Folders to skip
            val skipFolders = setOf(
                ".git", ".gradle", ".idea", "build", "out", "target", "node_modules",
                ".intellijPlatform", "__pycache__", ".venv", "venv"
            )
            
            // Collect all files recursively with content
            fun collectFilesWithContent(dir: com.intellij.openapi.vfs.VirtualFile, relativePath: String = "") {
                dir.children?.sortedBy { it.name }?.forEach { child ->
                    val childPath = if (relativePath.isEmpty()) child.name else "$relativePath/${child.name}"
                    
                    // Skip hidden and ignored folders
                    if (child.name.startsWith(".") && child.isDirectory) return@forEach
                    if (child.isDirectory && skipFolders.contains(child.name)) return@forEach
                    
                    if (child.isDirectory) {
                        collectFilesWithContent(child, childPath)
                    } else {
                        // Check if it's a text file we can read
                        val extension = child.extension?.lowercase() ?: ""
                        if (textExtensions.contains(extension) || child.name.endsWith(".gradle.kts")) {
                            try {
                                val content = String(child.contentsToByteArray(), Charsets.UTF_8)
                                sb.appendLine("=".repeat(60))
                                sb.appendLine("FILE: $childPath")
                                sb.appendLine("=".repeat(60))
                                sb.appendLine("```$extension")
                                sb.appendLine(content)
                                sb.appendLine("```")
                                sb.appendLine()
                            } catch (e: Exception) {
                                // Skip files that can't be read
                            }
                        }
                    }
                }
            }
            
            collectFilesWithContent(projectRoot)
            
            sb.toString()
        }
    }
    
    private fun buildContextMessage(): String? {
        return when (currentContextMode) {
            ContextMode.NONE -> null
            ContextMode.CURRENT_FILE -> getCurrentFileContent()?.let { 
                "Here is the current file:\n\n$it" 
            }
            ContextMode.SELECTION -> getSelectedCode()?.let { 
                "Here is the selected code:\n\n$it" 
            }
            ContextMode.ENTIRE_PROJECT -> getProjectContext()?.let { 
                "Here is the project context:\n\n$it" 
            }
        }
    }
    
    private fun sendMessage() {
        val message = inputArea.text.trim()
        if (message.isEmpty() || isProcessing) return
        
        inputArea.text = ""
        
        val contextMessage = buildContextMessage()
        val fullMessage = if (contextMessage != null) {
            "$contextMessage\n\nUser question: $message"
        } else {
            message
        }
        
        val contextIndicator = when (currentContextMode) {
            ContextMode.NONE -> ""
            ContextMode.CURRENT_FILE -> "📄 "
            ContextMode.SELECTION -> "✂️ "
            ContextMode.ENTIRE_PROJECT -> "📁 "
        }
        val displayMessage = if (contextMessage != null) {
            "$contextIndicator[With ${currentContextMode.name.lowercase().replace("_", " ")}]\n$message"
        } else {
            message
        }
        addMessageBubble("You", displayMessage, isUser = true)
        
        isProcessing = true
        updateUI(processing = true)
        
        coroutineScope.launch {
            try {
                if (settings.state.streamingEnabled) {
                    var fullResponse = ""
                    var streamingBubble: JPanel? = null
                    
                    aiService.sendMessageStreaming(fullMessage).collect { chunk ->
                        fullResponse += chunk
                        ApplicationManager.getApplication().invokeLater {
                            if (streamingBubble == null) {
                                streamingBubble = createBubblePanel("AI Assistant", fullResponse, isUser = false, isError = false)
                                messagesPanel.add(streamingBubble)
                                messagesPanel.add(Box.createVerticalStrut(12))
                            } else {
                                updateBubbleMessage(streamingBubble!!, fullResponse)
                            }
                            messagesPanel.revalidate()
                            messagesPanel.repaint()
                            messagesScroll.verticalScrollBar.value = messagesScroll.verticalScrollBar.maximum
                        }
                    }
                } else {
                    val result = aiService.sendMessage(fullMessage)
                    ApplicationManager.getApplication().invokeLater {
                        result.fold(
                            onSuccess = { response ->
                                addMessageBubble("AI Assistant", response, isUser = false)
                            },
                            onFailure = { error ->
                                addMessageBubble("Error", error.message ?: "Unknown error occurred", isUser = false, isError = true)
                            }
                        )
                    }
                }
            } catch (e: Exception) {
                ApplicationManager.getApplication().invokeLater {
                    addMessageBubble("Error", e.message ?: "Unknown error occurred", isUser = false, isError = true)
                }
            } finally {
                isProcessing = false
                updateUI(processing = false)
            }
        }
    }
    
    private fun updateBubbleMessage(bubble: JPanel, newMessage: String) {
        fun findTextArea(container: Container): JTextArea? {
            for (comp in container.components) {
                if (comp is JTextArea) return comp
                if (comp is Container) {
                    val found = findTextArea(comp)
                    if (found != null) return found
                }
            }
            return null
        }
        
        findTextArea(bubble)?.let { textArea ->
            // For streaming, update with cleaned markdown
            textArea.text = cleanMarkdown(newMessage)
        }
    }
    
    private fun clearChat() {
        aiService.clearHistory()
        messagesPanel.removeAll()
        messagesPanel.revalidate()
        messagesPanel.repaint()
        addMessageBubble("AI Assistant", "Chat cleared. How can I help you?", isUser = false)
    }
    
    private fun updateUI(processing: Boolean) {
        ApplicationManager.getApplication().invokeLater {
            sendButton.isEnabled = !processing
            inputArea.isEnabled = !processing
            statusLabel.text = if (processing) "Processing..." else "Ready"
            statusIndicator.foreground = if (processing) warningOrange else successGreen
            
            if (!processing) updateContextLabel()
        }
    }
    
    fun dispose() {
        coroutineScope.cancel()
    }
    
    private class AccentBorder(private val radius: Int, private val color: Color, private val thickness: Int) : AbstractBorder() {
        override fun paintBorder(c: Component, g: Graphics, x: Int, y: Int, width: Int, height: Int) {
            val g2d = g as Graphics2D
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g2d.color = color
            g2d.stroke = BasicStroke(thickness.toFloat())
            g2d.drawRoundRect(x + thickness/2, y + thickness/2, width - thickness, height - thickness, radius, radius)
        }
        
        override fun getBorderInsets(c: Component) = Insets(thickness + 2, thickness + 2, thickness + 2, thickness + 2)
    }
    
    /**
     * FlowLayout subclass that wraps components to the next line when the container is resized
     */
    private class WrapLayout(align: Int, hgap: Int, vgap: Int) : FlowLayout(align, hgap, vgap) {
        override fun preferredLayoutSize(target: Container): Dimension {
            return layoutSize(target, true)
        }
        
        override fun minimumLayoutSize(target: Container): Dimension {
            val minimum = layoutSize(target, false)
            minimum.width -= (hgap + 1)
            return minimum
        }
        
        private fun layoutSize(target: Container, preferred: Boolean): Dimension {
            synchronized(target.treeLock) {
                val targetWidth = if (target.size.width == 0) Int.MAX_VALUE else target.width
                val insets = target.insets
                val horizontalInsetsAndGap = insets.left + insets.right + (hgap * 2)
                val maxWidth = targetWidth - horizontalInsetsAndGap
                
                val dim = Dimension(0, 0)
                var rowWidth = 0
                var rowHeight = 0
                
                for (m in target.components) {
                    if (m.isVisible) {
                        val d = if (preferred) m.preferredSize else m.minimumSize
                        
                        if (rowWidth + d.width > maxWidth) {
                            dim.width = maxOf(dim.width, rowWidth)
                            if (dim.height > 0) dim.height += vgap
                            dim.height += rowHeight
                            rowWidth = 0
                            rowHeight = 0
                        }
                        
                        if (rowWidth != 0) rowWidth += hgap
                        rowWidth += d.width
                        rowHeight = maxOf(rowHeight, d.height)
                    }
                }
                
                dim.width = maxOf(dim.width, rowWidth)
                if (dim.height > 0) dim.height += vgap
                dim.height += rowHeight
                
                dim.width += horizontalInsetsAndGap
                dim.height += insets.top + insets.bottom + vgap * 2
                
                return dim
            }
        }
    }
}
