package com.hanan.mysavings.data.sync

import com.hanan.mysavings.data.local.entity.SyncCollection

/**
 * A foreign key as it travels to the cloud. Local ids mean nothing on another phone, so the
 * parent is sent by something every phone shares: its uuid, or a category's icon key.
 *
 * Example: savings_deposits.goalId = 4 goes up as goalUuid = "3f2a…", and comes down on
 * another phone as whatever local id that goal has there.
 */
data class ParentRef(
    /** Foreign key column in this table, e.g. "goalId". */
    val column: String,
    /** Field name in the cloud document, e.g. "goalUuid". */
    val field: String,
    val parentTable: String,
    /** Column of the parent that every phone shares, e.g. "uuid" or "iconKey". */
    val parentKeyColumn: String,
)

/** How one Room table is synced. The table name is also the cloud collection's name. */
data class SyncTable(
    val name: String,
    /** Plain data columns, copied as they are. Not id, uuid, updatedAt or dirty, nor the foreign key. */
    val columns: List<String>,
    val parent: ParentRef? = null,
    /** For the single-row profile: the local id a downloaded row must get. */
    val fixedId: Long? = null,
)

/**
 * Every synced table, parents before children: downloads insert a goal before its deposits,
 * so a deposit always finds its goal's local id.
 */
object SyncTables {
    private val category = { column: String -> ParentRef(column, "categoryKey", "categories", "iconKey") }

    val ALL = listOf(
        SyncTable(
            name = SyncCollection.TRANSACTIONS,
            columns = listOf("amount", "date", "merchant", "note", "source", "createdAt"),
            parent = category("categoryId"),
        ),
        SyncTable(
            name = SyncCollection.BUDGETS,
            columns = listOf("yearMonth", "limitAmount"),
            parent = category("categoryId"),
        ),
        SyncTable(
            name = SyncCollection.SAVINGS_GOALS,
            columns = listOf("name", "targetAmount", "categoryKey", "targetDate", "createdAt"),
        ),
        SyncTable(
            name = SyncCollection.SAVINGS_DEPOSITS,
            columns = listOf("amount", "date", "note", "createdAt"),
            parent = ParentRef("goalId", "goalUuid", SyncCollection.SAVINGS_GOALS, "uuid"),
        ),
        SyncTable(
            name = SyncCollection.DEBTS,
            columns = listOf("creditor", "totalAmount", "firstDueDate", "installmentCount", "note", "createdAt"),
        ),
        SyncTable(
            name = SyncCollection.DEBT_PAYMENTS,
            columns = listOf("amount", "date", "note", "createdAt"),
            parent = ParentRef("debtId", "debtUuid", SyncCollection.DEBTS, "uuid"),
        ),
        // Only the name for now: the photo follows in phase 11c, and sending photoUpdatedAt
        // without the photo would make other phones look for a file they do not have.
        SyncTable(
            name = SyncCollection.PROFILE,
            columns = listOf("name"),
            fixedId = 1,
        ),
    )

    fun byName(name: String): SyncTable = ALL.first { it.name == name }
}
