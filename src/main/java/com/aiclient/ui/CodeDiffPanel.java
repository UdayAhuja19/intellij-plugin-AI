package com.aiclient.ui;

import com.aiclient.service.AiClientService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * A panel that shows code diff with red/green highlighting,
 * an AI chat panel for follow-up questions, and an Apply button.
 * Designed to be used as a tab content.
 */
public class CodeDiffPanel extends JPanel {
    
    private final Project project;
    private final Editor editor;
    private final String originalCode;
    private final String improvedCode;
    private final int selectionStart;
    private final int selectionEnd;
    
    // Colors for diff highlighting
    private static final Color REMOVED_BG = new JBColor(new Color(60, 20, 20), new Color(60, 20, 20));
    private static final Color ADDED_BG = new JBColor(new Color(20, 60, 20), new Color(20, 60, 20));
    private static final Color REMOVED_TEXT = new JBColor(new Color(255, 100, 100), new Color(255, 100, 100));
    private static final Color ADDED_TEXT = new JBColor(new Color(100, 255, 100), new Color(100, 255, 100));

    // UI Colors
    private static final Color DARK_BG = new JBColor(new Color(30, 30, 30), new Color(30, 30, 30));
    private static final Color BLACK_BG = new JBColor(new Color(30, 30, 30), new Color(30, 30, 30)); // Match ChatPanel dark gray
    private static final Color ACCENT_BLUE = new JBColor(new Color(0, 122, 255), new Color(10, 132, 255));
    
    public CodeDiffPanel(Project project, Editor editor, String originalCode, String improvedCode) {
        super(new BorderLayout());
        this.project = project;
        this.editor = editor;
        this.originalCode = originalCode;
        this.improvedCode = improvedCode;
        this.selectionStart = editor.getSelectionModel().getSelectionStart();
        this.selectionEnd = editor.getSelectionModel().getSelectionEnd();
        
        initUI();
    }
    
    private void initUI() {
        setBorder(JBUI.Borders.empty(16));
        setBackground(BLACK_BG);
        
        // Top: Action buttons
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(BLACK_BG);
        topPanel.setBorder(JBUI.Borders.emptyBottom(16));
        
        JLabel titleLabel = new JLabel("Proposed Changes");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 18f));
        titleLabel.setForeground(Color.WHITE);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);
        
        JButton closeButton = new JButton("Discard");
        closeButton.addActionListener(e -> closeTab());
        closeButton.setForeground(Color.GRAY);
        closeButton.setContentAreaFilled(false);
        closeButton.setBorderPainted(false);
        closeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        // Custom Outlined Button
        JButton applyButton = new OutlinedButton("Apply Changes", ACCENT_BLUE);
        applyButton.addActionListener(e -> applyChanges());
        applyButton.setPreferredSize(new Dimension(140, 32));
        
        buttonPanel.add(closeButton);
        buttonPanel.add(applyButton);
        
        topPanel.add(titleLabel, BorderLayout.WEST);
        topPanel.add(buttonPanel, BorderLayout.EAST);
        
        add(topPanel, BorderLayout.NORTH);
        
        // Center: Diff panel (Full Width)
        JPanel diffPanel = createDiffPanel();
        add(diffPanel, BorderLayout.CENTER);
    }
    
    private void closeTab() {
        com.aiclient.ui.AiTabManager.getInstance(project).closeTab(this);
    }

    private JPanel createDiffPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BLACK_BG);
        panel.setBorder(JBUI.Borders.customLine(new Color(60, 60, 60), 1));
        
        // Create diff content
        JPanel diffContent = new JPanel();
        diffContent.setLayout(new BoxLayout(diffContent, BoxLayout.Y_AXIS));
        diffContent.setBackground(BLACK_BG);
        
        // Generate diff lines
        List<DiffLine> diffLines = generateDiff(originalCode, improvedCode);
        
        for (DiffLine line : diffLines) {
            JPanel linePanel = createDiffLinePanel(line);
            diffContent.add(linePanel);
        }
        
        // Add glue
        diffContent.add(Box.createVerticalGlue());
        
        JBScrollPane scrollPane = new JBScrollPane(diffContent);
        scrollPane.setBorder(JBUI.Borders.empty());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getViewport().setBackground(BLACK_BG);
        
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createDiffLinePanel(DiffLine line) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        panel.setPreferredSize(new Dimension(0, 26));
        panel.setBackground(BLACK_BG); // Default background
        
        // Line number
        JLabel lineNum = new JLabel(String.format("%4d", line.lineNumber));
        lineNum.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        lineNum.setForeground(Color.GRAY);
        lineNum.setBorder(JBUI.Borders.empty(0, 8, 0, 8));
        lineNum.setPreferredSize(new Dimension(45, 26));
        lineNum.setHorizontalAlignment(SwingConstants.RIGHT);
        
        // Content
        JLabel content = new JLabel(line.content.isEmpty() ? " " : line.content);
        content.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        content.setBorder(JBUI.Borders.emptyLeft(10));
        content.setForeground(new Color(220, 220, 220)); // Default white/gray text
        
        // Style based on type
        switch (line.type) {
            case REMOVED:
                panel.setBackground(REMOVED_BG);
                content.setForeground(REMOVED_TEXT);
                break;
            case ADDED:
                panel.setBackground(ADDED_BG);
                content.setForeground(ADDED_TEXT);
                break;
        }
        
        panel.add(lineNum, BorderLayout.WEST);
        panel.add(content, BorderLayout.CENTER);
        
        return panel;
    }
    
    private void applyChanges() {
        WriteCommandAction.runWriteCommandAction(project, () -> {
            Document document = editor.getDocument();
            document.replaceString(selectionStart, selectionEnd, improvedCode);
        });
        
        Messages.showInfoMessage(project, "Changes applied successfully!", "Apply Changes");
        closeTab();
    }
    
    private List<DiffLine> generateDiff(String original, String improved) {
        List<DiffLine> result = new ArrayList<>();
        String[] origLines = original.split("\n", -1);
        String[] newLines = improved.split("\n", -1);
        
        int lineNum = 1;
        int i = 0, j = 0;
        
        while (i < origLines.length || j < newLines.length) {
            if (i >= origLines.length) {
                result.add(new DiffLine(lineNum++, DiffType.ADDED, newLines[j]));
                j++;
            } else if (j >= newLines.length) {
                result.add(new DiffLine(lineNum++, DiffType.REMOVED, origLines[i]));
                i++;
            } else if (origLines[i].equals(newLines[j])) {
                result.add(new DiffLine(lineNum++, DiffType.UNCHANGED, origLines[i]));
                i++;
                j++;
            } else {
                int foundAt = findInArray(origLines[i], newLines, j + 1);
                int foundOrigAt = findInArray(newLines[j], origLines, i + 1);
                
                if (foundOrigAt != -1 && (foundAt == -1 || foundOrigAt < foundAt)) {
                    result.add(new DiffLine(lineNum++, DiffType.ADDED, newLines[j]));
                    j++;
                } else {
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

    /**
     * Custom Button with Blue Outline and Transparent Background
     */
    private static class OutlinedButton extends JButton {
        private final Color accentColor;
        
        public OutlinedButton(String text, Color accentColor) {
            super(text);
            this.accentColor = accentColor;
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setForeground(accentColor);
            setFont(getFont().deriveFont(Font.BOLD));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Draw Outline
            g2.setColor(accentColor);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 8, 8);
            
            // Hover effect (slight fill)
            if (getModel().isRollover()) {
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.1f));
                g2.fillRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 8, 8);
            }
            
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
