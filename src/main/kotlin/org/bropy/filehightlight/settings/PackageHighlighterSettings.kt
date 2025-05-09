package org.bropy.filehightlight.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

@Service
@State(
    name = "PackageHighlighterSettings",
    storages = [Storage("package-highlighter.xml")]
)
class PackageHighlighterSettings : PersistentStateComponent<PackageHighlighterSettings> {

    var isEnabled: Boolean = true
    var useDefaultBrightness: Boolean = true
    var brightness: Float = 0.82f  // Default brightness
    var saturation: Float = 0.15f  // Default saturation
    var sortAlphabetically: Boolean = false
    var packageColors: MutableMap<String, String> = mutableMapOf(
        "com.example.model" to "#FFE0E0",  // Light red
        "com.example.view" to "#E0FFE0",   // Light green
        "com.example.controller" to "#E0E0FF"  // Light blue
    )

    override fun getState(): PackageHighlighterSettings = this

    override fun loadState(state: PackageHighlighterSettings) {
        XmlSerializerUtil.copyBean(state, this)
    }

    companion object {
        fun getInstance(): PackageHighlighterSettings {
            return ApplicationManager.getApplication().getService(PackageHighlighterSettings::class.java)
        }
    }
}