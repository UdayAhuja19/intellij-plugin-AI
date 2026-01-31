package com.aiclient.ui;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;

/**
 * Panel for LinkedIn post generation.
 * Uses modern UI styling with rounded boxes and blue outlined buttons.
 */
public class LinkedInPostPanel extends JPanel {
    
    private final Project project;
    private final String postContent;
    private final String codeSnippet;
    
    public LinkedInPostPanel(Project project, String postContent, String codeSnippet) {
        super(new BorderLayout());
        this.project = project;
        this.postContent = postContent;
        this.codeSnippet = codeSnippet;
        
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
        
        JLabel titleLabel = UIStyles.createTitle("LinkedIn Post Draft");
        
        JButton discardBtn = UIStyles.createGhostButton("Discard");
        discardBtn.addActionListener(e -> closeTab());
        
        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(discardBtn, BorderLayout.EAST);
        
        // Content - Text area in rounded box
        String fullContent = postContent + "\n\n" + codeSnippet;
        
        JPanel textContainer = new UIStyles.RoundedPanel(UIStyles.CORNER_RADIUS);
        textContainer.setBackground(UIStyles.CARD_BG);
        textContainer.setLayout(new BorderLayout());
        textContainer.setBorder(JBUI.Borders.empty(16));
        
        JBTextArea textArea = new JBTextArea(fullContent);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setEditable(true);
        textArea.setBackground(UIStyles.CARD_BG);
        textArea.setForeground(UIStyles.TEXT_PRIMARY);
        textArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        textArea.setBorder(JBUI.Borders.empty());
        textArea.setCaretColor(UIStyles.ACCENT_BLUE);
        
        JBScrollPane scrollPane = new JBScrollPane(textArea);
        scrollPane.setBorder(JBUI.Borders.empty());
        scrollPane.getViewport().setBackground(UIStyles.CARD_BG);
        
        textContainer.add(scrollPane, BorderLayout.CENTER);
        
        // Bottom buttons  
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(JBUI.Borders.emptyTop(16));
        
        JButton copyBtn = UIStyles.createGhostButton("Copy Text");
        copyBtn.addActionListener(e -> copyToClipboard(textArea.getText()));
        
        JButton openBtn = UIStyles.createOutlinedButton("Open LinkedIn");
        openBtn.addActionListener(e -> openLinkedIn(textArea.getText()));
        
        buttonPanel.add(copyBtn);
        buttonPanel.add(openBtn);
        
        mainContainer.add(headerPanel, BorderLayout.NORTH);
        mainContainer.add(textContainer, BorderLayout.CENTER);
        mainContainer.add(buttonPanel, BorderLayout.SOUTH);
        
        add(mainContainer, BorderLayout.CENTER);
    }
    
    private void closeTab() {
        AiTabManager.getInstance(project).closeTab(this);
    }
    
    private void copyToClipboard(String text) {
        StringSelection selection = new StringSelection(text);
        com.intellij.openapi.ide.CopyPasteManager.getInstance().setContents(selection);
        Messages.showInfoMessage(project, "Post text copied to clipboard!", "Copied");
    }
    
    private void openLinkedIn(String text) {
        try {
            StringSelection selection = new StringSelection(text);
            com.intellij.openapi.ide.CopyPasteManager.getInstance().setContents(selection);
            
            BrowserUtil.browse("https://www.linkedin.com/feed/?shareActive=true");
            
            new Thread(() -> {
                try {
                    Thread.sleep(3000);
                    
                    Robot robot = new Robot();
                    robot.setAutoDelay(100);
                    
                    boolean isMac = System.getProperty("os.name").toLowerCase().contains("mac");
                    int mod = isMac ? KeyEvent.VK_META : KeyEvent.VK_CONTROL;
                    
                    robot.keyPress(mod);
                    Thread.sleep(100);
                    robot.keyPress(KeyEvent.VK_V);
                    Thread.sleep(100);
                    robot.keyRelease(KeyEvent.VK_V);
                    Thread.sleep(100);
                    robot.keyRelease(mod);
                    
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
            
            Messages.showInfoMessage(project, 
                "<html>Opening LinkedIn...<br><b>Auto-Pasting in 3 seconds...</b></html>", 
                "LinkedIn Ready");
                
        } catch (Exception e) {
            Messages.showWarningDialog(project, "Error: " + e.getMessage(), "Error");
        }
    }
}
