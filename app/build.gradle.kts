plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("androidx.room")
    kotlin("kapt")
}

android {
    namespace = "com.example.netmon"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.netmon"
        minSdk = 24
        targetSdk = 34
        versionCode = 4
        versionName = "1.3"
        vectorDrawables.useSupportLibrary = true
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { viewBinding = true }
room {
        schemaDirectory("$projectDir/schemas")
    }
}

dependencies {
    val room = "2.6.1"
    val retrofit = "2.11.0"
    val okhttp = "4.12.0"

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.activity:activity-ktx:1.9.2")

    // Room
    implementation("androidx.room:room-runtime:$room")
    implementation("androidx.room:room-ktx:$room")
    kapt("androidx.room:room-compiler:$room")

    // Retrofit + OkHttp
    implementation("com.squareup.retrofit2:retrofit:$retrofit")
    implementation("com.squareup.retrofit2:converter-moshi:$retrofit")
    implementation("com.squareup.okhttp3:okhttp:$okhttp")
    implementation("com.squareup.okhttp3:logging-interceptor:$okhttp")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
}


kapt {
    correctErrorTypes = true
}
