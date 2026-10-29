package com.hanan.mysavings.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.hanan.mysavings.MainActivity
import com.hanan.mysavings.R
import com.hanan.mysavings.domain.model.BudgetAlert
import com.hanan.mysavings.domain.model.BudgetAlertLevel
import com.hanan.mysavings.ui.util.formatRupiah
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Shows a budget alert to the user. An interface so the worker can be tested with a fake. */
fun interface BudgetAlertNotifier {
    fun show(alert: BudgetAlert)
}

class AndroidBudgetAlertNotifier @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : BudgetAlertNotifier {

    override fun show(alert: BudgetAlert) {
        // Android 13+ needs the runtime permission; without it the user chose no notifications.
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        ensureChannel()

        val (title, body) = when (alert.level) {
            BudgetAlertLevel.NEARLY_USED -> context.getString(R.string.notif_budget_nearly_title) to
                context.getString(R.string.notif_budget_nearly_body, alert.usedPercent, formatRupiah(alert.remaining))

            BudgetAlertLevel.USED_UP -> context.getString(R.string.notif_budget_used_up_title) to
                if (alert.remaining < 0) {
                    context.getString(R.string.notif_budget_over_body, formatRupiah(-alert.remaining))
                } else {
                    context.getString(R.string.notif_budget_used_up_body, formatRupiah(alert.limit))
                }
        }

        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()

        // One id per month: a "used up" alert replaces that month's "nearly used" one.
        NotificationManagerCompat.from(context).notify(alert.yearMonth.hashCode(), notification)
    }

    /** Creating an existing channel is a no-op, so this is safe to call every time. */
    private fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notif_channel_budget),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = context.getString(R.string.notif_channel_budget_desc) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private companion object {
        const val CHANNEL_ID = "budget_alerts"
    }
}
