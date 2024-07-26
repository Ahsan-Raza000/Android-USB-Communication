// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
    dependencies {
        classpath("com.android.tools.build:gradle:7.4.1")
        classpath(kotlin("gradle-plugin", version = "1.8.10"))

        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle.kts files
    }
}

allprojects {
    // Note: No repositories here; they are defined in settings.gradle.kts
}

tasks.register<Delete>("clean") {
    delete(rootProject.buildDir)
}