package org.mpdx.android.features.tasks.tasklist

import android.content.Context
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import org.mpdx.android.R

class TasksPagerAdapter(
    private val context: Context,
    fm: FragmentManager,
    private val taskId: String? = null,
    private val taskTime: Long = 0
) : FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
    private enum class Page(@StringRes val title: Int) {
        CURRENT(R.string.current_tasks_lowercase), HISTORY(R.string.task_history_lowercase)
    }

    override fun getCount() = Page.values().size
    override fun getPageTitle(position: Int) = context.getString(Page.values()[position].title)
    override fun getItem(position: Int): Fragment = when (Page.values()[position]) {
        Page.CURRENT -> CurrentTasksFragment.newInstance(taskId, taskTime)
        Page.HISTORY -> TasksHistoryFragment.newInstance()
    }
}
