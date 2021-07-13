package org.mpdx.android.base.databinding

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.annotation.VisibleForTesting
import androidx.databinding.BindingAdapter
import com.google.android.material.chip.Chip
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import jp.wasabeef.picasso.transformations.CropCircleTransformation
import org.mpdx.android.R

@BindingAdapter("chipIconUrl")
fun Chip.addIconFromUrl(url: String?) {
    Picasso.get().load(url?.takeUnless { it.isBlank() })
        .transform(CropCircleTransformation())
        .placeholder(R.drawable.cru_icon_avatar)
        .error(R.drawable.cru_icon_avatar)
        .into(ChipIconTarget.of(this))
}

class ChipIconTarget private constructor(@VisibleForTesting internal val chip: Chip) : Target {
    init {
        chip.setTag(R.id.chipIcon, this)
    }

    companion object {
        fun of(chip: Chip) = chip.getTag(R.id.chipIcon) as? ChipIconTarget ?: ChipIconTarget(chip)
    }

    override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
        chip.chipIcon = placeHolderDrawable
    }

    override fun onBitmapFailed(e: java.lang.Exception?, errorDrawable: Drawable?) {
        chip.chipIcon = errorDrawable
    }

    override fun onBitmapLoaded(bitmap: Bitmap, from: Picasso.LoadedFrom?) {
        chip.chipIcon = BitmapDrawable(chip.context.resources, bitmap)
    }
}
