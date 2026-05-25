import java.util.Properties
import java.io.File
import com.github.triplet.gradle.androidpublisher.ReleaseStatus

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.play.publisher)
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use(::load)
    }
}

val hasReleaseSigning = listOf(
    "RELEASE_STORE_FILE",
    "RELEASE_STORE_PASSWORD",
    "RELEASE_KEY_ALIAS",
    "RELEASE_KEY_PASSWORD"
).all { !localProperties.getProperty(it).isNullOrBlank() }

android {
    namespace = "com.legendsoftware.richmangoogleplaybillinglibrarytest"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.legendsoftware.richman"
        minSdk = 26
        targetSdk = 36
        versionCode = 21
        versionName = "1.6.4"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "RICHMAN_BACKEND_URL", "\"${localProperties.getProperty("RICHMAN_BACKEND_URL") ?: "https://richman-backend-kfy6nq5mia-uw.a.run.app"}\"")
        buildConfigField("String", "RICHMAN_BACKEND_API_KEY", "\"${localProperties.getProperty("RICHMAN_BACKEND_API_KEY") ?: ""}\"")
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(localProperties.getProperty("RELEASE_STORE_FILE"))
                storePassword = localProperties.getProperty("RELEASE_STORE_PASSWORD")
                keyAlias = localProperties.getProperty("RELEASE_KEY_ALIAS")
                keyPassword = localProperties.getProperty("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
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
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

play {
    val configuredServiceAccountFile = localProperties.getProperty("PLAY_SERVICE_ACCOUNT_FILE")
    val serviceAccountKey = configuredServiceAccountFile
        ?.takeIf { it.isNotBlank() }
        ?.let { path ->
            File(path).takeIf { it.isAbsolute } ?: rootProject.file(path)
        }
        ?: rootProject.file("service-account.json")

    if (serviceAccountKey.exists()) {
        serviceAccountCredentials.set(serviceAccountKey)
    }

    defaultToAppBundles.set(true)
    track.set(localProperties.getProperty("PLAY_TRACK") ?: "alpha")
    releaseStatus.set(ReleaseStatus.COMPLETED)
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
