plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

val dynamicVersionCode = (project.findProperty("versionCode") as? String)?.toIntOrNull()
    ?: (System.getenv("VERSION_CODE")?.toIntOrNull())
    ?: 1

val dynamicVersionName = (project.findProperty("versionName") as? String)
    ?: System.getenv("VERSION_NAME")
    ?: "1.0.01"

android {
    namespace = "com.fingo.finlauncher"
    compileSdk = 34

    signingConfigs {
        create("release") {
            val keystoreParam = (project.findProperty("KEYSTORE_FILE") as? String)
                ?: System.getenv("KEYSTORE_FILE")
                ?: "keystore/finlauncher.jks"
            val keystoreFile = sequenceOf(
                file(keystoreParam),
                rootProject.file("app/$keystoreParam"),
                rootProject.file(keystoreParam),
                file("../$keystoreParam"),
                file("keystore/finlauncher.jks"),
                rootProject.file("app/keystore/finlauncher.jks")
            ).firstOrNull { it.exists() } ?: file("keystore/finlauncher.jks")

            storeFile = keystoreFile
            storePassword = (project.findProperty("KEYSTORE_PASSWORD") as? String)
                ?: System.getenv("KEYSTORE_PASSWORD") ?: "finlauncher123"
            keyAlias = (project.findProperty("KEY_ALIAS") as? String)
                ?: System.getenv("KEY_ALIAS") ?: "finlauncher"
            keyPassword = (project.findProperty("KEY_PASSWORD") as? String)
                ?: System.getenv("KEY_PASSWORD") ?: "finlauncher123"
            enableV1Signing = true
            enableV2Signing = true
        }
    }

    defaultConfig {
        applicationId = "com.fingo.finlauncher"
        minSdk = 26
        targetSdk = 34
        versionCode = dynamicVersionCode
        versionName = dynamicVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.coil.compose)

    debugImplementation(libs.androidx.ui.tooling)
}
