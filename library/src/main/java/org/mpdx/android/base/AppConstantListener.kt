package org.mpdx.android.base

interface AppConstantListener {
    fun appID(): String?
    fun buildVersion(): String?
    fun isDebug(): Boolean
    fun baseApiUrl(): String
    fun versionCode(): Int
}
