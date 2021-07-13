package org.mpdx.android.utils

interface OpenRange<T : Comparable<T>> {
    val start: T
    val endExclusive: T
}

private class ComparableOpenRange<T : Comparable<T>>(
    override val start: T,
    override val endExclusive: T
) : OpenRange<T>

infix fun <T : Comparable<T>> T.until(to: T): OpenRange<T> = ComparableOpenRange(this, to)
