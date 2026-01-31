package com.aiclient.ui;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;

/**
 * Panel to display the "What Am I Doing" analysis.
 * Uses modern UI styling with rounded boxes.
 */
public class WhatAmIDoingPanel extends JPanel {

    private final Project project;
    private final String analysisText;

    public WhatAmIDoingPanel(Project project, String analysisText) {
        super(new BorderLayout());
        this.project = project;
        this.analysisText = analysisText;

        initUI();
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
        
        JLabel titleLabel = UIStyles.createTitle("What You Were Doing");
        
        JButton dismissBtn = UIStyles.createGhostButton("Dismiss");
        dismissBtn.addActionListener(e -> closeTab());
        
        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(dismissBtn, BorderLayout.EAST);
        
        // Content - Text in rounded card
        JPanel textCard = new UIStyles.RoundedPanel(UIStyles.CORNER_RADIUS);
        textCard.setBackground(UIStyles.CARD_BG);
        textCard.setLayout(new BorderLayout());
        textCard.setBorder(JBUI.Borders.empty(16));
        
        JBTextArea textArea = new JBTextArea(analysisText);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setEditable(false);
        textArea.setBackground(UIStyles.CARD_BG);
        textArea.setForeground(UIStyles.TEXT_PRIMARY);
        textArea.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        textArea.setBorder(JBUI.Borders.empty());
        
        JBScrollPane scrollPane = new JBScrollPane(textArea);
        scrollPane.setBorder(JBUI.Borders.empty());
        scrollPane.getViewport().setBackground(UIStyles.CARD_BG);
        
        textCard.add(scrollPane, BorderLayout.CENTER);
        
        // Footer
        JLabel footerLabel = UIStyles.createSubtitle("AI Context Analysis");
        footerLabel.setBorder(JBUI.Borders.emptyTop(12));
        
        mainContainer.add(headerPanel, BorderLayout.NORTH);
        mainContainer.add(textCard, BorderLayout.CENTER);
        mainContainer.add(footerLabel, BorderLayout.SOUTH);
        
        add(mainContainer, BorderLayout.CENTER);
    }

    private void closeTab() {
        AiTabManager.getInstance(project).closeTab(this);
    }
}
