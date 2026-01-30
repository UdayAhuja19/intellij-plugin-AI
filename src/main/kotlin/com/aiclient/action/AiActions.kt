package com.aiclient.action

import com.aiclient.service.AiClientService
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBUI
import kotlinx.coroutines.runBlocking
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * Base class for AI code assistance actions.
 */
abstract class BaseAiAction(
    private val actionName: String,
    private val prompt: String
) : AnAction() {
    
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
    
    override fun update(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        val hasSelection = editor?.selectionModel?.hasSelection() == true
        e.presentation.isEnabledAndVisible = hasSelection
    }
    
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val selectedText = editor.selectionModel.selectedText ?: return
        
        if (selectedText.isBlank()) {
            Messages.showWarningDialog(project, "Please select some code first.", actionName)
            return
        }
        
        val aiService = AiClientService.getInstance(project)
        
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, actionName, true) {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true
                indicator.text = "Asking AI..."
                
                val result = runBlocking {
                    aiService.askAboutCode(selectedText, prompt)
                }
                
                ApplicationManager.getApplication().invokeLater {
                    result.fold(
                        onSuccess = { response ->
                            showResultDialog(project, response)
                        },
                        onFailure = { error ->
                            Messages.showErrorDialog(
                                project,
                                "Error: ${error.message}",
                                actionName
                            )
                        }
                    )
                }
            }
        })
    }
    
    private fun showResultDialog(project: com.intellij.openapi.project.Project, response: String) {
        val dialog = object : DialogWrapper(project, true) {
            init {
                title = actionName
                init()
            }
            
            override fun createCenterPanel(): JComponent {
                val textArea = JBTextArea(response).apply {
                    isEditable = false
                    lineWrap = true
                    wrapStyleWord = true
                    border = JBUI.Borders.empty(10)
                }
                
                val scrollPane = JBScrollPane(textArea).apply {
                    preferredSize = Dimension(600, 400)
                }
                
                return JPanel(BorderLayout()).apply {
                    add(scrollPane, BorderLayout.CENTER)
                }
            }
        }
        dialog.show()
    }
}

/**
 * Action to explain selected code.
 */
class ExplainCodeAction : BaseAiAction(
    "Explain Code",
    "Please explain this code in detail. Describe what it does, how it works, and any important concepts or patterns used."
)

/**
 * Action to suggest improvements for selected code.
 */
class ImproveCodeAction : BaseAiAction(
    "Improve Code",
    "Please analyze this code and suggest improvements. Consider performance, readability, best practices, and potential bugs. Provide the improved code with explanations."
)

/**
 * Action to generate documentation for selected code.
 */
class GenerateDocsAction : BaseAiAction(
    "Generate Documentation",
    "Please generate comprehensive documentation for this code. Include descriptions of purpose, parameters, return values, and usage examples where appropriate."
)

/**
 * Action to ask a custom question about code.
 */
open class AskAboutCodeAction : AnAction() {
    
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
    
    override fun update(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        val hasSelection = editor?.selectionModel?.hasSelection() == true
        e.presentation.isEnabledAndVisible = hasSelection
    }
    
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val selectedText = editor.selectionModel.selectedText ?: return
        
        val question = Messages.showInputDialog(
            project,
            "What would you like to know about this code?",
            "Ask About Code",
            Messages.getQuestionIcon()
        )
        
        if (question.isNullOrBlank()) return
        
        val aiService = AiClientService.getInstance(project)
        
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "AI Analysis", true) {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true
                indicator.text = "Asking AI..."
                
                val result = runBlocking {
                    aiService.askAboutCode(selectedText, question)
                }
                
                ApplicationManager.getApplication().invokeLater {
                    result.fold(
                        onSuccess = { response ->
                            showResultDialog(project, response)
                        },
                        onFailure = { error ->
                            Messages.showErrorDialog(
                                project,
                                "Error: ${error.message}",
                                "AI Analysis"
                            )
                        }
                    )
                }
            }
        })
    }
    
    private fun showResultDialog(project: com.intellij.openapi.project.Project, response: String) {
        val dialog = object : DialogWrapper(project, true) {
            init {
                title = "AI Response"
                init()
            }
            
            override fun createCenterPanel(): JComponent {
                val textArea = JBTextArea(response).apply {
                    isEditable = false
                    lineWrap = true
                    wrapStyleWord = true
                    border = JBUI.Borders.empty(10)
                }
                
                val scrollPane = JBScrollPane(textArea).apply {
                    preferredSize = Dimension(600, 400)
                }
                
                return JPanel(BorderLayout()).apply {
                    add(scrollPane, BorderLayout.CENTER)
                }
            }
        }
        dialog.show()
    }
}

/**
 * Quick ask action with keyboard shortcut (Ctrl+Alt+A).
 */
class QuickAskAction : AskAboutCodeAction()
