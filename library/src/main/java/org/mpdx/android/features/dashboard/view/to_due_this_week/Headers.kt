package org.mpdx.android.features.dashboard.view.to_due_this_week

import androidx.annotation.StringRes
import org.mpdx.android.R

enum class Headers(@StringRes val string: Int) {
    TASK_DUE_THIS_WEEK(R.string.tasks_due_this_week),
    LATE_COMMITMENTS(R.string.late_commitment),
    PARTNER_CAPE_PRAYER(R.string.partner_care_prayer),
    PARTNER_CARE_CELEBRATIONS(R.string.partner_care_celebrations)
}
