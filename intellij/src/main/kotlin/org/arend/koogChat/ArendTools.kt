package org.arend.koogChat
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.platform.ide.provisioner.endpoint.AuthTokenResult
import kotlinx.coroutines.runBlocking
import org.arend.ext.error.GeneralError
import org.arend.ext.module.ModuleLocation
import org.arend.ext.module.ModulePath
import org.arend.server.ArendServerService
import org.arend.typechecking.runner.RunnerService
import kotlinx.serialization.json.*
import org.arend.util.findLibrary
import java.io.File

@LLMDescription("Tools for interacting with Arend editor")
class ArendTools(private val project: Project) : ToolSet {

// region Write to file at line tool
  @Tool
  @LLMDescription("Writes content to a file at a specific line. Lines are 1-indexed.")
  fun writeToFileAtLine(
    @LLMDescription("Full path to the file")
    filePath: String,
    @LLMDescription("Line number to insert at (1-indexed)")
    lineNumber: Int,
    @LLMDescription("Content to insert")
    content: String
  ): String {
    var result = "Success"
    ApplicationManager.getApplication().invokeAndWait {
      val virtualFile = LocalFileSystem.getInstance().findFileByIoFile(File(filePath))
      if (virtualFile == null) {
        result = "File not found: $filePath"
        return@invokeAndWait
      }

      WriteCommandAction.runWriteCommandAction(project) {
        val document = com.intellij.openapi.fileEditor.FileDocumentManager.getInstance().getDocument(virtualFile)

        if (document == null) {
          result = "Could not get document for $filePath"
          return@runWriteCommandAction
        }

        val lineCount = document.lineCount
        val line = if (lineNumber <= 0) 0 else if (lineNumber > lineCount) lineCount else lineNumber - 1
        val offset = if (line >= lineCount) document.textLength else document.getLineStartOffset(line)

        val contentToInsert = if (content.endsWith("\n")) content else content + "\n"
        document.insertString(offset, contentToInsert)
        com.intellij.openapi.fileEditor.FileDocumentManager.getInstance().saveDocument(document)
      }
    }
    return result
  } // endregion

// region Get user's open file content tool
  @Tool
  @LLMDescription("Returns the content of the currently open editor")
  fun getOpenEditorContent(): String {
    var content = "No file open"
    ApplicationManager.getApplication().runReadAction {
      val editor = FileEditorManager.getInstance(project).selectedTextEditor
      println("Open file path: ${editor?.virtualFile?.path}")
      content = editor?.document?.text ?: "No file open"
    }
    return content
  }
// endregion

// region Typecheck modules tool
  @Tool
  @LLMDescription("Typechecks the given list of modules and returns the result")
  fun getTypecheckResults(
    @LLMDescription("List of full module paths to typecheck")
    modulePaths : List<String>): String {
    val modules : List<ModuleLocation> = modulePaths.map {
      ModuleLocation(project.name, ModuleLocation.LocationKind.SOURCE, ModulePath(it))
    }
    val runnerService = project.service<RunnerService>()
    val server = project.service<ArendServerService>().server

    // Remove modules from server cache to force re-typechecking
    for (module in modules) {
      server.removeModule(module)
    }
    val currentErrorList = mutableListOf<GeneralError>()
    runBlocking {
      for (module in modules) {
        currentErrorList.addAll(runnerService.runCheckerForList(module))
      }
    }
    return if (currentErrorList.isEmpty()) {
      "Typechecking completed successfully. No errors found."
    } else {
      val result = currentErrorList.joinToString(separator = ",") { it.toString() }
      result
    }
//    return FileEditorManager.getInstance(project).selectedTextEditor?.virtualFile?.name ?: "No file open"
  }
// endregion

// region Get project structure tool
  @Tool
  @LLMDescription("Returns the project structure potentially with small comments on files as a string")
  fun getProjectStructure(): String {
    val server = project.service<ArendServerService>().server
    val result = StringBuilder()

    // Get all registered libraries
    val libraries = server.libraries
    result.appendLine("=== Registered Libraries ===")
    if (libraries.isEmpty()) {
      result.appendLine("No libraries registered.")
    } else {
      for (lib in libraries.sorted()) {
        result.appendLine("- $lib")
      }
    }

    result.appendLine()

// Get all registered modules
    val modules = server.modules
    println("modules = $modules")
    result.appendLine("=== All Modules (${modules.size} total) ===")

    if (modules.isEmpty()) {
      result.appendLine("No modules registered.")
    } else {
      val arendLibJsonSummaries = object {}.javaClass.getResource("/org/arend/aifeatures/storedinfo/arendLibSummaries.json")?.readText()
      val descriptionMap = if (arendLibJsonSummaries != null) {
        parseSummaries(arendLibJsonSummaries).second
      } else {
        result.appendLine("Note: arendLibSummaries.json not found, module descriptions will not be available.")
        emptyMap()
      }
      // Group modules by library for better readability
      val modulesByLibrary = modules.groupBy { it.libraryName }

      for ((libName, libModules) in modulesByLibrary.toSortedMap()) {
        result.appendLine("\n[$libName]")
        for (module in libModules.sortedBy { it.toString() }) {
          val modulePathString = module.modulePath.toString()
          println(modulePathString)
          // Try to find description by full path, path without leading dot, or partial path
          val moduleDescription : String? = descriptionMap[modulePathString]
            ?: descriptionMap[modulePathString.removePrefix(".")]
            ?: descriptionMap.entries.find { (name, _) -> modulePathString.endsWith(name) }?.value

          if (moduleDescription != null){
            result.appendLine("  ${module.modulePath} (description : ${moduleDescription}) (${module.locationKind})")
          } else {
            result.appendLine("  ${module.modulePath} (${module.locationKind})")
          }
        }
      }
    }
    return result.toString()
}
// endregion

  fun parseSummaries(jsonString: String): Pair<String, Map<String, String>> {
    val jsonObject = Json.parseToJsonElement(jsonString).jsonObject
    val version = jsonObject["version"]?.jsonPrimitive?.content ?: "unknown"
    val modulesArray = jsonObject["modules"]?.jsonArray ?: JsonArray(emptyList())

    val modulesMap = modulesArray.associate {
      val moduleObj = it.jsonObject
      val name = moduleObj["name"]?.jsonPrimitive?.content ?: ""
      val summary = moduleObj["summary"]?.jsonPrimitive?.content ?: ""
      name to summary
    }
    return version to modulesMap
  }
}