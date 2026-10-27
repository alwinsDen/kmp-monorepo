import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.openapiGenerator)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    android {
        namespace = "com.alwinsden.leptosagent.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
        androidResources {
            enable = true
        }
        withHostTest {
            isIncludeAndroidResources = true
        }
    }

    sourceSets {
        commonMain {
            kotlin.srcDir(layout.buildDirectory.dir("generated/openapi/src/main/kotlin"))
            dependencies {
                api(libs.kotlinx.serialization.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(ktorLibs.client.core)
                implementation(ktorLibs.client.contentNegotiation)
                implementation(ktorLibs.serialization.kotlinx.json)
            }
        }
        androidMain.dependencies {
            implementation(ktorLibs.client.okhttp)
        }
        iosMain.dependencies {
            implementation(ktorLibs.client.darwin)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

// Generates @Serializable Kotlin data classes from services/leptos-agent/api.yaml.
// Runs automatically before every compilation; rerun manually with ./gradlew :apps:leptos-agent:shared:sharedLogic:openApiGenerate
tasks.openApiGenerate {
    generatorName.set("kotlin")
    inputSpec.set(rootProject.file("services/leptos-agent/api.yaml").absolutePath)
    outputDir.set(layout.buildDirectory.dir("generated/openapi").get().asFile.absolutePath)
    modelPackage.set("com.alwinsden.leptosagent.shared")
    apiPackage.set("com.alwinsden.leptosagent.shared")
    globalProperties.set(
        mapOf(
            "models" to "",
            "modelDocs" to "false",
            "modelTests" to "false",
        )
    )
    configOptions.set(
        mapOf(
            "serializationLibrary" to "kotlinx_serialization",
            "enumPropertyNaming" to "UPPERCASE",
        )
    )
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>>().configureEach {
    dependsOn(tasks.openApiGenerate)
}

// The OpenAPI generator also writes into generated/openapi/src/main/baselineProfiles,
// which AGP's ART profile task consumes; make the ordering explicit.
tasks.matching { it.name == "prepareAndroidMainArtProfile" }.configureEach {
    dependsOn(tasks.openApiGenerate)
}
