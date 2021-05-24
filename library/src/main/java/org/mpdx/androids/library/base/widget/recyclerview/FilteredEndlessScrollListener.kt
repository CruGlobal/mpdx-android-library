package org.mpdx.androids.library.base.widget.recyclerview

import androidx.annotation.MainThread
import androidx.annotation.VisibleForTesting
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.common.primitives.Ints.max
import timber.log.Timber

private const val TAG = "EndlessScrollListener"

class FilteredEndlessScrollListener @JvmOverloads constructor(
    private val pageSize: Int,
    private val threshold: Int = pageSize / 2,
    private val loadMore: (Int, Boolean) -> Boolean
) : RecyclerView.OnScrollListener() {
    var filteredItems: Int = 0
        set(value) {
            field = value
            loadMoreDataIfNecessary()
        }
    var unfilteredItems: Int = 0
        get() = max(field, filteredItems)
        set(value) {
            field = value
            loadMoreDataIfNecessary()
        }

    var force: Boolean = false
        set(value) {
            field = value
            fetchedPages = 0
            loadMoreDataIfNecessary()
        }

    @VisibleForTesting
    internal var fetchedPages = 0
    @VisibleForTesting
    internal var lastVisibleItem: Int = 0
        set(value) {
            field = value
            loadMoreDataIfNecessary()
        }

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)
        lastVisibleItem = when (val layoutManager = recyclerView.layoutManager) {
            is LinearLayoutManager -> layoutManager.findLastVisibleItemPosition()
            else -> 0
        }
        filteredItems = recyclerView.adapter?.itemCount ?: 0
    }

    @MainThread
    fun loadMoreDataIfNecessary() {
        if (shouldLoadNextPage()) {
            Timber.tag(TAG).d(
                "Triggering %s load of %d-%d Position: %d/%d (%d total)",
                if (force) "forced" else "unforced",
                fetchedPages * pageSize + 1,
                (fetchedPages + 1) * pageSize,
                lastVisibleItem,
                filteredItems,
                unfilteredItems
            )

            if (loadMore(fetchedPages + 1, force)) {
                fetchedPages++
            }
        }
    }

    @VisibleForTesting
    internal fun shouldLoadNextPage() = when {
        // load more if we have scrolled past the data we have already loaded
        lastVisibleItem + threshold >= fetchedPages * pageSize -> true
        // load more if we don't have at least {threshold} items remaining in the list
        // unless we have already tried loading more than {unfilteredItems + threshold}
        lastVisibleItem + threshold >= filteredItems -> fetchedPages * pageSize <= unfilteredItems + threshold
        // default to not loading another page
        else -> false
    }
}
