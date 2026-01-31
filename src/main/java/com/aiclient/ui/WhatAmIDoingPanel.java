package com.aiclient.ui;

import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;

/**
 * Panel to display the "What Am I Doing" analysis.
 * Uses the standard Dark Theme (30, 30, 30).
 */
public class WhatAmIDoingPanel extends JPanel {

    private final Project project;
    private final String analysisText;

    // Standard Theme Colors
    private static final Color DARK_BG = new JBColor(new Color(30, 30, 30), new Color(30, 30, 30));
    private static final Color TEXT_COLOR = new JBColor(Color.WHITE, Color.WHITE);
    private static final Color ACCENT_BLUE = new JBColor(new Color(0, 122, 255), new Color(10, 132, 255));

    public WhatAmIDoingPanel(Project project, String analysisText) {
        super(new BorderLayout());
        this.project = project;
        this.analysisText = analysisText;

        initUI();
    }

    private void initUI() {
        setBorder(JBUI.Borders.empty(16));
        setBackground(DARK_BG);

        // Top: Heading & Close
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(DARK_BG);
        topPanel.setBorder(JBUI.Borders.emptyBottom(16));

        JLabel titleLabel = new JLabel("What You Were Doing");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 18f));
        titleLabel.setForeground(TEXT_COLOR);

        JButton closeButton = new JButton("Dismiss");
        closeButton.addActionListener(e -> closeTab());
        closeButton.setForeground(Color.GRAY);
        closeButton.setContentAreaFilled(false);
        closeButton.setBorderPainted(false);
        closeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        topPanel.add(titleLabel, BorderLayout.WEST);
        topPanel.add(closeButton, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        // Center: Analysis Text
        JBTextArea textArea = new JBTextArea(analysisText);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setEditable(false);
        textArea.setBackground(new Color(45, 45, 48)); // Slightly lighter for contrast
        textArea.setForeground(TEXT_COLOR);
        textArea.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        textArea.setBorder(JBUI.Borders.empty(12));

        JBScrollPane scrollPane = new JBScrollPane(textArea);
        scrollPane.setBorder(JBUI.Borders.empty());
        scrollPane.setBackground(DARK_BG);

        // Container with subtle border
        JPanel textContainer = new JPanel(new BorderLayout());
        textContainer.setBackground(DARK_BG);
        textContainer.setBorder(JBUI.Borders.customLine(new Color(60, 60, 60), 1));
        textContainer.add(scrollPane, BorderLayout.CENTER);

        add(textContainer, BorderLayout.CENTER);
        
        // Bottom: Status/Decor
        JLabel footerLabel = new JLabel("AI Context Analysis");
        footerLabel.setFont(footerLabel.getFont().deriveFont(10f));
        footerLabel.setForeground(Color.GRAY);
        footerLabel.setBorder(JBUI.Borders.emptyTop(8));
        add(footerLabel, BorderLayout.SOUTH);
    }

    private void closeTab() {
        AiTabManager.getInstance(project).closeTab(this);
    }
}
