package com.hanan.mysavings.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.hanan.mysavings.R

/** The four bottom-bar tabs. Adding or reordering a tab only happens here. */
enum class TopLevelTab(
    val destination: Destination,
    @StringRes val label: Int,
    @DrawableRes val icon: Int,
) {
    EXPENSES(Destination.Home, R.string.tab_expenses, R.drawable.ic_tab_expenses),
    SAVINGS(Destination.Savings, R.string.tab_savings, R.drawable.ic_tab_savings),
    DEBTS(Destination.Debts, R.string.tab_debts, R.drawable.ic_calendar),
    PROFILE(Destination.Profile, R.string.tab_profile, R.drawable.ic_tab_profile),
}
