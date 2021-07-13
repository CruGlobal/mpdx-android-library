package org.mpdx.android.features.filter.selector

import org.mpdx.android.features.filter.model.Filter

interface FilterActions {
    fun toggleFilter(filter: Filter?, isEnabled: Boolean)
}
