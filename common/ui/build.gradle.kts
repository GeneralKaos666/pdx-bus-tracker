plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.trimettransit.tracker.ui"
    compileSdk = 37

    defaultConfig {
        minSdk = 31
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    lint {
        abortOnError = true
        baseline = file("lint-baseline.xml")
    }
}

dependencies {
    implementation(project(":common:model"))
    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3:1.5.0-alpha28")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.core:core:1.19.0")
    implementation("androidx.core:core-ktx:1.19.0")
    implementation("com.materialkolor:material-kolor:5.0.1")

    // Testing
    testImplementation("junit:junit:4.13.2")
}