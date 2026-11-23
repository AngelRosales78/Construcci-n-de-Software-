package com.example.sp500auth.data.push

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.sp500auth.R
import com.example.sp500auth.data.local.TokenManager
import com.example.sp500auth.data.repository.favoritesOwner
import com.example.sp500auth.ui.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FinanceMessagingService:FirebaseMessagingService() {
    override fun onNewToken(token:String){PushRegistration.sync(this)}
    override fun onMessageReceived(message:RemoteMessage) {
        val owner=favoritesOwner(TokenManager(this).getAccessToken()) ?: return
        if(message.data["owner_id"]!=owner)return
        if(android.os.Build.VERSION.SDK_INT>=33 && ContextCompat.checkSelfPermission(this,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return
        val ticker=message.data["ticker"].orEmpty()
        if(!ticker.matches(Regex("[A-Z0-9.-]{1,10}")))return
        val intent=Intent(this,MainActivity::class.java).putExtra("ticker",ticker).putExtra("owner_id",owner).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val id=message.data["notification_id"]?.hashCode() ?: ticker.hashCode()
        val open=PendingIntent.getActivity(this,id,intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification=NotificationCompat.Builder(this,"financial_updates").setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("S&P 500 Finance").setContentText("Tienes una actualización en tus avisos.")
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE).setAutoCancel(true).setContentIntent(open).build()
        NotificationManagerCompat.from(this).notify(id,notification)
    }
}
