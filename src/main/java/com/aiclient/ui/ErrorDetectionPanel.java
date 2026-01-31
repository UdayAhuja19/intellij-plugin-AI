package com.aiclient.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Panel to manage "Error Detection" workflow: Mode Selection -> Learning/Solution.
 * 
 * Flow:
 * 1. User sees "Choose Mode" (Learning vs Solution).
 * 2. Learning Mode: Shows Hint 1 -> Hint 2 -> Hint 3 -> Solution.
 * 3. Solution Mode: Shows Solution directly.
 * 4. Apply Button: Applies the fix.
 */
public class ErrorDetectionPanel extends JPanel {

    private final Project project;
    private final Editor editor;
    private final String originalCode;
    
    // Parsed Data
    private String hint1 = "No hint available.";
    private String hint2 = "No hint available.";
    private String hint3 = "No hint available.";
    private String solutionCode = "";

    // UI State
    private int currentHintIndex = 0;
    
    // Colors (Dark Theme)
    private static final Color DARK_BG = new JBColor(new Color(30, 30, 30), new Color(30, 30, 30));
    private static final Color TEXT_COLOR = new JBColor(Color.WHITE, Color.WHITE);
    private static final Color ACCENT_BLUE = new JBColor(new Color(53, 116, 240), new Color(53, 116, 240));

    private JPanel contentPanel;

    public ErrorDetectionPanel(Project project, Editor editor, String originalCode, String aiResponse) {
        super(new BorderLayout());
        this.project = project;
        this.editor = editor;
        this.originalCode = originalCode;
        
        parseResponse(aiResponse);
        initUI();
    }

    private void parseResponse(String response) {
        hint1 = extractSection(response, "HINT1");
        hint2 = extractSection(response, "HINT2");
        hint3 = extractSection(response, "HINT3");
        solutionCode = extractCodeBlock(extractSection(response, "SOLUTION"));
        
        // Fallback: If parsing failed completely (no hints found), assume the whole response is the solution/explanation
        if (hint1.equals("Not found.") && solutionCode.isEmpty()) {
            hint1 = "Could not parse structured hints. See full response below.";
            hint2 = "See full response below.";
            hint3 = "See full response below.";
            solutionCode = response;
        } else if (solutionCode.isEmpty() || solutionCode.equals("Not found.")) {
             // If we found hints but no solution block, maybe the AI messed up the code block
             solutionCode = response; 
        }
    }

    private String extractSection(String response, String tag) {
        Pattern pattern = Pattern.compile("\\[" + tag + "\\](.*?)(?=\\[|$)", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(response);
        return matcher.find() ? matcher.group(1).trim() : "Not found.";
    }

    private String extractCodeBlock(String text) {
        // Logic copied exactly from ImproveCodeAction (user verified this works)
        if (text == null) return "";
        
        int start = text.indexOf("```");
        if (start == -1) return text.trim();
        
        // Skip language identifier line (e.g. ```java)
        int codeStart = text.indexOf('\n', start);
        if (codeStart == -1) return text.trim();
        codeStart++; // Move past newline
        
        int end = text.indexOf("```", codeStart);
        if (end == -1) return text.substring(codeStart).trim();
        
        return text.substring(codeStart, end).trim();
    }

    private void initUI() {
        setBackground(DARK_BG);
        setBorder(JBUI.Borders.empty(16));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(DARK_BG);
        
        JLabel title = new JLabel("Error Detection");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(TEXT_COLOR);
        
        JButton closeBtn = createButton("Dismiss", false);
        closeBtn.addActionListener(e -> AiTabManager.getInstance(project).closeTab(this));
        
        header.add(title, BorderLayout.WEST);
        header.add(closeBtn, BorderLayout.EAST);
        
        add(header, BorderLayout.NORTH);

        // Content Area (Card Layout or Swappable)
        contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(DARK_BG);
        contentPanel.setBorder(JBUI.Borders.emptyTop(20));
        
        add(contentPanel, BorderLayout.CENTER);

        // Skip mode selection (already chosen in DetectErrorsAction), go directly to Learning Mode
        showLearningMode();
    }

    private void showModeSelection() {
        contentPanel.removeAll();
        
        JPanel buttonContainer = new JPanel(new GridLayout(2, 1, 0, 20));
        buttonContainer.setBackground(DARK_BG);
        buttonContainer.setBorder(JBUI.Borders.empty(40));

        JButton solutionModeBtn = createButton("Solution Mode (Direct Fix)", true);
        solutionModeBtn.addActionListener(e -> showSolutionMode());
        
        JButton learningModeBtn = createButton("Learning Mode (Get Hints)", true);
        learningModeBtn.addActionListener(e -> showLearningMode());

        buttonContainer.add(solutionModeBtn);
        buttonContainer.add(learningModeBtn);

        contentPanel.add(buttonContainer, BorderLayout.CENTER);
        refreshUI();
    }

    private void showLearningMode() {
        contentPanel.removeAll();
        currentHintIndex = 1;
        
        updateLearningView();
    }
    
    private void updateLearningView() {
        contentPanel.removeAll();

        String hintText = switch (currentHintIndex) {
            case 1 -> hint1;
            case 2 -> hint2;
            case 3 -> hint3;
            default -> "All hints revealed. See solution below.";
        };

        // Hint Area
        JTextArea hintArea = createTextArea(hintText);
        hintArea.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        hintArea.setBorder(JBUI.Borders.compound(
            JBUI.Borders.customLine(ACCENT_BLUE, 2),
            JBUI.Borders.empty(15)
        ));

        // Navigation
        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        navPanel.setBackground(DARK_BG);
        
        JLabel status = new JLabel("Hint " + currentHintIndex + "/3 ");
        status.setForeground(Color.GRAY);
        navPanel.add(status);

        if (currentHintIndex < 3) {
            JButton nextBtn = createButton("Next Hint ->", true);
            nextBtn.addActionListener(e -> {
                currentHintIndex++;
                updateLearningView(); // Recursion-like UI refresh
            });
            navPanel.add(nextBtn);
        } else {
            JButton revealBtn = createButton("Reveal Solution", true);
            revealBtn.addActionListener(e -> showSolutionMode());
            navPanel.add(revealBtn);
        }

        contentPanel.add(new JLabel("Hint #" + currentHintIndex), BorderLayout.NORTH);
        contentPanel.add(hintArea, BorderLayout.CENTER);
        contentPanel.add(navPanel, BorderLayout.SOUTH);
        
        refreshUI();
    }

    private void showSolutionMode() {
        contentPanel.removeAll();
        
        // Show loading state
        JLabel loadingLabel = new JLabel("Fetching solution...");
        loadingLabel.setForeground(TEXT_COLOR);
        loadingLabel.setHorizontalAlignment(SwingConstants.CENTER);
        contentPanel.add(loadingLabel, BorderLayout.CENTER);
        refreshUI();
        
        // Fetch solution using the SAME method as Solution Mode
        com.intellij.openapi.progress.ProgressManager.getInstance().run(
            new com.intellij.openapi.progress.Task.Backgroundable(project, "Getting Solution...", false) {
                @Override
                public void run(@org.jetbrains.annotations.NotNull com.intellij.openapi.progress.ProgressIndicator indicator) {
                    try {
                        com.aiclient.service.AiClientService aiService = 
                            com.aiclient.service.AiClientService.getInstance(project);
                        
                        // Use the EXACT same prompt as Solution Mode
                        String response = aiService.askAboutCode(
                            originalCode, 
                            com.aiclient.action.DetectErrorsAction.SOLUTION_PROMPT
                        ).get();
                        
                        // Check if AI says no errors
                        if (response.trim().toUpperCase().contains("NO_ERRORS") || 
                            response.trim().toUpperCase().contains("NO ERRORS")) {
                            ApplicationManager.getApplication().invokeLater(() -> {
                                showNoErrorsMessage();
                            });
                            return;
                        }
                        
                        // Use the EXACT same extraction logic as Solution Mode
                        String fixedCode = com.aiclient.action.DetectErrorsAction.extractCode(response, originalCode);
                        
                        // Check if code is actually the same (no real changes needed)
                        if (fixedCode.trim().equals(originalCode.trim())) {
                            ApplicationManager.getApplication().invokeLater(() -> {
                                showNoErrorsMessage();
                            });
                            return;
                        }
                        
                        // OPEN CodeDiffPanel - EXACTLY like Solution Mode does
                        ApplicationManager.getApplication().invokeLater(() -> {
                            // Close this panel first
                            AiTabManager.getInstance(project).closeTab(ErrorDetectionPanel.this);
                            // Open CodeDiffPanel - the SAME component Solution Mode uses
                            CodeDiffPanel diffPanel = new CodeDiffPanel(project, editor, originalCode, fixedCode);
                            AiTabManager.getInstance(project).openTab("Error Detection", diffPanel);
                        });
                    } catch (Exception ex) {
                        ApplicationManager.getApplication().invokeLater(() -> {
                            contentPanel.removeAll();
                            JLabel errorLabel = new JLabel("Error: " + ex.getMessage());
                            errorLabel.setForeground(Color.RED);
                            contentPanel.add(errorLabel, BorderLayout.CENTER);
                            refreshUI();
                        });
                    }
                }
            }
        );
    }
    
    private void showNoErrorsMessage() {
        contentPanel.removeAll();
        
        JPanel messagePanel = new JPanel(new BorderLayout());
        messagePanel.setBackground(DARK_BG);
        messagePanel.setBorder(JBUI.Borders.empty(40));
        
        JLabel successIcon = new JLabel("✓");
        successIcon.setFont(new Font("Segoe UI", Font.BOLD, 48));
        successIcon.setForeground(new Color(100, 255, 100));
        successIcon.setHorizontalAlignment(SwingConstants.CENTER);
        
        JLabel message = new JLabel("No syntax or semantic errors found!");
        message.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        message.setForeground(TEXT_COLOR);
        message.setHorizontalAlignment(SwingConstants.CENTER);
        
        JLabel subMessage = new JLabel("The code appears to be correct.");
        subMessage.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subMessage.setForeground(Color.GRAY);
        subMessage.setHorizontalAlignment(SwingConstants.CENTER);
        
        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setBackground(DARK_BG);
        textPanel.add(Box.createVerticalGlue());
        successIcon.setAlignmentX(Component.CENTER_ALIGNMENT);
        message.setAlignmentX(Component.CENTER_ALIGNMENT);
        subMessage.setAlignmentX(Component.CENTER_ALIGNMENT);
        textPanel.add(successIcon);
        textPanel.add(Box.createVerticalStrut(20));
        textPanel.add(message);
        textPanel.add(Box.createVerticalStrut(10));
        textPanel.add(subMessage);
        textPanel.add(Box.createVerticalGlue());
        
        messagePanel.add(textPanel, BorderLayout.CENTER);
        
        contentPanel.add(messagePanel, BorderLayout.CENTER);
        refreshUI();
    }
    
    private void showDiffView() {
        contentPanel.removeAll();
        
        // Use Diff visualization
        JPanel diffView = createDiffPanel(originalCode, solutionCode);
        
        // Bottom Actions
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actionPanel.setBackground(DARK_BG);
        actionPanel.setBorder(JBUI.Borders.emptyTop(10));
        
        JButton applyBtn = createButton("Apply Changes", true);
        applyBtn.addActionListener(e -> applyFix());
        
        actionPanel.add(applyBtn);

        contentPanel.add(new JLabel("Proposed Solution (Red: Removed, Green: Added):"), BorderLayout.NORTH);
        contentPanel.add(diffView, BorderLayout.CENTER);
        contentPanel.add(actionPanel, BorderLayout.SOUTH);
        
        refreshUI();
    }

    private void applyFix() {
        WriteCommandAction.runWriteCommandAction(project, () -> {
            Document doc = editor.getDocument();
            String fullText = doc.getText();
            int start = fullText.indexOf(originalCode);
            if (start != -1) {
                doc.replaceString(start, start + originalCode.length(), solutionCode);
                AiTabManager.getInstance(project).closeTab(this);
            } else {
                // Fallback: Replace selection if original text not found (usually safe)
                int selStart = editor.getSelectionModel().getSelectionStart();
                int selEnd = editor.getSelectionModel().getSelectionEnd();
                if (selStart != selEnd) {
                    doc.replaceString(selStart, selEnd, solutionCode);
                }
                 AiTabManager.getInstance(project).closeTab(this);
            }
        });
    }

    // --- Helpers ---

    private JButton createButton(String text, boolean primary) {
        JButton btn = new JButton(text);
        btn.setFocusPainted(false);
        if (primary) {
            btn.setBackground(ACCENT_BLUE);
            btn.setForeground(Color.WHITE);
            // btn.setBorder(JBUI.Borders.empty(10, 20)); // Padding handled by LAF often
        } else {
            btn.setBackground(new Color(50, 50, 50));
            btn.setForeground(Color.WHITE);
        }
        return btn;
    }
    
    private JBTextArea createTextArea(String text) {
        JBTextArea area = new JBTextArea(text);
        area.setWrapStyleWord(true);
        area.setLineWrap(true);
        area.setEditable(false);
        area.setBackground(DARK_BG);
        area.setForeground(TEXT_COLOR);
        return area;
    }

    private void refreshUI() {
        revalidate();
        repaint();
    }
    
    // --- Diff Coloring Logic ---
    
    private static final Color REMOVED_BG = new JBColor(new Color(60, 20, 20), new Color(60, 20, 20));
    private static final Color ADDED_BG = new JBColor(new Color(20, 60, 20), new Color(20, 60, 20));
    private static final Color REMOVED_TEXT = new JBColor(new Color(255, 100, 100), new Color(255, 100, 100));
    private static final Color ADDED_TEXT = new JBColor(new Color(100, 255, 100), new Color(100, 255, 100));

    private JPanel createDiffPanel(String original, String improved) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(DARK_BG);
        panel.setBorder(JBUI.Borders.customLine(new Color(60, 60, 60), 1));
        
        JPanel diffContent = new JPanel();
        diffContent.setLayout(new BoxLayout(diffContent, BoxLayout.Y_AXIS));
        diffContent.setBackground(DARK_BG);
        
        java.util.List<DiffLine> diffLines = generateDiff(original, improved);
        for (DiffLine line : diffLines) {
            diffContent.add(createDiffLinePanel(line));
        }
        
        diffContent.add(Box.createVerticalGlue());
        
        JBScrollPane scrollPane = new JBScrollPane(diffContent);
        scrollPane.setBorder(JBUI.Borders.empty());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getViewport().setBackground(DARK_BG);
        
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createDiffLinePanel(DiffLine line) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        panel.setPreferredSize(new Dimension(0, 26));
        panel.setBackground(DARK_BG);
        
        JLabel lineNum = new JLabel(String.format("%4d", line.lineNumber));
        lineNum.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        lineNum.setForeground(Color.GRAY);
        lineNum.setBorder(JBUI.Borders.empty(0, 8, 0, 8));
        
        JLabel content = new JLabel(line.content.isEmpty() ? " " : line.content);
        content.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        content.setBorder(JBUI.Borders.emptyLeft(10));
        content.setForeground(new Color(220, 220, 220));
        
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

    private java.util.List<DiffLine> generateDiff(String original, String improved) {
        java.util.List<DiffLine> result = new java.util.ArrayList<>();
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
                result.add(new DiffLine(lineNum++, DiffType.UNCHANGED, origLines[i++]));
                i++; j++;
            } else {
                result.add(new DiffLine(lineNum++, DiffType.REMOVED, origLines[i++]));
                result.add(new DiffLine(lineNum++, DiffType.ADDED, newLines[j++]));
            }
        }
        return result;
    }

    private enum DiffType { UNCHANGED, ADDED, REMOVED }
    
    private static class DiffLine {
        final int lineNumber;
        final DiffType type;
        final String content;
        DiffLine(int n, DiffType t, String c) { lineNumber = n; type = t; content = c; }
    }
}
