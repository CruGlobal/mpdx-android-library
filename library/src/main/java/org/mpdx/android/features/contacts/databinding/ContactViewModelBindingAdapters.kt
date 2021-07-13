package org.mpdx.features.contacts.databinding

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.TextViewCompat
import androidx.databinding.BindingAdapter
import androidx.databinding.adapters.TextViewBindingAdapter
import org.mpdx.android.R
import org.mpdx.android.features.contacts.model.Contact.DonationLateState

@BindingAdapter("android:text")
@SuppressLint("RestrictedApi")
fun TextView.setText(state: DonationLateState?) {
    val textId = when (state) {
        DonationLateState.ON_TIME, null -> R.string.contact_0_days_late
        DonationLateState.LATE, DonationLateState.ALL_LATE -> R.string.contact_0_to_30_days_late
        DonationLateState.THIRTY_DAYS_LATE -> R.string.contact_30_to_60_days_late
        DonationLateState.SIXTY_DAYS_LATE, DonationLateState.NINETY_DAYS_LATE -> R.string.contact_60_plus_days_late
    }
    TextViewBindingAdapter.setText(this, context.getString(textId))
}

@BindingAdapter("drawableTint")
fun TextView.drawableTint(state: DonationLateState?) {
    val color = when (state) {
        DonationLateState.ON_TIME, null -> R.color.on_time
        DonationLateState.LATE, DonationLateState.ALL_LATE -> R.color.zero_to_thirty
        DonationLateState.THIRTY_DAYS_LATE -> R.color.thirty_to_sixty
        DonationLateState.SIXTY_DAYS_LATE, DonationLateState.NINETY_DAYS_LATE -> R.color.sixty_plus
    }
    TextViewCompat.setCompoundDrawableTintList(
        this, ColorStateList.valueOf(ResourcesCompat.getColor(resources, color, context.theme))
    )
}
