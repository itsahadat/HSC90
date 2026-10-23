plugins {
    id("com.android.application")
}

android {
    namespace = "com.banglabs.hsc90"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.banglabs.hsc90"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Signs the release build with the debug key so it installs directly.
            // Replace with your own keystore before publishing on Play Store.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
