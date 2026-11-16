import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import io.github.cdimascio.dotenv.Dotenv
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.openapiGenerator)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.buildkonfig)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room3)
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
                implementation(libs.androidx.room3.runtime)
                implementation(libs.androidx.sqlite.bundled)
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

dependencies {
    add("kspAndroid", libs.androidx.room3.compiler)
    add("kspIosArm64", libs.androidx.room3.compiler)
    add("kspIosSimulatorArm64", libs.androidx.room3.compiler)
}
// Generates @Serializable Kotlin data classes from services/leptos-agent/api.yaml.
// Runs automatically before every compilation; rerun manually with ./gradlew :apps:leptos-agent:shared:openApiGenerate
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

abstract class ServerEndpointValueSource : ValueSource<String, ServerEndpointValueSource.Params> {
    interface Params : ValueSourceParameters {
        val envFile: RegularFileProperty
    }

    override fun obtain(): String {
        val envFile = parameters.envFile.get().asFile
        if (!envFile.exists()) return ""
        val dotenv = Dotenv.configure()
            .directory(envFile.parentFile.absolutePath)
            .filename(envFile.name)
            .ignoreIfMissing()
            .load()
        return dotenv["LEPTOS_SERVER_ENDPOINT"]
    }
}

val serverEndpoint = providers.of(ServerEndpointValueSource::class) {
    parameters { envFile.set(rootProject.file(".commom.env")) }
}.get()

buildkonfig {
    packageName = "com.alwinsden.leptosagent"
    defaultConfigs {
        buildConfigField(STRING, "SERVER_ENDPOINT", serverEndpoint)
    }
}

room3 {
    schemaDirectory("$projectDir/schemas")
}
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>>().configureEach {
    dependsOn(tasks.openApiGenerate)
}
// KSP tasks consume commonMain sources too (which include the generated
// openapi dir), so they need the same ordering as the Kotlin compilation
// tasks above.
tasks.matching { it.name.startsWith("ksp") }.configureEach {
    dependsOn(tasks.openApiGenerate)
}

// The OpenAPI generator also writes into generated/openapi/src/main/baselineProfiles,
// which AGP's ART profile task consumes; make the ordering explicit.
tasks.matching { it.name == "prepareAndroidMainArtProfile" }.configureEach {
    dependsOn(tasks.openApiGenerate)
}
