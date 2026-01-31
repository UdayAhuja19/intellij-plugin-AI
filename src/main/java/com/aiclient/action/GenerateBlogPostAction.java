package com.aiclient.action;

import com.aiclient.service.AiClientService;
import com.aiclient.service.BlogPostService;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.awt.Desktop;
import java.io.File;
import java.util.concurrent.CompletableFuture;

/**
 * Action to generate a blog post from selected code or current file.
 * Creates an HTML documentation page with explanation, code walkthrough, and impact analysis.
 */
public class GenerateBlogPostAction extends AnAction {
    
    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    private static final String BLOG_PROMPT = """
        You are a technical writer creating a blog post about code.
        Analyze the following code and generate content for a blog post.
        
        Format your response with these exact section headers (include the headers):
        
        SUMMARY
        [Write 2-3 sentences explaining what this code does at a high level]
        
        KEY FEATURES
        - [Feature 1]
        - [Feature 2]
        - [Feature 3]
        (list the main features, patterns, or important aspects)
        
        WALKTHROUGH
        [Explain how the code works, walking through the important parts]
        
        IMPACT
        [Explain the purpose and importance of this code, when/why to use it]
        
        Now analyze this code:
        
        ```%s
        %s
        ```
        """;
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        
        if (project == null || editor == null) {
            Messages.showErrorDialog("No editor available", "Generate Blog Post");
            return;
        }
        
        // Get code - selection or full file
        String code = editor.getSelectionModel().getSelectedText();
        if (code == null || code.isBlank()) {
            code = editor.getDocument().getText();
        }
        
        if (code.isBlank()) {
            Messages.showErrorDialog("No code to analyze", "Generate Blog Post");
            return;
        }
        
        // Get language from file extension
        VirtualFile file = FileDocumentManager.getInstance().getFile(editor.getDocument());
        String language = file != null ? getLanguage(file.getExtension()) : "java";
        String fileName = file != null ? file.getName() : "Code";
        
        // Generate blog post in background
        String finalCode = code;
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Generating Blog Post...", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                indicator.setText("Analyzing code with AI...");
                
                AiClientService aiService = AiClientService.getInstance(project);
                String prompt = BLOG_PROMPT.formatted(language, finalCode);
                
                try {
                    // Get AI response
                    CompletableFuture<String> future = aiService.askAboutCode(finalCode, prompt);
                    String aiResponse = future.get();
                    
                    if (aiResponse.startsWith("[Error:")) {
                        ApplicationManager.getApplication().invokeLater(() -> 
                            Messages.showErrorDialog(aiResponse, "Blog Post Generation Failed")
                        );
                        return;
                    }
                    
                    indicator.setText("Generating HTML...");
                    
                    // Generate title from filename
                    String title = generateTitle(fileName);
                    
                    // Create blog post
                    BlogPostService blogService = new BlogPostService(project);
                    String outputPath = blogService.generateBlogPost(title, aiResponse, finalCode, language);
                    
                    // Open in browser
                    ApplicationManager.getApplication().invokeLater(() -> {
                        try {
                            Desktop.getDesktop().browse(new File(outputPath).toURI());
                            Messages.showInfoMessage(
                                "Blog post created at:\n" + outputPath,
                                "Blog Post Generated"
                            );
                        } catch (Exception ex) {
                            Messages.showInfoMessage(
                                "Blog post created at:\n" + outputPath + "\n\nCouldn't open browser automatically.",
                                "Blog Post Generated"
                            );
                        }
                    });
                    
                } catch (Exception ex) {
                    ApplicationManager.getApplication().invokeLater(() -> 
                        Messages.showErrorDialog("Error: " + ex.getMessage(), "Blog Post Generation Failed")
                    );
                }
            }
        });
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        e.getPresentation().setEnabledAndVisible(project != null && editor != null);
    }
    
    private String getLanguage(String extension) {
        if (extension == null) return "java";
        return switch (extension.toLowerCase()) {
            case "java" -> "java";
            case "kt", "kts" -> "kotlin";
            case "py" -> "python";
            case "js", "jsx" -> "javascript";
            case "ts", "tsx" -> "typescript";
            case "go" -> "go";
            case "rs" -> "rust";
            case "c", "h" -> "c";
            case "cpp", "hpp", "cc" -> "cpp";
            case "cs" -> "csharp";
            case "rb" -> "ruby";
            case "php" -> "php";
            case "swift" -> "swift";
            case "scala" -> "scala";
            case "html", "htm" -> "html";
            case "css", "scss", "sass" -> "css";
            case "xml" -> "xml";
            case "json" -> "json";
            case "yaml", "yml" -> "yaml";
            case "md" -> "markdown";
            case "sql" -> "sql";
            case "sh", "bash" -> "bash";
            default -> "java";
        };
    }
    
    private String generateTitle(String fileName) {
        // Remove extension
        int dotIndex = fileName.lastIndexOf('.');
        String name = dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
        
        // Convert camelCase/PascalCase to spaces
        StringBuilder titleBuilder = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (i > 0 && Character.isUpperCase(c)) {
                titleBuilder.append(' ');
            }
            titleBuilder.append(c);
        }
        String baseName = titleBuilder.toString().trim();
        
        // Creative title prefixes
        String[] prefixes = {
            "Deep Dive: ",
            "Mastering ",
            "The Power of ",
            "Inside ",
            "Exploring ",
            "How ",
            "Building with ",
            "Unlocking "
        };
        
        // Pick based on filename hash for consistency
        int index = Math.abs(fileName.hashCode()) % prefixes.length;
        String prefix = prefixes[index];
        
        // Special handling for some prefixes
        if (prefix.equals("How ")) {
            return prefix + baseName + " Works";
        }
        
        return prefix + baseName;
    }
}
