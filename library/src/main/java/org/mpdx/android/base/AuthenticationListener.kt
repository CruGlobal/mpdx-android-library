package org.mpdx.android.base

import android.app.Activity
import android.content.Context
import android.content.Intent

interface AuthenticationListener {
    fun logOutOfSession(activity: Activity)
    fun logIntoSession(activity: Activity)
    fun getSessionGuid(): String?
    fun getTicket(): String?
    fun getAccessToken(): String?
    fun startSplashActivity(activity: Activity)
    fun getSplashActivityIntent(context: Context): Intent
}
