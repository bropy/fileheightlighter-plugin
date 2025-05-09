package org.bropy.filehightlight

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.ide.AppLifecycleListener
import org.bropy.filehightlight.settings.PackageHighlighterSettings

/**
 * Main plugin class for Package Highlighter.
 * Handles initialization and provides service methods for the plugin.
 */
@Service(Service.Level.PROJECT)
class PackageHighlighterPlugin(private val project: Project) {

    /**
     * Activates the tool window if enabled in settings
     */
    fun activateToolWindowIfEnabled() {
        // Get application-level settings service properly
        val settings = ApplicationManager.getApplication().getService(PackageHighlighterSettings::class.java)

        if (settings.isEnabled) {
            val toolWindowManager = ToolWindowManager.getInstance(project)
            val toolWindow = toolWindowManager.getToolWindow("FileHightLight")
            toolWindow?.show()
        }
    }

    companion object {
        /**
         * Gets the plugin instance for the specified project
         */
        fun getInstance(project: Project): PackageHighlighterPlugin {
            return project.getService(PackageHighlighterPlugin::class.java)
        }
    }
}

/**
 * Listener for application lifecycle events
 * This is compatible across a wide range of IntelliJ versions
 */
class PackageHighlighterLifecycleListener : AppLifecycleListener {
    override fun appFrameCreated(commandLineArgs: MutableList<String>) {
        // Delayed execution to ensure UI components are loaded
        ApplicationManager.getApplication().invokeLater {
            // Activate tool windows for all open projects
            for (project in ProjectManager.getInstance().openProjects) {
                if (!project.isDisposed) {
                    val plugin = PackageHighlighterPlugin.getInstance(project)
                    plugin.activateToolWindowIfEnabled()
                }
            }
        }
    }
}