package org.mpdx.android.features.filter.selector

interface BulkFilterActions {
    fun toggleAllFilters(isEnabled: Boolean)
    fun toggleContactStatusFilters(selectActive: Boolean)
}
