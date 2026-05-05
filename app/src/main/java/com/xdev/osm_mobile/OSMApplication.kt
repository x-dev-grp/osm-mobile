package com.xdev.osm_mobile

import android.app.Application
import android.content.Intent
import android.util.Log
import com.onesignal.OneSignal
import com.onesignal.debug.LogLevel
import com.onesignal.notifications.INotificationClickEvent
import com.onesignal.notifications.INotificationClickListener
import com.onesignal.user.subscriptions.IPushSubscriptionObserver
import com.onesignal.user.subscriptions.PushSubscriptionChangedState
import com.xdev.osm_mobile.horsligne.SessionManager
import com.xdev.osm_mobile.models.RegistrationRequest
import com.xdev.osm_mobile.network.RetrofitClient
import com.xdev.osm_mobile.database.AppDatabase
import com.xdev.osm_mobile.database.AppRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class OSMApplication : Application() {

    companion object {
        lateinit var sessionManager: SessionManager
            private set
        lateinit var database: AppDatabase
            private set
        lateinit var repository: AppRepository
            private set
        lateinit var instance: OSMApplication
            private set

        const val ONESIGNAL_APP_ID = "e548b208-cac7-4a46-833b-e2ad16add4ac"


    }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        instance = this
        sessionManager = SessionManager.getInstance(this)
        database = AppDatabase.getDatabase(this)
        repository = AppRepository(RetrofitClient.instance, database.mainDao())

        OneSignal.Debug.logLevel = LogLevel.VERBOSE
        OneSignal.initWithContext(this, ONESIGNAL_APP_ID)

        OneSignal.User.pushSubscription.addObserver(object : IPushSubscriptionObserver {
            override fun onPushSubscriptionChange(state: PushSubscriptionChangedState) {
                val id = state.current.id
                if (!id.isNullOrBlank()) {
                    Log.d("ONESIGNAL_PLAYER_ID", "Player ID = $id")
                    registerDeviceIfPossible()
                }
            }
        })

        appScope.launch {
            delay(1000)
            registerDeviceIfPossible()
        }

        OneSignal.Notifications.addClickListener(object : INotificationClickListener {
            override fun onClick(event: INotificationClickEvent) {
                val ofId = event.notification.additionalData?.optString("ofId")
                if (!ofId.isNullOrBlank()) {
                    val intent = OfDetailActivity.newIntent(this@OSMApplication, ofId).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    startActivity(intent)
                }
            }
        })
    }

    fun registerDeviceIfPossible() {
        appScope.launch {
            if (!sessionManager.isLoggedIn()) {
                Log.d("DeviceReg", "Utilisateur non connecté, enregistrement ignoré.")
                return@launch
            }

            val userId = sessionManager.getUserId() ?: sessionManager.getUsername()
            val playerId = OneSignal.User.pushSubscription.id

            Log.d("DeviceReg", "Tentative enregistrement : userId=$userId, playerId=$playerId")

            if (userId.isNullOrBlank() || playerId.isNullOrBlank()) {
                Log.w("DeviceReg", "Impossible d'enregistrer : userId=$userId, playerId=$playerId")
                return@launch
            }

            try {
                val response = RetrofitClient.instance.registerDevice(
                    RegistrationRequest(userId = userId, playerId = playerId)
                )
                if (response.isSuccessful) {
                    Log.d("DeviceReg", "Device enregistré côté serveur : $playerId")
                } else {
                    Log.e("DeviceReg", "Échec enregistrement : ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("DeviceReg", "Erreur réseau : ${e.message}")
            }
        }
    }
}
