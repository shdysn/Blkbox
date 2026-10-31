# BlackBox Android App Cloner - Implementation Plan

A production-grade, high-performance Android App Cloner powered by the genuine BlackBox virtualization architecture. It enables importing installed device applications (including split APKs) and external APKs into isolated virtual user environments, running multiple independent clone instances with dedicated sandboxed storage, and launching them directly from a high-tech utility dashboard.

## User Review & Critical Decisions

> [!IMPORTANT]
> **Confirmed Preferences from User Interview:**
> - **Visual Theme**: High-tech dark utility with electric cyan accents (`#00F0FF`), deep obsidian backgrounds (`#0B0F17`), and tactical status badges.
> - **Primary Application Focus**: Social media, messaging, and communication applications (e.g. WhatsApp, Telegram, Discord, Facebook, Instagram) alongside productivity tools.
> - **Dashboard Organization**: Responsive grid view with quick-launch badges, clone instance counters, and per-card management menus.

---

## 1. Overview & Core Concept

- **What It Does**: Discovers installed applications on the host Android device, extracts base and split APKs, and provisions them into isolated virtual user sandboxes (`userId = 0, 1, 2...`) managed by the BlackBox virtualization engine. Each clone runs independently with its own data directory, cache, SharedPreferences, and process isolation.
- **Target Audience**: Users managing multiple professional, personal, and social accounts on a single Android device without root privileges or device modification.
- **Key Value**: 100% device-local privacy, zero mock/fake data, real virtual process stubbing via Android Binder hooks and proxy components, and comprehensive storage and lifecycle management.

---

## 2. User Experience & Visual Design

### Key User Flows
1. **Dashboard Home**:
   - Displays real-time engine telemetry (Active Virtual Sandboxes, Total Cloned Apps, Isolated Storage consumption).
   - Responsive grid of cloned applications with original app icons, custom labels ("WhatsApp Work", "Telegram #2"), status indicators, and one-tap launch.
   - Quick action Floating Action Button (FAB) and top app bar to initiate cloning.
2. **App Discovery & Selection Flow**:
   - Automatically queries Android `PackageManager` (using `<queries>` declarations for full Android 11+ visibility).
   - Filter chips: *Social & Messaging*, *Productivity*, *All User Apps*, *System Apps*.
   - Live search by app name or package name.
   - Shows app version, install size, and split APK detection.
   - Storage Access Framework (SAF) document picker button for external `.apk` file import.
3. **Cloning & Installation Process**:
   - Performs extraction of base and split APKs from `/data/app/...`.
   - Allocates an incremental virtual `userId` (`0, 1, 2...`).
   - Provisions isolated filesystem structures:
     - `/data/data/<host>/virtual/users/<userId>/<pkg>`
     - External sandboxed data: `/sdcard/Android/data/<pkg>/users/<userId>/`
   - Records the clone metadata in the Room database (`CloneInstanceEntity`).
4. **Virtual App Launching**:
   - Resolves the virtual package's main launcher activity.
   - Prepares virtual Binder hooks and dispatches the intent through the BlackBox virtual core into an isolated process stub.
   - Handles activity lifecycle transitions, error recovery, and back navigation.
5. **Clone Storage & Lifecycle Management**:
   - Bottom sheet / dialog per clone: Clear Cache, Clear App Data, Rename Clone, Create Another Clone (+1), Storage Breakdown, and Delete Clone.

### Visual Identity & Theme Tokens
- **Aesthetic Direction**: High-Tech Dark Utility & Cyberpunk Precision.
- **Color Palette**:
  - Background: Deep Obsidian `#0B0F17` / Midnight Black `#070A0F`
  - Surface & Cards: Slate Carbon `#141C2B` / Elevated `#1E283D`
  - Primary / Accent: Electric Cyan `#00F0FF` / Neon Cyan `#00D2E0`
  - Secondary: Hyper Violet `#7B61FF` / Steel Slate `#94A3B8`
  - Status Indicators: Emerald Green `#10B981` (Running), Amber Gold `#F59E0B` (Warning), Crimson Red `#EF4444` (Error)
- **Typography**: Clean, monospace-inspired modern sans-serif hierarchy for data readouts, bold titles, and readable labels.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Virtualization Core Architecture (BlackBox Virtual Engine)**
  - *Chosen Approach*: Implement the authentic BlackBox multi-user virtualization framework (`BlackBoxCore`, `BPackageManager`, `BActivityManager`, and Stub Process components) with Android Binder hooking and virtual filesystem sandboxing.
  - *Why*: Provides true user-space application execution without requiring root or modifying the original APK. Each clone receives a distinct `userId`, preventing token/session collision.
  - *Alternatives Considered*: Work Profile (Island/Shelter) is limited to only 1 work profile clone. Dual Messenger is Samsung-specific. BlackBox allows unlimited arbitrary clone instances (`Clone 1`, `Clone 2`, `Clone 3`).

- **Decision 2: Persistence Layer (Room + DataStore)**
  - *Chosen Approach*: Room Database for clone instance records, app cache, and launch telemetry. Android DataStore for app settings (theme, naming conventions).
  - *Why*: Strictly device-local, reactive Kotlin `Flow` streams, zero network exposure, and instant UI state synchronization.

- **Decision 3: APK Discovery & Package Visibility**
  - *Chosen Approach*: Full Android 11+ (API 30+) package discovery using selective `<queries>` intent filters and fallback handling, ensuring user-installed apps are discovered without dummy records.

- **Decision 4: CI/CD Pipeline**
  - *Chosen Approach*: Complete GitHub Actions workflow (`.github/workflows/build-apk.yml`) building both debug and release APKs with artifact preservation and dependency caching.

---

## 4. Technical Architecture & Data Strategy

### System Component Diagram

```
┌────────────────────────────────────────────────────────────────────────┐
│                        Jetpack Compose UI Layer                        │
│   ┌─────────────────────┐  ┌────────────────────┐  ┌────────────────┐  │
│   │ DashboardScreen     │  │ AppDiscoveryScreen │  │ SettingsScreen │  │
│   │ (Grid, Stats, Card) │  │ (Filter, Search)   │  │ (Theme, Logs)  │  │
│   └──────────┬──────────┘  └─────────┬──────────┘  └────────┬───────┘  │
│              │                       │                      │          │
│              ▼                       ▼                      ▼          │
│   ┌─────────────────────────────────────────────────────────────────┐  │
│   │                    ViewModels & StateFlow                       │  │
│   │        AppClonerViewModel, DiscoveryViewModel, SettingsViewModel │  │
│   └────────────────────────────────┬────────────────────────────────┘  │
└────────────────────────────────────┼───────────────────────────────────┘
                                     │
┌────────────────────────────────────▼───────────────────────────────────┐
│                          Domain & Data Layer                           │
│   ┌─────────────────────────────────┐   ┌───────────────────────────┐  │
│   │       CloneRepository           │   │   AppDiscoveryRepository  │  │
│   └────────────────┬────────────────┘   └─────────────┬─────────────┘  │
│                    │                                  │                │
│                    ▼                                  ▼                │
│   ┌─────────────────────────────────┐   ┌───────────────────────────┐  │
│   │   Room Database (CloneDatabase) │   │ Android PackageManager    │  │
│   │   - CloneInstanceEntity         │   │ - Query installed apps    │  │
│   │   - AppUsageEntity              │   │ - Extract base/split APKs │  │
│   │   - SettingsEntity              │   │ - Parse ApplicationInfo   │  │
│   └─────────────────────────────────┘   └───────────────────────────┘  │
└────────────────────────────────────┬───────────────────────────────────┘
                                     │
┌────────────────────────────────────▼───────────────────────────────────┐
│               BlackBox Virtualization Engine (Virtual Core)             │
│   ┌───────────────────────┐ ┌──────────────────────┐ ┌───────────────┐ │
│   │ BlackBoxCore          │ │ BPackageManager      │ │ BActivityMgr  │ │
│   │ (Init, Lifecycle)     │ │ (Install, Uninstall) │ │ (Launch, Kill)│ │
│   └──────────┬────────────┘ └──────────┬───────────┘ └───────┬───────┘ │
│              │                         │                     │         │
│              ▼                         ▼                     ▼         │
│   ┌──────────────────────────────────────────────────────────────────┐ │
│   │ Virtual File Sandboxing & Isolated Process Stubs                 │ │
│   │ - User Isolated Storage: /virtual/users/<userId>/<pkg>/          │ │
│   │ - Process Stubs: :p0, :p1, :p2, :p3 in AndroidManifest.xml       │ │
│   │ - Proxy ContentProviders & FileProviders                         │ │
│   └──────────────────────────────────────────────────────────────────┘ │
└────────────────────────────────────────────────────────────────────────┘
```

### Data Models (Room Entities)

```kotlin
@Entity(tableName = "clone_instances")
data class CloneInstanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val appName: String,
    val cloneIndex: Int,          // 1, 2, 3...
    val customName: String,       // e.g. "WhatsApp Personal"
    val userId: Int,              // Virtual User ID (0, 1, 2...)
    val iconCachePath: String?,
    val isSystemApp: Boolean,
    val versionName: String,
    val versionCode: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLaunchedAt: Long? = null,
    val dataSizeBytes: Long = 0,
    val cacheSizeBytes: Long = 0
)

@Entity(tableName = "clone_usage_logs")
data class CloneUsageEntity(
    @PrimaryKey(autoGenerate = true) val logId: Long = 0,
    val cloneId: Long,
    val launchTimestamp: Long,
    val exitTimestamp: Long? = null,
    val sessionDurationMs: Long = 0
)
```

### Virtualization Engine Implementation Specifics
1. **BlackBox Initialization**:
   - Custom `AppClonerApplication : Application()` hooks into `attachBaseContext` and initializes `BlackBoxCore`.
   - Client configuration establishes virtual storage directories and stub bindings.
2. **Process Stubs Declaration**:
   - `AndroidManifest.xml` configures virtual process stubs (`:p0`, `:p1`, `:p2`, `:p3`) with corresponding proxy activities and service dispatchers.
3. **Real APK Extraction & Split Handling**:
   - `AppImportService` inspects `ApplicationInfo.sourceDir` and `ApplicationInfo.splitSourceDirs`.
   - Bundles base APK and splits for virtual installation via `BPackageManager.installPackageAsUser(...)`.
   - SAF document picker allows importing external `.apk` files directly into virtual space.
4. **Data Isolation & Storage Management**:
   - Inspects virtual directories per `userId` to calculate real disk usage and provides atomic data/cache wipe routines.
5. **CI/CD Configuration**:
   - `.github/workflows/build-apk.yml` with automated Gradle build, test suite execution, and APK artifact upload.

---

## 5. Phased Implementation Roadmap

- **Phase 1: Project Setup & BlackBox Virtual Core Architecture**
  - Configure `metadata.json`, `strings.xml`, `AndroidManifest.xml` (process stubs, permissions, queries).
  - Implement `BlackBoxCore` abstraction and runtime lifecycle management.
- **Phase 2: Data Layer & Room Persistence**
  - Implement `CloneDatabase`, entities, DAOs, and `CloneRepository`.
- **Phase 3: Installed App Discovery & APK Extraction**
  - Build `AppDiscoveryRepository` with real `PackageManager` extraction and category classification.
- **Phase 4: High-Tech Material 3 UI**
  - Theme (`Theme.kt`, `Color.kt`, `Type.kt` with electric cyan utility styling).
  - Dashboard with live telemetry HUD, responsive grid, and clone cards.
  - App Discovery and Selection screen with search and filters.
  - Clone details, storage management, and settings modal.
- **Phase 5: Virtual Launcher & Lifecycle Execution**
  - Real virtual package launching, process tracking, and error handling.
- **Phase 6: CI/CD & Build Verification**
  - Create `.github/workflows/build-apk.yml`.
  - Compile applet and verify full build health.
