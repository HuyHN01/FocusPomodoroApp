plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)

    // Thêm plugin KSP cho Kotlin Symbol Processing (thay thế kapt)
    //id("com.google.devtools.ksp") version "2.2.20-2.0.3"
    alias(libs.plugins.hilt)

    id("com.google.devtools.ksp") version "2.2.20-2.0.3"
    id("com.google.gms.google-services")
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
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    testImplementation(libs.androidx.room.testing)

    // 1. Thư viện Hilt runtime
    implementation(libs.hilt.android)

    // 2. Trình biên dịch KSP cho Hilt (thay vì kapt)
    ksp(libs.hilt.compiler)

    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")

    // Thêm Platform BOM: Quản lý phiên bản tự động
    implementation(platform(libs.firebase.bom))

    // Thư viện cho Firebase Authentication (Auth)
    implementation(libs.firebase.auth)

    // Thư viện cho Cloud Firestore
    implementation(libs.firebase.firestore)

    // (Tùy chọn) Thư viện Firebase cho Kotlin
    implementation(libs.firebase.analytics.ktx) // Nếu có bật Analytics

    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)

    implementation(platform("com.google.firebase:firebase-bom:34.5.0"))
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-auth")
}