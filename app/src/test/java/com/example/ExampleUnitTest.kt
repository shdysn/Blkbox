package com.example

import com.example.blackbox.model.StorageUsage
import com.example.blackbox.model.VirtualEngineStats
import com.example.blackbox.model.VirtualPackage
import com.example.data.database.entity.CloneInstanceEntity
import com.example.ui.components.Formatters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Fast, deterministic pure JVM unit tests for business logic,
 * formatting calculations, and data models.
 * Runs instantly without requiring Android runtime or external shadow jars.
 */
class ExampleUnitTest {

    @Test
    fun testFormatBytes() {
        assertEquals("0 B", Formatters.formatBytes(0))
        assertEquals("0 B", Formatters.formatBytes(-100))
        assertEquals("1.0 KB", Formatters.formatBytes(1024))
        assertEquals("1.5 MB", Formatters.formatBytes(1572864))
        assertEquals("2.0 GB", Formatters.formatBytes(2147483648L))
    }

    @Test
    fun testFormatTimestamp() {
        assertEquals("Never", Formatters.formatTimestamp(null))
        assertEquals("Never", Formatters.formatTimestamp(0L))
        val formatted = Formatters.formatTimestamp(System.currentTimeMillis())
        assertTrue("Formatted timestamp should not be empty", formatted.isNotEmpty())
    }

    @Test
    fun testStorageUsageModel() {
        val storage = StorageUsage(dataBytes = 5000, cacheBytes = 3000)
        assertEquals(8000L, storage.totalBytes)
    }

    @Test
    fun testVirtualEngineStats() {
        val stats = VirtualEngineStats(
            totalPackages = 3,
            totalUsers = 4,
            runningProcesses = 2,
            virtualStorageBytes = 1048576L
        )
        assertEquals(3, stats.totalPackages)
        assertEquals(4, stats.totalUsers)
        assertEquals(2, stats.runningProcesses)
        assertEquals("BlackBox Core v2.4.0-release", stats.engineVersion)
    }

    @Test
    fun testCloneInstanceEntityDefaults() {
        val entity = CloneInstanceEntity(
            packageName = "com.whatsapp",
            appName = "WhatsApp",
            cloneIndex = 1,
            customName = "WhatsApp Clone #1",
            userId = 0,
            category = "Social & Messaging"
        )

        assertEquals("com.whatsapp", entity.packageName)
        assertEquals(1, entity.cloneIndex)
        assertEquals(0, entity.userId)
        assertEquals("WhatsApp Clone #1", entity.customName)
        assertEquals("Social & Messaging", entity.category)
        assertTrue(entity.createdAt > 0L)
    }
}
