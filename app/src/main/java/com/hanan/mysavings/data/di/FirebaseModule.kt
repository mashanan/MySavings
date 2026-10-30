package com.hanan.mysavings.data.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.memoryCacheSettings
import com.hanan.mysavings.data.auth.FirebaseAuthRepository
import com.hanan.mysavings.data.sync.FirestoreRemoteStore
import com.hanan.mysavings.data.sync.RemoteStore
import com.hanan.mysavings.data.sync.SyncAccountDataManager
import com.hanan.mysavings.data.sync.WorkManagerSyncController
import com.hanan.mysavings.domain.repository.AccountDataManager
import com.hanan.mysavings.domain.repository.AuthRepository
import com.hanan.mysavings.domain.repository.SyncController
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class FirebaseModule {
    @Binds
    abstract fun bindAuthRepository(impl: FirebaseAuthRepository): AuthRepository

    @Binds
    abstract fun bindRemoteStore(impl: FirestoreRemoteStore): RemoteStore

    @Binds
    abstract fun bindSyncController(impl: WorkManagerSyncController): SyncController

    @Binds
    abstract fun bindAccountDataManager(impl: SyncAccountDataManager): AccountDataManager

    companion object {
        // Provided rather than called inside classes, so tests never touch the real Firebase.
        @Provides
        fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

        @Provides
        @Singleton
        fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance().apply {
            // Memory cache only: Room already is the offline copy, a second one on disk would
            // only take space and could answer from stale data.
            firestoreSettings = firestoreSettings { setLocalCacheSettings(memoryCacheSettings {}) }
        }
    }
}
