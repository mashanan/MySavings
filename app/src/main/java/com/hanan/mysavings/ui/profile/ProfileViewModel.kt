package com.hanan.mysavings.ui.profile

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hanan.mysavings.domain.usecase.FinancialSummary
import com.hanan.mysavings.domain.usecase.ObserveFinancialSummaryUseCase
import com.hanan.mysavings.domain.usecase.ObserveProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    observeProfile: ObserveProfileUseCase,
    observeFinancialSummary: ObserveFinancialSummaryUseCase,
    private val photoDecoder: ProfilePhotoDecoder,
) : ViewModel() {

    val uiState: StateFlow<ProfileUiState> = combine(observeProfile(), observeFinancialSummary()) { profile, summary ->
        ProfileUiState(
            name = profile.name,
            initials = initialsOf(profile.name),
            photo = profile.photo?.let { photoDecoder.decode(it.bytes) },
            summary = summary,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())
}

data class ProfileUiState(
    val name: String? = null,
    val initials: String? = null,
    val photo: ImageBitmap? = null,
    /** `null` while loading. */
    val summary: FinancialSummary? = null,
)
