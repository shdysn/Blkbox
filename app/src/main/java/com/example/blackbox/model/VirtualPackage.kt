package com.example.blackbox.model

import java.io.File

/**
 * Represents an application installed inside BlackBox's virtual sandboxed environment.
 */
data class VirtualPackage(
    val packageName: String,
    val userId: Int,
    val appName: String,
    val versionName: String,
    val versionCode: Long,
    val baseApkPath: String,
    val splitApkPaths: List<String> = emptyList(),
    val dataDir: String,
    val isRunning: Boolean = false
) {
    val totalApkSize: Long
        get() {
            var size = File(baseApkPath).let { if (it.exists()) it.length() else 0L }
            splitApkPaths.forEach { path ->
                val f = File(path)
                if (f.exists()) size += f.length()
            }
            return size
        }
}

sealed class VirtualInstallResult {
    data class Success(val virtualPackage: VirtualPackage) : VirtualInstallResult()
    data class Error(val message: String, val cause: Throwable? = null) : VirtualInstallResult()
}

data class VirtualEngineStats(
    val totalPackages: Int,
    val totalUsers: Int,
    val runningProcesses: Int,
    val virtualStorageBytes: Long,
    val engineVersion: String = "BlackBox Core v2.4.0-release"
)

data class StorageUsage(
    val dataBytes: Long,
    val cacheBytes: Long
) {
    val totalBytes: Long get() = dataBytes + cacheBytes
}
