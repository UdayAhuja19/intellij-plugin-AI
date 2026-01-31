package com.aiclient.ui;

import com.aiclient.service.AiClientService;
import com.aiclient.settings.AiClientSettings;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Minimal, iMessage-style chat panel with clean design.
 * 
 * Features:
 * - Clean, minimal color palette
 * - Text-message style conversation bubbles
 * - Responsive context chips
 * - Clear, readable text
 */
public class ChatPanel extends JPanel {
    
    // ========================================================================
    // Constants
    // ========================================================================
    
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
        "java", "kt", "kts", "py", "js", "ts", "jsx", "tsx", "go", "rs",
        "c", "cpp", "h", "hpp", "cs", "rb", "php", "swift", "scala",
        "html", "css", "scss", "xml", "json", "yaml", "yml", "md", "txt"
    );
    
    private static final Set<String> SKIP_FOLDERS = Set.of(
        "node_modules", ".git", ".idea", "build", "dist", "target",
        "__pycache__", ".gradle", "venv", ".venv"
    );
    
    // ========================================================================
    // Clean, Minimal Color Palette
    // ========================================================================
    
    // Backgrounds
    private final Color bgMain = new JBColor(new Color(255, 255, 255), new Color(30, 30, 30));
    private final Color bgInput = new JBColor(new Color(245, 245, 247), new Color(45, 45, 48));
    
    // Message bubbles - like iMessage
    private final Color userBubble = new JBColor(new Color(0, 122, 255), new Color(0, 122, 255));
    private final Color aiBubble = new JBColor(new Color(229, 229, 234), new Color(58, 58, 60));
    
    // Text
    private final Color textDark = new JBColor(new Color(0, 0, 0), new Color(255, 255, 255));
    private final Color textLight = new JBColor(new Color(255, 255, 255), new Color(255, 255, 255));
    private final Color textGray = new JBColor(new Color(142, 142, 147), new Color(142, 142, 147));
    
    // Accents
    private final Color accentBlue = new JBColor(new Color(0, 122, 255), new Color(10, 132, 255));
    private final Color borderLight = new JBColor(new Color(210, 210, 215), new Color(70, 70, 74));
    
    // ========================================================================
    // Fields
    // ========================================================================
    
    private final Project project;
    private final AiClientService aiService;
    private final JPanel messagesPanel;
    private final JBScrollPane messagesScroll;
    private final JBTextArea inputArea;
    private final JButton sendButton;
    private final JLabel statusLabel;
    
    private final ButtonGroup contextButtonGroup;
    private ContextMode currentContextMode = ContextMode.NONE;
    private volatile boolean isProcessing = false;
    
    public enum ContextMode {
        NONE, CURRENT_FILE, SELECTION, ENTIRE_PROJECT
    }
    
    // ========================================================================
    // Constructor
    // ========================================================================
    
    public ChatPanel(Project project) {
        super(new BorderLayout());
        this.project = project;
        this.aiService = AiClientService.getInstance(project);
        
        setBackground(bgMain);
        setBorder(JBUI.Borders.empty());
        
        // ====================================================================
        // Messages Area
        // ====================================================================
        
        messagesPanel = new JPanel();
        messagesPanel.setLayout(new BoxLayout(messagesPanel, BoxLayout.Y_AXIS));
        messagesPanel.setBackground(bgMain);
        messagesPanel.setBorder(JBUI.Borders.empty(16));
        
        messagesScroll = new JBScrollPane(messagesPanel);
        messagesScroll.setBorder(JBUI.Borders.empty());
        messagesScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        messagesScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        messagesScroll.setBackground(bgMain);
        messagesScroll.getViewport().setBackground(bgMain);
        
        // ====================================================================
        // Input Area - Clean, visible text area
        // ====================================================================
        
        inputArea = new JBTextArea(3, 40);
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        inputArea.setFont(UIUtil.getLabelFont().deriveFont(14f));
        inputArea.setBorder(JBUI.Borders.empty(12, 14));
        inputArea.setBackground(bgInput);
        inputArea.setForeground(textDark);
        inputArea.setCaretColor(accentBlue);
        
        // Placeholder text behavior
        inputArea.setText("");
        
        inputArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER && (e.isControlDown() || e.isMetaDown())) {
                    e.consume();
                    sendMessage();
                }
            }
        });
        
        // Input wrapper with rounded border
        JPanel inputWrapper = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bgInput);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.setColor(borderLight);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
                g2.dispose();
            }
        };
        inputWrapper.setOpaque(false);
        
        JScrollPane inputScroll = new JBScrollPane(inputArea);
        inputScroll.setBorder(JBUI.Borders.empty());
        inputScroll.setOpaque(false);
        inputScroll.getViewport().setOpaque(false);
        
        // Send button - simple arrow
        sendButton = new JButton("↑") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isEnabled() && !isProcessing ? accentBlue : textGray);
                g2.fillOval(2, 2, getWidth() - 4, getHeight() - 4);
                g2.dispose();
                
                g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.setFont(getFont().deriveFont(Font.BOLD, 16f));
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth("↑")) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString("↑", x, y);
                g2.dispose();
            }
        };
        sendButton.setPreferredSize(new Dimension(32, 32));
        sendButton.setMinimumSize(new Dimension(32, 32));
        sendButton.setContentAreaFilled(false);
        sendButton.setBorderPainted(false);
        sendButton.setFocusPainted(false);
        sendButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        sendButton.addActionListener(e -> sendMessage());
        
        JPanel sendPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        sendPanel.setOpaque(false);
        sendPanel.setBorder(JBUI.Borders.empty(0, 4, 0, 8));
        sendPanel.add(sendButton);
        
        inputWrapper.add(inputScroll, BorderLayout.CENTER);
        inputWrapper.add(sendPanel, BorderLayout.EAST);
        
        // ====================================================================
        // Context Chips - Minimal wrapping layout
        // ====================================================================
        
        contextButtonGroup = new ButtonGroup();
        
        JPanel contextPanel = new JPanel(new WrapLayout(FlowLayout.LEFT, 6, 4));
        contextPanel.setOpaque(false);
        contextPanel.setBorder(JBUI.Borders.empty(8, 0, 12, 0));
        
        contextPanel.add(createChip("None", ContextMode.NONE, true));
        contextPanel.add(createChip("Current File", ContextMode.CURRENT_FILE, false));
        contextPanel.add(createChip("Selection", ContextMode.SELECTION, false));
        contextPanel.add(createChip("Project", ContextMode.ENTIRE_PROJECT, false));
        
        // ====================================================================
        // Bottom bar - Status and Clear
        // ====================================================================
        
        statusLabel = new JLabel("Ready");
        statusLabel.setFont(UIUtil.getLabelFont().deriveFont(11f));
        statusLabel.setForeground(textGray);
        
        JButton clearButton = new JButton("Clear");
        clearButton.setFont(UIUtil.getLabelFont().deriveFont(11f));
        clearButton.setForeground(textGray);
        clearButton.setContentAreaFilled(false);
        clearButton.setBorderPainted(false);
        clearButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        clearButton.addActionListener(e -> clearChat());
        
        JPanel bottomBar = new JPanel(new BorderLayout());
        bottomBar.setOpaque(false);
        bottomBar.add(statusLabel, BorderLayout.WEST);
        bottomBar.add(clearButton, BorderLayout.EAST);
        bottomBar.setBorder(JBUI.Borders.empty(8, 4, 0, 4));
        
        // ====================================================================
        // Input Container
        // ====================================================================
        
        JPanel inputContainer = new JPanel(new BorderLayout());
        inputContainer.setOpaque(false);
        inputContainer.setBorder(JBUI.Borders.empty(8, 16, 16, 16));
        inputContainer.add(contextPanel, BorderLayout.NORTH);
        inputContainer.add(inputWrapper, BorderLayout.CENTER);
        inputContainer.add(bottomBar, BorderLayout.SOUTH);
        
        // ====================================================================
        // Simple Header
        // ====================================================================
        
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(bgMain);
        header.setBorder(JBUI.Borders.empty(16, 20));
        
        JLabel title = new JLabel("FocusFlow");
        title.setFont(UIUtil.getLabelFont().deriveFont(Font.BOLD, 17f));
        title.setForeground(textDark);
        
        header.add(title, BorderLayout.WEST);
        
        // Separator line
        JPanel separator = new JPanel();
        separator.setPreferredSize(new Dimension(0, 1));
        separator.setBackground(borderLight);
        
        JPanel headerWithSeparator = new JPanel(new BorderLayout());
        headerWithSeparator.setOpaque(false);
        headerWithSeparator.add(header, BorderLayout.CENTER);
        headerWithSeparator.add(separator, BorderLayout.SOUTH);
        
        // ====================================================================
        // Layout
        // ====================================================================
        
        add(headerWithSeparator, BorderLayout.NORTH);
        add(messagesScroll, BorderLayout.CENTER);
        add(inputContainer, BorderLayout.SOUTH);
        
        // Welcome
        addAiMessage("Hi! I'm FocusFlow, your coding assistant. Ask me anything about your code.\n\nUse Ctrl+Enter to send.");
    }
    
    // ========================================================================
    // Context Chip
    // ========================================================================
    
    private JToggleButton createChip(String text, ContextMode mode, boolean selected) {
        JToggleButton chip = new JToggleButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                if (isSelected()) {
                    g2.setColor(accentBlue);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                    setForeground(textLight);
                } else {
                    g2.setColor(borderLight);
                    g2.setStroke(new BasicStroke(1f));
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                    setForeground(textGray);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        
        chip.setFont(UIUtil.getLabelFont().deriveFont(11f));
        chip.setSelected(selected);
        chip.setContentAreaFilled(false);
        chip.setBorderPainted(false);
        chip.setFocusPainted(false);
        chip.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        chip.setBorder(JBUI.Borders.empty(5, 10));
        chip.addActionListener(e -> currentContextMode = mode);
        contextButtonGroup.add(chip);
        
        return chip;
    }
    
    // ========================================================================
    // Sending Messages
    // ========================================================================
    
    public void handleUserMessage(String message) {
        if (message == null || message.trim().isEmpty() || isProcessing) return;
        processMessage(message.trim());
    }
    
    private void sendMessage() {
        String message = inputArea.getText().trim();
        if (message.isEmpty() || isProcessing) return;
        
        inputArea.setText("");
        processMessage(message);
    }

    private void processMessage(String message) {
        isProcessing = true;
        updateStatus(true);
        
        // Build context in background thread to avoid EDT issues
        CompletableFuture.supplyAsync(() -> buildFullMessage(message))
            .thenAccept(fullMessage -> {
                ApplicationManager.getApplication().invokeLater(() -> {
                    // Add user bubble
                    addUserMessage(message);
                    
                    // AI placeholder
                    JPanel aiBubble = createAiBubble("...");
                    messagesPanel.add(aiBubble);
                    messagesPanel.add(Box.createVerticalStrut(8));
                    messagesPanel.revalidate();
                    scrollToBottom();
                    
                    // Send to AI
                    AiClientSettings.State settings = AiClientSettings.getInstance().getState();
                    
                    if (settings.streamingEnabled) {
                        StringBuilder response = new StringBuilder();
                        aiService.sendMessageStreaming(
                            fullMessage,
                            chunk -> ApplicationManager.getApplication().invokeLater(() -> {
                                response.append(chunk);
                                updateBubbleText(aiBubble, response.toString());
                                scrollToBottom();
                            }),
                            () -> ApplicationManager.getApplication().invokeLater(() -> {
                                String r = response.toString();
                                updateBubbleText(aiBubble, r.isEmpty() ? "No response." : cleanText(r));
                                isProcessing = false;
                                updateStatus(false);
                            }),
                            error -> ApplicationManager.getApplication().invokeLater(() -> {
                                updateBubbleText(aiBubble, "Error: " + error);
                                isProcessing = false;
                                updateStatus(false);
                            })
                        );
                    } else {
                        aiService.sendMessage(fullMessage).thenAccept(r -> {
                            ApplicationManager.getApplication().invokeLater(() -> {
                                updateBubbleText(aiBubble, cleanText(r));
                                isProcessing = false;
                                updateStatus(false);
                            });
                        });
                    }
                });
            });
    }
    
    // ========================================================================
    // Message Bubbles - iMessage style
    // ========================================================================
    
    private void addUserMessage(String message) {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JPanel bubble = createBubble(message, true);
        
        // Push to right
        row.add(Box.createHorizontalGlue());
        row.add(bubble);
        
        messagesPanel.add(row);
        messagesPanel.add(Box.createVerticalStrut(12));
        messagesPanel.revalidate();
        scrollToBottom();
    }
    
    private void addAiMessage(String message) {
        JPanel aiBubble = createAiBubble(message);
        messagesPanel.add(aiBubble);
        messagesPanel.add(Box.createVerticalStrut(8));
        messagesPanel.revalidate();
        scrollToBottom();
    }
    
    private JPanel createAiBubble(String message) {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JPanel bubble = createBubble(message, false);
        
        // Align left with glue on right
        row.add(bubble);
        row.add(Box.createHorizontalGlue());
        
        return row;
    }
    
    private JPanel createBubble(String message, boolean isUser) {
        JTextArea text = new JTextArea(cleanText(message));
        text.setName("bubbleText");
        text.setEditable(false);
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        text.setFont(UIUtil.getLabelFont().deriveFont(15f));
        text.setOpaque(false);
        text.setForeground(isUser ? textLight : textDark);
        text.setBorder(JBUI.Borders.empty(14, 18));
        
        // Reference to scroll pane for responsive sizing
        final JBScrollPane scrollRef = messagesScroll;
        
        JPanel bubble = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isUser ? userBubble : aiBubble);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
                g2.dispose();
            }
            
            @Override
            public Dimension getPreferredSize() {
                // Get scroll pane width for responsive sizing
                int availableWidth = scrollRef.getViewport().getWidth();
                if (availableWidth < 100) availableWidth = 400; // Fallback
                
                // Bubble takes up to 80% of available width
                int maxWidth = (int)(availableWidth * 0.80) - 40;
                maxWidth = Math.max(maxWidth, 250); // Minimum 250px
                
                // Set text columns based on available width
                text.setColumns(0);
                text.setSize(new Dimension(maxWidth, Short.MAX_VALUE));
                
                Dimension d = super.getPreferredSize();
                d.width = Math.min(d.width, maxWidth);
                return d;
            }
            
            @Override
            public Dimension getMaximumSize() {
                return getPreferredSize();
            }
        };
        bubble.setOpaque(false);
        bubble.add(text, BorderLayout.CENTER);
        
        return bubble;
    }
    
    private void updateBubbleText(JPanel row, String text) {
        findTextArea(row, area -> area.setText(cleanText(text)));
    }
    
    private void findTextArea(Container c, java.util.function.Consumer<JTextArea> action) {
        for (Component comp : c.getComponents()) {
            if (comp instanceof JTextArea && "bubbleText".equals(comp.getName())) {
                action.accept((JTextArea) comp);
                return;
            }
            if (comp instanceof Container) {
                findTextArea((Container) comp, action);
            }
        }
    }
    
    // ========================================================================
    // Helpers
    // ========================================================================
    
    private void scrollToBottom() {
        SwingUtilities.invokeLater(() -> {
            JScrollBar bar = messagesScroll.getVerticalScrollBar();
            bar.setValue(bar.getMaximum());
        });
    }
    
    private void clearChat() {
        aiService.clearHistory();
        messagesPanel.removeAll();
        addAiMessage("Chat cleared. How can I help?");
        messagesPanel.revalidate();
        messagesPanel.repaint();
    }
    
    private void updateStatus(boolean processing) {
        statusLabel.setText(processing ? "Thinking..." : "Ready");
        statusLabel.setForeground(processing ? accentBlue : textGray);
        sendButton.setEnabled(!processing);
        inputArea.setEnabled(!processing);
    }
    
    private String cleanText(String text) {
        if (text == null) return "";
        
        StringBuilder result = new StringBuilder();
        String[] lines = text.split("\n");
        boolean inCode = false;
        
        for (String line : lines) {
            String t = line.trim();
            
            if (t.startsWith("```")) {
                inCode = !inCode;
                if (!inCode) result.append("\n");
                continue;
            }
            
            if (inCode) {
                result.append("  ").append(line).append("\n");
                continue;
            }
            
            String clean = line;
            if (t.startsWith("#")) {
                clean = "\n" + t.replaceFirst("^#+\\s*", "").toUpperCase() + "\n";
            } else {
                clean = clean.replace("**", "").replace("__", "").replace("`", "");
                if (t.matches("^[*-]\\s+.*")) {
                    clean = "• " + t.substring(2).trim().replace("**", "").replace("`", "");
                }
            }
            result.append(clean).append("\n");
        }
        return result.toString().trim();
    }
    
    // ========================================================================
    // Context Building - Run off EDT
    // ========================================================================
    
    private String buildFullMessage(String userMessage) {
        StringBuilder sb = new StringBuilder();
        
        switch (currentContextMode) {
            case CURRENT_FILE:
                String file = getCurrentFileContent();
                if (file != null && !file.isEmpty()) {
                    sb.append("Current file:\n```\n").append(file).append("\n```\n\n");
                }
                break;
            case SELECTION:
                String sel = getSelectedText();
                if (sel != null && !sel.isEmpty()) {
                    sb.append("Selected code:\n```\n").append(sel).append("\n```\n\n");
                }
                break;
            case ENTIRE_PROJECT:
                String ctx = getProjectContext();
                if (!ctx.isEmpty()) {
                    sb.append("Project:\n").append(ctx).append("\n\n");
                }
                break;
        }
        
        sb.append(userMessage);
        return sb.toString();
    }
    
    private String getCurrentFileContent() {
        return ReadAction.compute(() -> {
            var editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
            return editor != null ? editor.getDocument().getText() : null;
        });
    }
    
    private String getSelectedText() {
        return ReadAction.compute(() -> {
            var editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
            return editor != null ? editor.getSelectionModel().getSelectedText() : null;
        });
    }
    
    private String getProjectContext() {
        return ApplicationManager.getApplication().runReadAction((com.intellij.openapi.util.Computable<String>) () -> {
            StringBuilder ctx = new StringBuilder();
            ctx.append(project.getName()).append("\n");
            
            VirtualFile[] roots = ProjectRootManager.getInstance(project).getContentRoots();
            for (VirtualFile root : roots) {
                appendTree(root, "", ctx, 0);
            }
            return ctx.toString();
        });
    }
    
    private void appendTree(VirtualFile file, String indent, StringBuilder sb, int depth) {
        if (depth > 3) return;
        if (SKIP_FOLDERS.contains(file.getName())) return;
        
        sb.append(indent).append(file.isDirectory() ? "📁 " : "  ").append(file.getName()).append("\n");
        
        if (file.isDirectory() && depth < 3) {
            VirtualFile[] children = file.getChildren();
            if (children != null) {
                for (VirtualFile child : children) {
                    appendTree(child, indent + "  ", sb, depth + 1);
                }
            }
        }
    }
    
    // ========================================================================
    // WrapLayout
    // ========================================================================
    
    private static class WrapLayout extends FlowLayout {
        public WrapLayout(int align, int hgap, int vgap) {
            super(align, hgap, vgap);
        }
        
        @Override
        public Dimension preferredLayoutSize(Container target) {
            return layoutSize(target, true);
        }
        
        @Override
        public Dimension minimumLayoutSize(Container target) {
            Dimension min = layoutSize(target, false);
            min.width -= (getHgap() + 1);
            return min;
        }
        
        private Dimension layoutSize(Container target, boolean preferred) {
            synchronized (target.getTreeLock()) {
                int targetWidth = target.getSize().width;
                if (targetWidth == 0) targetWidth = Integer.MAX_VALUE;
                
                int hgap = getHgap();
                int vgap = getVgap();
                Insets insets = target.getInsets();
                int maxWidth = targetWidth - insets.left - insets.right - hgap * 2;
                
                Dimension dim = new Dimension(0, 0);
                int rowWidth = 0;
                int rowHeight = 0;
                
                for (int i = 0; i < target.getComponentCount(); i++) {
                    Component m = target.getComponent(i);
                    if (m.isVisible()) {
                        Dimension d = preferred ? m.getPreferredSize() : m.getMinimumSize();
                        
                        if (rowWidth + d.width > maxWidth) {
                            addRow(dim, rowWidth, rowHeight);
                            rowWidth = 0;
                            rowHeight = 0;
                        }
                        
                        if (rowWidth != 0) rowWidth += hgap;
                        rowWidth += d.width;
                        rowHeight = Math.max(rowHeight, d.height);
                    }
                }
                addRow(dim, rowWidth, rowHeight);
                
                dim.width += insets.left + insets.right + hgap * 2;
                dim.height += insets.top + insets.bottom + vgap * 2;
                
                return dim;
            }
        }
        
        private void addRow(Dimension dim, int rowWidth, int rowHeight) {
            dim.width = Math.max(dim.width, rowWidth);
            if (dim.height > 0) dim.height += getVgap();
            dim.height += rowHeight;
        }
    }
}
