package com.aiclient.ui;

import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import java.awt.*;

/**
 * Shared UI styling utilities for consistent look across all panels.
 * Design: Dark theme with rounded boxes and blue outlined buttons.
 */
public class UIStyles {
    
    // Colors
    public static final Color DARK_BG = new JBColor(new Color(30, 30, 30), new Color(30, 30, 30));
    public static final Color DARKER_BG = new JBColor(new Color(20, 20, 20), new Color(20, 20, 20));
    public static final Color CARD_BG = new JBColor(new Color(40, 40, 40), new Color(40, 40, 40));
    public static final Color BORDER_COLOR = new JBColor(new Color(60, 60, 60), new Color(60, 60, 60));
    public static final Color TEXT_PRIMARY = new JBColor(Color.WHITE, Color.WHITE);
    public static final Color TEXT_SECONDARY = new JBColor(new Color(160, 160, 160), new Color(160, 160, 160));
    public static final Color ACCENT_BLUE = new JBColor(new Color(53, 116, 240), new Color(53, 116, 240));
    
    // Diff colors
    public static final Color REMOVED_BG = new JBColor(new Color(60, 20, 20), new Color(60, 20, 20));
    public static final Color ADDED_BG = new JBColor(new Color(20, 60, 20), new Color(20, 60, 20));
    public static final Color REMOVED_TEXT = new JBColor(new Color(255, 100, 100), new Color(255, 100, 100));
    public static final Color ADDED_TEXT = new JBColor(new Color(100, 255, 100), new Color(100, 255, 100));
    
    // Sizing
    public static final int CORNER_RADIUS = 12;
    public static final int BUTTON_CORNER_RADIUS = 8;
    public static final int PADDING = 16;
    
    /**
     * Creates a rounded border for containers/cards.
     */
    public static Border createRoundedBorder() {
        return new RoundedBorder(BORDER_COLOR, CORNER_RADIUS, PADDING);
    }
    
    /**
     * Creates a rounded border with custom padding.
     */
    public static Border createRoundedBorder(int padding) {
        return new RoundedBorder(BORDER_COLOR, CORNER_RADIUS, padding);
    }
    
    /**
     * Creates a blue outlined button (border only, transparent fill).
     */
    public static JButton createOutlinedButton(String text) {
        return new OutlinedButton(text, ACCENT_BLUE);
    }
    
    /**
     * Creates a secondary/ghost button (no border, just text).
     */
    public static JButton createGhostButton(String text) {
        JButton btn = new JButton(text);
        btn.setForeground(TEXT_SECONDARY);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }
    
    /**
     * Creates a rounded panel/card with dark background and border.
     */
    public static JPanel createRoundedCard() {
        RoundedPanel panel = new RoundedPanel(CORNER_RADIUS);
        panel.setBackground(CARD_BG);
        panel.setBorder(JBUI.Borders.empty(PADDING));
        return panel;
    }
    
    /**
     * Creates a title label with consistent styling.
     */
    public static JLabel createTitle(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 18));
        label.setForeground(TEXT_PRIMARY);
        return label;
    }
    
    /**
     * Creates a subtitle/secondary label.
     */
    public static JLabel createSubtitle(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        label.setForeground(TEXT_SECONDARY);
        return label;
    }
    
    // ========== Custom Components ==========
    
    /**
     * Rounded border with padding.
     */
    public static class RoundedBorder extends AbstractBorder {
        private final Color color;
        private final int radius;
        private final int padding;
        
        public RoundedBorder(Color color, int radius, int padding) {
            this.color = color;
            this.radius = radius;
            this.padding = padding;
        }
        
        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1f));
            g2.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
            g2.dispose();
        }
        
        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(padding, padding, padding, padding);
        }
        
        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            insets.set(padding, padding, padding, padding);
            return insets;
        }
    }
    
    /**
     * Panel with rounded corners and filled background.
     */
    public static class RoundedPanel extends JPanel {
        private final int radius;
        
        public RoundedPanel(int radius) {
            this.radius = radius;
            setOpaque(false);
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            g2.dispose();
            super.paintComponent(g);
        }
        
        @Override
        protected void paintBorder(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(BORDER_COLOR);
            g2.setStroke(new BasicStroke(1f));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            g2.dispose();
        }
    }
    
    /**
     * Blue outlined button with transparent background.
     */
    public static class OutlinedButton extends JButton {
        private final Color accentColor;
        
        public OutlinedButton(String text, Color accentColor) {
            super(text);
            this.accentColor = accentColor;
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setForeground(accentColor);
            setFont(getFont().deriveFont(Font.BOLD, 13f));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(140, 36));
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Hover effect: slight blue fill
            if (getModel().isRollover()) {
                g2.setColor(new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 30));
                g2.fillRoundRect(1, 1, getWidth() - 2, getHeight() - 2, BUTTON_CORNER_RADIUS, BUTTON_CORNER_RADIUS);
            }
            
            // Draw outline
            g2.setColor(accentColor);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, BUTTON_CORNER_RADIUS, BUTTON_CORNER_RADIUS);
            
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
