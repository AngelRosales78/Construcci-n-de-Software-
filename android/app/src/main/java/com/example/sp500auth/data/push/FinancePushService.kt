package com.example.sp500auth.data.push

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.sp500auth.R
import com.example.sp500auth.ui.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

const val PUSH_CHANNEL = "financial_updates"
const val EXTRA_TICKER = "ticker"
const val EXTRA_OWNER = "owner_id"
const val EXTRA_NOTICE = "notification_id"

/** The channel hides its content on the lock screen. */
fun createPushChannel(context: Context) {
    val channel = NotificationChannel(PUSH_CHANNEL, "Cambios en tus favoritas", NotificationManager.IMPORTANCE_DEFAULT).apply {
        description = "Avisos de cambios de calificación de empresas favoritas"
        lockscreenVisibility = NotificationCompat.VISIBILITY_PRIVATE
    }
    context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
}

/** HU12_02. In background Android shows the server's generic text; in foreground this service does. */
@AndroidEntryPoint
class FinancePushService : FirebaseMessagingService() {
    @Inject lateinit var registrar: PushRegistrar
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) { scope.launch { registrar.renewed(token) } }

    @SuppressLint("MissingPermission")
    override fun onMessageReceived(message: RemoteMessage) {
        val target = pushTarget(message.data[EXTRA_TICKER], message.data[EXTRA_OWNER], message.data[EXTRA_NOTICE]) ?: return
        // A message for an account that is no longer open on this phone is discarded.
        if (registrar.owner() != target.owner || !registrar.permissionGranted()) return
        val id = (target.noticeId ?: target.ticker.hashCode().toLong()).toInt()
        val intent = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(EXTRA_TICKER, target.ticker).putExtra(EXTRA_OWNER, target.owner).putExtra(EXTRA_NOTICE, target.noticeId?.toString())
        val open = PendingIntent.getActivity(this, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        fun base() = NotificationCompat.Builder(this, PUSH_CHANNEL).setSmallIcon(R.drawable.ic_stat_notice)
            .setContentTitle(getString(R.string.app_name)).setContentText("Tienes una actualización en tus avisos.")
        NotificationManagerCompat.from(this).notify("notice-$id", id, base().setContentIntent(open).setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE).setPublicVersion(base().build()).build())
    }

    override fun onDestroy() { scope.cancel(); super.onDestroy() }
}
