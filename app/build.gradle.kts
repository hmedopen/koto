import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp")
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    FileInputStream(localPropertiesFile).use { stream ->
        localProperties.load(stream)
    }
}
val deeplApiKey = localProperties.getProperty("DEEPL_API_KEY") ?: ""

android {
    namespace = "com.koto.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.koto.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"

        buildConfigField("String", "DEEPL_API_KEY", "\"$deeplApiKey\"")
    }

    buildFeatures { compose = true; buildConfig = true }

    // Release-like timing without replacing the installed app or its data.
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
        create("benchmark") {
            initWith(getByName("release"))
            applicationIdSuffix = ".benchmark"
            signingConfig = signingConfigs.getByName("debug")
            isDebuggable = false
            matchingFallbacks += "release"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/CONTRIBUTORS.md"
            excludes += "/META-INF/LICENSE.md"
            excludes += "/META-INF/README.md"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/LICENSE"
            excludes += "/META-INF/LICENSE.txt"
            excludes += "/META-INF/NOTICE"
            excludes += "/META-INF/NOTICE.txt"
        }
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

    implementation("com.google.mlkit:translate:17.0.3")
    implementation("com.atilika.kuromoji:kuromoji-ipadic:0.9.0")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.compose.ui:ui-text-google-fonts")
}
