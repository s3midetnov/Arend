package org.arend.highlight

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.util.TextRange
import org.arend.IArendFile
import org.arend.ext.error.GeneralError
import java.io.File

abstract class FileBasePass(override val file: IArendFile, editor: Editor, name: String, override val textRange: TextRange)
  : BasePass(file, editor, name, textRange) {
  val fileForErrors = File("/Users/artem.semidetnov/Dev/mcpArendServer/src/main/kotlin/errorList.txt")
  override fun report(error: GeneralError) {
    errorList.add(error)
    fileForErrors.appendText(error.toString() + "\n")
  }
}