package org.mpdx.android.features.tasks.editor

import android.content.Context
import org.mpdx.android.features.constants.Constants
import org.mpdx.android.features.tasks.databinding.chronoUnitLabel
import org.mpdx.android.features.tasks.model.Task
import org.threeten.bp.temporal.ChronoUnit

object TaskEditorBindingConstants {
    @JvmStatic
    fun getTaskResultLabels(context: Context, type: String?) =
        Constants.getResults(type).mapNotNull { Task.Result.fromValue(it)?.label?.let { context.getString(it) } }

    @JvmStatic
    fun getNotificationTypes(context: Context) = Task.NotificationType.values().map { context.getString(it.labelRes) }

    @JvmStatic
    fun getNotificationTimeUnits(context: Context) = listOf(
        chronoUnitLabel(context, ChronoUnit.MINUTES),
        chronoUnitLabel(context, ChronoUnit.HOURS)
    )
}
