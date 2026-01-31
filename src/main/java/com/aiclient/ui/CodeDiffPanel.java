package com.aiclient.ui;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * A panel that shows code diff with red/green highlighting.
 * Uses modern UI styling with rounded boxes and blue outlined buttons.
 */
public class CodeDiffPanel extends JPanel {
    
    private final Project project;
    private final Editor editor;
    private final String originalCode;
    private final String improvedCode;
    private final int selectionStart;
    private final int selectionEnd;
    
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
        setBorder(JBUI.Borders.empty(20));
        setBackground(UIStyles.DARK_BG);
        
        // Main container with rounded border
        JPanel mainContainer = new JPanel(new BorderLayout(0, 16));
        mainContainer.setBackground(UIStyles.DARK_BG);
        mainContainer.setBorder(UIStyles.createRoundedBorder(20));
        
        // Top: Header with title and buttons
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        
        JLabel titleLabel = UIStyles.createTitle("Proposed Changes");
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        buttonPanel.setOpaque(false);
        
        JButton discardBtn = UIStyles.createGhostButton("Discard");
        discardBtn.addActionListener(e -> closeTab());
        
        JButton applyBtn = UIStyles.createOutlinedButton("Apply Changes");
        applyBtn.addActionListener(e -> applyChanges());
        
        buttonPanel.add(discardBtn);
        buttonPanel.add(applyBtn);
        
        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(buttonPanel, BorderLayout.EAST);
        
        // Center: Diff panel in rounded box
        JPanel diffContainer = new UIStyles.RoundedPanel(UIStyles.CORNER_RADIUS);
        diffContainer.setBackground(UIStyles.DARKER_BG);
        diffContainer.setLayout(new BorderLayout());
        diffContainer.setBorder(JBUI.Borders.empty(12));
        
        JPanel diffContent = new JPanel();
        diffContent.setLayout(new BoxLayout(diffContent, BoxLayout.Y_AXIS));
        diffContent.setBackground(UIStyles.DARKER_BG);
        
        List<DiffLine> diffLines = generateDiff(originalCode, improvedCode);
        for (DiffLine line : diffLines) {
            diffContent.add(createDiffLinePanel(line));
        }
        diffContent.add(Box.createVerticalGlue());
        
        JBScrollPane scrollPane = new JBScrollPane(diffContent);
        scrollPane.setBorder(JBUI.Borders.empty());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getViewport().setBackground(UIStyles.DARKER_BG);
        
        diffContainer.add(scrollPane, BorderLayout.CENTER);
        
        mainContainer.add(headerPanel, BorderLayout.NORTH);
        mainContainer.add(diffContainer, BorderLayout.CENTER);
        
        add(mainContainer, BorderLayout.CENTER);
    }
    
    private void closeTab() {
        AiTabManager.getInstance(project).closeTab(this);
    }
    
    private JPanel createDiffLinePanel(DiffLine line) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        panel.setPreferredSize(new Dimension(0, 26));
        panel.setBackground(UIStyles.DARKER_BG);
        
        JLabel lineNum = new JLabel(String.format("%4d", line.lineNumber));
        lineNum.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        lineNum.setForeground(UIStyles.TEXT_SECONDARY);
        lineNum.setBorder(JBUI.Borders.empty(0, 8, 0, 8));
        lineNum.setPreferredSize(new Dimension(45, 26));
        lineNum.setHorizontalAlignment(SwingConstants.RIGHT);
        
        JLabel content = new JLabel(line.content.isEmpty() ? " " : line.content);
        content.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        content.setBorder(JBUI.Borders.emptyLeft(10));
        content.setForeground(UIStyles.TEXT_PRIMARY);
        
        switch (line.type) {
            case REMOVED:
                panel.setBackground(UIStyles.REMOVED_BG);
                content.setForeground(UIStyles.REMOVED_TEXT);
                break;
            case ADDED:
                panel.setBackground(UIStyles.ADDED_BG);
                content.setForeground(UIStyles.ADDED_TEXT);
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
                result.add(new DiffLine(lineNum++, DiffType.ADDED, newLines[j++]));
            } else if (j >= newLines.length) {
                result.add(new DiffLine(lineNum++, DiffType.REMOVED, origLines[i++]));
            } else if (origLines[i].equals(newLines[j])) {
                result.add(new DiffLine(lineNum++, DiffType.UNCHANGED, origLines[i]));
                i++; j++;
            } else {
                int foundAt = findInArray(origLines[i], newLines, j + 1);
                int foundOrigAt = findInArray(newLines[j], origLines, i + 1);
                
                if (foundOrigAt != -1 && (foundAt == -1 || foundOrigAt < foundAt)) {
                    result.add(new DiffLine(lineNum++, DiffType.ADDED, newLines[j++]));
                } else {
                    result.add(new DiffLine(lineNum++, DiffType.REMOVED, origLines[i++]));
                    result.add(new DiffLine(lineNum++, DiffType.ADDED, newLines[j++]));
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
    
    private enum DiffType { UNCHANGED, ADDED, REMOVED }
    
    private static class DiffLine {
        final int lineNumber;
        final DiffType type;
        final String content;
        DiffLine(int n, DiffType t, String c) { lineNumber = n; type = t; content = c; }
    }
}
