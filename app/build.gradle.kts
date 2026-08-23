plugins {
    id("com.android.application")
}

android {
    namespace = "com.helloapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.helloapp"
        minSdk = 26
        targetSdk = 34
        versionCode = 2
        versionName = "2.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Stage 4: embedded adb client (kept for TV remote)
    implementation("com.tananaev:adblib:1.3")

    // Room database (Alarms, Todos, Notes)
    implementation("androidx.room:room-runtime:2.6.1")
    annotationProcessor("androidx.room:room-compiler:2.6.1")
}
