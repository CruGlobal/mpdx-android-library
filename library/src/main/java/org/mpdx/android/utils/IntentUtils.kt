package org.mpdx.android.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_SENDTO
import android.content.Intent.EXTRA_EMAIL
import android.content.Intent.EXTRA_SUBJECT
import android.content.Intent.EXTRA_TEXT
import android.net.Uri
import timber.log.Timber

@JvmOverloads
fun Context.sendEmail(emails: Array<String>, subject: String? = null, body: String? = null): Boolean {
    val intent = Intent(ACTION_SENDTO).apply {
        data = Uri.parse("mailto:")
        putExtra(EXTRA_EMAIL, emails)
        if (subject != null) putExtra(EXTRA_SUBJECT, subject)
        if (body != null) putExtra(EXTRA_TEXT, body)
    }
    return try {
        startActivity(intent)
        true
    } catch (ex: ActivityNotFoundException) {
        Timber.tag("IntentUtils").d(ex)
        false
    }
}

@JvmOverloads
fun Context.sendSms(phoneNumbers: Array<out String>, body: String? = null) = try {
    val intent = Intent(ACTION_SENDTO).apply {
        data = Uri.parse("smsto:${phoneNumbers.joinToString(";")}")
        if (body != null) putExtra(EXTRA_TEXT, body)
    }
    startActivity(intent)
    true
} catch (ex: ActivityNotFoundException) {
    Timber.tag("IntentUtils").d(ex)
    false
}
