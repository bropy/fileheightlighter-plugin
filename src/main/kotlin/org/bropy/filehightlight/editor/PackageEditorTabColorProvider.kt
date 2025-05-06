package org.bropy.filehightlight.editor

import com.intellij.openapi.fileEditor.impl.EditorTabColorProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager
import org.bropy.filehightlight.settings.PackageHighlighterSettings
import java.awt.Color

class PackageEditorTabColorProvider : EditorTabColorProvider {

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
                return Color.decode(colorHex)
            }
        }

        // Generate a color based on the package name hash
        return generateColorFromString(packageName)
    }

    private fun generateColorFromString(input: String): Color {
        val hash = input.hashCode()

        // Create a pastel color that's easy on the eyes
        val r = ((hash and 0xFF0000) shr 16) % 156 + 100
        val g = ((hash and 0x00FF00) shr 8) % 156 + 100
        val b = (hash and 0x0000FF) % 156 + 100

        return Color(r, g, b)
    }
}