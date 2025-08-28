import eu.lepicekmichal.signalrkore.HubCommunicationTask
import org.gradle.internal.os.OperatingSystem
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    androidTarget {
        // ✅ 发布 Android release 变体
        publishLibraryVariants("release")

        tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile>().configureEach {
            compilerOptions {
                jvmTarget.set(JvmTarget.JVM_1_8)
            }
        }
    }

    // ❌ 删除 jvm()，避免重复类
    // jvm()

    if (OperatingSystem.current().isMacOsX) {
        listOf(
            iosX64(),
            iosArm64(),
            iosSimulatorArm64()
        ).forEach { iosTarget ->
            iosTarget.binaries.framework {
                baseName = "SignalRKore"
                isStatic = true
            }
        }
    }

    jvmToolchain(8)

    sourceSets {
        applyDefaultHierarchyTemplate()

        all {
            languageSettings {
                optIn("kotlin.RequiresOptIn")
                optIn("kotlin.time.ExperimentalTime")
            }
        }

        val commonMain by getting {
            kotlin.srcDir(project.layout.buildDirectory.dir("generated/kotlin").get().asFile)

            dependencies {
                implementation(libs.kotlin.stdlib.common)
                implementation(libs.ktor.core)
                implementation(libs.ktor.websockets)
                implementation(libs.ktor.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx.json)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.datetime)
                implementation(libs.okio)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        // ❌ 删除 jvmMain（因为没了 jvm()）

        val androidMain by getting {
            dependencies {
                implementation(libs.okhttp)
                implementation(libs.ktor.okhttp)
            }
        }

        if (OperatingSystem.current().isMacOsX) {
            val iosMain by getting {
                dependencies {
                    // iOS 依赖（如果有）
                }
            }
        }
    }
}

android {
    compileSdk = 34
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")

    defaultConfig {
        minSdk = 21
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    namespace = "eu.lepicekmichal.signalrkore"
}

tasks.register<HubCommunicationTask>("HubCommunicationGeneration") {
    this.group = "build"
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    dependsOn += tasks["HubCommunicationGeneration"]
}
