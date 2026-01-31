package com.aiclient.ui;

import com.aiclient.service.AiClientService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * A dialog that shows code diff with red/green highlighting,
 * an AI chat panel for follow-up questions, and an Apply button.
 */
public class CodeDiffPanel extends DialogWrapper {
    
    private final Project project;
    private final Editor editor;
    private final String originalCode;
    private final String improvedCode;
    private final int selectionStart;
    private final int selectionEnd;
    
    // Chat components
    private JPanel chatMessagesPanel;
    private JTextField chatInput;
    private JBScrollPane chatScroll;
    
    // Colors for diff highlighting
    private static final Color REMOVED_BG = new JBColor(new Color(255, 220, 220), new Color(80, 40, 40));
    private static final Color ADDED_BG = new JBColor(new Color(220, 255, 220), new Color(40, 80, 40));
    private static final Color REMOVED_TEXT = new JBColor(new Color(180, 0, 0), new Color(255, 100, 100));
    private static final Color ADDED_TEXT = new JBColor(new Color(0, 120, 0), new Color(100, 255, 100));
    
    public CodeDiffPanel(Project project, Editor editor, String originalCode, String improvedCode) {
        super(project, true);
        this.project = project;
        this.editor = editor;
        this.originalCode = originalCode;
        this.improvedCode = improvedCode;
        this.selectionStart = editor.getSelectionModel().getSelectionStart();
        this.selectionEnd = editor.getSelectionModel().getSelectionEnd();
        
        setTitle("Code Improvement Review");
        setSize(1000, 700);
        init();
    }
    
    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(JBUI.Borders.empty(10));
        
        // Split pane: Diff (left) | Chat (right)
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.65);
        splitPane.setDividerSize(5);
        
        // Left: Diff panel
        JPanel diffPanel = createDiffPanel();
        splitPane.setLeftComponent(diffPanel);
        
        // Right: Chat panel
        JPanel chatPanel = createChatPanel();
        splitPane.setRightComponent(chatPanel);
        
        mainPanel.add(splitPane, BorderLayout.CENTER);
        
        return mainPanel;
    }
    
    private JPanel createDiffPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Changes"));
        
        // Create diff content
        JPanel diffContent = new JPanel();
        diffContent.setLayout(new BoxLayout(diffContent, BoxLayout.Y_AXIS));
        diffContent.setBackground(JBColor.background());
        
        // Generate diff lines
        List<DiffLine> diffLines = generateDiff(originalCode, improvedCode);
        
        for (DiffLine line : diffLines) {
            JPanel linePanel = createDiffLinePanel(line);
            diffContent.add(linePanel);
        }
        
        JBScrollPane scrollPane = new JBScrollPane(diffContent);
        scrollPane.setPreferredSize(new Dimension(500, 600));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createDiffLinePanel(DiffLine line) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        
        // Line number
        JLabel lineNum = new JLabel(String.format("%4d ", line.lineNumber));
        lineNum.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        lineNum.setForeground(JBColor.GRAY);
        lineNum.setBorder(new EmptyBorder(2, 5, 2, 5));
        
        // Symbol (+, -, space)
        JLabel symbol = new JLabel(line.type.symbol + " ");
        symbol.setFont(new Font(Font.MONOSPACED, Font.BOLD, 12));
        
        // Content
        JLabel content = new JLabel(line.content);
        content.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        
        // Style based on type
        switch (line.type) {
            case REMOVED:
                panel.setBackground(REMOVED_BG);
                symbol.setForeground(REMOVED_TEXT);
                content.setForeground(REMOVED_TEXT);
                break;
            case ADDED:
                panel.setBackground(ADDED_BG);
                symbol.setForeground(ADDED_TEXT);
                content.setForeground(ADDED_TEXT);
                break;
            default:
                panel.setBackground(JBColor.background());
                symbol.setForeground(JBColor.GRAY);
                content.setForeground(JBColor.foreground());
        }
        
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        leftPanel.setOpaque(false);
        leftPanel.add(lineNum);
        leftPanel.add(symbol);
        leftPanel.add(content);
        
        panel.add(leftPanel, BorderLayout.WEST);
        
        return panel;
    }
    
    private JPanel createChatPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Ask about changes"));
        panel.setPreferredSize(new Dimension(350, 600));
        
        // Chat messages area
        chatMessagesPanel = new JPanel();
        chatMessagesPanel.setLayout(new BoxLayout(chatMessagesPanel, BoxLayout.Y_AXIS));
        chatMessagesPanel.setBackground(JBColor.background());
        
        // Add initial message
        addChatMessage("AI", "I've analyzed your code and suggested improvements. Ask me anything about the changes!");
        
        chatScroll = new JBScrollPane(chatMessagesPanel);
        chatScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        chatScroll.getVerticalScrollBar().setUnitIncrement(16);
        
        panel.add(chatScroll, BorderLayout.CENTER);
        
        // Input area
        JPanel inputPanel = new JPanel(new BorderLayout(5, 0));
        chatInput = new JTextField();
        chatInput.setToolTipText("Ask about the changes...");
        chatInput.addActionListener(e -> sendChatMessage());
        
        JButton sendBtn = new JButton("Send");
        sendBtn.addActionListener(e -> sendChatMessage());
        
        inputPanel.add(chatInput, BorderLayout.CENTER);
        inputPanel.add(sendBtn, BorderLayout.EAST);
        
        panel.add(inputPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private void addChatMessage(String sender, String message) {
        JPanel msgPanel = new JPanel(new BorderLayout());
        msgPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        msgPanel.setBorder(JBUI.Borders.empty(5, 10));
        
        boolean isUser = sender.equals("You");
        msgPanel.setBackground(isUser ? 
            new JBColor(new Color(230, 240, 255), new Color(40, 50, 70)) :
            new JBColor(new Color(240, 240, 240), new Color(50, 50, 55)));
        
        JLabel senderLabel = new JLabel(sender + ":");
        senderLabel.setFont(senderLabel.getFont().deriveFont(Font.BOLD));
        senderLabel.setForeground(isUser ? 
            new JBColor(new Color(0, 100, 200), new Color(100, 180, 255)) :
            new JBColor(new Color(100, 100, 100), new Color(180, 180, 180)));
        
        JTextArea msgText = new JTextArea(message);
        msgText.setEditable(false);
        msgText.setLineWrap(true);
        msgText.setWrapStyleWord(true);
        msgText.setOpaque(false);
        msgText.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setOpaque(false);
        contentPanel.add(senderLabel, BorderLayout.NORTH);
        contentPanel.add(msgText, BorderLayout.CENTER);
        
        msgPanel.add(contentPanel, BorderLayout.CENTER);
        
        chatMessagesPanel.add(msgPanel);
        chatMessagesPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        chatMessagesPanel.revalidate();
        chatMessagesPanel.repaint();
        
        // Scroll to bottom
        SwingUtilities.invokeLater(() -> {
            JScrollBar vertical = chatScroll.getVerticalScrollBar();
            vertical.setValue(vertical.getMaximum());
        });
    }
    
    private void sendChatMessage() {
        String message = chatInput.getText().trim();
        if (message.isEmpty()) return;
        
        chatInput.setText("");
        addChatMessage("You", message);
        
        // Context for the AI
        String context = "Original code:\n```\n" + originalCode + "\n```\n\n" +
                        "Improved code:\n```\n" + improvedCode + "\n```\n\n" +
                        "User question: " + message;
        
        // Get AI response
        AiClientService aiService = AiClientService.getInstance(project);
        CompletableFuture<String> future = aiService.askAboutCode(context, 
            "Answer the user's question about the code changes. Be concise and helpful.");
        
        future.thenAccept(response -> {
            ApplicationManager.getApplication().invokeLater(() -> {
                addChatMessage("AI", response);
            });
        }).exceptionally(ex -> {
            ApplicationManager.getApplication().invokeLater(() -> {
                addChatMessage("AI", "Error: " + ex.getMessage());
            });
            return null;
        });
    }
    
    @Override
    protected Action @NotNull [] createActions() {
        return new Action[]{
            new DialogWrapperAction("Apply Changes") {
                @Override
                protected void doAction(ActionEvent e) {
                    applyChanges();
                    close(OK_EXIT_CODE);
                }
            },
            getCancelAction()
        };
    }
    
    private void applyChanges() {
        WriteCommandAction.runWriteCommandAction(project, () -> {
            Document document = editor.getDocument();
            document.replaceString(selectionStart, selectionEnd, improvedCode);
        });
    }
    
    // ========================================================================
    // Diff Generation
    // ========================================================================
    
    private List<DiffLine> generateDiff(String original, String improved) {
        List<DiffLine> result = new ArrayList<>();
        String[] origLines = original.split("\n", -1);
        String[] newLines = improved.split("\n", -1);
        
        int lineNum = 1;
        int i = 0, j = 0;
        
        // Simple diff algorithm - LCS based would be better but this works for demo
        while (i < origLines.length || j < newLines.length) {
            if (i >= origLines.length) {
                // All remaining are additions
                result.add(new DiffLine(lineNum++, DiffType.ADDED, newLines[j]));
                j++;
            } else if (j >= newLines.length) {
                // All remaining are removals
                result.add(new DiffLine(lineNum++, DiffType.REMOVED, origLines[i]));
                i++;
            } else if (origLines[i].equals(newLines[j])) {
                // Same line
                result.add(new DiffLine(lineNum++, DiffType.UNCHANGED, origLines[i]));
                i++;
                j++;
            } else {
                // Different - check if it's a modification or add/remove
                // Look ahead to see if original line appears later
                int foundAt = findInArray(origLines[i], newLines, j + 1);
                int foundOrigAt = findInArray(newLines[j], origLines, i + 1);
                
                if (foundOrigAt != -1 && (foundAt == -1 || foundOrigAt < foundAt)) {
                    // Original line found later in new - current new line is addition
                    result.add(new DiffLine(lineNum++, DiffType.ADDED, newLines[j]));
                    j++;
                } else {
                    // Show as modification (remove old, add new)
                    result.add(new DiffLine(lineNum++, DiffType.REMOVED, origLines[i]));
                    result.add(new DiffLine(lineNum++, DiffType.ADDED, newLines[j]));
                    i++;
                    j++;
                }
            }
        }
        
        return result;
    }
    
    private int findInArray(String needle, String[] haystack, int startFrom) {
        for (int i = startFrom; i < haystack.length; i++) {
            if (haystack[i].equals(needle)) return i;
        }
        return -1;
    }
    
    // ========================================================================
    // Diff Types
    // ========================================================================
    
    private enum DiffType {
        UNCHANGED(" "),
        ADDED("+"),
        REMOVED("-");
        
        final String symbol;
        
        DiffType(String symbol) {
            this.symbol = symbol;
        }
    }
    
    private static class DiffLine {
        final int lineNumber;
        final DiffType type;
        final String content;
        
        DiffLine(int lineNumber, DiffType type, String content) {
            this.lineNumber = lineNumber;
            this.type = type;
            this.content = content;
        }
    }
}
