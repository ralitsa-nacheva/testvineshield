plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.composeCompiler)
}
android {
    namespace = "com.rncoding.testvineshield"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.rncoding.testvineshield"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}


dependencies {
    implementation(project(":shared"))
    implementation(libs.androidx.activity.compose)

    debugImplementation("androidx.compose.ui:ui-tooling:1.12.0")
    implementation("androidx.compose.ui:ui-tooling-preview:1.12.0")
}