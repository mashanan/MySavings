package com.hanan.mysavings.domain.usecase

import com.hanan.mysavings.domain.model.AccountUser
import com.hanan.mysavings.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveAccountUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    operator fun invoke(): Flow<AccountUser?> = authRepository.observeUser()
}
