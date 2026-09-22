plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.koto.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.koto.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
    }

    buildFeatures { compose = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.all {
            // Keep Robolectric's download lock inside the writable build directory.
            val testHome = layout.buildDirectory.dir("test-home").get().asFile
            testHome.mkdirs()
            it.systemProperty("user.home", testHome.absolutePath)
            it.systemProperty("robolectric.graphicsMode", "NATIVE")
            it.systemProperty("robolectric.dependency.repo.url", "https://repo.maven.apache.org/maven2")
            // Robolectric's native loader mishandles URL-escaped spaces in Maven paths.
            // Windows TEMP normally uses a short path; allow an explicit override elsewhere.
            it.systemProperty(
                "maven.repo.local",
                providers.gradleProperty("robolectricMavenCache").getOrElse(
                    File(System.getProperty("java.io.tmpdir"), "koto-robolectric-maven").absolutePath,
                ),
            )
            it.jvmArgs("--enable-native-access=ALL-UNNAMED")
            it.maxHeapSize = "2g"
        }
    }
}

kotlin {
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.androidx.compose.ui.test.junit4)
}
