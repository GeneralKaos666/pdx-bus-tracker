plugins {
    id("com.android.library")
}

private fun quoteForBuildConfig(value: String): String {
    val safe = value.replace("\\", "\\\\").replace("\"", "\\\"")
    return "\"$safe\""
}

val trimetApiKey = (project.findProperty("TRIMET_API_KEY") ?: System.getenv("TRIMET_API_KEY") ?: "").toString()

android {
    namespace = "com.trimettransit.tracker.transit"
    compileSdk = 37

    defaultConfig {
        minSdk = 31
        buildConfigField("String", "TRIMET_API_KEY", quoteForBuildConfig(trimetApiKey))
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

    testOptions {
        unitTests {
            // The trip-planner XML parser calls android.net.Uri.decode() while mapping
            // the echoed from/to place labels; that android.jar stub must return a
            // default instead of throwing in plain JVM unit tests.
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    implementation(project(":common:utils"))
    implementation(project(":common:model"))
    implementation("com.squareup.okhttp3:okhttp:5.5.0")
    implementation("net.danlew:android.joda:2.14.2.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
    implementation("com.jakewharton.timber:timber:5.0.1")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20260814")
    testImplementation("net.danlew:android.joda:2.14.2.1")
}