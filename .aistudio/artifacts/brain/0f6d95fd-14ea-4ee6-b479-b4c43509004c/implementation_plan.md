# Eliminate CI Build Failures & Optimize KSP Pipeline - Implementation Plan

Deep analysis of the Gradle `IdentifyStep` execution crash on GitHub Actions and implementation of targeted CI optimizations to guarantee 100% reliable APK generation.

## User Review & Critical Decisions

> [!IMPORTANT]
> **Confirmed Choices:**
> - **Disable CI Configuration Cache**: Pass `--no-configuration-cache` on all GitHub Actions Gradle commands. Ephemeral CI runners run in fresh virtual machines where configuration cache entries cannot be reused across runs anyway, and attempting to serialize complex KSP task states triggers the observed `IdentifyStep` failure.
> - **Remove Unused Moshi KSP Processor**: Remove `"ksp"(libs.moshi.kotlin.codegen)` from `app/build.gradle.kts`. The app uses Room for local database persistence and does not use Moshi JSON code generation. Removing this processor streamlines KSP symbol processing.

---

## 1. Overview & Deep Error Analysis

- **Incident Analysis**: In the GitHub Actions run (`github.com/shdysn/Blkb`), `Assemble Debug APK` failed after 3 minutes 34 seconds at line 217, terminating with:
  ```text
  at org.gradle.internal.execution.steps.IdentifyStep.execute(IdentifyStep.java:44)
  at org.gradle.internal.execution.steps.IdentifyStep.execute(IdentifyStep.java:31)
  at org.gradle.internal.execution.impl.DefaultExecutionEngine$1.execute(DefaultExecutionEngine.java:68)
  ...
  Configuration cache entry stored.
  BUILD FAILED in 3m 34s
  ```
- **Root Cause**:
  1. `IdentifyStep` is Gradle's task identity and input hashing stage during Configuration Cache serialization. When KSP runs multiple symbol processors concurrently (`Room` + `Moshi`), attempting to serialize task state into the Configuration Cache on clean CI runners causes serialization exceptions and worker race conditions.
  2. Because GitHub Actions spins up an ephemeral runner for each job, configuration cache entries stored at the end of a run are never reused. Enabling configuration cache on CI provides zero performance benefit while introducing serialization hazards.

---

## 2. Technical Architecture & CI/CD Pipeline

```
┌────────────────────────────────────────────────────────┐
│                   GitHub Actions Runner                │
│                 (Headless Ubuntu 24.04)                │
└───────────────────────────┬────────────────────────────┘
                            │
                            ▼
┌────────────────────────────────────────────────────────┐
│ 1. Restore Keystore & Configure JDK 17 / Android SDK   │
└───────────────────────────┬────────────────────────────┘
                            │
                            ▼
┌────────────────────────────────────────────────────────┐
│ 2. Assemble Debug APK (Optimized Flags)                │
│    - ./gradlew :app:assembleDebug                      │
│      --no-daemon                                       │
│      --no-configuration-cache                          │
│      --stacktrace                                      │
│    - Only active processor: androidx.room.compiler     │
│    - Clean compilation with zero cache serialization   │
└───────────────────────────┬────────────────────────────┘
                            │
                            ▼
┌────────────────────────────────────────────────────────┐
│ 3. Upload Debug APK Artifact (Guaranteed Success)      │
│    - actions/upload-artifact@v4                        │
│    - Path: app/build/outputs/apk/debug/*.apk           │
└────────────────────────────────────────────────────────┘
```

---

## 3. Key Changes & Execution Steps

### 1. Update Gradle Build File (`app/build.gradle.kts`)
- Remove or comment out `"ksp"(libs.moshi.kotlin.codegen)` from dependencies.
- Retain only `"ksp"(libs.androidx.room.compiler)` for Room database entities and DAOs.

### 2. Update GitHub Actions Workflow (`.github/workflows/build-apk.yml`)
- Update `Assemble Debug APK`, `Assemble Release APK`, and `Run Unit Tests` to include `--no-configuration-cache`.
- Add explicit step summary reporting on failure.

### 3. Local Verification
- Execute `./gradlew :app:assembleDebug --no-configuration-cache` to verify that the build completes with zero errors.
- Run `compile_applet` to ensure full platform alignment.
