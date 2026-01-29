package org.arend

import com.intellij.codeInsight.inline.completion.*
import com.intellij.codeInsight.inline.completion.elements.InlineCompletionGrayTextElement
import com.intellij.openapi.util.TextRange
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.*

class ArendCopilotProvider : InlineCompletionProvider {
  private val client = HttpClient(CIO) {
    install(ContentNegotiation) {
      json()
    }
    engine {
      requestTimeout = 60000 // 60 seconds
    }
  }

  // 1. Unique ID for this provider
  override val id: InlineCompletionProviderID = InlineCompletionProviderID("SimpleHardcodedCopilot")

  // 2. Enable it for your files (or all files for testing)
  override fun isEnabled(event: InlineCompletionEvent): Boolean {
    println("isEnabled called for event: $event")
    return true // Enabled everywhere for this test!
  }

  override suspend fun getSuggestion(request: InlineCompletionRequest): InlineCompletionSuggestion {
    println("getSuggestion called")
    val document = request.document
    val caretOffset = request.endOffset

    val lineNumber = document.getLineNumber(caretOffset)
    val startLine = maxOf(0, lineNumber - 10)
    val endLine = minOf(document.lineCount - 1, lineNumber + 10)

    val startOffset = document.getLineStartOffset(startLine)
    val endOffset = document.getLineEndOffset(endLine)

    val textBeforeCaret = document.getText(TextRange(startOffset, caretOffset))
    val textAfterCaret = document.getText(TextRange(caretOffset, endOffset))

    try {
      val payload = buildJsonObject {
        put("prefix", textBeforeCaret)
        put("suffix", textAfterCaret)
        put("max_new_tokens", 512)
      }

      val response: HttpResponse = try {
        println("Sending request to server: http://127.0.0.1:9999/completion")
        client.post("http://127.0.0.1:9999/completion") {
          contentType(io.ktor.http.ContentType.Application.Json)
          setBody(payload)
        }
      } catch (e: Exception) {
        if (e is kotlinx.coroutines.CancellationException) {
          println("Request cancelled (likely user continued typing)")
          throw e
        }
        println("Error during request: ${e.message}")
        e.printStackTrace()
        return InlineCompletionSuggestion.Default(flowOf())
      }

      val responseBody = response.body<JsonObject>()
      val completion = responseBody["completion"]?.jsonPrimitive?.content ?: ""

      return InlineCompletionSuggestion.Default(flowOf(InlineCompletionGrayTextElement(completion)))
    } catch (e: Exception) {
      if (e is kotlinx.coroutines.CancellationException) throw e
      return InlineCompletionSuggestion.Default(flowOf())
    }
  }

  // NOTE: InlineCompletionSuggestion is deprecated and will be replaced by InlineCompletionItems in future versions.
}