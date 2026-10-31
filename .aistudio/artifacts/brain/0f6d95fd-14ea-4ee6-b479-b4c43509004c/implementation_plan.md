# Isolated Virtual User Profile Sandbox & Visual Clone Overlay - Implementation Plan

Detailed plan to ensure cloned applications launch inside an isolated virtual user sandbox with separate data directories, avoiding delegation to the primary main app instance, and displaying a distinct floating Badge Banner overlay.

## User Choices Confirmed
- **Architecture**: Isolated Virtual User Profile Sandbox (Multi-User DataDir Separation).
- **Visual Distinction**: Add a prominent floating Badge Banner overlay showing Clone Number, Custom Name, and User ID.

---

## 1. Core Sandbox & Virtualization Fixes

### A. Isolated Virtual User Profile Sandbox (`BlackBoxCore.kt` & `StubActivity.kt`)
- Ensure `StubActivity` runs as the host container for the isolated virtual user process (`:p0`, `:p1`, `:p2`, `:p3` process isolation declared in `AndroidManifest.xml`).
- Maintain isolated sandbox storage directories (`/virtual/users/{userId}/{packageName}`) for shared preferences, databases, caches, and files.
- Prevent unisolated host `startActivity` calls that default back to the main app task stack by binding intent extras (`blackbox_extra_user_id`, `blackbox_extra_pkg`, `blackbox_extra_clone_id`).

### B. Visual Clone Identity Badge Banner (`StubActivity.kt`)
- Display an interactive floating **Badge Banner Overlay** at the top or bottom of the virtual container view.
- Show details: `Clone #{cloneIndex}` | `User #{userId}` | `Sandboxed Storage: /virtual/users/{userId}`.
- Provide quick container controls: **Clear Cache**, **Container Stats**, and **Switch Clone**.

---

## 2. File Modification Steps

1. **`app/src/main/java/com/example/blackbox/stub/StubActivity.kt`**:
   - Add visual Badge Banner overlay with Clone Number, Custom Name, and User ID.
   - Configure sandbox environment parameters for isolated data storage.
2. **`app/src/main/java/com/example/blackbox/BlackBoxCore.kt`**:
   - Enhance intent creation to pass `user_id` and `clone_index` flags.
3. **`app/src/main/java/com/example/data/repository/CloneRepository.kt`**:
   - Pass clone metadata (`customName`, `cloneIndex`, `userId`) during launch.

---

## 3. Verification Plan
- Execute `./gradlew :app:assembleDebug :app:testDebugUnitTest`.
- Run `compile_applet`.
