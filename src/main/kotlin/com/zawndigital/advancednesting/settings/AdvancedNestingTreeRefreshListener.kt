package com.zawndigital.advancednesting.settings

import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

/**
 * Subscribes to settings changes and refreshes the project view.
 *
 * Registered as a project-level postStartupActivity to ensure it's initialized
 * when the project opens.
 */
class AdvancedNestingTreeRefreshListener : ProjectActivity {

    override suspend fun execute(project: Project) {
        // Subscribe to settings changes
        project.messageBus.connect().subscribe(
            AdvancedNestingSettingsListener.TOPIC,
            object : AdvancedNestingSettingsListener {
                override fun settingsChanged() {
                    // Refresh all project view panes
                    ProjectView.getInstance(project).refresh()
                }
            }
        )
    }
}
