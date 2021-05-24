package org.mpdx.androids.library.base.databinding

import android.view.LayoutInflater
import android.view.View
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.core.view.children
import androidx.databinding.BindingAdapter
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import org.ccci.gto.android.common.base.Constants.INVALID_ID_RES
import org.mpdx.R

@BindingAdapter("chips", "chipLayout", "chipCloseOnClick", requireAll = false)
fun ChipGroup.setChips(chips: Collection<String>?, @LayoutRes layout: Int, chipOnCloseClick: View.OnClickListener?) =
    setChipsInt(chips?.associateBy { it }, layout = layout, chipOnCloseClick = chipOnCloseClick)

@BindingAdapter(
    "chips",
    "chipsBefore",
    "chipLayout",
    "chipIconUrl",
    "chipOnClickTag",
    "chipCloseOnClickTag",
    requireAll = false
)
fun ChipGroup.setChips(
    chips: Map<out Any, String>?,
    @IdRes before: Int?,
    @LayoutRes layout: Int?,
    chipIconUrl: ChipUrlClosure?,
    chipOnClick: ChipClickListener?,
    chipOnCloseClick: ChipClickListener?
) = setChipsInt(
    chips,
    layout = layout,
    before = before,
    chipIconUrl = chipIconUrl,
    chipOnClick = chipOnClick?.toOnClickListener(),
    chipOnCloseClick = chipOnCloseClick?.toOnClickListener()
)

private fun ChipGroup.setChipsInt(
    chips: Map<out Any, String>?,
    @LayoutRes layout: Int? = null,
    @IdRes after: Int = INVALID_ID_RES,
    @IdRes before: Int? = null,
    chipIconUrl: ChipUrlClosure? = null,
    chipOnClick: View.OnClickListener? = null,
    chipOnCloseClick: View.OnClickListener? = null
) {
    val beforeView = findViewById<View>(before ?: INVALID_ID_RES)
    val (current, invalid) = children
        .dropWhile { after != INVALID_ID_RES && it.id != after }
        .drop(if (after != INVALID_ID_RES) 1 else 0)
        .takeWhile { it != beforeView }
        .filterIsInstance<Chip>()
        .partition { it.getTag(R.id.chipLayout) == layout }
        .let { it.first.toMutableList() to it.second }

    chips?.forEach { chip ->
        val view = when {
            current.isNotEmpty() -> current.removeAt(0)
            layout != null && layout != 0 -> LayoutInflater.from(context).inflate(layout, this, false) as Chip
            else -> Chip(context)
        }
        if (view.parent == null) addView(view, indexOfChild(beforeView))
        view.setTag(R.id.chipLayout, layout)
        view.setTag(R.id.chipTag, chip.key)
        if (chipIconUrl != null) view.addIconFromUrl(chipIconUrl.resolveUrl(chip.key))
        view.text = chip.value
        view.setOnClickListener(chipOnClick)
        view.setOnCloseIconClickListener(chipOnCloseClick)
    }

    // remove any remaining excess views
    (current + invalid).forEach { removeView(it) }
}

interface ChipUrlClosure {
    fun resolveUrl(tag: Any?): String?
}

interface ChipClickListener {
    fun onChipClicked(tag: Any?)
}

private inline fun ChipClickListener.toOnClickListener() =
    View.OnClickListener { v -> onChipClicked(v.getTag(R.id.chipTag)) }
