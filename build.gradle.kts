plugins {
    kotlin("jvm") version "2.1.0"
    id("org.jetbrains.compose") version "1.7.3"
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.0"
    kotlin("plugin.serialization") version "2.1.0"
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")

    testImplementation("junit:junit:4.13.2")
}

compose.desktop {
    application {
        mainClass = "com.stopmotion.animator.MainKt"
        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Exe,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb
            )
            packageName = "Stop Motion Pancake"
            packageVersion = "1.0.0"
            description = "Pretty pretty stop motion"
            copyright = "2026 Stop Motion Pancake"
            vendor = "Stop Motion Pancake"

            macOS {
                bundleID = "com.stopmotion.pancake"
                infoPlist {
                    extraKeysRawXml = """
                        <key>NSCameraUsageDescription</key>
                        <string>Stop Motion Pancake needs camera access to capture animation frames and live stream Continuity Camera.</string>
                        <key>NSMicrophoneUsageDescription</key>
                        <string>Stop Motion Pancake needs microphone access to record and sync audio.</string>
                    """.trimIndent()
                }

                // 2. If signing the app or enabling Hardened Runtime:
                entitlementsFile.set(project.file("src/main/resources/entitlements.plist"))
            }
            windows {
                menuGroup = "Stop Motion Pancake"
                upgradeUuid = "a687f872-9852-4742-b912-1d54be5ce82a"
            }
            linux {
                packageName = "stop-motion-pancake"
            }
        }
    }
}
