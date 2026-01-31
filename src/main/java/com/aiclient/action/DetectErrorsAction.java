package com.aiclient.action;

import com.aiclient.service.AiClientService;
import com.aiclient.ui.AiTabManager;
import com.aiclient.ui.CodeDiffPanel;
import com.aiclient.ui.ErrorDetectionPanel;
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
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;

/**
 * AI Action: Detects errors in selected code.
 * Offers two modes:
 * - Solution Mode: Shows the fixed code directly using CodeDiffPanel.
 * - Learning Mode: Shows progressive hints, then fetches the solution separately.
 */
public class DetectErrorsAction extends AnAction {

    // First, check if there are ANY errors
    private static final String ERROR_CHECK_PROMPT = """
        Analyze this code for ACTUAL ERRORS ONLY.
        
        Errors are: syntax errors, compilation errors, undefined variables, type mismatches, obvious bugs.
        NOT errors: style issues, naming conventions, comments, unused variables, performance.
        
        If the code has NO actual errors that would prevent compilation or cause runtime failures, respond with exactly:
        NO_ERRORS
        
        If the code HAS actual errors, respond with exactly:
        HAS_ERRORS
        
        Code to check:
        """;

    // Prompt for getting the fixed solution
    public static final String SOLUTION_PROMPT = """
        Fix the errors in this code.
        
        CRITICAL: Make MINIMAL changes. Only fix actual bugs/errors.
        - Do NOT change variable names
        - Do NOT change method names  
        - Do NOT reorder code
        - Do NOT add comments
        - Do NOT change formatting/style
        - ONLY fix the actual error
        
        Return ONLY the fixed code in a code block. No explanations.
        
        Code:
        """;

    // Prompt for getting hints ONLY
    private static final String HINTS_PROMPT = """
        This code has an error. Provide 3 hints to help find the bug.
        Do NOT reveal the exact solution.
        
        Format:
        [HINT1] First hint (vague)
        [HINT2] Second hint (more specific)
        [HINT3] Third hint (almost reveals it)
        
        Code:
        """;

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
        if (selectedText == null || selectedText.isBlank()) {
            Messages.showWarningDialog(project, "Please select some code first.", "Detect Errors");
            return;
        }

        // Ask user which mode they want
        int choice = Messages.showDialog(
            project,
            "How would you like to learn from this error?",
            "Error Detection Mode",
            new String[]{"Solution Mode (Direct Fix)", "Learning Mode (Get Hints)"},
            0,
            Messages.getQuestionIcon()
        );

        if (choice == -1) return;

        boolean useLearningMode = (choice == 1);
        
        AiClientService aiService = AiClientService.getInstance(project);

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Detecting Errors...", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);

                try {
                    // FIRST: Check if there are any errors at all
                    indicator.setText("Checking for errors...");
                    String checkResponse = aiService.askAboutCode(selectedText, ERROR_CHECK_PROMPT + selectedText).get();
                    
                    boolean hasErrors = !checkResponse.trim().toUpperCase().contains("NO_ERRORS");
                    
                    if (!hasErrors) {
                        // No errors - show popup and return (for BOTH modes)
                        ApplicationManager.getApplication().invokeLater(() -> {
                            Messages.showInfoMessage(project, 
                                "No syntax or semantic errors found in the selected code.", 
                                "Error Detection");
                        });
                        return;
                    }
                    
                    // There ARE errors - proceed with the selected mode
                    if (useLearningMode) {
                        // Learning Mode: Get hints
                        indicator.setText("Getting hints...");
                        String hintsResponse = aiService.askAboutCode(selectedText, HINTS_PROMPT + selectedText).get();
                        
                        ApplicationManager.getApplication().invokeLater(() -> {
                            ErrorDetectionPanel panel = new ErrorDetectionPanel(project, editor, selectedText, hintsResponse);
                            AiTabManager.getInstance(project).openTab("Error Detection", panel);
                        });
                    } else {
                        // Solution Mode: Get fix
                        indicator.setText("Getting solution...");
                        String response = aiService.askAboutCode(selectedText, SOLUTION_PROMPT).get();
                        String fixedCode = extractCode(response, selectedText);
                        
                        // Double-check: if code is actually the same, no changes needed
                        if (fixedCode.trim().equals(selectedText.trim())) {
                            ApplicationManager.getApplication().invokeLater(() -> {
                                Messages.showInfoMessage(project, 
                                    "No changes needed - code appears correct.", 
                                    "Error Detection");
                            });
                            return;
                        }
                        
                        ApplicationManager.getApplication().invokeLater(() -> {
                            CodeDiffPanel diffPanel = new CodeDiffPanel(project, editor, selectedText, fixedCode);
                            AiTabManager.getInstance(project).openTab("Error Detection", diffPanel);
                        });
                    }

                } catch (Exception ex) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        Messages.showErrorDialog(project, "Error: " + ex.getMessage(), "Detect Errors");
                    });
                }
            }
        });
    }

    /**
     * Extracts code from AI response.
     */
    public static String extractCode(String response, String fallback) {
        int start = response.indexOf("```");
        if (start == -1) return response.trim();

        int codeStart = response.indexOf('\n', start);
        if (codeStart == -1) return response.trim();
        codeStart++;

        int end = response.indexOf("```", codeStart);
        if (end == -1) return response.substring(codeStart).trim();

        return response.substring(codeStart, end).trim();
    }
}
