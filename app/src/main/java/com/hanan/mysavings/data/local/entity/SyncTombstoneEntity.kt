package com.hanan.mysavings.data.local.entity

import androidx.room.Entity

/**
 * A synced row that was deleted on this phone and whose deletion is not uploaded yet.
 *
 * Rows are still really deleted (so CASCADE and every existing query keep working); this
 * small table only remembers *which* uuid to report as deleted. It is emptied after upload.
 */
@Entity(tableName = "sync_tombstones", primaryKeys = ["collection", "uuid"])
data class SyncTombstoneEntity(
    /** One of [SyncCollection]'s names, e.g. "savings_goals". */
    val collection: String,
    val uuid: String,
    /** Epoch millis; a deletion is a change too, and the newest change wins. */
    val deletedAt: Long,
)

/** The synced tables, by the name they have both in Room and in the cloud. */
object SyncCollection {
    const val TRANSACTIONS = "transactions"
    const val BUDGETS = "budgets"
    const val SAVINGS_GOALS = "savings_goals"
    const val SAVINGS_DEPOSITS = "savings_deposits"
    const val DEBTS = "debts"
    const val DEBT_PAYMENTS = "debt_payments"
    const val PROFILE = "user_profile"
}

/** Budgets are unique per month and category, so two phones setting October's budget must
 * land on the same row: the uuid is derived from that key instead of being random.
 * [categoryKey] is the category's icon key ("food"), which is the same on every phone,
 * or `null` for the overall budget. */
fun budgetUuid(yearMonth: Int, categoryKey: String?): String = "budget-$yearMonth-${categoryKey ?: "total"}"

/** There is only one profile per account. */
const val PROFILE_UUID = "profile"
