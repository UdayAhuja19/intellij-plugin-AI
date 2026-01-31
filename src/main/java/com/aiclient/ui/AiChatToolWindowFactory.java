package com.aiclient.ui;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

/**
 * Factory for creating the AI Chat tool window.
 * 
 * This class is registered in plugin.xml as the factory for the "AI Assistant"
 * tool window. When the user opens the tool window, IntelliJ calls
 * createToolWindowContent() to populate it with our ChatPanel.
 * 
 * Implements DumbAware to allow the tool window to be available during
 * indexing (when the IDE is in "dumb" mode).
 * 
 * Registration in plugin.xml:
 * <pre>
 * &lt;toolWindow id="AI Assistant"
 *             secondary="true"
 *             anchor="right"
 *             factoryClass="com.aiclient.ui.AiChatToolWindowFactory"/&gt;
 * </pre>
 */
public class AiChatToolWindowFactory implements ToolWindowFactory, DumbAware {
    
    /**
     * Creates the content for the AI Chat tool window.
     * Called by IntelliJ when the tool window is first opened.
     * 
     * @param project The current project
     * @param toolWindow The tool window to populate
     */
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        // Create the main chat panel
        ChatPanel chatPanel = new ChatPanel(project);
        
        // Wrap the panel in a Content object
        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(chatPanel, "Chat", false);
        content.setCloseable(false);
        
        // Add the content to the tool window
        toolWindow.getContentManager().addContent(content);
    }
    
    /**
     * Determines if the tool window should be available for the given project.
     * Returning true makes it available for all projects.
     * 
     * @param project The project to check
     * @return true to make the tool window available
     */
    @Override
    public boolean shouldBeAvailable(@NotNull Project project) {
        return true;
    }
}
