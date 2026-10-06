@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.jetbrains.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

kotlin {

    // -------------------------------------------------------------------------
    // Android target
    // -------------------------------------------------------------------------

    android {
        namespace = "com.rncoding.testvineshieldlibrary"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        // Enable Java compilation support.
        withJava()

        // Host/unit tests
        withHostTestBuilder {}

        // Instrumented/device tests
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }

        androidResources {
            enable = true
        }
    }

    // -------------------------------------------------------------------------
    // iOS targets
    // -------------------------------------------------------------------------
    val iosTargets = listOf(
        iosArm64(),
        iosSimulatorArm64()
    )

    // Configure the generated iOS frameworks.
    iosTargets.forEach { target ->
        target.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }
    // -------------------------------------------------------------------------
    // Room
    // -------------------------------------------------------------------------

    room {
        schemaDirectory("$projectDir/schemas")
    }

    // -------------------------------------------------------------------------
    // Source sets
    // -------------------------------------------------------------------------

    sourceSets {

        // =====================================================================
        // COMMON MAIN
        // =====================================================================

        commonMain.dependencies {

            // Compose Multiplatform
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)


            // Lifecycle
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)

            // Navigation
            implementation(libs.navigation3.ui)
            implementation(libs.lifecycle.viewmodel.navigation3)

            // Kotlin serialization
            implementation(libs.kotlinx.serialization.json)

            // Room
            implementation(libs.androidx.room.runtime)
            implementation(libs.sqlite.bundled)

            // Koin
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            api(libs.koin.core)

            // Ktor
            implementation(libs.bundles.ktor)

            // Coil
            implementation(libs.bundles.coil)

            // Date/time
            implementation(libs.kotlinx.datetime)

        }

        // =====================================================================
        // ANDROID MAIN
        // =====================================================================

        androidMain.dependencies {

            // Android Activity
            implementation(libs.androidx.activity.compose)

            // Koin
            implementation(libs.koin.android)
            implementation(libs.koin.androidx.compose)

            // Ktor Android engine
            implementation(libs.ktor.client.okhttp)

            // WorkManager
            implementation(libs.worker.runtime.ktx)

            // Biometric authentication
            implementation(libs.androidx.biometric)

            // Compose tooling
            implementation(libs.ui.tooling)
            implementation(libs.ui.tooling.preview)
        }

        // =====================================================================
        // NATIVE MAIN
        // =====================================================================

        nativeMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }

        // =====================================================================
        // COMMON TEST
        // =====================================================================

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }

    // -------------------------------------------------------------------------
    // Common compiler options
    // -------------------------------------------------------------------------

    compilerOptions {
        optIn.add("kotlin.time.ExperimentalTime")
    }
}

// -----------------------------------------------------------------------------
// Room KSP
// -----------------------------------------------------------------------------
//
// KSP 2 requires processors to be attached explicitly to each target.
// Room needs to run for Android and all three iOS targets.
// -----------------------------------------------------------------------------

dependencies {
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspIosArm64", libs.androidx.room.compiler)
    add("kspIosSimulatorArm64", libs.androidx.room.compiler)
}