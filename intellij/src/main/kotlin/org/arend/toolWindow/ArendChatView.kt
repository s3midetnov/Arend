package org.arend.toolWindow
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import java.awt.BorderLayout
import javax.swing.JButton
import javax.swing.JPanel

class ArendChatView {
  // The main panel that holds everything
  val mainPanel = JPanel(BorderLayout())

  private val chatDisplay = JBTextArea().apply {
    isEditable = false
    lineWrap = true
    wrapStyleWord = true
    text = "Welcome to Arend Chat!\n"
  }

  private val inputField = JBTextField()
  private val sendButton = JButton("Send")

  init {
    // Center: The chat history
    mainPanel.add(JBScrollPane(chatDisplay), BorderLayout.CENTER)

    // South: Input field + Send button
    val inputPanel = JPanel(BorderLayout())
    inputPanel.add(inputField, BorderLayout.CENTER)
    inputPanel.add(sendButton, BorderLayout.EAST)

    mainPanel.add(inputPanel, BorderLayout.SOUTH)

    // Logic: Add listener to Button and Enter key
    sendButton.addActionListener { postMessage() }
    inputField.addActionListener { postMessage() }
  }

  private fun postMessage() {
    val text = inputField.text.trim()
    if (text.isNotEmpty()) {
      chatDisplay.append("You: $text\n") // Echo the message
      inputField.text = ""               // Clear input
    }
  }
}