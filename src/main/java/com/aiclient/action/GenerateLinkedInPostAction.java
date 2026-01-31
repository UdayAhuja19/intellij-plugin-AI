package com.aiclient.action;

import com.aiclient.service.AiClientService;
import com.aiclient.ui.AiTabManager;
import com.aiclient.ui.LinkedInPostPanel;
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
 * Action to generate a LinkedIn post from code.
 */
public class GenerateLinkedInPostAction extends AnAction {

    private static final String LINKEDIN_PROMPT = """
        You are a social media expert for software developers.
        Create a professional, engaging LinkedIn post about the following code.
        
        Topic/Focus: %s
        
        The post must include:
        1. A catchy hook/headline.
        2. Brief explanation of the technical concept (for a general tech audience).
        3. Why this approach is interesting or useful.
        4. Relevant hashtags.
        5. A professional but conversational tone.
        
        IMPORTANT: The code snippet will be appended as text at the bottom of the post.
        You can refer to "the code below".
        
        Code:
        ```
        %s
        ```
        
        Return ONLY the post content. Do not include the code snippet itself (it will be added automatically).
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
            Messages.showErrorDialog("No editor available", "LinkedIn Post Generator");
            return;
        }

        // 1. Get Code (Selection or Full File)
        String code = editor.getSelectionModel().getSelectedText();
        boolean isSelection = code != null && !code.isBlank();
        if (!isSelection) {
            code = editor.getDocument().getText();
        }
        
        if (code.isBlank()) {
            Messages.showWarningDialog("Please select some code or open a file first.", "LinkedIn Post Generator");
            return;
        }

        // 2. prompt user for topic
        String topic = Messages.showInputDialog(
            project,
            "What should this post be about? (e.g., 'Optimizing loop performance', 'Clean Architecture pattern')",
            "Generate LinkedIn Post",
            Messages.getQuestionIcon()
        );
        
        if (topic == null || topic.isBlank()) return;

        // 3. Call AI in background
        String finalCode = code;
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Drafting LinkedIn Post...", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                indicator.setText("Asking AI expert...");
                
                try {
                    AiClientService aiService = AiClientService.getInstance(project);
                    String fullPrompt = String.format(LINKEDIN_PROMPT, topic, finalCode);
                    
                    String response = aiService.askAboutCode(finalCode, fullPrompt).get();
                    
                    // 4. Open in Tab
                    ApplicationManager.getApplication().invokeLater(() -> {
                        LinkedInPostPanel postPanel = new LinkedInPostPanel(project, response, finalCode);
                        AiTabManager.getInstance(project).openTab("LinkedIn Post", postPanel);
                    });
                    
                } catch (Exception ex) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        Messages.showErrorDialog("Error generating post: " + ex.getMessage(), "Generation Failed");
                    });
                }
            }
        });
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        // Enable if project and editor are available
        e.getPresentation().setEnabledAndVisible(e.getProject() != null && e.getData(CommonDataKeys.EDITOR) != null);
    }
}
