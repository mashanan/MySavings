package com.hanan.mysavings.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hanan.mysavings.data.local.entity.UserProfileEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** The single-row profile: created on the first change, and one change never wipes the other field. */
@RunWith(AndroidJUnit4::class)
class ProfileDaoTest {

    private lateinit var db: MySavingsDatabase
    private val dao get() = db.profileDao()

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), MySavingsDatabase::class.java).build()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun noProfileUntilSomethingIsSaved() = runTest {
        assertNull(dao.observe().first())
    }

    @Test
    fun updatesKeepTheOtherFieldAndNeverAddASecondRow() = runTest {
        dao.update { it.copy(name = "Louise") }
        dao.update { it.copy(photoUpdatedAt = 42) }
        dao.update { it.copy(name = "Louise Hanan") }

        assertEquals(UserProfileEntity(name = "Louise Hanan", photoUpdatedAt = 42), dao.observe().first())
    }
}
