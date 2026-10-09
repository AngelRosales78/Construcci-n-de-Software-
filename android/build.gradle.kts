// build.gradle.kts (Project level)
buildscript {
    ext {
        composeBom = "2024.06.00"
        kotlinVersion = "1.9.22"
        agpVersion = "8.2.2"
        hiltVersion = "2.51.1"
        navigationVersion = "2.7.7"
        retrofitVersion = "2.11.0"
        okhttpVersion = "4.12.0"
        coroutinesVersion = "1.7.3"
        biomtricVersion = "1.1.0-alpha05"
        encryptionVersion = "1.1.0-alpha06"
    }
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
