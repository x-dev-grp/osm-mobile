package com.xdev.osm_mobile.models

import com.onesignal.OneSignal

object OneSignalManager {
    fun login(userId: String) {
        OneSignal.login(userId)
        OneSignal.User.addTag("user_id", userId)
    }
    fun logout() {
        OneSignal.logout()
    }
}
