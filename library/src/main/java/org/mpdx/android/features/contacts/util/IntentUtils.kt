package org.mpdx.android.features.contacts.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.annotation.AnyThread
import androidx.annotation.MainThread
import org.mpdx.android.R
import org.mpdx.android.features.contacts.model.Address

@MainThread
fun Address.startMapsActivity(context: Context?) {
    if (context == null) return

    try {
        context.startActivity(buildMapsIntent())
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, R.string.app_not_found, Toast.LENGTH_SHORT).show()
    }
}

@AnyThread
fun Address.buildMapsIntent() = Intent(Intent.ACTION_VIEW).apply {
    data = Uri.parse("geo:0,0").buildUpon()
        .appendQueryParameter("q", listOf(line1, line2, line3).filterNot { it.isNullOrEmpty() }.joinToString(" "))
        .build()
}
