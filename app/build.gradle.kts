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
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Stage 4: embedded adb client - the phone speaks the ADB protocol directly
    // to the TV's adbd over TCP (no laptop/relay). Pure-Java implementation of the
    // ADB wire protocol (CNXN/AUTH/OPEN/WRTE/CLSE) including the RSA key
    // handshake. Runs on a normal Android app, no root needed.
    // https://github.com/tananaev/adblib (maintained fork of CrowdStrike adblib)
    implementation("com.tananaev:adblib:1.3")
}
