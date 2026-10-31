# Fix KSP AWT-EventQueue NullPointerException in CI - Implementation Plan

Resolve the `kspDebugKotlin` failure caused by the known KSP 2.3.5 IntelliJ AWT EventQueue bug on headless CI runners by updating to KSP 2.3.6 and enforcing headless JVM arguments.

## User Review & Critical Decisions

> [!IMPORTANT]
> **Confirmed Choices:**
> - **KSP Plugin Upgrade**: Bump `googleDevtoolsKsp` from `2.3.5` to `2.3.6` in `gradle/libs.versions.toml`. This brings in the upstream patch that prevents KSP's bundled IntelliJ file decompilers from dispatching to uninitialized AWT EventQueues.
> - **Headless JVM Configuration**: Add `-Djava.awt.headless=true` to `gradle.properties` and the GitHub Actions environment (`JAVA_OPTS` and `GRADLE_OPTS`) to guarantee that all background compiler daemons and worker processes run in pure headless mode.

---

## 1. Overview & Problem Diagnosis

- **Incident Analysis**: In the GitHub Actions run, `Assemble Debug APK` failed during `:app:kspDebugKotlin` with:
  ```text
  Exception in thread "AWT-EventQueue-0" java.lang.NullPointerException: Cannot invoke "ksp.com.intellij.openapi.application.Application.getService(java.lang.Class)" because the return value of "ksp.com.intellij.openapi.application.ApplicationManager.getApplication()" is null
      at ksp.com.intellij.openapi.fileEditor.FileDocumentManager.getInstance(FileDocumentManager.java:35)
      at ksp.com.intellij.openapi.fileTypes.BinaryFileTypeDecompilers.lambda$notifyDecompilerSetChange$1(BinaryFileTypeDecompilers.java:29)
  ```
- **Root Cause**: KSP 2.3.5 contains an IntelliJ platform shading defect where `BinaryFileTypeDecompilers` triggers an event on `AWT-EventQueue-0`. In headless Linux environments (like GitHub's Ubuntu runners), the IntelliJ Application context is null, triggering a NullPointerException that halts task execution.

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
│ Environment Variables Configured                       │
│ - JAVA_OPTS="-Djava.awt.headless=true"                 │
│ - GRADLE_OPTS="-Djava.awt.headless=true"               │
│ - org.gradle.jvmargs with -Djava.awt.headless=true     │
└───────────────────────────┬────────────────────────────┘
                            │
                            ▼
┌────────────────────────────────────────────────────────┐
│ KSP 2.3.6 (googleDevtoolsKsp = "2.3.6")                │
│ - Upstream patch applied: AWT callback eliminated      │
│ - Clean compilation of Room DAOs & Moshi adapters      │
└───────────────────────────┬────────────────────────────┘
                            │
                            ▼
┌────────────────────────────────────────────────────────┐
│ APK Assembly & Artifact Upload                         │
│ - :app:assembleDebug                                   │
│ - app-cloner-debug-apk artifact uploaded               │
└────────────────────────────────────────────────────────┘
```

---

## 3. Key Changes & Execution Steps

### 1. Update Version Catalog (`gradle/libs.versions.toml`)
- Change `googleDevtoolsKsp = "2.3.5"` to `googleDevtoolsKsp = "2.3.6"`.

### 2. Configure Headless Flag in `gradle.properties`
- Update `org.gradle.jvmargs`:
  ```properties
  org.gradle.jvmargs=-Xmx4g -Dfile.encoding=UTF-8 -Djava.awt.headless=true
  ```

### 3. Update GitHub Actions Workflow (`.github/workflows/build-apk.yml`)
- Set environment variables in the workflow:
  ```yaml
  env:
    JAVA_OPTS: "-Djava.awt.headless=true"
    GRADLE_OPTS: "-Djava.awt.headless=true"
  ```

### 4. Verification & Validation
- Run `./gradlew :app:assembleDebug` locally with the updated KSP plugin and headless flags.
- Confirm compilation succeeds via `compile_applet`.
