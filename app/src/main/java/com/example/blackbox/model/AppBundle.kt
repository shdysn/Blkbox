package com.example.blackbox.model

import android.content.pm.PackageInfo
import android.content.pm.PackageManager

/**
 * Represents an approved internal app or app variant that the Black Box host may launch.
 *
 * This model intentionally does not represent cloning of third-party apps. It represents an
 * original application profile managed by the host's own sandbox policy.
 */
data class AppBundle(
    val id: String,
    val name: String,
    val packageName: String,
    val versionName: String,
    val launcherActivity: String? = null,
    val isApproved: Boolean = true,
    val isInstalled: Boolean = false,
    val sourcePackage: String? = null,
) {
    companion object {
        fun fromPackageInfo(
            packageInfo: PackageInfo,
            packageManager: PackageManager,
        ): AppBundle {
            val appInfo = packageInfo.applicationInfo
            val label = appInfo?.loadLabel(packageManager)?.toString() ?: packageInfo.packageName

            return AppBundle(
                id = packageInfo.packageName,
                name = label,
                packageName = packageInfo.packageName,
                versionName = packageInfo.versionName ?: "unknown",
                launcherActivity = null,
                isApproved = true,
                isInstalled = true,
                sourcePackage = packageInfo.packageName,
            )
        }
    }
}
