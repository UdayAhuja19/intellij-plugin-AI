package com.aiclient.action;

import com.aiclient.service.AiClientService;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * AI-powered code assistance actions.
 * 
 * This file contains several IntelliJ actions that allow users to
 * interact with the AI directly from code selections:
 * 
 * - ExplainCodeAction: Explains selected code in detail
 * - ImproveCodeAction: Suggests improvements for selected code
 * - GenerateDocsAction: Generates documentation for selected code
 * - AskAboutCodeAction: Asks a custom question about selected code
 * - QuickAskAction: Same as AskAboutCodeAction, but with keyboard shortcut
 * 
 * All actions require text to be selected in the editor.
 */

// ============================================================================
// BaseAiAction - Abstract base class for AI code actions
// ============================================================================

/**
 * Base class for AI code assistance actions.
 * Provides common functionality for sending selected code to the AI.
 */
abstract class BaseAiAction extends AnAction {
    
    /** The name shown in dialogs and progress indicators */
    private final String actionName;
    
    /** The prompt to send to the AI along with the code */
    private final String prompt;
    
    /**
     * Creates a new AI action.
     * 
     * @param actionName The display name for this action
     * @param prompt The prompt to send with the selected code
     */
    protected BaseAiAction(String actionName, String prompt) {
        this.actionName = actionName;
        this.prompt = prompt;
    }
    
    /**
     * Specifies that update() should run on a background thread.
     */
    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
    
    /**
     * Updates the action's enabled state based on selection.
     * The action is only enabled when text is selected.
     */
    @Override
    public void update(@NotNull AnActionEvent e) {
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        boolean hasSelection = editor != null && editor.getSelectionModel().hasSelection();
        e.getPresentation().setEnabledAndVisible(hasSelection);
    }
    
    /**
     * Performs the action when triggered.
     * Sends the selected code to the AI and displays the response.
     */
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;
        
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        if (editor == null) return;
        
        String selectedText = editor.getSelectionModel().getSelectedText();
        if (selectedText == null || selectedText.isBlank()) {
            Messages.showWarningDialog(project, "Please select some code first.", actionName);
            return;
        }
        
        // Get the AI service
        AiClientService aiService = AiClientService.getInstance(project);
        
        // Run in background with progress indicator
        ProgressManager.getInstance().run(new Task.Backgroundable(project, actionName, true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                indicator.setText("Asking AI...");
                
                try {
                    // Send to AI and wait for response
                    String response = aiService.askAboutCode(selectedText, prompt).get();
                    
                    // Show result on EDT
                    ApplicationManager.getApplication().invokeLater(() -> {
                        showResultDialog(project, response);
                    });
                } catch (Exception ex) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        Messages.showErrorDialog(project, "Error: " + ex.getMessage(), actionName);
                    });
                }
            }
        });
    }
    
    /**
     * Shows the AI response in a dialog.
     */
    private void showResultDialog(Project project, String response) {
        DialogWrapper dialog = new DialogWrapper(project, true) {
            {
                setTitle(actionName);
                init();
            }
            
            @Override
            protected @Nullable JComponent createCenterPanel() {
                JBTextArea textArea = new JBTextArea(response);
                textArea.setEditable(false);
                textArea.setLineWrap(true);
                textArea.setWrapStyleWord(true);
                textArea.setBorder(JBUI.Borders.empty(10));
                
                JBScrollPane scrollPane = new JBScrollPane(textArea);
                scrollPane.setPreferredSize(new Dimension(600, 400));
                
                JPanel panel = new JPanel(new BorderLayout());
                panel.add(scrollPane, BorderLayout.CENTER);
                return panel;
            }
        };
        dialog.show();
    }
}

// ============================================================================
// Concrete Action Implementations
// ============================================================================

/**
 * Action to explain selected code.
 * Provides a detailed explanation of what the code does.
 */
class ExplainCodeAction extends BaseAiAction {
    public ExplainCodeAction() {
        super(
            "Explain Code",
            "Please explain this code in detail. Describe what it does, how it works, " +
            "and any important concepts or patterns used."
        );
    }
}

/**
 * Action to suggest improvements for selected code.
 * Analyzes code for performance, readability, and best practices.
 */
class ImproveCodeAction extends BaseAiAction {
    public ImproveCodeAction() {
        super(
            "Improve Code",
            "Please analyze this code and suggest improvements. Consider performance, " +
            "readability, best practices, and potential bugs. Provide the improved code " +
            "with explanations."
        );
    }
}

/**
 * Action to generate documentation for selected code.
 * Creates comprehensive documentation including descriptions and examples.
 */
class GenerateDocsAction extends BaseAiAction {
    public GenerateDocsAction() {
        super(
            "Generate Documentation",
            "Please generate comprehensive documentation for this code. Include " +
            "descriptions of purpose, parameters, return values, and usage examples " +
            "where appropriate."
        );
    }
}

// ============================================================================
// AskAboutCodeAction - Custom question about code
// ============================================================================

/**
 * Action to ask a custom question about selected code.
 * Prompts the user for a question and sends it to the AI.
 */
class AskAboutCodeAction extends AnAction {
    
    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        boolean hasSelection = editor != null && editor.getSelectionModel().hasSelection();
        e.getPresentation().setEnabledAndVisible(hasSelection);
    }
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;
        
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        if (editor == null) return;
        
        String selectedText = editor.getSelectionModel().getSelectedText();
        if (selectedText == null || selectedText.isBlank()) return;
        
        // Ask user for their question
        String question = Messages.showInputDialog(
            project,
            "What would you like to know about this code?",
            "Ask About Code",
            Messages.getQuestionIcon()
        );
        
        if (question == null || question.isBlank()) return;
        
        // Get the AI service
        AiClientService aiService = AiClientService.getInstance(project);
        
        // Run in background
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "AI Analysis", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                indicator.setText("Asking AI...");
                
                try {
                    String response = aiService.askAboutCode(selectedText, question).get();
                    
                    ApplicationManager.getApplication().invokeLater(() -> {
                        showResultDialog(project, response);
                    });
                } catch (Exception ex) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        Messages.showErrorDialog(project, "Error: " + ex.getMessage(), "AI Analysis");
                    });
                }
            }
        });
    }
    
    /**
     * Shows the AI response in a dialog.
     */
    private void showResultDialog(Project project, String response) {
        DialogWrapper dialog = new DialogWrapper(project, true) {
            {
                setTitle("AI Response");
                init();
            }
            
            @Override
            protected @Nullable JComponent createCenterPanel() {
                JBTextArea textArea = new JBTextArea(response);
                textArea.setEditable(false);
                textArea.setLineWrap(true);
                textArea.setWrapStyleWord(true);
                textArea.setBorder(JBUI.Borders.empty(10));
                
                JBScrollPane scrollPane = new JBScrollPane(textArea);
                scrollPane.setPreferredSize(new Dimension(600, 400));
                
                JPanel panel = new JPanel(new BorderLayout());
                panel.add(scrollPane, BorderLayout.CENTER);
                return panel;
            }
        };
        dialog.show();
    }
}

// ============================================================================
// QuickAskAction - Keyboard shortcut variant
// ============================================================================

/**
 * Quick ask action with keyboard shortcut (Ctrl+Alt+A).
 * Same functionality as AskAboutCodeAction, registered with a shortcut.
 */
class QuickAskAction extends AskAboutCodeAction {
    // Inherits all functionality from AskAboutCodeAction
}
