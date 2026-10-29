package com.hanan.mysavings.ui.profile

import app.cash.turbine.test
import androidx.compose.ui.graphics.ImageBitmap
import com.hanan.mysavings.domain.model.ProfilePhoto
import com.hanan.mysavings.domain.model.UserProfile
import com.hanan.mysavings.domain.usecase.ChangeProfilePhotoUseCase
import com.hanan.mysavings.domain.usecase.FinancialSummary
import com.hanan.mysavings.domain.usecase.ObserveFinancialSummaryUseCase
import com.hanan.mysavings.domain.usecase.ObserveProfileUseCase
import com.hanan.mysavings.domain.usecase.RemoveProfilePhotoUseCase
import com.hanan.mysavings.domain.usecase.UpdateProfileNameUseCase
import com.hanan.mysavings.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant

class ProfileViewModelsTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val photo = ProfilePhoto(byteArrayOf(1), Instant.EPOCH)
    private val profile = MutableStateFlow(UserProfile(name = "Louise Hanan", photo = photo))
    private val observeProfile = mockk<ObserveProfileUseCase> { every { this@mockk.invoke() } returns profile }

    // ImageBitmap cannot be created on the JVM, so the decoder hands out a mock.
    private val decoded = mockk<ImageBitmap>()
    private val decoder = ProfilePhotoDecoder { decoded }

    private val summary = FinancialSummary(1_650_000, 4_500_000, 8_500_000)
    private val observeSummary = mockk<ObserveFinancialSummaryUseCase> { every { this@mockk.invoke() } returns flowOf(summary) }

    private val updateName = mockk<UpdateProfileNameUseCase> { coEvery { this@mockk.invoke(any()) } just runs }
    private val changePhoto = mockk<ChangeProfilePhotoUseCase>()
    private val removePhoto = mockk<RemoveProfilePhotoUseCase> { coEvery { this@mockk.invoke() } just runs }

    private fun editViewModel() = EditProfileViewModel(observeProfile, updateName, changePhoto, removePhoto, decoder)

    @Test
    fun `the profile tab shows name, initials, the decoded photo and the summary`() = runTest {
        ProfileViewModel(observeProfile, observeSummary, decoder).uiState.test {
            val state = awaitItem().takeIf { it.summary != null } ?: awaitItem()

            assertEquals("Louise Hanan", state.name)
            assertEquals("LH", state.initials)
            assertSame(decoded, state.photo)
            assertEquals(summary, state.summary)
        }
    }

    @Test
    fun `editing starts from the stored name and photo`() {
        val state = editViewModel().uiState.value

        assertFalse(state.isLoading)
        assertEquals("Louise Hanan", state.name)
        assertSame(decoded, state.photo)
    }

    @Test
    fun `saving stores the typed name and finishes`() = runTest {
        val viewModel = editViewModel()

        viewModel.onNameChange("Louise")
        viewModel.onSave()

        coVerify { updateName("Louise") }
        assertTrue(viewModel.uiState.value.isSaved)
    }

    @Test
    fun `an unusable photo shows an error and keeps the old one`() = runTest {
        coEvery { changePhoto(any()) } returns false
        val viewModel = editViewModel()

        viewModel.onPhotoPicked("content://broken")

        assertTrue(viewModel.uiState.value.photoError)
        assertFalse(viewModel.uiState.value.isPhotoBusy)
        assertSame(decoded, viewModel.uiState.value.photo)
    }

    @Test
    fun `removing the photo clears it right away`() = runTest {
        val viewModel = editViewModel()

        viewModel.onRemovePhoto()

        coVerify { removePhoto() }
        assertNull(viewModel.uiState.value.photo)
    }

    @Test
    fun `initials come from the first two words`() {
        assertEquals("LH", initialsOf("  louise   hanan putri "))
        assertEquals("B", initialsOf("budi"))
        assertNull(initialsOf("   "))
        assertNull(initialsOf(null))
    }
}
