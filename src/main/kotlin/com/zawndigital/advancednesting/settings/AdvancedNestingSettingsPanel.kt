package com.zawndigital.advancednesting.settings

import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.ui.HyperlinkLabel
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import javax.swing.*

/**
 * UI panel for Advanced Nesting settings.
 *
 * Allows users to configure which file extensions support directory nesting.
 */
class AdvancedNestingSettingsPanel(private val project: Project) {

    private val enabledCheckBox = JBCheckBox("Enable Advanced Nesting")
    private val extensionListModel = DefaultListModel<String>()
    private val extensionList = JBList(extensionListModel)
    private val addExtensionField = JBTextField()
    private val addButton = JButton("Add")
    private val removeButton = JButton("Remove")

    var isEnabled: Boolean
        get() = enabledCheckBox.isSelected
        set(value) {
            enabledCheckBox.isSelected = value
        }

    var enabledExtensions: List<String>
        get() = extensionListModel.elements().toList()
        set(value) {
            extensionListModel.clear()
            value.forEach { extensionListModel.addElement(it) }
        }

    fun createPanel(): JPanel {
        setupListeners()

        val helpText = JLabel("<html><b>Nests directories under matching files</b> (e.g., User.php contains User/ directory).<br>" +
                "For standard file-to-file nesting, use the built-in File Nesting settings.</html>").apply {
            foreground = UIUtil.getContextHelpForeground()
        }

        val fileNestingLink = HyperlinkLabel("Open File Nesting settings").apply {
            addHyperlinkListener {
                ShowSettingsUtil.getInstance().showSettingsDialog(project, "File Nesting")
            }
        }

        val helpPanel = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(0, 0, 10, 0)
            add(helpText, BorderLayout.NORTH)
            add(fileNestingLink, BorderLayout.SOUTH)
        }

        val listPanel = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(5)
            add(JBScrollPane(extensionList), BorderLayout.CENTER)

            val buttonPanel = JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                add(removeButton)
                add(Box.createVerticalStrut(5))
            }
            add(buttonPanel, BorderLayout.EAST)
        }

        val addPanel = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(5, 0, 0, 0)
            add(JLabel("File extension (without dot):"), BorderLayout.WEST)
            add(addExtensionField, BorderLayout.CENTER)
            add(addButton, BorderLayout.EAST)
        }

        return FormBuilder.createFormBuilder()
            .addComponent(helpPanel)
            .addComponent(enabledCheckBox)
            .addSeparator()
            .addLabeledComponent("Enabled extensions:", listPanel, true)
            .addComponentToRightColumn(addPanel)
            .addComponentFillVertically(JPanel(), 0)
            .panel
    }

    private fun setupListeners() {
        addButton.addActionListener {
            val extension = addExtensionField.text.trim().lowercase()
            if (extension.isNotEmpty() && !extensionListModel.contains(extension)) {
                extensionListModel.addElement(extension)
                addExtensionField.text = ""
            }
        }

        removeButton.addActionListener {
            extensionList.selectedValue?.let { selected ->
                extensionListModel.removeElement(selected)
            }
        }

        addExtensionField.addActionListener {
            addButton.doClick()
        }
    }
}
