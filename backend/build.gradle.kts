plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

application {
    mainClass.set("com.legendsoftware.richman.backend.BackendApplicationKt")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation(libs.google.api.services.androidpublisher)
    implementation(libs.google.auth.library.oauth2.http)
    implementation(libs.google.http.client.gson)

    testImplementation(libs.junit)
}

tasks.test {
    useJUnit()
}
