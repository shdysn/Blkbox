package com.example.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "clone_instances",
    indices = [
        Index(value = ["packageName", "userId"], unique = true),
        Index(value = ["packageName", "cloneIndex"], unique = true)
    ]
)
data class CloneInstanceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val appName: String,
    val cloneIndex: Int,
    val customName: String,
    val userId: Int,
    val iconCachePath: String? = null,
    val isSystemApp: Boolean = false,
    val versionName: String = "1.0",
    val versionCode: Long = 1L,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLaunchedAt: Long? = null,
    val dataSizeBytes: Long = 0L,
    val cacheSizeBytes: Long = 0L,
    val category: String = "Other"
)

@Entity(tableName = "installed_apps")
data class InstalledAppEntity(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val versionName: String,
    val versionCode: Long,
    val isSystemApp: Boolean,
    val sourceApkPath: String,
    val category: String,
    val hasSplitApks: Boolean,
    val lastDiscoveredAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "clone_usage_logs")
data class CloneUsageEntity(
    @PrimaryKey(autoGenerate = true)
    val logId: Long = 0,
    val cloneId: Long,
    val packageName: String,
    val userId: Int,
    val launchTimestamp: Long = System.currentTimeMillis(),
    val durationSeconds: Long = 0L
)
