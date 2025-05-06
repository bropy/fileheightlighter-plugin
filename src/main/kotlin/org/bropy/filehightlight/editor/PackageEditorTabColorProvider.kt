package org.bropy.filehightlight.editor

import com.intellij.openapi.fileEditor.impl.EditorTabColorProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager
import org.bropy.filehightlight.settings.PackageHighlighterSettings
import java.awt.Color
import kotlin.math.abs

class PackageEditorTabColorProvider : EditorTabColorProvider {

    // Cache to store previously generated colors for packages
    private val colorCache = mutableMapOf<String, Color>()

    // Set to track used colors to avoid repetition
    private val usedColors = mutableSetOf<Int>()

    override fun getEditorTabColor(project: Project, file: VirtualFile): Color? {
        if (!file.isValid) return null

        val settings = PackageHighlighterSettings.getInstance()
        if (!settings.isEnabled) return null

        // Get the package name from the PSI file
        val psiFile = PsiManager.getInstance(project).findFile(file) ?: return null

        // Only process Java and Kotlin files
        val fileExt = file.extension
        if (fileExt != "java" && fileExt != "kt" && fileExt != "kts") return null

        val packageName = when {
            psiFile is PsiJavaFile -> psiFile.packageName
            fileExt == "kt" || fileExt == "kts" -> extractKotlinPackageName(psiFile)
            else -> return null
        }

        // Return a color based on the package name
        return getColorForPackage(packageName)
    }

    private fun extractKotlinPackageName(psiFile: PsiFile): String {
        // Simple extraction based on text for MVP
        // In a full implementation, use Kotlin PSI API
        val text = psiFile.text
        val packageRegex = "package\\s+([\\w.]+)".toRegex()
        val matchResult = packageRegex.find(text)
        return matchResult?.groupValues?.getOrNull(1) ?: ""
    }

    private fun getColorForPackage(packageName: String): Color {
        val settings = PackageHighlighterSettings.getInstance()

        // Check if we have predefined colors for specific packages
        settings.packageColors.forEach { (pkg, colorHex) ->
            if (packageName.startsWith(pkg)) {
                // Make predefined colors lighter if necessary for better text visibility
                val definedColor = Color.decode(colorHex)
                val hsb = Color.RGBtoHSB(definedColor.red, definedColor.green, definedColor.blue, null)
                return if (hsb[1] > 0.4f || hsb[2] < 0.85f) {
                    // If color is too saturated or dark, make it lighter
                    Color.getHSBColor(hsb[0], hsb[1] * 0.7f, maxOf(hsb[2], 0.85f))
                } else {
                    definedColor
                }
            }
        }

        // Check if we've already generated a color for this package
        colorCache[packageName]?.let { return it }

        // Consider package segments to ensure packages at the same level get distinct colors
        val packageSegments = packageName.split(".")
        val color = when {
            packageSegments.size > 2 -> {
                // For deeply nested packages, use the last two segments for more uniqueness
                val key = "${packageSegments[packageSegments.size - 2]}.${packageSegments.last()}"
                generateDistinctColor(key, packageName)
            }
            packageSegments.size > 1 -> {
                // For packages with multiple segments, focus on the last segment for differentiation
                // but still incorporate the full package for uniqueness
                generateDistinctColor(packageSegments.last(), packageName)
            }
            else -> {
                // For simple packages, just use the package name
                generateDistinctColor(packageName, packageName)
            }
        }

        // Cache the color for future use
        colorCache[packageName] = color
        return color
    }

    private fun generateDistinctColor(key: String, fullPackage: String): Color {
        for (attempt in 0 until 15) {
            val baseHash = (key.hashCode() + attempt * 10000 + fullPackage.length * 17) * (attempt + 1)

            val h = ((abs(baseHash) % 360) / 360.0f)  // Hue (0-1)
            val s = (0.10f + (abs(baseHash / 1000) % 10) / 100.0f)  // Saturation (0.10-0.20) — дуже низька
            val b = (0.80f + (abs(baseHash / 10000) % 5) / 100.0f)  // Brightness (0.80-0.85) — трохи приглушено

            val color = Color.getHSBColor(h, s, b)
            val colorValue = color.rgb

            if (!isTooSimilarToExisting(colorValue)) {
                usedColors.add(colorValue)
                return color
            }
        }

        // Фолбек — теж з менш інтенсивним кольором
        val hash = fullPackage.hashCode()
        val h = ((abs(hash) % 360) / 360.0f)
        val s = 0.15f  // Низька насиченість
        val b = 0.82f  // Помірна яскравість

        val color = Color.getHSBColor(h, s, b)
        usedColors.add(color.rgb)
        return color
    }

    private fun isTooSimilarToExisting(colorValue: Int): Boolean {
        // Check if the color is exactly the same as an existing color
        if (colorValue in usedColors) return true

        val newColor = Color(colorValue)
        val newHsb = Color.RGBtoHSB(newColor.red, newColor.green, newColor.blue, null)

        for (existingColorValue in usedColors) {
            val existingColor = Color(existingColorValue)
            val existingHsb = Color.RGBtoHSB(existingColor.red, existingColor.green, existingColor.blue, null)

            // Calculate hue difference (considering the circular nature of hue)
            val hueDiff = minHueDifference(newHsb[0], existingHsb[0])

            // Require more hue difference for better visual distinction
            // About 45 degrees difference in hue (0.125 * 360 ≈ 45)
            if (hueDiff < 0.125f) {
                // If hues are somewhat similar, check if they have different enough brightness/saturation
                val satDiff = abs(newHsb[1] - existingHsb[1])
                val brightDiff = abs(newHsb[2] - existingHsb[2])

                // If all attributes are too similar, reject the color
                if (satDiff < 0.1f && brightDiff < 0.1f) {
                    return true
                }
            }
        }

        return false
    }

    private fun minHueDifference(h1: Float, h2: Float): Float {
        // Calculate the minimum distance between two hues (considering the circular nature)
        val diff = abs(h1 - h2)
        return if (diff > 0.5f) 1.0f - diff else diff
    }
}