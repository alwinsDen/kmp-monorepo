plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
}

group = "com.alwinsden"
version = "1"

repositories {
    mavenCentral()
}

dependencies {

}

kotlin {
    jvm()

    // build targets for the ios.
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "LeptosCore"
            isStatic = true
        }
    }
}
