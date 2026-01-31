package com.aiclient.ui;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * A panel that shows a generated LinkedIn post.
 * Features a dark theme, copy button, and "Open LinkedIn" button.
 */
public class LinkedInPostPanel extends JPanel {
    
    private final Project project;
    private final String postContent;
    private final String codeSnippet;

    // UI Colors - Matching ChatPanel/CodeDiffPanel
    private static final Color DARK_BG = new JBColor(new Color(30, 30, 30), new Color(30, 30, 30));
    private static final Color ACCENT_BLUE = new JBColor(new Color(0, 122, 255), new Color(10, 132, 255));
    private static final Color TEXT_COLOR = new JBColor(Color.WHITE, Color.WHITE);
    
    public LinkedInPostPanel(Project project, String postContent, String codeSnippet) {
        super(new BorderLayout());
        this.project = project;
        this.postContent = postContent;
        this.codeSnippet = codeSnippet;
        
        initUI();
    }
    
    private void initUI() {
        setBorder(JBUI.Borders.empty(16));
        setBackground(DARK_BG);
        
        // Top: Heading & Close
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(DARK_BG);
        topPanel.setBorder(JBUI.Borders.emptyBottom(16));
        
        JLabel titleLabel = new JLabel("LinkedIn Post Draft");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 18f));
        titleLabel.setForeground(TEXT_COLOR);
        
        JButton closeButton = new JButton("Discard");
        closeButton.addActionListener(e -> closeTab());
        closeButton.setForeground(Color.GRAY);
        closeButton.setContentAreaFilled(false);
        closeButton.setBorderPainted(false);
        closeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        topPanel.add(titleLabel, BorderLayout.WEST);
        topPanel.add(closeButton, BorderLayout.EAST);
        
        add(topPanel, BorderLayout.NORTH);
        
        // Center: Post Content
        // Combine post content with code snippet
        String fullContent = postContent + "\n\n" + codeSnippet;
        
        JBTextArea textArea = new JBTextArea(fullContent);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setEditable(true); // Allow user to tweak
        textArea.setBackground(new Color(45, 45, 48));
        textArea.setForeground(TEXT_COLOR);
        textArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        textArea.setBorder(JBUI.Borders.empty(12));
        textArea.setCaretColor(ACCENT_BLUE);
        
        JBScrollPane scrollPane = new JBScrollPane(textArea);
        scrollPane.setBorder(JBUI.Borders.empty());
        scrollPane.setBackground(DARK_BG);
        
        // Container for text area with rounded border look (simulated)
        JPanel textContainer = new JPanel(new BorderLayout());
        textContainer.setBackground(DARK_BG);
        textContainer.setBorder(JBUI.Borders.customLine(new Color(60, 60, 60), 1));
        textContainer.add(scrollPane, BorderLayout.CENTER);
        
        add(textContainer, BorderLayout.CENTER);
        
        // Bottom: Action Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(JBUI.Borders.emptyTop(16));
        
        JButton copyButton = new JButton("Copy Text");
        copyButton.setForeground(Color.GRAY);
        copyButton.setContentAreaFilled(false);
        copyButton.setBorderPainted(false);
        copyButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        copyButton.addActionListener(e -> copyToClipboard(textArea.getText()));
        
        JButton openButton = new OutlinedButton("Open LinkedIn", ACCENT_BLUE);
        openButton.setPreferredSize(new Dimension(140, 36));
        openButton.addActionListener(e -> openLinkedIn(textArea.getText()));
        
        buttonPanel.add(copyButton);
        buttonPanel.add(openButton);
        
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    private void closeTab() {
        com.aiclient.ui.AiTabManager.getInstance(project).closeTab(this);
    }
    
    private void copyToClipboard(String text) {
        StringSelection selection = new StringSelection(text);
        com.intellij.openapi.ide.CopyPasteManager.getInstance().setContents(selection);
        Messages.showInfoMessage(project, "Post text copied to clipboard!", "Copied");
    }
    
    private void openLinkedIn(String text) {
        try {
            // 1. Copy FULL text to Clipboard
            StringSelection selection = new StringSelection(text);
            com.intellij.openapi.ide.CopyPasteManager.getInstance().setContents(selection);
            
            // 2. Open LinkedIn Feed (Share Modal)
            BrowserUtil.browse("https://www.linkedin.com/feed/?shareActive=true");
            
            // 3. Auto-Paste Magic (Pure Java Robot)
            new Thread(() -> {
                try {
                    // Give browser time to focus and load (3 secs is usually enough)
                    Thread.sleep(3000);
                    
                    Robot robot = new Robot();
                    robot.setAutoDelay(100); // Slower, more reliable typing
                    
                    // Determine modifier (Cmd for Mac, Ctrl for others)
                    boolean isMac = System.getProperty("os.name").toLowerCase().contains("mac");
                    int mod = isMac ? KeyEvent.VK_META : KeyEvent.VK_CONTROL;
                    
                    // Simulate Paste with deliberate delays
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
                "<html>Opening LinkedIn...<br><b>Auto-Pasting in 3 seconds...</b><br><i>(Note: IntelliJ needs Accessibility permission on Mac)</i></html>", 
                "LinkedIn Ready");
                
        } catch (Exception e) {
            Messages.showWarningDialog(project, "Error opening LinkedIn: " + e.getMessage(), "Error");
        }
    }
    
    /**
     * Custom Outlined Button (Same as CodeDiffPanel)
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
            
            g2.setColor(accentColor);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 8, 8);
            
            if (getModel().isRollover()) {
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.1f));
                g2.fillRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 8, 8);
            }
            
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
