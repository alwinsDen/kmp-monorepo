buildscript {
    dependencies {
        // .env parsing for BuildKonfig injection (see apps/leptos-agent/shared/build.gradle.kts)
        classpath("io.github.cdimascio:dotenv-java:3.2.0")
    }
}

plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.buildkonfig) apply false
    alias(ktorLibs.plugins.ktor) apply false
}