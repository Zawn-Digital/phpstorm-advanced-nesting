package com.zawndigital.advancednesting.settings

import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.project.Project
import javax.swing.JComponent

/**
 * Settings panel for Advanced Nesting plugin.
 *
 * Appears under Settings → Editor → Advanced Nesting
 */
class AdvancedNestingConfigurable(private val project: Project) : SearchableConfigurable {

    private var settingsPanel: AdvancedNestingSettingsPanel? = null

    override fun getId(): String = "editor.advanced.nesting"

    override fun getDisplayName(): String = "File & Directory Nesting"

    override fun createComponent(): JComponent {
        settingsPanel = AdvancedNestingSettingsPanel(project)
        return settingsPanel!!.createPanel()
    }

    override fun isModified(): Boolean {
        val settings = AdvancedNestingSettings.instance
        val panel = settingsPanel ?: return false

        return panel.isEnabled != settings.isEnabled ||
                panel.enabledExtensions != settings.enabledExtensions
    }

    override fun apply() {
        val settings = AdvancedNestingSettings.instance
        val panel = settingsPanel ?: return

        settings.isEnabled = panel.isEnabled
        settings.enabledExtensions = panel.enabledExtensions.toMutableList()

        // Trigger project tree refresh
        project.messageBus.syncPublisher(AdvancedNestingSettingsListener.TOPIC).settingsChanged()
    }

    override fun reset() {
        val settings = AdvancedNestingSettings.instance
        settingsPanel?.apply {
            isEnabled = settings.isEnabled
            enabledExtensions = settings.enabledExtensions.toMutableList()
        }
    }

    override fun disposeUIResources() {
        settingsPanel = null
    }
}
