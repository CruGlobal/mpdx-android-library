package org.mpdx.android.utils

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.LayerDrawable
import android.view.MenuItem
import com.google.android.material.badge.BadgeDrawable
import org.mpdx.android.R
import org.mpdx.android.base.widget.material.updateCenterAndBoundsForBounds

fun MenuItem.getBadgeDrawable(context: Context) = (icon as? LayerDrawable)?.run {
    findDrawableByLayerId(R.id.badge) as? BadgeDrawable
        ?: BadgeDrawable.create(context).also { drawable ->
            mutate()
            if (setDrawableByLayerId(R.id.badge, drawable)) {
                drawable.updateCenterAndBoundsForBounds(context)
            } else {
                return@run null
            }
        }
}

@SuppressLint("NewApi")
fun MenuItem.updateBadgeNumber(context: Context, number: Int) {
    getBadgeDrawable(context)?.also {
        it.number = number
        it.isVisible = number > 0
        it.updateCenterAndBoundsForBounds(context)
    }
}
