import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val uploadPropertiesFile = rootProject.file("signing/upload-key.properties")
val uploadProperties = Properties().apply {
    if (uploadPropertiesFile.exists()) {
        uploadPropertiesFile.inputStream().use(::load)
    }
}

android {
    namespace = "com.fluffnark.motoringdashboard"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.fluffnark.motoringdashboard"
        minSdk = 28
        targetSdk = 36
        versionCode = 4
        versionName = "0.3.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("upload") {
            if (uploadPropertiesFile.exists()) {
                storeFile = rootProject.file(uploadProperties.getProperty("storeFile"))
                storePassword = uploadProperties.getProperty("storePassword")
                keyAlias = uploadProperties.getProperty("keyAlias")
                keyPassword = uploadProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("upload")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true }

    packaging.resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
}

tasks.register<Copy>("stagePrototypeRelease") {
    dependsOn("assembleRelease", "bundleRelease")
    from(layout.buildDirectory.file("outputs/apk/release/app-release.apk"))
    from(layout.buildDirectory.file("outputs/bundle/release/app-release.aab"))
    into(rootProject.layout.projectDirectory.dir("releases"))
    rename("app-release.apk", "motoring-dashboard-v0.3.1.apk")
    rename("app-release.aab", "motoring-dashboard-v0.3.1.aab")
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    // Newer releases require the API 37 / AGP 9.1 preview toolchain.
    implementation(platform("androidx.compose:compose-bom:2025.08.01"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.3")
    implementation("androidx.media:media:1.7.0")

    implementation("androidx.car.app:app:1.8.0-rc01")
    implementation("androidx.car.app:app-projected:1.8.0-rc01")

    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
    testImplementation("androidx.car.app:app-testing:1.8.0-rc01")
}
