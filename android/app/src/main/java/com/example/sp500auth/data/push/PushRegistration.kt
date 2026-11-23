package com.example.sp500auth.data.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.example.sp500auth.BuildConfig
import com.example.sp500auth.data.local.TokenManager
import com.example.sp500auth.data.repository.favoritesOwner
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object PushRegistration {
    private val executor=Executors.newSingleThreadExecutor()
    private val http=OkHttpClient.Builder().callTimeout(15,TimeUnit.SECONDS).build()
    fun configured()=BuildConfig.FCM_APP_ID.isNotBlank()&&BuildConfig.FCM_PROJECT_ID.isNotBlank()&&BuildConfig.FCM_API_KEY.isNotBlank()
    fun sync(context:Context) {
        val appContext=context.applicationContext
        val manager=appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(NotificationChannel("financial_updates","Novedades financieras",NotificationManager.IMPORTANCE_DEFAULT).apply{lockscreenVisibility=android.app.Notification.VISIBILITY_PRIVATE})
        if(!configured())return
        val store=TokenManager(appContext)
        val access=store.getAccessToken() ?: return
        val owner=favoritesOwner(access) ?: return
        try {
            if(FirebaseApp.getApps(appContext).isEmpty())FirebaseApp.initializeApp(appContext,FirebaseOptions.Builder()
                .setApplicationId(BuildConfig.FCM_APP_ID).setApiKey(BuildConfig.FCM_API_KEY)
                .setProjectId(BuildConfig.FCM_PROJECT_ID).setGcmSenderId(BuildConfig.FCM_SENDER_ID).build())
            FirebaseMessaging.getInstance().token.addOnSuccessListener{token->executor.execute {
                if(favoritesOwner(store.getAccessToken())!=owner)return@execute
                val prefs=appContext.getSharedPreferences("push_installation",Context.MODE_PRIVATE)
                val id=prefs.getString("id",null) ?: UUID.randomUUID().toString().also{prefs.edit().putString("id",it).apply()}
                val payload=JSONObject().put("installation_id",id).put("token",token).put("permission_granted",NotificationManagerCompat.from(appContext).areNotificationsEnabled())
                try {http.newCall(Request.Builder().url(BuildConfig.API_BASE_URL+"user/push-installations/")
                    .header("Authorization","Bearer $access").post(payload.toString().toRequestBody("application/json".toMediaType())).build()).execute().close()}catch(_:Exception){}
            }}
        } catch(_:Exception){}
    }
    fun clear(context:Context) {
        NotificationManagerCompat.from(context).cancelAll()
        try {if(FirebaseApp.getApps(context).isNotEmpty())FirebaseMessaging.getInstance().deleteToken()}catch(_:Exception){}
    }
}
