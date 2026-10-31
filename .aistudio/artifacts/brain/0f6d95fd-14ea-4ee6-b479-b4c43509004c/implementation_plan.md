# CI/CD Fix & Test Optimization - Implementation Plan

Resolve the GitHub Actions build failure on `Run Unit Tests` (2m 30s timeout) and ensure APK compilation and artifact uploads succeed reliably.

## User Review & Critical Decisions

> [!IMPORTANT]
> **Confirmed Choices:**
> - **Build Order**: Prioritize APK assembly (`:app:assembleDebug` and `:app:assembleRelease`) and artifact upload before running tests. This ensures that installable APK artifacts are always built and available even if a test suite encounters runner network issues.
> - **Test Strategy**: Convert tests into fast, pure JVM unit tests for business logic, data models, Room entity validation, and formatters. This eliminates Robolectric's 2.5-minute download timeouts for SDK 36 shadow jars on clean GitHub Actions runners.

---

## 1. Overview & Problem Diagnosis

- **Incident Analysis**: In the latest GitHub Actions run, the workflow failed after 2 minutes 32 seconds during `Run Unit Tests` with exit code 1.
- **Root Cause**: Robolectric attempted to download Android SDK 36 shadow artifacts (`android-all-36`) over the network from Maven Central. Because SDK 36 is preview/unreleased in Robolectric's Maven repository, the runner spent 2.5 minutes attempting retries before failing with `MavenArtifactFetcher` / `HttpURLConnection` `IOException`.
- **Pipeline Architecture Flaw**: `Run Unit Tests` was ordered prior to `Assemble Debug APK`. Consequently, the failure in the test step aborted the entire pipeline before the APK could ever be assembled or uploaded.

---

## 2. Technical Architecture & CI/CD Pipeline

### Updated Resilient Workflow Execution Flow

```
┌────────────────────────────────────────────────────────┐
│                   GitHub Actions Runner                │
│                     (ubuntu-latest)                    │
└───────────────────────────┬────────────────────────────┘
                            │
                            ▼
┌────────────────────────────────────────────────────────┐
│ 1. Checkout Code & Set up JDK 17 (with Gradle Cache)   │
└───────────────────────────┬────────────────────────────┘
                            │
                            ▼
┌────────────────────────────────────────────────────────┐
│ 2. Assemble Debug APK (CRITICAL PATH FIRST)            │
│    - ./gradlew :app:assembleDebug                      │
│    - Verifies non-zero APK binary output               │
└───────────────────────────┬────────────────────────────┘
                            │
                            ▼
┌────────────────────────────────────────────────────────┐
│ 3. Upload Debug APK Artifact (Guaranteed Delivery)     │
│    - actions/upload-artifact@v4                        │
│    - Artifact: app-cloner-debug-apk                    │
└───────────────────────────┬────────────────────────────┘
                            │
                            ▼
┌────────────────────────────────────────────────────────┐
│ 4. Assemble Release APK (Unsigned/Configured)          │
│    - ./gradlew :app:assembleRelease                    │
│    - Upload Artifact: app-cloner-release-apk           │
└───────────────────────────┬────────────────────────────┘
                            │
                            ▼
┌────────────────────────────────────────────────────────┐
│ 5. Execute Fast JVM Unit Tests                         │
│    - ./gradlew :app:testDebugUnitTest                  │
│    - Pure JVM tests: instant execution (< 2 seconds)   │
│    - No external Maven shadow jar downloads            │
└────────────────────────────────────────────────────────┘
```

---

## 3. Key Changes & Execution Steps

### 1. Update GitHub Actions Workflow (`.github/workflows/build-apk.yml`)
- Move `Assemble Debug APK`, `Verify Debug APK Output`, and `Upload Debug APK Artifact` to the top of the job immediately after setup.
- Follow with `Assemble Release APK` and `Upload Release APK Artifact`.
- Run `Run Unit Tests` as a verification step without blocking APK generation.

### 2. Optimize Unit Test Suite (`app/src/test/java/com/example/`)
- In `ExampleUnitTest.kt`:
  - Add comprehensive unit tests for `Formatters.formatBytes` and `formatTimestamp`.
  - Add validation tests for `VirtualPackage` total APK size calculations.
  - Add tests for `CloneInstanceEntity` creation and data defaults.
- In `ExampleRobolectricTest.kt`:
  - Configure Robolectric to disable remote Maven dependency fetching (`robolectric.offline = true` or use standard JVM test runner) to prevent CI timeouts.

### 3. Local Verification
- Execute `./gradlew :app:assembleDebug` and `./gradlew :app:testDebugUnitTest`.
- Ensure execution completes in seconds without network fetches.
- Run `compile_applet` to verify platform build health.
