package com.stopmotion.animator.util

import com.stopmotion.animator.model.AppSettings
import kotlinx.serialization.json.Json
import java.io.File

object SettingsManager {
    private val settingsFile = File(System.getProperty("user.home"), ".stopmotion_animator/settings.json")
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    fun loadSettings(): AppSettings {
        return try {
            if (settingsFile.exists()) {
                val content = settingsFile.readText()
                json.decodeFromString(AppSettings.serializer(), content)
            } else {
                AppSettings()
            }
        } catch (e: Exception) {
            AppSettings()
        }
    }

    fun saveSettings(settings: AppSettings) {
        try {
            settingsFile.parentFile?.mkdirs()
            val content = json.encodeToString(AppSettings.serializer(), settings)
            settingsFile.writeText(content)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
