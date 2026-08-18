# Gradle 10 Deprecation & Code Scan Report

## Executive Summary

A comprehensive scan was conducted across all 12 modules of the **TriMet Bus Tracker** codebase to identify compilation errors, lint issues, and potential deprecations or breaking changes in preparation for **Gradle 10**.

The scan included executing full project checks with Gradle's warning mode enabled:
```sh
./gradlew check --warning-mode=all
```

### Scan Result
- **Build Status**: `BUILD SUCCESSFUL` (476 actionable tasks executed)
- **Compilation Errors**: `0`
- **Android Lint Errors/Warnings**: `0`
- **Gradle 9.7 Deprecation Warnings**: `0`

While the current build configuration runs cleanly on **Gradle 9.7.0** and **Android Gradle Plugin (AGP) 9.3.1**, several forward-compatibility recommendations and modernization items have been identified to ensure seamless upgradeability to **Gradle 10**.

---

## Environment & Tooling Stack

| Property | Configured Version |
|---|---|
| **Gradle** | 9.7.0 |
| **Android Gradle Plugin (AGP)** | 9.3.1 |
| **Kotlin Plugin** | 2.4.10 / Compose Plugin 2.4.10 |
| **Java JDK** | JDK 21 |
| **Compile SDK / Target SDK** | 37 |
| **Min SDK** | 31 |

---

## Detailed Scan Findings & Gradle 10 Compatibility Analysis

### 1. Build Execution Analysis (`--warning-mode=all`)
Execution of `./gradlew check --warning-mode=all` completed without emitting active deprecation warnings from Gradle 9.7.0. The build pipeline (including lint checks across `:app`, `:common:*`, `:component:*`, and `:feature:*`) passed with zero errors or warnings.

### 2. Maven Repository URL Declarations (`settings.gradle`)
- **Current Code**:
  ```groovy
  dependencyResolutionManagement {
      repositories {
          google()
          mavenCentral()
          maven { url = 'https://jitpack.io' }
      }
  }
  ```
- **Gradle 10 Requirement**: In Gradle 9 and 10, passing plain string values to `url` without converting to `java.net.URI` or using `uri(...)` is deprecated/disallowed in Kotlin DSL and strict property setters.
- **Recommended Action**:
  Update repository declarations to use explicit URI objects:
  ```groovy
  maven { url = uri('https://jitpack.io') }
  ```

### 3. Custom Task & Configuration Cache Compatibility (`app/build.gradle`)
- **Current Code**:
  `app/build.gradle` defines a custom task `RenameApkTask`:
  ```groovy
  abstract class RenameApkTask extends DefaultTask {
      @Internal
      abstract Property<BuiltArtifactsLoader> getBuiltArtifactsLoader()
      @InputDirectory
      @PathSensitive(PathSensitivity.RELATIVE)
      abstract DirectoryProperty getInput()
      @OutputDirectory
      abstract DirectoryProperty getOutput()
      @Input
      abstract Property<String> getVariantName()
      ...
  }
  ```
- **Analysis**:
  The task properly uses Gradle Managed Properties (`Property<T>`, `DirectoryProperty`) and task annotations (`@InputDirectory`, `@OutputDirectory`, `@Input`, `@Internal`). It does not reference the `Project` object inside `@TaskAction`.
- **Recommended Action**:
  Enable Configuration Cache explicitly in `gradle.properties` to test and ensure full Gradle 10 configuration cache readiness:
  ```properties
  org.gradle.configuration-cache=true
  ```

### 4. Build Script Language Modernization (Groovy DSL to Kotlin DSL)
- **Current Code**: All 13 build files (`build.gradle`, `settings.gradle`, and 11 module `build.gradle` files) use Groovy DSL (`.gradle`).
- **Gradle 10 Direction**: Kotlin DSL (`.gradle.kts`) is the primary standard in modern Gradle ecosystem and Gradle 10 releases.
- **Recommended Action**:
  Migrate `.gradle` files to `.gradle.kts` for type safety, better IDE auto-completion, and alignment with Gradle 10 standards.

### 5. Dependency Management (Version Catalog)
- **Current Code**: Dependencies and library versions are hardcoded as string literals across module `build.gradle` files.
- **Recommended Action**:
  Extract dependencies into a centralized Version Catalog file (`gradle/libs.versions.toml`). This simplifies dependency updates and prevents version mismatch issues across modules in Gradle 10.

---

## Action Plan for Gradle 10 Upgrade Readiness

1. **Update Repository Declarations**:
   Ensure `settings.gradle` wraps repository URLs in `uri(...)`.

2. **Enable Configuration Cache**:
   Add `org.gradle.configuration-cache=true` to `gradle.properties` and verify clean build execution.

3. **Migrate to Gradle Version Catalog**:
   Create `gradle/libs.versions.toml` and centralize AGP, Kotlin, AndroidX, and third-party library dependencies.

4. **Convert Build Scripts to Kotlin DSL**:
   Rename build scripts to `.gradle.kts` and update Groovy syntax to Kotlin syntax.

---

## Conclusion
The codebase is currently in excellent health with **zero active deprecation warnings or build errors on Gradle 9.7.0**. Following the recommended modernization steps above will ensure full readiness when upgrading to **Gradle 10**.
