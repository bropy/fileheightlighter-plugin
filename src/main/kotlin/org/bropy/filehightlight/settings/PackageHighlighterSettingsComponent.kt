package org.bropy.filehightlight.settings

import com.intellij.openapi.options.Configurable
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.FormBuilder
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.event.ActionEvent
import javax.swing.*
import javax.swing.table.AbstractTableModel
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.TableCellEditor

class PackageHighlighterSettingsComponent : Configurable {
    private val settings = PackageHighlighterSettings.getInstance()
    private val enabledCheckbox = JBCheckBox("Enable package highlighting", settings.isEnabled)
    private val useDefaultBrightnessCheckbox = JBCheckBox("Use default brightness settings", settings.useDefaultBrightness)

    private val brightnessSlider = JSlider(JSlider.HORIZONTAL, 50, 100, (settings.brightness * 100).toInt())
    private val saturationSlider = JSlider(JSlider.HORIZONTAL, 5, 40, (settings.saturation * 100).toInt())
    private val packageTableModel = PackageColorTableModel()
    private val packageTable = JBTable(packageTableModel)
    private val sortAlphabeticallyCheckbox = JBCheckBox("Sort packages alphabetically", settings.sortAlphabetically)

    private val mainPanel: JPanel

    init {
        // Main panel initialization
        mainPanel = FormBuilder.createFormBuilder()
            .addComponent(JBLabel("Package Highlighter Settings"))
            .addComponent(enabledCheckbox)
            .addComponent(sortAlphabeticallyCheckbox)
            .addComponent(useDefaultBrightnessCheckbox)
            .addComponent(JPanel()) // Placeholder for brightness panel
            .addComponent(JPanel()) // Placeholder for saturation panel
            .addComponent(JPanel()) // Placeholder for table panel
            .addComponentFillVertically(JPanel(), 0)
            .panel
        // Configure table
        packageTable.setShowGrid(true)
        packageTable.setDefaultRenderer(Color::class.java, ColorRenderer())
        packageTable.columnModel.getColumn(1).cellEditor = ColorEditor()

        // Set up brightness control panel
        val brightnessPanel = JPanel(BorderLayout())
        brightnessPanel.border = BorderFactory.createTitledBorder("Brightness")
        brightnessSlider.majorTickSpacing = 10
        brightnessSlider.minorTickSpacing = 5
        brightnessSlider.paintTicks = true
        brightnessSlider.paintLabels = true
        brightnessPanel.add(brightnessSlider, BorderLayout.CENTER)

        // Set up saturation control panel
        val saturationPanel = JPanel(BorderLayout())
        saturationPanel.border = BorderFactory.createTitledBorder("Saturation")
        saturationSlider.majorTickSpacing = 10
        saturationSlider.minorTickSpacing = 5
        saturationSlider.paintTicks = true
        saturationSlider.paintLabels = true
        saturationPanel.add(saturationSlider, BorderLayout.CENTER)

        // Configure brightness controls visibility based on checkbox state
        useDefaultBrightnessCheckbox.addActionListener { e: ActionEvent ->
            val useDefault = useDefaultBrightnessCheckbox.isSelected
            brightnessSlider.isEnabled = !useDefault
            saturationSlider.isEnabled = !useDefault
        }
        brightnessSlider.isEnabled = !settings.useDefaultBrightness
        saturationSlider.isEnabled = !settings.useDefaultBrightness

        // Set up table with buttons
        val tablePanel = JPanel(BorderLayout())
        tablePanel.border = BorderFactory.createTitledBorder("Package Color Mappings")

        val buttonPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        val addButton = JButton("Add")
        val removeButton = JButton("Remove")

        addButton.addActionListener {
            val packageName = JOptionPane.showInputDialog(mainPanel, "Enter package name:")
            if (!packageName.isNullOrBlank()) {
                val colorChooser = JColorChooser(Color.LIGHT_GRAY)
                val result = JOptionPane.showConfirmDialog(
                    mainPanel,
                    colorChooser,
                    "Choose Color",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE
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

        buttonPanel.add(addButton)
        buttonPanel.add(removeButton)

        tablePanel.add(JBScrollPane(packageTable), BorderLayout.CENTER)
        tablePanel.add(buttonPanel, BorderLayout.SOUTH)

        // Replace placeholder panels with actual components
        val formBuilder = FormBuilder.createFormBuilder()
        mainPanel.removeAll()
        formBuilder.addComponent(JBLabel("Package Highlighter Settings"))
        formBuilder.addComponent(enabledCheckbox)
        formBuilder.addComponent(sortAlphabeticallyCheckbox)
        formBuilder.addComponent(useDefaultBrightnessCheckbox)
        formBuilder.addComponent(brightnessPanel)
        formBuilder.addComponent(saturationPanel)
        formBuilder.addComponent(tablePanel)
        formBuilder.addComponentFillVertically(JPanel(), 0)
    }

    override fun createComponent(): JComponent = mainPanel

    override fun isModified(): Boolean {
        return settings.isEnabled != enabledCheckbox.isSelected ||
                settings.useDefaultBrightness != useDefaultBrightnessCheckbox.isSelected ||
                settings.brightness != brightnessSlider.value.toFloat() / 100f ||
                settings.saturation != saturationSlider.value.toFloat() / 100f ||
                settings.sortAlphabetically != sortAlphabeticallyCheckbox.isSelected
    }

    override fun apply() {
        settings.isEnabled = enabledCheckbox.isSelected
        settings.useDefaultBrightness = useDefaultBrightnessCheckbox.isSelected
        settings.brightness = brightnessSlider.value.toFloat() / 100f
        settings.saturation = saturationSlider.value.toFloat() / 100f
        settings.sortAlphabetically = sortAlphabeticallyCheckbox.isSelected
    }

    override fun reset() {
        enabledCheckbox.isSelected = settings.isEnabled
        useDefaultBrightnessCheckbox.isSelected = settings.useDefaultBrightness
        brightnessSlider.value = (settings.brightness * 100).toInt()
        saturationSlider.value = (settings.saturation * 100).toInt()
        sortAlphabeticallyCheckbox.isSelected = settings.sortAlphabetically
        packageTableModel.fireTableDataChanged()
    }

    override fun getDisplayName(): String = "Package Highlighter"

    // Table model for package colors
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

        override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean {
            return columnIndex == 1
        }

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

    // Color renderer for table
    private class ColorRenderer : DefaultTableCellRenderer() {
        init {
            border = BorderFactory.createEmptyBorder(4, 4, 4, 4)
        }

        override fun getTableCellRendererComponent(
            table: JTable, value: Any, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int
        ): JComponent {
            val panel = JPanel()
            panel.preferredSize = Dimension(30, 15)

            if (value is Color) {
                panel.background = value
            }

            return panel
        }
    }

    // Color editor for table
    private class ColorEditor : AbstractCellEditor(), TableCellEditor {
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
}