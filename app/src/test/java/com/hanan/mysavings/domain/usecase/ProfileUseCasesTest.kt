package com.hanan.mysavings.domain.usecase

import app.cash.turbine.test
import com.hanan.mysavings.domain.model.ProfilePhoto
import com.hanan.mysavings.domain.model.ProfileRecord
import com.hanan.mysavings.domain.repository.ProfilePhotoStore
import com.hanan.mysavings.domain.repository.ProfileRepository
import com.hanan.mysavings.testutil.MutableClock
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/** The four small profile use cases share the same two fakes, so they are tested together. */
class ProfileUseCasesTest {

    private val now = Instant.parse("2026-10-29T03:00:00Z")
    private val record = MutableStateFlow(ProfileRecord(name = null, photoUpdatedAt = null))
    private val jpeg = byteArrayOf(1, 2, 3)

    private val repository = mockk<ProfileRepository> {
        every { observe() } returns record
        coEvery { saveName(any()) } just runs
        coEvery { savePhotoUpdatedAt(any()) } just runs
    }
    private val photoStore = mockk<ProfilePhotoStore> {
        coEvery { load() } returns jpeg
        coEvery { delete() } just runs
    }

    @Test
    fun `observing loads the photo only when the record says there is one`() = runTest {
        ObserveProfileUseCase(repository, photoStore)().test {
            val empty = awaitItem()
            assertNull(empty.name)
            assertNull(empty.photo)

            record.value = ProfileRecord(name = "Louise", photoUpdatedAt = now)

            val withPhoto = awaitItem()
            assertEquals("Louise", withPhoto.name)
            assertEquals(ProfilePhoto(byteArrayOf(), now), withPhoto.photo) // equal by version
            assertArrayEquals(jpeg, withPhoto.photo!!.bytes)
            cancelAndIgnoreRemainingEvents()
        }
        coVerify(exactly = 1) { photoStore.load() }
    }

    @Test
    fun `a name is trimmed and capped, a blank one is removed`() = runTest {
        val updateName = UpdateProfileNameUseCase(repository)

        updateName("  Louise Hanan  ")
        updateName("   ")
        updateName("x".repeat(60))

        coVerifyOrder {
            repository.saveName("Louise Hanan")
            repository.saveName(null)
            repository.saveName("x".repeat(UpdateProfileNameUseCase.MAX_LENGTH))
        }
    }

    @Test
    fun `a usable photo is stored and stamped with the current time`() = runTest {
        coEvery { photoStore.import("content://picked") } returns true

        assertTrue(ChangeProfilePhotoUseCase(photoStore, repository, MutableClock(now))("content://picked"))
        coVerify { repository.savePhotoUpdatedAt(now) }
    }

    @Test
    fun `an unusable photo changes nothing`() = runTest {
        coEvery { photoStore.import(any()) } returns false

        assertFalse(ChangeProfilePhotoUseCase(photoStore, repository, MutableClock(now))("content://broken"))
        coVerify(exactly = 0) { repository.savePhotoUpdatedAt(any()) }
    }

    @Test
    fun `removing the photo clears the record before the file`() = runTest {
        RemoveProfilePhotoUseCase(photoStore, repository)()

        coVerifyOrder {
            repository.savePhotoUpdatedAt(null)
            photoStore.delete()
        }
    }
}
