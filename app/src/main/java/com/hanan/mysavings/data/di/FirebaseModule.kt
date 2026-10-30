package com.hanan.mysavings.data.di

import com.google.firebase.auth.FirebaseAuth
import com.hanan.mysavings.data.auth.FirebaseAuthRepository
import com.hanan.mysavings.domain.repository.AuthRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class FirebaseModule {
    @Binds
    abstract fun bindAuthRepository(impl: FirebaseAuthRepository): AuthRepository

    companion object {
        // Provided rather than called inside classes, so tests never touch the real Firebase.
        @Provides
        fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()
    }
}
