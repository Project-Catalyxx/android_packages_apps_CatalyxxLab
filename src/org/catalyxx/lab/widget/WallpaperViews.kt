/*
 * SPDX-FileCopyrightText: 2023-2026 Project Matrixx
 * SPDX-FileCopyrightText: 2026 Project Catalyxx
 * SPDX-License-Identifier: Apache-2.0
 */

package org.catalyxx.lab.widget

import android.animation.ValueAnimator
import android.app.WallpaperManager
import android.content.Context
import android.graphics.Matrix
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import kotlin.math.max

/**
 * The home screen wallpaper, cropped to fill the view, after Matrixx Lab's WallpaperView. It is
 * read once, when the view is attached. A live wallpaper has no picture to show, so the view's
 * background stays.
 */
open class WallpaperView
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    ImageView(context, attrs, defStyleAttr) {

    init {
        scaleType = ScaleType.CENTER_CROP
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (drawable == null) {
            setImageDrawable(loadWallpaper())
        }
    }

    private fun loadWallpaper(): Drawable? =
        try {
            WallpaperManager.getInstance(context).drawable
        } catch (e: SecurityException) {
            null
        }
}

/**
 * The wallpaper, slightly zoomed in, drifting slowly from one corner to the other and back, after
 * Matrixx Lab's AnimateWallpaperView. It only moves while it is on screen and animations are on.
 */
class DriftingWallpaperView
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    WallpaperView(context, attrs, defStyleAttr) {

    private val drawMatrix = Matrix()
    private var position = 0f
    private val drift =
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = DRIFT_MS
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                position = it.animatedValue as Float
                updateMatrix()
            }
        }

    init {
        scaleType = ScaleType.MATRIX
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        updateMatrix()
    }

    override fun onDetachedFromWindow() {
        drift.cancel()
        super.onDetachedFromWindow()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateMatrix()
    }

    override fun onVisibilityAggregated(isVisible: Boolean) {
        super.onVisibilityAggregated(isVisible)
        when {
            !isVisible || !ValueAnimator.areAnimatorsEnabled() -> drift.pause()
            drift.isStarted -> drift.resume()
            else -> drift.start()
        }
    }

    private fun updateMatrix() {
        val picture = drawable ?: return
        val pictureWidth = picture.intrinsicWidth.toFloat()
        val pictureHeight = picture.intrinsicHeight.toFloat()
        if (width == 0 || height == 0 || pictureWidth <= 0f || pictureHeight <= 0f) return

        val scale = max(width / pictureWidth, height / pictureHeight) * ZOOM
        drawMatrix.setScale(scale, scale)
        drawMatrix.postTranslate(
            (width - pictureWidth * scale) * position,
            (height - pictureHeight * scale) * position,
        )
        imageMatrix = drawMatrix
    }

    private companion object {
        const val ZOOM = 1.15f
        const val DRIFT_MS = 60_000L
    }
}
