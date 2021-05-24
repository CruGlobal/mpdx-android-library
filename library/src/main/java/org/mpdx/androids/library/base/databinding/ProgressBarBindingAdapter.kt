package org.mpdx.androids.library.base.databinding

import android.os.Build
import android.widget.ProgressBar
import androidx.databinding.BindingAdapter

@BindingAdapter("android:min", "android:progress", "android:secondaryProgress", "android:max", requireAll = false)
fun ProgressBar.bindProgress(min: Int?, progress: Int?, secondaryProgress: Int?, max: Int?) {
    if (min != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) setMin(min)
    if (max != null) setMax(max)
    if (progress != null) setProgress(progress)
    if (secondaryProgress != null) setSecondaryProgress(secondaryProgress)
}
