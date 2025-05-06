package org.bropy.filehightlight.settings

import com.intellij.openapi.options.Configurable
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent
import javax.swing.JPanel

class PackageHighlighterSettingsComponent : Configurable {
    private val settings = PackageHighlighterSettings.getInstance()
    private val enabledCheckbox = JBCheckBox("Enable package highlighting", settings.isEnabled)

    // In a full implementation, you would add a table or list to edit package-color mappings

    override fun createComponent(): JComponent {
        return FormBuilder.createFormBuilder()
                .addComponent(JBLabel("Package Highlighter Settings"))
                .addComponent(enabledCheckbox)
                .addComponentFillVertically(JPanel(), 0)
                .panel
    }

    override fun isModified(): Boolean {
        return settings.isEnabled != enabledCheckbox.isSelected
    }

    override fun apply() {
        settings.isEnabled = enabledCheckbox.isSelected
    }

    override fun reset() {
        enabledCheckbox.isSelected = settings.isEnabled
    }

    override fun getDisplayName(): String = "Package Highlighter"
}