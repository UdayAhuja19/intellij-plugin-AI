package com.aiclient.action;

import com.aiclient.service.AiClientService;
import com.aiclient.ui.AiTabManager;
import com.aiclient.ui.WhatAmIDoingPanel;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;

/**
 * AI Action: Analyzes the last 50 lines of code before the cursor
 * to infer what the developer is currently working on.
 */
public class WhatAmIDoingAction extends AnAction {

    private static final String CONTEXT_PROMPT = """
        Analyze the following code snippet, which represents the context immediately preceding the developer's cursor.
        Based on this code, explain clearly and concisely what the developer is likely trying to achieve or implement right now.
        
        Use a helpful, observant tone. Start with "It looks like you are..." or similar.
        Keep it brief (2-3 sentences max).
        
        Code Context:
        ```
        %s
        ```
        """;

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);

        if (project == null || editor == null) {
            Messages.showErrorDialog("No editor available", "What Am I Doing");
            return;
        }

        // 1. Get Context (Last 50 lines before cursor)
        String contextCode = getPrecedingCode(editor, 50);
        if (contextCode.isBlank()) {
            Messages.showWarningDialog("Not enough code context to analyze.", "What Am I Doing");
            return;
        }

        AiClientService aiService = AiClientService.getInstance(project);

        // 2. Run AI Analysis in Background
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Analyzing Context...", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                indicator.setText("Figuring out what you're doing...");

                try {
                    String prompt = String.format(CONTEXT_PROMPT, contextCode);
                    String response = aiService.askAboutCode(contextCode, prompt).get();

                    // 3. Show Result in Tool Window Tab
                    ApplicationManager.getApplication().invokeLater(() -> {
                        WhatAmIDoingPanel panel = new WhatAmIDoingPanel(project, response);
                        AiTabManager.getInstance(project).openTab("Current Context", panel);
                    });

                } catch (Exception ex) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        Messages.showErrorDialog(project, "Error analyzing context: " + ex.getMessage(), "Error");
                    });
                }
            }
        });
    }

    private String getPrecedingCode(Editor editor, int maxLines) {
        Document document = editor.getDocument();
        int caretOffset = editor.getCaretModel().getOffset();
        int lineNumber = document.getLineNumber(caretOffset);

        int startLine = Math.max(0, lineNumber - maxLines);
        int startOffset = document.getLineStartOffset(startLine);

        return document.getText(new TextRange(startOffset, caretOffset));
    }
}
