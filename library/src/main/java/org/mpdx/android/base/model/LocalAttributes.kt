package org.mpdx.android.base.model

interface LocalAttributes {
    fun mergeInLocalAttributes(existing: LocalAttributes) = Unit
}
