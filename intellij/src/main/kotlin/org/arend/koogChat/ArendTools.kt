package org.arend.koogChat
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project


@LLMDescription("Tools for interacting with Arend editor")
class ArendTools(private val project: Project) : ToolSet {
  @Tool
  @LLMDescription("Returns the content of the currently open editor")
  fun getOpenEditorContent(): String {
    var content = "No file open"
    ApplicationManager.getApplication().runReadAction {
      content = FileEditorManager.getInstance(project).selectedTextEditor?.document?.text ?: "No file open"
    }
    return content
  }
}