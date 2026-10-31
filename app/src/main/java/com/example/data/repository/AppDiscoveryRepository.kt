package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.example.data.database.dao.InstalledAppDao
import com.example.data.database.entity.InstalledAppEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

class AppDiscoveryRepository(
    private val context: Context,
    private val installedAppDao: InstalledAppDao
) {
    companion object {
        private const val TAG = "AppDiscoveryRepo"

        private val SOCIAL_KEYWORDS = listOf(
            "whatsapp", "telegram", "facebook", "messenger", "discord", "instagram",
            "twitter", "x.com", "signal", "wechat", "line", "viber", "skype", "slack",
            "tiktok", "snapchat", "reddit", "vk", "threads", "linkedin", "social", "chat"
        )

        private val PRODUCTIVITY_KEYWORDS = listOf(
            "mail", "gmail", "outlook", "chrome", "firefox", "browser", "docs", "sheets",
            "slides", "drive", "dropbox", "onedrive", "notion", "trello", "asana", "evernote",
            "keep", "calendar", "notes", "office", "zoom", "teams", "pdf"
        )
    }

    val installedApps: Flow<List<InstalledAppEntity>> = installedAppDao.getAllInstalledApps()

    suspend fun refreshInstalledApps(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val pm = context.packageManager
            val hostPackageName = context.packageName

            // Query apps with launcher intent for reliable discoverability
            val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val resolveInfos = pm.queryIntentActivities(launcherIntent, 0)

            val packageNames = resolveInfos.map { it.activityInfo.packageName }.distinct()

            val appEntities = mutableListOf<InstalledAppEntity>()

            for (pkgName in packageNames) {
                // Don't list this app cloner inside itself
                if (pkgName == hostPackageName) continue

                try {
                    val packageInfo: PackageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        pm.getPackageInfo(pkgName, PackageManager.PackageInfoFlags.of(0))
                    } else {
                        @Suppress("DEPRECATION")
                        pm.getPackageInfo(pkgName, 0)
                    }

                    val appInfo = packageInfo.applicationInfo
                        ?: if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            pm.getApplicationInfo(pkgName, PackageManager.ApplicationInfoFlags.of(0))
                        } else {
                            @Suppress("DEPRECATION")
                            pm.getApplicationInfo(pkgName, 0)
                        }
                    val appLabel = pm.getApplicationLabel(appInfo).toString()
                    val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

                    val versionName = packageInfo.versionName ?: "1.0"
                    val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        packageInfo.longVersionCode
                    } else {
                        @Suppress("DEPRECATION")
                        packageInfo.versionCode.toLong()
                    }

                    val hasSplits = appInfo.splitSourceDirs != null && appInfo.splitSourceDirs!!.isNotEmpty()
                    val category = classifyCategory(pkgName, appLabel, isSystem)

                    appEntities.add(
                        InstalledAppEntity(
                            packageName = pkgName,
                            appName = appLabel,
                            versionName = versionName,
                            versionCode = versionCode,
                            isSystemApp = isSystem,
                            sourceApkPath = appInfo.sourceDir,
                            category = category,
                            hasSplitApks = hasSplits,
                            lastDiscoveredAt = System.currentTimeMillis()
                        )
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Could not inspect package $pkgName: ${e.message}")
                }
            }

            // Fallback: also scan all installed applications if list is small
            if (appEntities.size < 5) {
                val installedApplications = pm.getInstalledApplications(0)
                for (appInfo in installedApplications) {
                    if (appInfo.packageName == hostPackageName) continue
                    if (appEntities.none { it.packageName == appInfo.packageName }) {
                        try {
                            val label = pm.getApplicationLabel(appInfo).toString()
                            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                            appEntities.add(
                                InstalledAppEntity(
                                    packageName = appInfo.packageName,
                                    appName = label,
                                    versionName = "1.0",
                                    versionCode = 1L,
                                    isSystemApp = isSystem,
                                    sourceApkPath = appInfo.sourceDir,
                                    category = classifyCategory(appInfo.packageName, label, isSystem),
                                    hasSplitApks = appInfo.splitSourceDirs != null && appInfo.splitSourceDirs!!.isNotEmpty(),
                                    lastDiscoveredAt = System.currentTimeMillis()
                                )
                            )
                        } catch (_: Exception) {}
                    }
                }
            }

            installedAppDao.clearAll()
            installedAppDao.insertInstalledApps(appEntities)
            Log.i(TAG, "Discovered ${appEntities.size} installed applications.")
            Result.success(appEntities.size)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to discover installed apps", e)
            Result.failure(e)
        }
    }

    private fun classifyCategory(pkgName: String, label: String, isSystem: Boolean): String {
        val lowerPkg = pkgName.lowercase()
        val lowerLabel = label.lowercase()

        if (SOCIAL_KEYWORDS.any { lowerPkg.contains(it) || lowerLabel.contains(it) }) {
            return "Social & Messaging"
        }
        if (PRODUCTIVITY_KEYWORDS.any { lowerPkg.contains(it) || lowerLabel.contains(it) }) {
            return "Productivity"
        }
        if (isSystem) {
            return "System Tools"
        }
        return "Utility & Tools"
    }
}
