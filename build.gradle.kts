// Pin build-classpath (Gradle plugin) transitive deps to versions that close open
// Dependabot advisories. These come from AGP 9.5.0 / Kotlin plugin tooling and are
// never shipped in the APK. Bump the whole org.bouncycastle group together so bcprov,
// bcpkix and bcutil stay on one version and don't skew against each other.
buildscript {
    configurations.classpath {
        resolutionStrategy {
            force(
                "com.google.guava:guava:33.7.2-jre",
                "org.bitbucket.b_c:jose4j:0.9.6",
                "org.jdom:jdom2:2.0.6.1",
                "org.apache.commons:commons-lang3:3.18.0",
                "org.apache.httpcomponents:httpclient:4.5.14",
            )
            eachDependency {
                if (requested.group == "org.bouncycastle") {
                    useVersion("1.85")
                }
            }
        }
    }
}

plugins {
    id("com.android.application") version "9.5.0" apply false
    id("com.android.library") version "9.5.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.5.0-Beta2" apply false
}