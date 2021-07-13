@file:JvmName("TaskBindingConverters")

package org.mpdx.android.features.tasks.databinding

import android.content.Context
import androidx.databinding.InverseMethod
import org.mpdx.android.R
import org.mpdx.android.features.tasks.model.Task
import org.threeten.bp.temporal.ChronoUnit

fun taskNotificationTypeLabel(context: Context, type: Task.NotificationType?) =
    context.getString((type ?: Task.NotificationType.DEFAULT).labelRes)

@InverseMethod("taskNotificationTypeLabel")
fun taskNotificationTypeFromLabel(context: Context, label: String?) =
    Task.NotificationType.values().firstOrNull { context.getString(it.labelRes) == label }
        ?: Task.NotificationType.DEFAULT

fun chronoUnitLabel(context: Context, unit: ChronoUnit?) = when (unit) {
    ChronoUnit.MINUTES -> context.getString(R.string.task_notification_unit_minutes)
    ChronoUnit.HOURS -> context.getString(R.string.task_notification_unit_hours)
    else -> unit?.toString()
}

@InverseMethod("chronoUnitLabel")
fun chronoUnitFromLabel(context: Context, label: String?) = when (label) {
    context.getString(R.string.task_notification_unit_minutes) -> ChronoUnit.MINUTES
    context.getString(R.string.task_notification_unit_hours) -> ChronoUnit.HOURS
    else -> ChronoUnit.values().firstOrNull { label == it.toString() }
}

fun taskResultLabel(context: Context, result: Task.Result?) = result?.let { context.getString(result.label) }

@InverseMethod("taskResultLabel")
fun taskResultFromLabel(context: Context, label: String?) =
    Task.Result.values().firstOrNull { context.getString(it.label) == label }
