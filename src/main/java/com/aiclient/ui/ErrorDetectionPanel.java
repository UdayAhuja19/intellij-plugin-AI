package com.aiclient.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Panel for Learning Mode in Error Detection.
 * Shows progressive hints (Hint 1 -> 2 -> 3 -> Reveal Solution).
 * Uses modern UI styling with rounded boxes and blue outlined buttons.
 */
public class ErrorDetectionPanel extends JPanel {

    private final Project project;
    private final Editor editor;
    private final String originalCode;
    
    // Parsed hints
    private String hint1 = "No hint available.";
    private String hint2 = "No hint available.";
    private String hint3 = "No hint available.";
    
    // UI State
    private int currentHintIndex = 1;
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
        
        // Fallback if parsing fails
        if (hint1.equals("Not found.")) {
            hint1 = "The AI could not generate structured hints. Click 'Reveal Solution' to see the fix.";
            hint2 = "Try examining the code carefully for syntax errors.";
            hint3 = "Common issues: missing semicolons, unclosed brackets, type mismatches.";
        }
    }

    private String extractSection(String response, String tag) {
        Pattern pattern = Pattern.compile("\\[" + tag + "\\](.*?)(?=\\[|$)", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(response);
        return matcher.find() ? matcher.group(1).trim() : "Not found.";
    }

    private void initUI() {
        setBackground(UIStyles.DARK_BG);
        setBorder(JBUI.Borders.empty(20));
        
        // Main container with rounded border
        JPanel mainContainer = new JPanel(new BorderLayout(0, 16));
        mainContainer.setBackground(UIStyles.DARK_BG);
        mainContainer.setBorder(UIStyles.createRoundedBorder(20));
        
        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        
        JLabel titleLabel = UIStyles.createTitle("Learning Mode");
        
        JButton dismissBtn = UIStyles.createGhostButton("Dismiss");
        dismissBtn.addActionListener(e -> AiTabManager.getInstance(project).closeTab(this));
        
        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(dismissBtn, BorderLayout.EAST);
        
        mainContainer.add(headerPanel, BorderLayout.NORTH);
        
        // Content panel (will show hints)
        contentPanel = new JPanel(new BorderLayout());
        contentPanel.setOpaque(false);
        
        mainContainer.add(contentPanel, BorderLayout.CENTER);
        
        add(mainContainer, BorderLayout.CENTER);
        
        // Show first hint
        updateHintView();
    }
    
    private void updateHintView() {
        contentPanel.removeAll();
        
        String hintText = switch (currentHintIndex) {
            case 1 -> hint1;
            case 2 -> hint2;
            case 3 -> hint3;
            default -> "All hints revealed.";
        };
        
        // Hint card container
        JPanel hintCard = new UIStyles.RoundedPanel(UIStyles.CORNER_RADIUS);
        hintCard.setBackground(UIStyles.CARD_BG);
        hintCard.setLayout(new BorderLayout());
        hintCard.setBorder(JBUI.Borders.empty(20));
        
        // Hint header
        JLabel hintLabel = UIStyles.createSubtitle("Hint " + currentHintIndex + " of 3");
        hintLabel.setBorder(JBUI.Borders.emptyBottom(12));
        
        // Hint text
        JTextArea hintArea = new JTextArea(hintText);
        hintArea.setWrapStyleWord(true);
        hintArea.setLineWrap(true);
        hintArea.setEditable(false);
        hintArea.setBackground(UIStyles.CARD_BG);
        hintArea.setForeground(UIStyles.TEXT_PRIMARY);
        hintArea.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        hintArea.setBorder(JBUI.Borders.empty());
        
        JBScrollPane scrollPane = new JBScrollPane(hintArea);
        scrollPane.setBorder(JBUI.Borders.empty());
        scrollPane.getViewport().setBackground(UIStyles.CARD_BG);
        
        hintCard.add(hintLabel, BorderLayout.NORTH);
        hintCard.add(scrollPane, BorderLayout.CENTER);
        
        // Navigation buttons
        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        navPanel.setOpaque(false);
        navPanel.setBorder(JBUI.Borders.emptyTop(16));
        
        // Progress indicator
        JPanel progressPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        progressPanel.setOpaque(false);
        for (int i = 1; i <= 3; i++) {
            JLabel dot = new JLabel("●");
            dot.setForeground(i <= currentHintIndex ? UIStyles.ACCENT_BLUE : UIStyles.BORDER_COLOR);
            progressPanel.add(dot);
        }
        
        if (currentHintIndex < 3) {
            JButton nextBtn = UIStyles.createOutlinedButton("Next Hint →");
            nextBtn.addActionListener(e -> {
                currentHintIndex++;
                updateHintView();
            });
            navPanel.add(nextBtn);
        } else {
            JButton revealBtn = UIStyles.createOutlinedButton("Reveal Solution");
            revealBtn.addActionListener(e -> showSolution());
            navPanel.add(revealBtn);
        }
        
        // Bottom panel with progress and nav
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setOpaque(false);
        bottomPanel.add(progressPanel, BorderLayout.WEST);
        bottomPanel.add(navPanel, BorderLayout.EAST);
        
        contentPanel.add(hintCard, BorderLayout.CENTER);
        contentPanel.add(bottomPanel, BorderLayout.SOUTH);
        
        revalidate();
        repaint();
    }
    
    private void showSolution() {
        contentPanel.removeAll();
        
        // Loading state
        JLabel loadingLabel = new JLabel("Fetching solution...");
        loadingLabel.setForeground(UIStyles.TEXT_PRIMARY);
        loadingLabel.setHorizontalAlignment(SwingConstants.CENTER);
        contentPanel.add(loadingLabel, BorderLayout.CENTER);
        revalidate();
        repaint();
        
        // Fetch solution using same method as Solution Mode
        com.intellij.openapi.progress.ProgressManager.getInstance().run(
            new com.intellij.openapi.progress.Task.Backgroundable(project, "Getting Solution...", false) {
                @Override
                public void run(@org.jetbrains.annotations.NotNull com.intellij.openapi.progress.ProgressIndicator indicator) {
                    try {
                        com.aiclient.service.AiClientService aiService = 
                            com.aiclient.service.AiClientService.getInstance(project);
                        
                        String response = aiService.askAboutCode(
                            originalCode, 
                            com.aiclient.action.DetectErrorsAction.SOLUTION_PROMPT
                        ).get();
                        
                        // Check for NO_ERRORS
                        if (response.trim().toUpperCase().contains("NO_ERRORS") || 
                            response.trim().toUpperCase().contains("NO ERRORS")) {
                            ApplicationManager.getApplication().invokeLater(() -> showNoErrorsMessage());
                            return;
                        }
                        
                        String fixedCode = com.aiclient.action.DetectErrorsAction.extractCode(response, originalCode);
                        
                        if (fixedCode.trim().equals(originalCode.trim())) {
                            ApplicationManager.getApplication().invokeLater(() -> showNoErrorsMessage());
                            return;
                        }
                        
                        // Open CodeDiffPanel - same as Solution Mode
                        ApplicationManager.getApplication().invokeLater(() -> {
                            AiTabManager.getInstance(project).closeTab(ErrorDetectionPanel.this);
                            CodeDiffPanel diffPanel = new CodeDiffPanel(project, editor, originalCode, fixedCode);
                            AiTabManager.getInstance(project).openTab("Error Detection", diffPanel);
                        });
                        
                    } catch (Exception ex) {
                        ApplicationManager.getApplication().invokeLater(() -> {
                            contentPanel.removeAll();
                            JLabel errorLabel = new JLabel("Error: " + ex.getMessage());
                            errorLabel.setForeground(UIStyles.REMOVED_TEXT);
                            contentPanel.add(errorLabel, BorderLayout.CENTER);
                            revalidate();
                            repaint();
                        });
                    }
                }
            }
        );
    }
    
    private void showNoErrorsMessage() {
        contentPanel.removeAll();
        
        JPanel messageCard = new UIStyles.RoundedPanel(UIStyles.CORNER_RADIUS);
        messageCard.setBackground(UIStyles.CARD_BG);
        messageCard.setLayout(new BoxLayout(messageCard, BoxLayout.Y_AXIS));
        messageCard.setBorder(JBUI.Borders.empty(40));
        
        JLabel checkmark = new JLabel("✓");
        checkmark.setFont(new Font("Segoe UI", Font.BOLD, 48));
        checkmark.setForeground(UIStyles.ADDED_TEXT);
        checkmark.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel message = new JLabel("No errors found!");
        message.setFont(new Font("Segoe UI", Font.BOLD, 18));
        message.setForeground(UIStyles.TEXT_PRIMARY);
        message.setAlignmentX(Component.CENTER_ALIGNMENT);
        message.setBorder(JBUI.Borders.emptyTop(16));
        
        JLabel subMessage = new JLabel("The code appears to be correct.");
        subMessage.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subMessage.setForeground(UIStyles.TEXT_SECONDARY);
        subMessage.setAlignmentX(Component.CENTER_ALIGNMENT);
        subMessage.setBorder(JBUI.Borders.emptyTop(8));
        
        messageCard.add(Box.createVerticalGlue());
        messageCard.add(checkmark);
        messageCard.add(message);
        messageCard.add(subMessage);
        messageCard.add(Box.createVerticalGlue());
        
        contentPanel.add(messageCard, BorderLayout.CENTER);
        revalidate();
        repaint();
    }
}
