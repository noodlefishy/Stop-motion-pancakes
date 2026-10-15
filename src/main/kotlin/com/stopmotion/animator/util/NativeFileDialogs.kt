package com.stopmotion.animator.util

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.io.FilenameFilter

object NativeFileDialogs {

    var isNativeDialogVisible by mutableStateOf(false)

    fun openProject(parent: Frame? = null): File? {
        try {
            isNativeDialogVisible = true
            val dialog = FileDialog(parent, "Open Stop Motion Pancake Project", FileDialog.LOAD).apply {
                filenameFilter = FilenameFilter { _, name ->
                    name.endsWith(".smproj", ignoreCase = true) || name.endsWith(".json", ignoreCase = true)
                }
                isVisible = true
            }
            val file = dialog.file ?: return null
            val dir = dialog.directory ?: return null
            return File(dir, file)
        } finally {
            isNativeDialogVisible = false
        }
    }

    fun saveProject(
        defaultName: String = "MyAnimation.smproj",
        defaultDirectory: String? = System.getProperty("user.home"),
        parent: Frame? = null
    ): File? {
        try {
            isNativeDialogVisible = true
            val dialog = FileDialog(parent, "Save Stop Motion Pancake Project", FileDialog.SAVE).apply {
                file = defaultName
                if (defaultDirectory != null) directory = defaultDirectory
                filenameFilter = FilenameFilter { _, name ->
                    name.endsWith(".smproj", ignoreCase = true)
                }
                isVisible = true
            }
            val file = dialog.file ?: return null
            val dir = dialog.directory ?: return null
            val chosen = File(dir, file)
            return if (!chosen.name.endsWith(".smproj", ignoreCase = true)) {
                File(dir, "${chosen.name}.smproj")
            } else {
                chosen
            }
        } finally {
            isNativeDialogVisible = false
        }
    }

    fun chooseNewProjectLocation(parent: Frame? = null): File? {
        return saveProject(defaultName = "Untitled.smproj", parent = parent)
    }

    fun openImages(parent: Frame? = null): List<File> {
        try {
            isNativeDialogVisible = true
            val dialog = FileDialog(parent, "Import Frame Images", FileDialog.LOAD).apply {
                isMultipleMode = true
                filenameFilter = FilenameFilter { _, name ->
                    val lower = name.lowercase()
                    lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".webp")
                }
                isVisible = true
            }
            return dialog.files?.toList() ?: run {
                val file = dialog.file ?: return emptyList()
                val dir = dialog.directory ?: return emptyList()
                listOf(File(dir, file))
            }
        } finally {
            isNativeDialogVisible = false
        }
    }

    fun openAudio(parent: Frame? = null): File? {
        try {
            isNativeDialogVisible = true
            val dialog = FileDialog(parent, "Import Audio Track", FileDialog.LOAD).apply {
                filenameFilter = FilenameFilter { _, name ->
                    val lower = name.lowercase()
                    lower.endsWith(".wav") || lower.endsWith(".aiff") || lower.endsWith(".aif") || lower.endsWith(".mp3")
                }
                isVisible = true
            }
            val file = dialog.file ?: return null
            val dir = dialog.directory ?: return null
            return File(dir, file)
        } finally {
            isNativeDialogVisible = false
        }
    }

    fun saveExport(
        defaultName: String,
        extension: String,
        parent: Frame? = null
    ): File? {
        try {
            isNativeDialogVisible = true
            val dialog = FileDialog(parent, "Export Video / Animation", FileDialog.SAVE).apply {
                file = defaultName
                directory = System.getProperty("user.home")
                filenameFilter = FilenameFilter { _, name ->
                    name.endsWith(".$extension", ignoreCase = true)
                }
                isVisible = true
            }
            val file = dialog.file ?: return null
            val dir = dialog.directory ?: return null
            val chosen = File(dir, file)
            return if (!chosen.name.endsWith(".$extension", ignoreCase = true)) {
                File(dir, "${chosen.name}.$extension")
            } else {
                chosen
            }
        } finally {
            isNativeDialogVisible = false
        }
    }
}
