package org.mpdx.androids.library.base.widget.constraintlayout

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintHelper
import androidx.constraintlayout.widget.ConstraintLayout

class EnabledHelper @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    ConstraintHelper(context, attrs) {
    override fun updatePreLayout(container: ConstraintLayout) {
        val enabled = isEnabled
        for (i in 0 until mCount) {
            container.getViewById(mIds[i])?.apply { isEnabled = enabled }
        }
    }
}
