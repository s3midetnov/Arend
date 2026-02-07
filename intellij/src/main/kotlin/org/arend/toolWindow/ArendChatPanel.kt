package org.arend.toolWindow
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import java.awt.BorderLayout
import javax.swing.JButton
import javax.swing.JPanel

class ArendChatPanel : JPanel(BorderLayout()) {
  // 1. The display area (where messages appear)
  private val chatDisplay = JBTextArea().apply {
    isEditable = false
    lineWrap = true
    wrapStyleWord = true
  }

  // 2. The input area (where you type)
  private val inputField = JBTextField()

  init {
    // Add the display area to the center (wrapped in a scroll pane)
    add(JBScrollPane(chatDisplay), BorderLayout.CENTER)

    // Create a bottom panel for Input + Button
    val bottomPanel = JPanel(BorderLayout())
    val sendButton = JButton("Send")

    bottomPanel.add(inputField, BorderLayout.CENTER)
    bottomPanel.add(sendButton, BorderLayout.EAST)

    add(bottomPanel, BorderLayout.SOUTH)

    // 3. Add logic to the button and Enter key
    sendButton.addActionListener { sendMessage() }
    inputField.addActionListener { sendMessage() } // Allows pressing 'Enter' to send
  }

  private fun sendMessage() {
    val message = inputField.text.trim()
    if (message.isNotEmpty()) {
      // Append message to display
      chatDisplay.append("Me: $message\n")
      // Clear input
      inputField.text = ""
    }
  }
}