import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.BuiltArtifact
import com.android.build.api.variant.BuiltArtifacts
import com.android.build.api.variant.BuiltArtifactsLoader
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.github.triplet.play") version "4.1.1"
}

abstract class RenameApkTask : DefaultTask() {
    @get:Internal
    abstract val builtArtifactsLoader: Property<BuiltArtifactsLoader>

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val input: DirectoryProperty

    @get:OutputDirectory
    abstract val output: DirectoryProperty

    @get:Input
    abstract val variantName: Property<String>

    @TaskAction
    fun renameApk() {
        val builtArtifacts = builtArtifactsLoader.get().load(input.get())
        val outDir = output.get().asFile
        outDir.deleteRecursively()
        outDir.mkdirs()
        builtArtifacts!!.elements.forEach { artifact ->
            val newName = "PdxBusTracker-${variantName.get()}-${artifact.versionName}.apk"
            Files.copy(
                File(artifact.outputFile).toPath(),
                File(output.get().asFile, newName).toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
        }
    }
}

android {
    namespace = "com.trimettransit.tracker"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.trimettransit.tracker"
        minSdk = 31
        targetSdk = 37
        versionCode = 516
        versionName = "4.15.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = file(project.findProperty("STORE_FILE") ?: "release.keystore")
            val storePw = project.findProperty("STORE_PASSWORD") as? String ?: System.getenv("STORE_PASSWORD")
            val alias = project.findProperty("KEY_ALIAS") as? String ?: System.getenv("KEY_ALIAS")
            val keyPw = project.findProperty("KEY_PASSWORD") as? String ?: System.getenv("KEY_PASSWORD")
            val isReleaseBuild = gradle.startParameter.taskNames.any { it.lowercase().contains("release") }
            if (storePw != null && alias != null && keyPw != null) {
                storePassword = storePw
                keyAlias = alias
                keyPassword = keyPw
            } else if (project.hasProperty("releaseSigningFallback")) {
                storePassword = "android"
                keyAlias = "release"
                keyPassword = "android"
            } else if (isReleaseBuild) {
                throw GradleException(
                    "Release signing env vars STORE_PASSWORD/KEY_ALIAS/KEY_PASSWORD not set. " +
                        "Set them or build with -PreleaseSigningFallback=true."
                )
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            ndk.debugSymbolLevel = "FULL"
        }
        debug {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        buildConfig = true
    }

    lint {
        abortOnError = true
        baseline = file("lint-baseline.xml")
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/DEPENDENCIES", "META-INF/LICENSE", "META-INF/LICENSE.txt",
                "META-INF/NOTICE", "META-INF/NOTICE.txt"
            )
        }
        // Prevents Gradle from calling the incompatible x86_64 llvm-strip binary on native dependencies
        jniLibs {
            doNotStrip("**/*.so")
        }
    }

    playConfigs {
        maybeCreate("release").resolutionStrategy.set(com.github.triplet.gradle.androidpublisher.ResolutionStrategy.AUTO_OFFSET)
    }
}

val playCredentialsConfigured = System.getenv("ANDROID_PUBLISHER_CREDENTIALS") != null ||
    project.hasProperty("playServiceAccountJsonPath")

play {
    enabled.set(playCredentialsConfigured)
    if (project.hasProperty("playServiceAccountJsonPath")) {
        serviceAccountCredentials.set(file(project.property("playServiceAccountJsonPath").toString()))
    }
}

androidComponents {
    onVariants(selector().all()) { variant ->
        val renameTask = tasks.register(
            "renameApkFor${variant.name.replaceFirstChar { it.uppercase() }}",
            RenameApkTask::class.java
        ) {
            output.set(layout.buildDirectory.dir("outputs/renamed_apks/${variant.name}"))
            variantName.set(variant.name)
            builtArtifactsLoader.set(variant.artifacts.getBuiltArtifactsLoader())
        }
        variant.artifacts.use(renameTask)
            .wiredWith { it.input }
            .toListenTo(SingleArtifact.APK)
    }
}

dependencies {
    // Modularized projects
    implementation(project(":feature:home"))
    implementation(project(":feature:stops"))
    implementation(project(":feature:trips"))
    implementation(project(":feature:arrivals"))
    implementation(project(":feature:settings"))
    implementation(project(":common:ui"))
    implementation(project(":common:model"))
    implementation(project(":common:utils"))
    implementation(project(":component:transit"))
    implementation(project(":component:localdata"))

    // AndroidX Core
    implementation("androidx.core:core:1.19.0")
    implementation("androidx.appcompat:appcompat:1.8.0")
    // Compose
    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3:1.5.0-alpha28")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.navigation:navigation-compose:2.10.1")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // UI
    implementation("com.google.android.material:material:1.14.0")

    // Preference/Settings
    implementation("androidx.preference:preference:1.2.1")

    // Maps (MapLibre GL Native, OpenGL backend — the plain `android-sdk` artifact is Vulkan-only since 13.0)
    implementation("org.maplibre.gl:android-sdk-opengl:13.6.1")

    // Home-screen widget (Glance) + its background refresh (WorkManager)
    implementation("androidx.glance:glance-appwidget:1.2.0")
    implementation("androidx.glance:glance-material3:1.2.0")
    implementation("androidx.work:work-runtime-ktx:2.11.2")

    // Logging (Timber tree planted in TrimetTransitTracker)
    implementation("com.jakewharton.timber:timber:5.0.1")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("net.danlew:android.joda:2.14.2.1")
}
