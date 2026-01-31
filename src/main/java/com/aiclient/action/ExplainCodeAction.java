package com.aiclient.action;

import com.aiclient.ui.AiTabManager;
import com.aiclient.ui.ChatPanel;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * Action to ask the AI Chat to explain the selected code.
 */
public class ExplainCodeAction extends AnAction {

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        
        if (project == null || editor == null) {
            Messages.showErrorDialog("No editor available", "Explain Code");
            return;
        }

        // 1. Get Code Selection
        String code = editor.getSelectionModel().getSelectedText();
        if (code == null || code.isBlank()) {
            Messages.showWarningDialog("Please select some code to explain.", "Explain Code");
            return;
        }

        // 2. Open Tool Window & Chat Tab
        AiTabManager tabManager = AiTabManager.getInstance(project);
        tabManager.focusChatTab(); // Ensures tool window is visible and Chat tab is selected
        
        // 3. Find ChatPanel instance (Robust search)
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("AI Chat");
        if (toolWindow != null) {
            ChatPanel chatPanel = null;
            
            // Iterate all contents to find ChatPanel
            for (Content content : toolWindow.getContentManager().getContents()) {
                if (content.getComponent() instanceof ChatPanel) {
                    chatPanel = (ChatPanel) content.getComponent();
                    toolWindow.getContentManager().setSelectedContent(content); // Ensure it's focused
                    break;
                }
            }

            if (chatPanel != null) {
                // 4. Send Message (must be on EDT)
                ChatPanel finalChatPanel = chatPanel;
                ApplicationManager.getApplication().invokeLater(() -> {
                    String prompt = "Explain this code:\n```\n" + code + "\n```";
                    finalChatPanel.handleUserMessage(prompt);
                });
            } else {
                Messages.showErrorDialog("Could not find AI Chat panel. Please try closing/reopening the tool window.", "Error");
            }
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        boolean hasSelection = editor != null && editor.getSelectionModel().hasSelection();
        e.getPresentation().setEnabledAndVisible(hasSelection);
    }
}
