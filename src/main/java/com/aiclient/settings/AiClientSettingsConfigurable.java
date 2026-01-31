package com.aiclient.settings;

import com.aiclient.model.AiProvider;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import java.awt.*;

/**
 * Modern settings UI panel for AI Client configuration.
 * Accessible via Settings > Tools > AI Client
 * 
 * This class implements IntelliJ's Configurable interface to provide
 * a settings page for the plugin. The UI is organized into sections:
 * - Connection: Provider, API key, endpoint
 * - Model Settings: Model name, temperature, max tokens
 * - Advanced: System prompt, streaming toggle
 * 
 * The panel uses a modern card-based design with rounded borders
 * and proper color theming for both light and dark modes.
 */
public class AiClientSettingsConfigurable implements Configurable {
    
    // ========================================================================
    // UI Components
    // ========================================================================
    
    private JPanel mainPanel;
    private ComboBox<AiProvider> providerCombo;
    private JBPasswordField apiKeyField;
    private JBTextField endpointField;
    private JBTextField modelField;
    private JSpinner temperatureSpinner;
    private JSpinner maxTokensSpinner;
    private JBTextArea systemPromptArea;
    private JCheckBox streamingCheckbox;
    
    // ========================================================================
    // Color Palette (supports light and dark themes)
    // ========================================================================
    
    /** Section card background color */
    private final Color sectionBg = JBColor.namedColor("Panel.background", 
        new JBColor(new Color(248, 249, 252), new Color(48, 50, 55)));
    
    /** Border color for cards and inputs */
    private final Color borderColor = JBColor.namedColor("Borders.color",
        new JBColor(new Color(225, 228, 235), new Color(60, 63, 68)));
    
    /** Accent color for highlights */
    private final Color accentColor = new JBColor(new Color(64, 120, 242), new Color(74, 136, 255));
    
    // ========================================================================
    // Configurable Implementation
    // ========================================================================
    
    /**
     * Returns the display name shown in the settings tree.
     * @return "AI Client"
     */
    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "AI Client";
    }
    
    /**
     * Creates and returns the settings UI panel.
     * This is called once when the settings page is first opened.
     * 
     * @return The main settings panel
     */
    @Nullable
    @Override
    public JComponent createComponent() {
        // Create provider dropdown with custom renderer
        providerCombo = new ComboBox<>(AiProvider.values());
        providerCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, 
                    int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof AiProvider) {
                    setText(((AiProvider) value).getDisplayName());
                }
                setBorder(JBUI.Borders.empty(4, 8));
                return this;
            }
        });
        
        // Update endpoint and model when provider changes
        providerCombo.addActionListener(e -> {
            AiProvider selected = (AiProvider) providerCombo.getSelectedItem();
            if (selected != null) {
                endpointField.setText(selected.getDefaultEndpoint());
                modelField.setText(selected.getDefaultModel());
            }
        });
        
        // API Key field (password field for security)
        apiKeyField = new JBPasswordField();
        apiKeyField.setColumns(40);
        apiKeyField.setBorder(createFieldBorder());
        
        // Endpoint URL field
        endpointField = new JBTextField();
        endpointField.setColumns(40);
        endpointField.setBorder(createFieldBorder());
        
        // Model name field
        modelField = new JBTextField();
        modelField.setColumns(30);
        modelField.setBorder(createFieldBorder());
        
        // Temperature spinner (0.0 to 2.0)
        temperatureSpinner = new JSpinner(new SpinnerNumberModel(0.7, 0.0, 2.0, 0.1));
        temperatureSpinner.setPreferredSize(new Dimension(100, 30));
        
        // Max tokens spinner
        maxTokensSpinner = new JSpinner(new SpinnerNumberModel(2048, 100, 32000, 100));
        maxTokensSpinner.setPreferredSize(new Dimension(120, 30));
        
        // System prompt text area
        systemPromptArea = new JBTextArea(4, 40);
        systemPromptArea.setLineWrap(true);
        systemPromptArea.setWrapStyleWord(true);
        systemPromptArea.setBorder(JBUI.Borders.empty(10));
        systemPromptArea.setFont(UIUtil.getLabelFont().deriveFont(12f));
        JScrollPane systemPromptScroll = new JScrollPane(systemPromptArea);
        systemPromptScroll.setBorder(new RoundedBorder(8, borderColor));
        
        // Streaming toggle checkbox
        streamingCheckbox = new JCheckBox("Enable streaming responses");
        streamingCheckbox.setFont(streamingCheckbox.getFont().deriveFont(Font.PLAIN, 12f));
        streamingCheckbox.setOpaque(false);
        streamingCheckbox.setToolTipText("Show AI responses in real-time as they're generated");
        
        // Build the main panel with sections
        mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(JBUI.Borders.empty(16));
        mainPanel.setBackground(UIUtil.getPanelBackground());
        
        // Connection Section
        mainPanel.add(createSection("Connection", new JComponent[][] {
            {new JBLabel("Provider:"), providerCombo},
            {new JBLabel("API Key:"), apiKeyField},
            {new JBLabel("API Endpoint:"), endpointField}
        }));
        
        mainPanel.add(Box.createVerticalStrut(16));
        
        // Model Settings Section
        mainPanel.add(createSection("Model Settings", new JComponent[][] {
            {new JBLabel("Model:"), modelField},
            {new JBLabel("Temperature:"), temperatureSpinner},
            {new JBLabel("Max Tokens:"), maxTokensSpinner}
        }));
        
        mainPanel.add(Box.createVerticalStrut(16));
        
        // Advanced Section
        mainPanel.add(createSection("Advanced", new JComponent[][] {
            {new JBLabel("System Prompt:"), systemPromptScroll},
            {null, streamingCheckbox}
        }));
        
        mainPanel.add(Box.createVerticalGlue());
        
        // Wrap in scroll pane
        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.setBorder(JBUI.Borders.empty());
        scrollPane.setBackground(UIUtil.getPanelBackground());
        scrollPane.getViewport().setBackground(UIUtil.getPanelBackground());
        
        return scrollPane;
    }
    
    /**
     * Creates a section panel with a title and form rows.
     * 
     * @param title Section header text
     * @param rows Array of [label, component] pairs
     * @return The section panel
     */
    private JPanel createSection(String title, JComponent[][] rows) {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(12, borderColor),
            JBUI.Borders.empty(16)
        ));
        section.setBackground(sectionBg);
        section.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Section header
        JLabel header = new JLabel(title);
        header.setFont(header.getFont().deriveFont(Font.BOLD, 14f));
        header.setForeground(accentColor);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        section.add(header);
        section.add(Box.createVerticalStrut(12));
        
        // Add form rows
        for (int i = 0; i < rows.length; i++) {
            JComponent[] row = rows[i];
            JPanel rowPanel = new JPanel(new BorderLayout(12, 0));
            rowPanel.setOpaque(false);
            rowPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            rowPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
            
            if (row[0] != null) {
                row[0].setPreferredSize(new Dimension(120, 24));
                rowPanel.add(row[0], BorderLayout.WEST);
            }
            rowPanel.add(row[1], BorderLayout.CENTER);
            
            section.add(rowPanel);
            if (i < rows.length - 1) {
                section.add(Box.createVerticalStrut(12));
            }
        }
        
        return section;
    }
    
    /**
     * Creates a border for text fields.
     */
    private javax.swing.border.Border createFieldBorder() {
        return BorderFactory.createCompoundBorder(
            new RoundedBorder(6, borderColor),
            JBUI.Borders.empty(6, 10)
        );
    }
    
    /**
     * Checks if settings have been modified.
     * @return true if any setting differs from stored value
     */
    @Override
    public boolean isModified() {
        AiClientSettings.State state = getSettings().getState();
        return providerCombo.getSelectedItem() != state.provider ||
               !new String(apiKeyField.getPassword()).equals(state.getApiKey()) ||
               !endpointField.getText().equals(state.apiEndpoint) ||
               !modelField.getText().equals(state.model) ||
               !temperatureSpinner.getValue().equals(state.temperature) ||
               !maxTokensSpinner.getValue().equals(state.maxTokens) ||
               !systemPromptArea.getText().equals(state.systemPrompt) ||
               streamingCheckbox.isSelected() != state.streamingEnabled;
    }
    
    /**
     * Applies the current UI values to settings.
     * Called when user clicks "Apply" or "OK".
     */
    @Override
    public void apply() {
        AiClientSettings.State state = getSettings().getState();
        state.provider = (AiProvider) providerCombo.getSelectedItem();
        state.setApiKey(new String(apiKeyField.getPassword()));
        state.apiEndpoint = endpointField.getText();
        state.model = modelField.getText();
        state.temperature = (Double) temperatureSpinner.getValue();
        state.maxTokens = (Integer) maxTokensSpinner.getValue();
        state.systemPrompt = systemPromptArea.getText();
        state.streamingEnabled = streamingCheckbox.isSelected();
    }
    
    /**
     * Resets UI to stored settings values.
     * Called when settings page is opened or user clicks "Reset".
     */
    @Override
    public void reset() {
        AiClientSettings.State state = getSettings().getState();
        providerCombo.setSelectedItem(state.provider);
        apiKeyField.setText(state.getApiKey());
        endpointField.setText(state.apiEndpoint);
        modelField.setText(state.model);
        temperatureSpinner.setValue(state.temperature);
        maxTokensSpinner.setValue(state.maxTokens);
        systemPromptArea.setText(state.systemPrompt);
        streamingCheckbox.setSelected(state.streamingEnabled);
    }
    
    /**
     * Disposes UI resources.
     * Called when settings dialog is closed.
     */
    @Override
    public void disposeUIResources() {
        mainPanel = null;
        providerCombo = null;
        apiKeyField = null;
        endpointField = null;
        modelField = null;
        temperatureSpinner = null;
        maxTokensSpinner = null;
        systemPromptArea = null;
        streamingCheckbox = null;
    }
    
    /**
     * Gets the settings instance.
     */
    private AiClientSettings getSettings() {
        return AiClientSettings.getInstance();
    }
    
    // ========================================================================
    // Custom Border Classes
    // ========================================================================
    
    /**
     * Custom rounded border for modern card-style sections.
     */
    private static class RoundedBorder extends AbstractBorder {
        private final int radius;
        private final Color color;
        
        public RoundedBorder(int radius, Color color) {
            this.radius = radius;
            this.color = color;
        }
        
        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setColor(color);
            g2d.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
        }
        
        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(1, 1, 1, 1);
        }
    }
}
