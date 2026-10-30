package com.hanan.mysavings.domain.model

/** The signed-in account. Everything except [uid] can be missing, depending on the provider. */
data class AccountUser(
    /** Stable id of the account; the key under which its data will be synced. */
    val uid: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: String?,
)
