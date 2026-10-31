package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.CloneDatabase
import com.example.data.database.entity.CloneInstanceEntity
import com.example.ui.components.Formatters
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var database: CloneDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, CloneDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("App Cloner", appName)
    }

    @Test
    fun `insert and retrieve clone instance in room`() = runBlocking {
        val entity = CloneInstanceEntity(
            packageName = "com.whatsapp",
            appName = "WhatsApp",
            cloneIndex = 1,
            customName = "WhatsApp Personal",
            userId = 0,
            category = "Social & Messaging"
        )

        val id = database.cloneDao().insertClone(entity)
        assertNotNull(id)

        val list = database.cloneDao().getAllClones().first()
        assertEquals(1, list.size)
        assertEquals("WhatsApp Personal", list[0].customName)
        assertEquals(0, list[0].userId)
    }

    @Test
    fun `format bytes utility`() {
        assertEquals("0 B", Formatters.formatBytes(0))
        assertEquals("1.0 KB", Formatters.formatBytes(1024))
        assertEquals("1.5 MB", Formatters.formatBytes(1572864))
    }
}
