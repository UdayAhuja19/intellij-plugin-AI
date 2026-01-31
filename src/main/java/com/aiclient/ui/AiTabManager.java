package com.aiclient.ui;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

@Service(Service.Level.PROJECT)
public final class AiTabManager {
    
    private final Project project;
    private static final String TOOL_WINDOW_ID = "FocusFlow";
    
    public AiTabManager(Project project) {
        this.project = project;
    }
    
    public static AiTabManager getInstance(@NotNull Project project) {
        return project.getService(AiTabManager.class);
    }
    
    /**
     * Opens a new closable tab in the AI tool window.
     * 
     * @param title Title of the tab
     * @param component The content component
     */
    public void openTab(String title, JComponent component) {
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(TOOL_WINDOW_ID);
        if (toolWindow == null) return;
        
        // Show tool window if hidden
        if (!toolWindow.isVisible()) {
            toolWindow.show();
        }
        
        ContentManager contentManager = toolWindow.getContentManager();
        ContentFactory contentFactory = ContentFactory.getInstance();
        
        Content content = contentFactory.createContent(component, title, true);
        content.setCloseable(true);
        
        contentManager.addContent(content);
        contentManager.setSelectedContent(content);
    }
    
    /**
     * Closes the tab containing the given component.
     */
    public void closeTab(JComponent component) {
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(TOOL_WINDOW_ID);
        if (toolWindow == null) return;
        
        ContentManager contentManager = toolWindow.getContentManager();
        Content content = contentManager.getContent(component);
        if (content != null) {
            contentManager.removeContent(content, true);
        }
    }
    
    /**
     * Ensures the main Chat tab is visible.
     */
    public void focusChatTab() {
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(TOOL_WINDOW_ID);
        if (toolWindow == null) return;
        
        ContentManager contentManager = toolWindow.getContentManager();
        if (contentManager.getContentCount() > 0) {
            contentManager.setSelectedContent(contentManager.getContent(0));
        }
    }
}
