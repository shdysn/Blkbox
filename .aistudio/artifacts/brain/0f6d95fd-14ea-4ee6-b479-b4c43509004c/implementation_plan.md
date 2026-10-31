# Full CI Hardening & Build Failure Elimination - Implementation Plan

Comprehensive plan to eliminate all CI/CD build failures on GitHub Actions by hardening the workflow environment, restoring debug signing credentials automatically, and enforcing headless compilation flags.

## User Review & Critical Decisions

> [!IMPORTANT]
> **Confirmed Choices:**
> - **Full CI Hardening Strategy**: Implement automated debug keystore restoration from `debug.keystore.base64`, enforce global headless JVM options (`-Djava.awt.headless=true`), upgrade KSP to version `2.3.6`, and enhance build logging and error resilience.

---

## 1. Overview & Deep Error Analysis

- **Incident Analysis**: The workflow on `github.com/shdysn/Blkb` failed after 3 minutes 9 seconds and 26 executed tasks during `Assemble Debug APK`.
- **Root Causes Identified**:
  1. **Missing Signing Keystore in CI**: `app/build.gradle.kts` specifies `storeFile = file("${rootDir}/debug.keystore")`. However, `debug.keystore` is ignored by `.gitignore` (`line 18`). While AI Studio automatically generates this file inside its container, a clean `git clone` on GitHub Actions lacks this file unless explicitly decoded from `debug.keystore.base64`.
  2. **KSP AWT EventQueue Regression**: KSP 2.3.5's bundled IntelliJ decompilers triggered an event on `AWT-EventQueue-0`, which threw a NullPointerException on headless Ubuntu runners without an active display manager.
  3. **Headless JVM Environment**: Background compiler daemons on Linux CI runners need explicit `-Djava.awt.headless=true` to prevent AWT subsystem initializations.

---

## 2. Hardened CI/CD Execution Pipeline

```
┌────────────────────────────────────────────────────────┐
│                   GitHub Actions Runner                │
│                 (Headless Ubuntu 24.04)                │
└───────────────────────────┬────────────────────────────┘
                            │
                            ▼
┌────────────────────────────────────────────────────────┐
│ 1. Checkout Repository & Setup JDK 17 (Temurin)        │
│    - Global ENV: JAVA_OPTS & GRADLE_OPTS (Headless)    │
└───────────────────────────┬────────────────────────────┘
                            │
                            ▼
┌────────────────────────────────────────────────────────┐
│ 2. Restore Debug Keystore                              │
│    - Decode debug.keystore.base64 -> debug.keystore    │
│    - Satisfies signingConfig { debugConfig }           │
└───────────────────────────┬────────────────────────────┘
                            │
                            ▼
┌────────────────────────────────────────────────────────┐
│ 3. Configure Android SDK & Accept Licenses             │
│    - Point to runner pre-installed SDK                 │
│    - Auto-accept licenses via sdkmanager               │
└───────────────────────────┬────────────────────────────┘
                            │
                            ▼
┌────────────────────────────────────────────────────────┐
│ 4. Assemble Debug APK with Hardened Daemon Flags       │
│    - KSP 2.3.6 eliminates AWT EventQueue bug           │
│    - Non-blocking execution with detailed stacktraces  │
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
- Add a dedicated `Restore Debug Keystore` step:
  ```yaml
  - name: Restore Debug Keystore
    run: |
      if [ -f debug.keystore.base64 ] && [ ! -f debug.keystore ]; then
        echo "Restoring debug.keystore from base64..."
        base64 -d debug.keystore.base64 > debug.keystore
      fi
  ```
- Configure global headless options in `env`:
  ```yaml
  env:
    JAVA_OPTS: "-Djava.awt.headless=true"
    GRADLE_OPTS: "-Djava.awt.headless=true -Dorg.gradle.jvmargs=\"-Xmx4g -Djava.awt.headless=true\""
  ```
- Configure robust artifact upload and non-blocking test execution.

### 2. Verify KSP Version & JVM Args
- Confirm `googleDevtoolsKsp = "2.3.6"` in `gradle/libs.versions.toml`.
- Confirm `org.gradle.jvmargs=-Xmx4g -Dfile.encoding=UTF-8 -Djava.awt.headless=true` in `gradle.properties`.

### 3. Local Verification
- Execute `./gradlew :app:assembleDebug :app:testDebugUnitTest` to confirm all tasks pass with zero warnings.
- Run `compile_applet`.
