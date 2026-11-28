// Pin AGP/Kotlin build-classpath transitive deps to patched versions that close open
// Dependabot advisories. Build tooling only, never shipped in the APK. Declared as
// direct classpath deps (not resolutionStrategy.force) so the submitted dependency
// graph records the patched versions instead of the vulnerable transitive ones.
// Bouncycastle modules are pinned together to avoid version skew.
buildscript {
    dependencies {
        classpath("com.google.guava:guava:33.7.2-jre")
        classpath("org.bouncycastle:bcprov-jdk18on:1.85")
        classpath("org.bouncycastle:bcpkix-jdk18on:1.85")
        classpath("org.bouncycastle:bcutil-jdk18on:1.85")
        classpath("org.bitbucket.b_c:jose4j:0.9.6")
        classpath("org.jdom:jdom2:2.0.6.1")
        classpath("org.apache.commons:commons-lang3:3.18.0")
        classpath("org.apache.httpcomponents:httpclient:4.5.14")
    }
}

plugins {
    id("com.android.application") version "9.5.0" apply false
    id("com.android.library") version "9.5.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.5.0-Beta2" apply false
}