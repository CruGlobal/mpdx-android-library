package org.mpdx.android.utils

fun String?.firstEquals(other: String?, ignoreCase: Boolean = false): Boolean {
    val thisChar = this?.firstOrNull()
    val otherChar = other?.firstOrNull()
    return when {
        thisChar != null && otherChar != null -> thisChar.equals(otherChar, ignoreCase)
        else -> thisChar == otherChar
    }
}
