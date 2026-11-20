plugins {
    id("com.android.application")
}

android {
    namespace = "com.pulse.pulse"
    compileSdk = 33

    defaultConfig {
        applicationId = "com.pulse.pulse"
        minSdk = 26
        targetSdk = 33
        versionCode = 21
        versionName = "2.1"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

// O Pulse não usa nenhuma biblioteca (nem AppCompat, nem Material, nem Kotlin).
