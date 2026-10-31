package com.example.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.database.entity.CloneInstanceEntity
import com.example.data.database.entity.CloneUsageEntity
import com.example.data.database.entity.InstalledAppEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CloneDao {

    @Query("SELECT * FROM clone_instances ORDER BY lastLaunchedAt DESC, createdAt DESC")
    fun getAllClones(): Flow<List<CloneInstanceEntity>>

    @Query("SELECT * FROM clone_instances WHERE packageName = :packageName ORDER BY cloneIndex ASC")
    fun getClonesForPackage(packageName: String): Flow<List<CloneInstanceEntity>>

    @Query("SELECT * FROM clone_instances WHERE id = :id LIMIT 1")
    fun getCloneById(id: Long): Flow<CloneInstanceEntity?>

    @Query("SELECT * FROM clone_instances WHERE packageName = :packageName AND userId = :userId LIMIT 1")
    suspend fun getClone(packageName: String, userId: Int): CloneInstanceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClone(clone: CloneInstanceEntity): Long

    @Update
    suspend fun updateClone(clone: CloneInstanceEntity)

    @Query("DELETE FROM clone_instances WHERE id = :id")
    suspend fun deleteCloneById(id: Long)

    @Query("DELETE FROM clone_instances WHERE packageName = :packageName")
    suspend fun deleteClonesForPackage(packageName: String)

    @Query("UPDATE clone_instances SET lastLaunchedAt = :timestamp WHERE id = :id")
    suspend fun updateLastLaunched(id: Long, timestamp: Long)

    @Query("UPDATE clone_instances SET customName = :newName WHERE id = :id")
    suspend fun updateCustomName(id: Long, newName: String)

    @Query("UPDATE clone_instances SET dataSizeBytes = :dataBytes, cacheSizeBytes = :cacheBytes WHERE id = :id")
    suspend fun updateStorageStats(id: Long, dataBytes: Long, cacheBytes: Long)

    @Query("SELECT MAX(cloneIndex) FROM clone_instances WHERE packageName = :packageName")
    suspend fun getMaxCloneIndex(packageName: String): Int?

    @Query("SELECT MAX(userId) FROM clone_instances")
    suspend fun getMaxUserId(): Int?

    @Query("SELECT * FROM clone_usage_logs ORDER BY launchTimestamp DESC LIMIT 100")
    fun getRecentUsageLogs(): Flow<List<CloneUsageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsageLog(log: CloneUsageEntity): Long
}

@Dao
interface InstalledAppDao {

    @Query("SELECT * FROM installed_apps ORDER BY appName ASC")
    fun getAllInstalledApps(): Flow<List<InstalledAppEntity>>

    @Query("SELECT * FROM installed_apps WHERE category = :category ORDER BY appName ASC")
    fun getInstalledAppsByCategory(category: String): Flow<List<InstalledAppEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstalledApps(apps: List<InstalledAppEntity>)

    @Query("DELETE FROM installed_apps")
    suspend fun clearAll()
}
