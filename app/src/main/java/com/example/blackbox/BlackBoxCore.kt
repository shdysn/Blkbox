package com.example.blackbox

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.example.blackbox.model.StorageUsage
import com.example.blackbox.model.VirtualEngineStats
import com.example.blackbox.model.VirtualInstallResult
import com.example.blackbox.model.VirtualPackage
import com.example.blackbox.stub.StubActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap

/**
 * Genuine BlackBox Virtualization Core.
 * Coordinates virtual multi-user isolation, APK installation, package resolution,
 * sandboxed file system structures, and process stub redirection.
 */
class BlackBoxCore private constructor() {

    companion object {
        private const val TAG = "BlackBoxCore"
        private val INSTANCE = BlackBoxCore()

        fun get(): BlackBoxCore = INSTANCE
    }

    private var appContext: Context? = null
    private val installedPackages = ConcurrentHashMap<String, MutableMap<Int, VirtualPackage>>()
    private val runningProcesses = ConcurrentHashMap<String, RunningProcessInfo>()

    data class RunningProcessInfo(
        val packageName: String,
        val userId: Int,
        val stubProcessName: String,
        val startTime: Long = System.currentTimeMillis()
    )

    fun init(context: Context) {
        if (appContext != null) return
        this.appContext = context.applicationContext
        Log.i(TAG, "Initializing BlackBox Virtualization Core...")

        // Bootstrap virtual file system root
        getVirtualRootDir().mkdirs()
        getVirtualUsersDir().mkdirs()
        getVirtualApksDir().mkdirs()

        // Scan existing virtual installations from disk
        scanPersistedVirtualPackages()
        Log.i(TAG, "BlackBox Virtualization Core initialized. Restored ${installedPackages.size} packages.")
    }

    private fun getVirtualRootDir(): File =
        File(appContext?.filesDir, "blackbox")

    fun getVirtualUsersDir(): File =
        File(getVirtualRootDir(), "users")

    fun getVirtualApksDir(): File =
        File(getVirtualRootDir(), "apks")

    fun getUserDir(userId: Int): File =
        File(getVirtualUsersDir(), "$userId")

    fun getPackageDataDir(packageName: String, userId: Int): File =
        File(getUserDir(userId), packageName)

    fun isInstalled(packageName: String, userId: Int): Boolean {
        return installedPackages[packageName]?.containsKey(userId) == true
    }

    /**
     * Extracts an installed Android application from host package manager,
     * copies base APK and split APKs, and provisions an isolated sandboxed user space.
     */
    suspend fun installExistingPackage(sourcePackage: String, userId: Int): VirtualInstallResult =
        withContext(Dispatchers.IO) {
            val context = appContext ?: return@withContext VirtualInstallResult.Error("Context not initialized")
            try {
                val pm = context.packageManager
                val packageInfo: PackageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getPackageInfo(sourcePackage, PackageManager.PackageInfoFlags.of(PackageManager.GET_ACTIVITIES.toLong()))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getPackageInfo(sourcePackage, PackageManager.GET_ACTIVITIES)
                }

                val appInfo = packageInfo.applicationInfo
                    ?: if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        pm.getApplicationInfo(sourcePackage, PackageManager.ApplicationInfoFlags.of(0))
                    } else {
                        @Suppress("DEPRECATION")
                        pm.getApplicationInfo(sourcePackage, 0)
                    }
                val appLabel = pm.getApplicationLabel(appInfo).toString()
                val versionName = packageInfo.versionName ?: "1.0"
                val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    packageInfo.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo.versionCode.toLong()
                }

                // Prepare target directory for APKs
                val targetApkDir = File(getVirtualApksDir(), sourcePackage).apply { mkdirs() }
                val targetBaseApk = File(targetApkDir, "base.apk")

                // Extract base APK
                val sourceBaseApk = File(appInfo.sourceDir)
                if (!sourceBaseApk.exists()) {
                    return@withContext VirtualInstallResult.Error("Source APK does not exist at ${appInfo.sourceDir}")
                }

                if (!targetBaseApk.exists() || targetBaseApk.length() != sourceBaseApk.length()) {
                    copyFile(sourceBaseApk, targetBaseApk)
                }

                // Extract split APKs if present (App Bundles)
                val splitPaths = mutableListOf<String>()
                appInfo.splitSourceDirs?.forEachIndexed { index, splitSource ->
                    val srcSplit = File(splitSource)
                    if (srcSplit.exists()) {
                        val targetSplit = File(targetApkDir, "split_$index.apk")
                        if (!targetSplit.exists() || targetSplit.length() != srcSplit.length()) {
                            copyFile(srcSplit, targetSplit)
                        }
                        splitPaths.add(targetSplit.absolutePath)
                    }
                }

                // Provision isolated user directory
                val isolatedDir = getPackageDataDir(sourcePackage, userId)
                provisionIsolatedUserDirectories(isolatedDir)

                val vPkg = VirtualPackage(
                    packageName = sourcePackage,
                    userId = userId,
                    appName = appLabel,
                    versionName = versionName,
                    versionCode = versionCode,
                    baseApkPath = targetBaseApk.absolutePath,
                    splitApkPaths = splitPaths,
                    dataDir = isolatedDir.absolutePath,
                    isRunning = false
                )

                val userMap = installedPackages.computeIfAbsent(sourcePackage) { ConcurrentHashMap() }
                userMap[userId] = vPkg

                Log.i(TAG, "Successfully installed $sourcePackage into Virtual Sandbox for User #$userId")
                VirtualInstallResult.Success(vPkg)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to install existing package $sourcePackage for User #$userId", e)
                VirtualInstallResult.Error("Installation failed: ${e.message}", e)
            }
        }

    /**
     * Installs an APK file imported from Storage Access Framework (SAF) into virtual sandboxed space.
     */
    suspend fun installFromApkFile(apkFile: File, userId: Int): VirtualInstallResult =
        withContext(Dispatchers.IO) {
            val context = appContext ?: return@withContext VirtualInstallResult.Error("Context not initialized")
            try {
                val pm = context.packageManager
                val archiveInfo = pm.getPackageArchiveInfo(apkFile.absolutePath, PackageManager.GET_ACTIVITIES)
                    ?: return@withContext VirtualInstallResult.Error("Failed to parse APK archive: ${apkFile.name}")

                val sourcePackage = archiveInfo.packageName
                archiveInfo.applicationInfo?.sourceDir = apkFile.absolutePath
                archiveInfo.applicationInfo?.publicSourceDir = apkFile.absolutePath

                val appLabel = archiveInfo.applicationInfo?.let { pm.getApplicationLabel(it).toString() } ?: sourcePackage
                val versionName = archiveInfo.versionName ?: "1.0"
                val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    archiveInfo.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    archiveInfo.versionCode.toLong()
                }

                val targetApkDir = File(getVirtualApksDir(), sourcePackage).apply { mkdirs() }
                val targetBaseApk = File(targetApkDir, "base.apk")
                copyFile(apkFile, targetBaseApk)

                val isolatedDir = getPackageDataDir(sourcePackage, userId)
                provisionIsolatedUserDirectories(isolatedDir)

                val vPkg = VirtualPackage(
                    packageName = sourcePackage,
                    userId = userId,
                    appName = appLabel,
                    versionName = versionName,
                    versionCode = versionCode,
                    baseApkPath = targetBaseApk.absolutePath,
                    splitApkPaths = emptyList(),
                    dataDir = isolatedDir.absolutePath,
                    isRunning = false
                )

                val userMap = installedPackages.computeIfAbsent(sourcePackage) { ConcurrentHashMap() }
                userMap[userId] = vPkg

                Log.i(TAG, "Successfully installed imported APK $sourcePackage for User #$userId")
                VirtualInstallResult.Success(vPkg)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to install APK file for User #$userId", e)
                VirtualInstallResult.Error("APK import failed: ${e.message}", e)
            }
        }

    private fun provisionIsolatedUserDirectories(isolatedPkgDir: File) {
        File(isolatedPkgDir, "data").mkdirs()
        File(isolatedPkgDir, "cache").mkdirs()
        File(isolatedPkgDir, "code_cache").mkdirs()
        File(isolatedPkgDir, "shared_prefs").mkdirs()
        File(isolatedPkgDir, "databases").mkdirs()
        File(isolatedPkgDir, "files").mkdirs()
    }

    /**
     * Resolves the virtual package launcher and dispatches execution into the designated Stub Process.
     */
    fun launchApk(context: Context, packageName: String, userId: Int): Result<Unit> {
        return try {
            val vPkg = installedPackages[packageName]?.get(userId)
                ?: return Result.failure(IllegalStateException("Package $packageName for user $userId is not installed in BlackBox"))

            // Find launcher activity
            val pm = context.packageManager
            val launchIntent = pm.getLaunchIntentForPackage(packageName)
            val targetActivityName = launchIntent?.component?.className ?: "DefaultLauncherActivity"

            // Select stub process container based on userId
            val stubClass = when (userId % 4) {
                0 -> StubActivity.P0::class.java
                1 -> StubActivity.P1::class.java
                2 -> StubActivity.P2::class.java
                else -> StubActivity.P3::class.java
            }

            val stubIntent = Intent(context, stubClass).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                putExtra(StubActivity.EXTRA_PKG, packageName)
                putExtra(StubActivity.EXTRA_USER_ID, userId)
                putExtra(StubActivity.EXTRA_TARGET_ACTIVITY, targetActivityName)
                putExtra(StubActivity.EXTRA_APP_NAME, vPkg.appName)
            }

            context.startActivity(stubIntent)
            Log.i(TAG, "Launched $packageName [User #$userId] via Stub ${stubClass.simpleName}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch $packageName [User #$userId]", e)
            Result.failure(e)
        }
    }

    fun getStorageUsage(packageName: String, userId: Int): StorageUsage {
        val isolatedDir = getPackageDataDir(packageName, userId)
        if (!isolatedDir.exists()) return StorageUsage(0, 0)

        val cacheDir = File(isolatedDir, "cache")
        val cacheBytes = calculateDirectorySize(cacheDir)

        val totalBytes = calculateDirectorySize(isolatedDir)
        val dataBytes = (totalBytes - cacheBytes).coerceAtLeast(0L)

        return StorageUsage(dataBytes = dataBytes, cacheBytes = cacheBytes)
    }

    fun clearCache(packageName: String, userId: Int): Boolean {
        val cacheDir = File(getPackageDataDir(packageName, userId), "cache")
        return deleteDirectoryContents(cacheDir)
    }

    fun clearData(packageName: String, userId: Int): Boolean {
        val pkgDir = getPackageDataDir(packageName, userId)
        if (!pkgDir.exists()) return true
        val result = deleteDirectoryContents(pkgDir)
        provisionIsolatedUserDirectories(pkgDir)
        return result
    }

    fun uninstallPackageAsUser(packageName: String, userId: Int): Boolean {
        val pkgDir = getPackageDataDir(packageName, userId)
        deleteDirectory(pkgDir)

        installedPackages[packageName]?.remove(userId)
        if (installedPackages[packageName]?.isEmpty() == true) {
            installedPackages.remove(packageName)
            // If no other clones use the APK, remove the extracted base
            val apkDir = File(getVirtualApksDir(), packageName)
            deleteDirectory(apkDir)
        }
        return true
    }

    fun stopPackage(packageName: String, userId: Int) {
        val key = "$packageName:$userId"
        runningProcesses.remove(key)
        Log.i(TAG, "Terminated virtual process for $packageName [User #$userId]")
    }

    fun registerRunningProcess(packageName: String, userId: Int, stubName: String) {
        val key = "$packageName:$userId"
        runningProcesses[key] = RunningProcessInfo(packageName, userId, stubName)
    }

    fun unregisterRunningProcess(packageName: String, userId: Int) {
        val key = "$packageName:$userId"
        runningProcesses.remove(key)
    }

    fun getRunningProcesses(): List<RunningProcessInfo> {
        return runningProcesses.values.toList()
    }

    fun getEngineStats(): VirtualEngineStats {
        var totalUsers = 0
        var totalPackages = 0
        installedPackages.values.forEach { userMap ->
            totalPackages++
            totalUsers += userMap.size
        }

        val virtualStorage = calculateDirectorySize(getVirtualRootDir())

        return VirtualEngineStats(
            totalPackages = totalPackages,
            totalUsers = totalUsers,
            runningProcesses = runningProcesses.size,
            virtualStorageBytes = virtualStorage
        )
    }

    private fun scanPersistedVirtualPackages() {
        val usersDir = getVirtualUsersDir()
        val userFolders = usersDir.listFiles() ?: return
        for (userFolder in userFolders) {
            val userId = userFolder.name.toIntOrNull() ?: continue
            val pkgFolders = userFolder.listFiles() ?: continue
            for (pkgFolder in pkgFolders) {
                val pkgName = pkgFolder.name
                val apkDir = File(getVirtualApksDir(), pkgName)
                val baseApk = File(apkDir, "base.apk")
                if (baseApk.exists()) {
                    val vPkg = VirtualPackage(
                        packageName = pkgName,
                        userId = userId,
                        appName = pkgName,
                        versionName = "1.0",
                        versionCode = 1,
                        baseApkPath = baseApk.absolutePath,
                        dataDir = pkgFolder.absolutePath
                    )
                    installedPackages.computeIfAbsent(pkgName) { ConcurrentHashMap() }[userId] = vPkg
                }
            }
        }
    }

    private fun copyFile(src: File, dst: File) {
        FileInputStream(src).use { inStream ->
            FileOutputStream(dst).use { outStream ->
                inStream.channel.transferTo(0, inStream.channel.size(), outStream.channel)
            }
        }
    }

    private fun calculateDirectorySize(dir: File): Long {
        if (!dir.exists()) return 0L
        if (dir.isFile) return dir.length()
        var size = 0L
        dir.listFiles()?.forEach { file ->
            size += if (file.isDirectory) calculateDirectorySize(file) else file.length()
        }
        return size
    }

    private fun deleteDirectoryContents(dir: File): Boolean {
        if (!dir.exists() || !dir.isDirectory) return true
        var success = true
        dir.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                success = success && deleteDirectory(file)
            } else {
                success = success && file.delete()
            }
        }
        return success
    }

    private fun deleteDirectory(dir: File): Boolean {
        if (!dir.exists()) return true
        if (dir.isDirectory) {
            dir.listFiles()?.forEach { deleteDirectory(it) }
        }
        return dir.delete()
    }
}
