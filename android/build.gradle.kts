// android/build.gradle.kts (Project level)

buildscript {
    val agpVersion = "8.2.2"
    val kotlinVersion = "1.9.22"
    val hiltVersion = "2.51.1"

    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:$agpVersion")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
        classpath("com.google.dagger:hilt-android-gradle-plugin:$hiltVersion")
    }
}

// Propiedades extra globales para acceso en app/build.gradle.kts
extra["composeBom"] = "2024.06.00"
extra["kotlinVersion"] = "1.9.22"
extra["agpVersion"] = "8.2.2"
extra["hiltVersion"] = "2.51.1"
extra["navigationVersion"] = "2.7.7"
extra["retrofitVersion"] = "2.11.0"
extra["okhttpVersion"] = "4.12.0"
extra["coroutinesVersion"] = "1.7.3"
extra["biometricVersion"] = "1.1.0-alpha05"
extra["encryptionVersion"] = "1.1.0-alpha06"

plugins {
    id("com.android.application") version "8.2.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
    id("com.google.dagger.hilt.android") version "2.51.1" apply false
}
