plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.priangradingsystem"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.priangradingsystem"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    sourceSets {
        getByName("main") {
            res.srcDirs(
                "src/main/res",
                "src/main/res-splash",
                "src/main/res-main",
                "src/main/res-student",
                "src/main/res-reports",
                "src/main/res-records",
                "src/main/res-common",
                "src/main/res-login"
            )
        }
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    implementation(libs.gson)

    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}