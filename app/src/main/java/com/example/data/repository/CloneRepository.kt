package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.blackbox.BlackBoxCore
import com.example.blackbox.model.VirtualEngineStats
import com.example.blackbox.model.VirtualInstallResult
import com.example.data.database.dao.CloneDao
import com.example.data.database.entity.CloneInstanceEntity
import com.example.data.database.entity.CloneUsageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

class CloneRepository(
    private val context: Context,
    private val cloneDao: CloneDao
) {
    companion object {
        private const val TAG = "CloneRepository"
    }

    val allClones: Flow<List<CloneInstanceEntity>> = cloneDao.getAllClones()

    suspend fun createClone(
        sourcePackage: String,
        appName: String,
        isSystem: Boolean = false,
        versionName: String = "1.0",
        versionCode: Long = 1L,
        category: String = "Other",
        customName: String? = null
    ): Result<CloneInstanceEntity> = withContext(Dispatchers.IO) {
        try {
            val maxUserId = cloneDao.getMaxUserId() ?: -1
            val nextUserId = maxUserId + 1

            val maxIndex = cloneDao.getMaxCloneIndex(sourcePackage) ?: 0
            val nextCloneIndex = maxIndex + 1

            val displayName = customName?.takeIf { it.isNotBlank() }
                ?: "$appName Clone #$nextCloneIndex"

            Log.i(TAG, "Creating clone for $sourcePackage (User #$nextUserId, Clone #$nextCloneIndex)")

            // Invoke BlackBox Core
            val installResult = BlackBoxCore.get().installExistingPackage(sourcePackage, nextUserId)
            when (installResult) {
                is VirtualInstallResult.Error -> {
                    return@withContext Result.failure(Exception(installResult.message, installResult.cause))
                }
                is VirtualInstallResult.Success -> {
                    val storage = BlackBoxCore.get().getStorageUsage(sourcePackage, nextUserId)
                    val entity = CloneInstanceEntity(
                        packageName = sourcePackage,
                        appName = appName,
                        cloneIndex = nextCloneIndex,
                        customName = displayName,
                        userId = nextUserId,
                        isSystemApp = isSystem,
                        versionName = versionName,
                        versionCode = versionCode,
                        dataSizeBytes = storage.dataBytes,
                        cacheSizeBytes = storage.cacheBytes,
                        category = category
                    )
                    val insertedId = cloneDao.insertClone(entity)
                    val finalEntity = entity.copy(id = insertedId)
                    Result.success(finalEntity)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during clone creation", e)
            Result.failure(e)
        }
    }

    suspend fun createCloneFromApkFile(
        apkFile: File,
        customName: String? = null
    ): Result<CloneInstanceEntity> = withContext(Dispatchers.IO) {
        try {
            val maxUserId = cloneDao.getMaxUserId() ?: -1
            val nextUserId = maxUserId + 1

            val installResult = BlackBoxCore.get().installFromApkFile(apkFile, nextUserId)
            when (installResult) {
                is VirtualInstallResult.Error -> {
                    return@withContext Result.failure(Exception(installResult.message, installResult.cause))
                }
                is VirtualInstallResult.Success -> {
                    val vPkg = installResult.virtualPackage
                    val maxIndex = cloneDao.getMaxCloneIndex(vPkg.packageName) ?: 0
                    val nextCloneIndex = maxIndex + 1
                    val displayName = customName?.takeIf { it.isNotBlank() }
                        ?: "${vPkg.appName} Clone #$nextCloneIndex"

                    val storage = BlackBoxCore.get().getStorageUsage(vPkg.packageName, nextUserId)
                    val entity = CloneInstanceEntity(
                        packageName = vPkg.packageName,
                        appName = vPkg.appName,
                        cloneIndex = nextCloneIndex,
                        customName = displayName,
                        userId = nextUserId,
                        isSystemApp = false,
                        versionName = vPkg.versionName,
                        versionCode = vPkg.versionCode,
                        dataSizeBytes = storage.dataBytes,
                        cacheSizeBytes = storage.cacheBytes,
                        category = "Imported APK"
                    )
                    val insertedId = cloneDao.insertClone(entity)
                    Result.success(entity.copy(id = insertedId))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import clone from APK", e)
            Result.failure(e)
        }
    }

    suspend fun launchClone(clone: CloneInstanceEntity): Result<Unit> = withContext(Dispatchers.IO) {
        val result = BlackBoxCore.get().launchApk(context, clone.packageName, clone.userId)
        if (result.isSuccess) {
            val now = System.currentTimeMillis()
            cloneDao.updateLastLaunched(clone.id, now)
            cloneDao.insertUsageLog(
                CloneUsageEntity(
                    cloneId = clone.id,
                    packageName = clone.packageName,
                    userId = clone.userId,
                    launchTimestamp = now
                )
            )
        }
        result
    }

    suspend fun renameClone(cloneId: Long, newName: String) = withContext(Dispatchers.IO) {
        cloneDao.updateCustomName(cloneId, newName)
    }

    suspend fun clearCloneCache(clone: CloneInstanceEntity): Boolean = withContext(Dispatchers.IO) {
        val success = BlackBoxCore.get().clearCache(clone.packageName, clone.userId)
        refreshStorageStats(clone)
        success
    }

    suspend fun clearCloneData(clone: CloneInstanceEntity): Boolean = withContext(Dispatchers.IO) {
        val success = BlackBoxCore.get().clearData(clone.packageName, clone.userId)
        refreshStorageStats(clone)
        success
    }

    suspend fun deleteClone(clone: CloneInstanceEntity): Boolean = withContext(Dispatchers.IO) {
        BlackBoxCore.get().stopPackage(clone.packageName, clone.userId)
        BlackBoxCore.get().uninstallPackageAsUser(clone.packageName, clone.userId)
        cloneDao.deleteCloneById(clone.id)
        true
    }

    suspend fun refreshStorageStats(clone: CloneInstanceEntity) = withContext(Dispatchers.IO) {
        val storage = BlackBoxCore.get().getStorageUsage(clone.packageName, clone.userId)
        cloneDao.updateStorageStats(clone.id, storage.dataBytes, storage.cacheBytes)
    }

    fun getEngineStats(): VirtualEngineStats {
        return BlackBoxCore.get().getEngineStats()
    }
}
