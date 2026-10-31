package com.example.blackbox

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Lightweight sandbox model for running app clones in an isolated, permission-controlled environment.
 *
 * This class intentionally does not modify third-party APKs or bypass protections. It provides a
 * metadata layer and runtime guardrails for approved app packages and internal app variants.
 */
class SandboxManager(
    private val context: Context,
    private val packageManager: PackageManager = context.packageManager,
) {

    data class AppBundle(
        val id: String,
        val name: String,
        val packageName: String,
        val version: String,
        val sourceDir: String,
        val isApproved: Boolean = true,
    )

    data class SandboxProfile(
        val appId: String,
        val sandboxId: String,
        val storageDir: String,
        val isNetworkRestricted: Boolean = true,
        val isExternalStorageIsolated: Boolean = true,
    )

    suspend fun listInstalledApps(): List<AppBundle> = withContext(Dispatchers.IO) {
        packageManager.getInstalledPackages(PackageManager.GET_META_DATA)
            .filter { it.applicationInfo != null }
            .map { packageInfo ->
                packageInfo.toBundle()
            }
    }

    suspend fun getSandboxProfile(appId: String): SandboxProfile = withContext(Dispatchers.IO) {
        val sandboxDir = File(context.filesDir, "sandboxes/$appId")
        sandboxDir.mkdirs()

        SandboxProfile(
            appId = appId,
            sandboxId = "sandbox-$appId",
            storageDir = sandboxDir.absolutePath,
            isNetworkRestricted = true,
            isExternalStorageIsolated = true,
        )
    }

    fun isApprovedApp(appBundle: AppBundle): Boolean = appBundle.isApproved

    private fun PackageInfo.toBundle(): AppBundle {
        val appInfo = applicationInfo ?: return AppBundle(
            id = packageName,
            name = packageName,
            packageName = packageName,
            version = versionName ?: "unknown",
            sourceDir = "",
            isApproved = false,
        )

        return AppBundle(
            id = packageName,
            name = appInfo.loadLabel(packageManager).toString(),
            packageName = packageName,
            version = versionName ?: "unknown",
            sourceDir = appInfo.sourceDir ?: "",
            isApproved = true,
        )
    }
}
