plugins {
    id("com.android.library")
}

android {
    namespace = "com.trimettransit.tracker.data.local"
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
    implementation("androidx.core:core-ktx:1.19.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
}