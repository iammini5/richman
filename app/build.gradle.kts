plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.legendsoftware.richmangoogleplaybillinglibrarytest"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.legendsoftware.richmangoogleplaybillinglibrarytest"
        minSdk = 26
        targetSdk = 36
        versionCode = 6
        versionName = "1.5.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
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
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.billing)
    implementation(libs.billing.ktx)
    implementation(libs.play.services.ads)
    implementation(libs.guava)

    implementation(libs.app.update)
    implementation(libs.app.update.ktx)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}