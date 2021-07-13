package org.mpdx.android.base.model

interface JsonApiModel : UniqueItem {
    val jsonApiType: String

    val isPlaceholder: Boolean
    val replacePlaceholder get() = false

    fun prepareForApi() {}
}
