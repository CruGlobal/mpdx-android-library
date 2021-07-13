package org.mpdx.android.base.widget.material

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.view.View
import androidx.annotation.experimental.UseExperimental
import androidx.core.graphics.drawable.DrawableCompat
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils
import com.google.android.material.badge.ExperimentalBadgeUtils
import com.google.android.material.shape.MaterialShapeDrawable
import org.ccci.gto.android.common.util.getDeclaredFieldOrNull
import org.ccci.gto.android.common.util.getDeclaredMethodOrNull
import org.mpdx.android.utils.resolveDp

internal fun BadgeDrawable.updateCenterAndBoundsForBounds(
    context: Context,
    defaultWidth: Int = context.resolveDp(24).toInt(),
    defaultHeight: Int = context.resolveDp(24).toInt(),
    bounds: Rect = this.bounds.takeUnless { it.isEmpty } ?: Rect(0, 0, defaultWidth, defaultHeight)
) {
    calculateBadgeCoordinatesForBounds(context, bounds)
    invalidateSelf()
}

@SuppressLint("RestrictedApi")
@UseExperimental(markerClass = ExperimentalBadgeUtils::class)
private fun BadgeDrawable.calculateBadgeCoordinatesForBounds(context: Context, bounds: Rect = this.bounds) {
    // calculate bounds, we create a tmp view to expose the layoutDirection for calculateCenterAndBounds()
    val tmpView = View(context).also { it.layoutDirection = DrawableCompat.getLayoutDirection(this) }
    calculateCenterAndBoundsMethod?.invoke(this, context, bounds, tmpView)

    // update the badge bounds
    val badgeBounds = badgeBounds ?: return
    BadgeUtils.updateBadgeBounds(
        badgeBounds,
        badgeCenterX ?: return,
        badgeCenterY ?: return,
        halfBadgeWidth ?: return,
        halfBadgeHeight ?: return
    )

    // update the shape drawable
    shapeDrawable?.setCornerSize(cornerRadius ?: return)
    shapeDrawable?.bounds = badgeBounds
}

private val BadgeDrawable.badgeBounds get() = badgeBoundsField?.get(this) as? Rect
private val BadgeDrawable.badgeCenterX get() = badgeCenterXField?.get(this) as? Float
private val BadgeDrawable.badgeCenterY get() = badgeCenterYField?.get(this) as? Float
private val BadgeDrawable.halfBadgeWidth get() = halfBadgeWidthField?.get(this) as? Float
private val BadgeDrawable.halfBadgeHeight get() = halfBadgeHeightField?.get(this) as? Float
private val BadgeDrawable.cornerRadius get() = cornerRadiusField?.get(this) as? Float
private val BadgeDrawable.shapeDrawable get() = shapeDrawableField?.get(this) as? MaterialShapeDrawable

private val badgeBoundsField by lazy { getDeclaredFieldOrNull<BadgeDrawable>("badgeBounds") }
private val badgeCenterXField by lazy { getDeclaredFieldOrNull<BadgeDrawable>("badgeCenterX") }
private val badgeCenterYField by lazy { getDeclaredFieldOrNull<BadgeDrawable>("badgeCenterY") }
private val halfBadgeWidthField by lazy { getDeclaredFieldOrNull<BadgeDrawable>("halfBadgeWidth") }
private val halfBadgeHeightField by lazy { getDeclaredFieldOrNull<BadgeDrawable>("halfBadgeHeight") }
private val cornerRadiusField by lazy { getDeclaredFieldOrNull<BadgeDrawable>("cornerRadius") }
private val shapeDrawableField by lazy { getDeclaredFieldOrNull<BadgeDrawable>("shapeDrawable") }

private val calculateCenterAndBoundsMethod by lazy {
    getDeclaredMethodOrNull<BadgeDrawable>(
        "calculateCenterAndBounds",
        Context::class.java, Rect::class.java, View::class.java
    )
}
