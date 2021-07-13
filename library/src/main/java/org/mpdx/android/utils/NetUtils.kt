package org.mpdx.android.utils

import android.content.Context
import android.net.ConnectivityManager
import androidx.core.content.getSystemService

val Context.isNetworkAvailable: Boolean
    get() = getSystemService<ConnectivityManager>()?.activeNetworkInfo?.isConnected == true
