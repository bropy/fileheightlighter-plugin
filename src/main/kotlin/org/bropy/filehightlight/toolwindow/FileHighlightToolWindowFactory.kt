package org.bropy.filehightlight.toolwindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.FormBuilder
import java.awt.*
import javax.swing.*
import javax.swing.table.AbstractTableModel
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.TableCellEditor
import org.bropy.filehightlight.settings.PackageHighlighterSettings
import com.intellij.ui.content.ContentFactory
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.psi.PsiManager
import com.intellij.openapi.vfs.VirtualFile
import java.awt.event.ActionEvent
import javax.swing.AbstractCellEditor

class FileHighlightToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val toolWindowContent = FileHighlightToolWindowContent(project)
        val contentFactory = ContentFactory.SERVICE.getInstance()
        val content = contentFactory.createContent(toolWindowContent.getContent(), "", false)
        toolWindow.contentManager.addContent(content)
    }
}

class FileHighlightToolWindowContent(private val project: Project) {
    private val settings = PackageHighlighterSettings.getInstance()
    private val contentPanel = JPanel(BorderLayout())
    private val enabledCheckbox = JBCheckBox("Enable package highlighting", settings.isEnabled)
    private val useDefaultBrightnessCheckbox = JBCheckBox("Use default brightness settings", settings.useDefaultBrightness)
    private val brightnessSlider = JSlider(JSlider.HORIZONTAL, 0, 100, (settings.brightness * 100).toInt())
    private val saturationSlider = JSlider(JSlider.HORIZONTAL, 5, 40, (settings.saturation * 100).toInt())
    private val packageTableModel = PackageColorTableModel()
    private val packageTable = JBTable(packageTableModel)
    private val sortAlphabeticallyCheckbox = JBCheckBox("Sort packages alphabetically", settings.sortAlphabetically)

    init {
        packageTable.setShowGrid(true)
        packageTable.setDefaultRenderer(Color::class.java, ColorRenderer())
        packageTable.columnModel.getColumn(1).cellEditor = ColorEditor()

        val brightnessPanel = JPanel(BorderLayout())
        brightnessPanel.border = BorderFactory.createTitledBorder("Brightness")
        brightnessSlider.majorTickSpacing = 10
        brightnessSlider.minorTickSpacing = 5
        brightnessSlider.paintTicks = true
        brightnessSlider.paintLabels = true
        brightnessPanel.add(brightnessSlider, BorderLayout.CENTER)

        val saturationPanel = JPanel(BorderLayout())
        saturationPanel.border = BorderFactory.createTitledBorder("Saturation")
        saturationSlider.majorTickSpacing = 10
        saturationSlider.minorTickSpacing = 5
        saturationSlider.paintTicks = true
        saturationSlider.paintLabels = true
        saturationPanel.add(saturationSlider, BorderLayout.CENTER)

        useDefaultBrightnessCheckbox.addActionListener {
            val useDefault = useDefaultBrightnessCheckbox.isSelected
            brightnessSlider.isEnabled = !useDefault
            saturationSlider.isEnabled = !useDefault
        }
        brightnessSlider.isEnabled = !settings.useDefaultBrightness
        saturationSlider.isEnabled = !settings.useDefaultBrightness

        val tablePanel = JPanel(BorderLayout())
        tablePanel.border = BorderFactory.createTitledBorder("Package Color Mappings")

        val buttonPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        val addButton = JButton("Add")
        val removeButton = JButton("Remove")
        val applyButton = JButton("Apply")

        addButton.addActionListener {
            val packageName = JOptionPane.showInputDialog(contentPanel, "Enter package name:")
            if (!packageName.isNullOrBlank()) {
                val colorChooser = JColorChooser(Color.LIGHT_GRAY)
                val result = JOptionPane.showConfirmDialog(
                    contentPanel, colorChooser, "Choose Color",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE
                )
                if (result == JOptionPane.OK_OPTION) {
                    val selectedColor = colorChooser.color
                    val hexColor = String.format("#%02X%02X%02X",
                        selectedColor.red, selectedColor.green, selectedColor.blue)

                    settings.packageColors[packageName] = hexColor
                    packageTableModel.fireTableDataChanged()
                }
            }
        }

        removeButton.addActionListener {
            val selectedRow = packageTable.selectedRow
            if (selectedRow != -1) {
                val packageName = packageTableModel.getPackageAt(selectedRow)
                settings.packageColors.remove(packageName)
                packageTableModel.fireTableDataChanged()
            }
        }

        applyButton.addActionListener {
            settings.isEnabled = enabledCheckbox.isSelected
            settings.useDefaultBrightness = useDefaultBrightnessCheckbox.isSelected
            settings.brightness = brightnessSlider.value.toFloat() / 100f
            settings.saturation = saturationSlider.value.toFloat() / 100f
            settings.sortAlphabetically = sortAlphabeticallyCheckbox.isSelected
            packageTableModel.fireTableDataChanged()
            // Оновлюємо всі відкриті файли
            val openFiles = FileEditorManager.getInstance(project).openFiles
            openFiles.forEach { file ->
                val document = FileDocumentManager.getInstance().getDocument(file) ?: return@forEach
                val editors = EditorFactory.getInstance().getEditors(document, project)
                val psiFile = PsiManager.getInstance(project).findFile(file) ?: return@forEach
                val fqName = psiFile.virtualFile.path

                val matchedEntry = settings.packageColors.entries.find { fqName.contains(it.key) } ?: return@forEach
                val color = Color.decode(matchedEntry.value)

                val attributes = TextAttributes().apply {
                    backgroundColor = color
                }

                for (editor in editors) {
                    val markupModel = editor.markupModel
                    markupModel.removeAllHighlighters()
                    markupModel.addRangeHighlighter(
                        0,
                        editor.document.textLength,
                        0,
                        attributes,
                        HighlighterTargetArea.EXACT_RANGE
                    )
                }
            }

            packageTableModel.fireTableDataChanged()

            JOptionPane.showMessageDialog(contentPanel, "Settings applied and highlighting updated!", "Success", JOptionPane.INFORMATION_MESSAGE)
        }



        buttonPanel.add(addButton)
        buttonPanel.add(removeButton)
        buttonPanel.add(applyButton)

        tablePanel.add(JBScrollPane(packageTable), BorderLayout.CENTER)
        tablePanel.add(buttonPanel, BorderLayout.SOUTH)

        val mainPanel = FormBuilder.createFormBuilder()
            .addComponent(JBLabel("Package Highlighter Settings"))
            .addComponent(enabledCheckbox)
            .addComponent(sortAlphabeticallyCheckbox)
            .addComponent(useDefaultBrightnessCheckbox)
            .addComponent(brightnessPanel)
            .addComponent(saturationPanel)
            .addComponent(tablePanel)
            .addComponentFillVertically(JPanel(), 0)
            .panel

        contentPanel.add(mainPanel, BorderLayout.CENTER)
    }

    fun getContent(): JPanel = contentPanel

    private fun applyHighlightingToAllOpenFiles(project: Project) {
        val openFiles = FileEditorManager.getInstance(project).openFiles
        for (file in openFiles) {
            applyHighlightingToFile(file, project)
        }
    }

    private fun applyHighlightingToFile(file: VirtualFile, project: Project) {
        val psiFile = PsiManager.getInstance(project).findFile(file) ?: return
        val fqName = psiFile.containingDirectory?.virtualFile?.path ?: return

        val document = FileDocumentManager.getInstance().getDocument(file) ?: return
        val matchedEntry = settings.packageColors.entries.find { fqName.contains(it.key) } ?: return
        val color = Color.decode(matchedEntry.value)

        val attributes = TextAttributes().apply {
            backgroundColor = color
        }

        val editors = EditorFactory.getInstance().getEditors(document, project)
        for (editor in editors) {
            val markupModel = editor.markupModel
            markupModel.removeAllHighlighters()
            markupModel.addRangeHighlighter(
                0,
                editor.document.textLength,
                0,
                attributes,
                HighlighterTargetArea.EXACT_RANGE
            )
        }
    }



    private inner class PackageColorTableModel : AbstractTableModel() {
        private val columnNames = arrayOf("Package", "Color")
        private val packages: List<String>
            get() = if (settings.sortAlphabetically) {
                settings.packageColors.keys.sorted()
            } else {
                settings.packageColors.keys.toList()
            }

        override fun getRowCount(): Int = settings.packageColors.size
        override fun getColumnCount(): Int = 2
        override fun getColumnName(column: Int): String = columnNames[column]

        override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
            val packageName = packages[rowIndex]
            return when (columnIndex) {
                0 -> packageName
                1 -> Color.decode(settings.packageColors[packageName])
                else -> ""
            }
        }

        override fun getColumnClass(columnIndex: Int): Class<*> {
            return when (columnIndex) {
                0 -> String::class.java
                1 -> Color::class.java
                else -> Any::class.java
            }
        }

        override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean = columnIndex == 1

        override fun setValueAt(value: Any, rowIndex: Int, columnIndex: Int) {
            if (columnIndex == 1 && value is Color) {
                val packageName = packages[rowIndex]
                val hexColor = String.format("#%02X%02X%02X", value.red, value.green, value.blue)
                settings.packageColors[packageName] = hexColor
                fireTableCellUpdated(rowIndex, columnIndex)
            }
        }

        fun getPackageAt(rowIndex: Int): String = packages[rowIndex]
    }
}

class ColorRenderer : DefaultTableCellRenderer() {
    init {
        border = BorderFactory.createEmptyBorder(4, 4, 4, 4)
    }

    override fun getTableCellRendererComponent(
        table: JTable, value: Any, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int
    ): JComponent {
        val panel = JPanel()
        panel.preferredSize = Dimension(30, 15)
        if (value is Color) panel.background = value
        return panel
    }
}

class ColorEditor : AbstractCellEditor(), TableCellEditor {
    private val button = JButton()
    private var currentColor: Color = Color.WHITE

    init {
        button.isBorderPainted = false
        button.addActionListener {
            val newColor = JColorChooser.showDialog(button, "Choose Color", currentColor)
            if (newColor != null) {
                currentColor = newColor
                button.background = newColor
            }
            stopCellEditing()
        }
    }

    override fun getTableCellEditorComponent(
        table: JTable, value: Any, isSelected: Boolean, row: Int, column: Int
    ): JComponent {
        currentColor = value as Color
        button.background = currentColor
        return button
    }

    override fun getCellEditorValue(): Any = currentColor
}
