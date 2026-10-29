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
import com.hanan.mysavings.domain.model.DebtReminder
import com.hanan.mysavings.domain.model.DebtReminderKind
import com.hanan.mysavings.ui.settings.AppCompatLanguageController
import com.hanan.mysavings.ui.util.formatRupiah
import com.hanan.mysavings.ui.util.formatShortDate
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Shows a debt reminder to the user. An interface so the worker can be tested with a fake. */
fun interface DebtReminderNotifier {
    fun show(reminder: DebtReminder)
}

class AndroidDebtReminderNotifier @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
) : DebtReminderNotifier {

    override fun show(reminder: DebtReminder) {
        // Workers have no Activity, so take the language the user chose in the app explicitly.
        val context = AppCompatLanguageController.localizedContext(appContext)
        // Android 13+ needs the runtime permission; without it the user chose no notifications.
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        ensureChannel(context)

        val title = when (reminder.kind) {
            DebtReminderKind.DUE_SOON ->
                context.resources.getQuantityString(R.plurals.notif_debt_soon_title, reminder.daysLeft, reminder.daysLeft)
            DebtReminderKind.DUE_TODAY -> context.getString(R.string.notif_debt_today_title)
        }
        val body = context.getString(
            R.string.notif_debt_body,
            formatRupiah(reminder.amountDue),
            reminder.creditor,
            formatShortDate(reminder.installment.dueDate, context.resources.configuration.locales[0]),
        )

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

        // One id per installment: the due-today reminder replaces the earlier one for the same payment.
        NotificationManagerCompat.from(context).notify("debt-${reminder.debtId}-${reminder.installment.number}".hashCode(), notification)
    }

    /** Creating an existing channel is a no-op, so this is safe to call every time. */
    private fun ensureChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notif_channel_debt),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = context.getString(R.string.notif_channel_debt_desc) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private companion object {
        const val CHANNEL_ID = "debt_reminders"
    }
}
