package org.mpdx.androids.library.base.widget.recyclerview

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private const val threshold = 50

class FilteredEndlessScrollListenerTest {
    @Test
    fun verifyShouldLoadNextPageWhenPastAlreadyLoaded() {
        val listener = FilteredEndlessScrollListener(100, threshold = threshold) { _, _ -> false }
        listener.filteredItems = 1000
        listener.fetchedPages = 1

        listener.lastVisibleItem = 5
        assertFalse("Not within the already loaded threshold", listener.shouldLoadNextPage())
        listener.lastVisibleItem = 51
        assertTrue("Within the already loaded threshold", listener.shouldLoadNextPage())
    }

    @Test
    fun verifyShouldLoadNextPageWhenMoreItemsAreNeeded() {
        val listener = FilteredEndlessScrollListener(100, threshold = threshold) { _, _ -> false }
        listener.filteredItems = 70
        listener.fetchedPages = 1

        listener.lastVisibleItem = 5
        assertFalse("Not within $threshold of the end", listener.shouldLoadNextPage())
        listener.lastVisibleItem = 21
        assertTrue("Within $threshold of the end", listener.shouldLoadNextPage())
    }

    @Test
    fun verifyShouldLoadNextPageNotWhenMoreThanUnfilteredHasBeenLoaded() {
        val listener = FilteredEndlessScrollListener(100, threshold = threshold) { _, _ -> false }
        listener.filteredItems = 70
        listener.lastVisibleItem = 70

        listener.unfilteredItems = 151
        listener.fetchedPages = 2
        assertTrue("Loaded less than ${listener.unfilteredItems + threshold}", listener.shouldLoadNextPage())
        listener.fetchedPages = 3
        assertFalse("Loaded more than ${listener.unfilteredItems + threshold}", listener.shouldLoadNextPage())
    }
}
