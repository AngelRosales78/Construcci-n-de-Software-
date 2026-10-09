// build.gradle.kts (Project level)
val composeBom by extra("2024.06.00")
val kotlinVersion by extra("1.9.22")
val agpVersion by extra("8.2.2")
val hiltVersion by extra("2.51.1")
val navigationVersion by extra("2.7.7")
val retrofitVersion by extra("2.11.0")
val okhttpVersion by extra("4.12.0")
val coroutinesVersion by extra("1.7.3")
val biometricVersion by extra("1.1.0-alpha05")
val encryptionVersion by extra("1.1.0-alpha06")

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:${agpVersion}")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${kotlinVersion}")
        classpath("com.google.dagger:hilt-android-gradle-plugin:${hiltVersion}")
    }
}

plugins {
    id("com.android.application") version "8.2.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
    id("com.google.dagger.hilt.android") version "2.51.1" apply false
}

subprojects {
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        kotlinOptions.jvmTarget = "17"
    }
}
