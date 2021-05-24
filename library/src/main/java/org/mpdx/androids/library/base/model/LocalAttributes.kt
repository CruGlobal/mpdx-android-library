package org.mpdx.androids.library.base.model

interface LocalAttributes {
    fun mergeInLocalAttributes(existing: LocalAttributes) = Unit
}
