# CI/CD Fix & Gradle Wrapper Generation - Implementation Plan

Resolve the GitHub Actions build failure on the "Setup Android SDK" step and generate complete Gradle Wrapper scripts to ensure reliable, automated APK building, testing, and artifact deployment on GitHub.

## User Review & Critical Decisions

> [!IMPORTANT]
> **Confirmed Choices:**
> - **SDK Configuration**: Use GitHub-hosted Ubuntu runner's pre-installed Android SDK environment (`$ANDROID_HOME` / `$ANDROID_SDK_ROOT`) with automated license acceptance instead of the fragile third-party `android-actions/setup-android` action.
> - **Gradle Wrapper**: Generate full official Gradle Wrapper scripts (`gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`) in the repository so CI runners and developers build using the exact pinned Gradle version.

---

## 1. Overview & Problem Diagnosis

- **Incident Analysis**: As seen in the workflow run log, the action failed 8 seconds into the `Setup Android SDK` step using `android-actions/setup-android@v3`. This third-party action encountered repository resolution failures and Node runtime incompatibilities on `ubuntu-latest`.
- **Environment Reality**: GitHub's `ubuntu-latest` runner images already provide a fully configured Android SDK (`/usr/local/lib/android/sdk`) containing platform tools, build tools, and SDK managers. Redownloading the entire SDK via third-party actions is redundant and error-prone.
- **Repository Missing Files**: The repository was missing the executable `gradlew` script and `gradle-wrapper.jar`, requiring a fallback that can vary across different runner environments.

---

## 2. Technical Architecture & CI/CD Pipeline

### Workflow Execution Flow

```
┌────────────────────────────────────────────────────────┐
│                   GitHub Actions Runner                │
│                     (ubuntu-latest)                    │
└───────────────────────────┬────────────────────────────┘
                            │
                            ▼
┌────────────────────────────────────────────────────────┐
│ 1. Checkout Code (actions/checkout@v4)                 │
└───────────────────────────┬────────────────────────────┘
                            │
                            ▼
┌────────────────────────────────────────────────────────┐
│ 2. Set up JDK 17 (actions/setup-java@v4)               │
│    - Temurin distribution with native Gradle caching   │
└───────────────────────────┬────────────────────────────┘
                            │
                            ▼
┌────────────────────────────────────────────────────────┐
│ 3. Configure Android SDK & Accept Licenses             │
│    - Point ANDROID_HOME to pre-installed SDK root      │
│    - Accept licenses: yes | sdkmanager --licenses      │
└───────────────────────────┬────────────────────────────┘
                            │
                            ▼
┌────────────────────────────────────────────────────────┐
│ 4. Execute Gradle Test & Assemble Tasks                │
│    - ./gradlew :app:testDebugUnitTest                  │
│    - ./gradlew :app:assembleDebug                      │
│    - ./gradlew :app:assembleRelease (optional/unsigned)│
└───────────────────────────┬────────────────────────────┘
                            │
                            ▼
┌────────────────────────────────────────────────────────┐
│ 5. Upload APK Artifacts (actions/upload-artifact@v4)   │
│    - app-cloner-debug-apk                              │
│    - app-cloner-release-apk                            │
└────────────────────────────────────────────────────────┘
```

---

## 3. Key Changes & Execution Steps

### 1. Update GitHub Actions Workflow (`.github/workflows/build-apk.yml`)
- Remove the failing `android-actions/setup-android@v3` step.
- Replace with built-in Android SDK configuration:
  - Export `ANDROID_HOME=$ANDROID_SDK_ROOT` and `ANDROID_NDK_HOME` to `$GITHUB_ENV`.
  - Accept all SDK licenses automatically via `sdkmanager --licenses`.
- Ensure `chmod +x gradlew` runs before Gradle tasks.
- Keep dependency caching enabled via `actions/setup-java@v4 (cache: 'gradle')`.
- Upload built APKs as GitHub artifacts with retention.

### 2. Generate Full Gradle Wrapper
- Run `gradle wrapper --gradle-version 8.11.1` to generate:
  - `/gradlew` (Linux/macOS executable shell script)
  - `/gradlew.bat` (Windows batch script)
  - `/gradle/wrapper/gradle-wrapper.jar` (Wrapper runtime)
  - `/gradle/wrapper/gradle-wrapper.properties` (Distribution configuration)

### 3. Verification & Validation
- Test Gradle wrapper execution locally (`./gradlew --version`).
- Verify compilation and unit tests succeed with the wrapper (`./gradlew :app:assembleDebug :app:testDebugUnitTest`).
