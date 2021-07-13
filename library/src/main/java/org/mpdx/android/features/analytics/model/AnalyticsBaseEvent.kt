package org.mpdx.android.features.analytics.model

import org.mpdx.android.features.analytics.AnalyticsSystem

abstract class AnalyticsBaseEvent {
    open fun isForSystem(system: AnalyticsSystem) = true

    open val siteSection: String? get() = null
    open val siteSubSection: String? get() = null
}
