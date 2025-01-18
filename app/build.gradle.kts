plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("de.undercouch.download") version "5.4.0"
    id ("kotlin-parcelize")
    id ("kotlin-kapt")

}

android {
    namespace = "com.example.rcapp"
    compileSdk = 34

    buildFeatures {
        viewBinding = true
        dataBinding = true
    }
    defaultConfig {
        applicationId = "com.example.rcapp"
        minSdk = 31
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        ndkVersion ="21.4.7075529"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11" // 确保 Kotlin 使用与 Java 相同的 JVM 版本
    }

}
// Import DownloadMPTasks task equivalent
apply(from = "$projectDir/download_tasks.gradle")

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.camera.core)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.video)
    implementation(libs.camera.view)
    implementation(libs.camera.extensions)
    implementation(libs.recyclerview)
    implementation(libs.navigation.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation (libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.legacy.support.v4)
    implementation (libs.androidx.lifecycle.livedata.ktx) // 或最新版本
    testImplementation(libs.junit)

    implementation(libs.okhttp)
    implementation(libs.swiperefreshlayout)
    implementation (libs.tasks.vision)
    implementation (libs.kotlinx.serialization.json)

    implementation (libs.com.squareup.retrofit2.retrofit)
    implementation (libs.converter.gson)
    implementation (libs.logging.interceptor)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

}
