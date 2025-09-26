plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)

    // Thêm plugin KSP cho Kotlin Symbol Processing (thay thế kapt)
    id("com.google.devtools.ksp") version "2.2.20-2.0.3"
}

android {
    namespace = "com.example.focusmate"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.focusmate"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        viewBinding = true
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
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(libs.androidx.fragment.ktx)
    // Room Runtime
    implementation(libs.androidx.room.runtime)

    // Kotlin Extensions và Coroutines cho Room
    implementation(libs.androidx.room.ktx)

    // KSP (Kotlin Symbol Processing) - Bộ xử lý chú thích
    ksp(libs.androidx.room.compiler)

    // Thư viện Test
    testImplementation(libs.androidx.room.testing)
}