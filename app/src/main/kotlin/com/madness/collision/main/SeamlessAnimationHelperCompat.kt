package com.madness.collision.main

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.View
import com.oplus.animation.OplusViewSeamless

class SeamlessAnimationHelperCompat(private val context: Context) {

    companion object {
        private const val TAG = "SeamlessAnim"
        private var isAvailable: Boolean? = null

        fun checkAvailability(): Boolean {
            if (isAvailable != null) return isAvailable!!
            isAvailable = try {
                val version = OplusViewSeamless.getVersion()
                Log.d(TAG, "OplusViewSeamless version: $version")
                version >= OplusViewSeamless.OS_16_0_BASE
            } catch (e: NoSuchMethodError) {
                Log.d(TAG, "OplusViewSeamless not available")
                false
            } catch (e: RuntimeException) {
                Log.e(TAG, "checkAvailability error", e)
                false
            }
            return isAvailable!!
        }
    }

    private var isSupported = false
    private var animationView: View? = null

    init {
        isSupported = checkAvailability()
    }

    fun isSupported(): Boolean = isSupported

    fun setAnimationView(view: View) {
        animationView = view
    }

    fun setupSeamlessTransition(
        view: View? = animationView,
        cornerRadius: Float = 0f,
        backgroundColor: Int = -1,
        bitmap: Bitmap? = null,
        rect: Rect? = null,
        viewVisible: Boolean = false,
        listCover: FloatArray? = null,
        forceAlphaOut: Boolean = false,
        alphaOutOnPositionChange: Boolean = false,
        viewWithAlpha: Boolean = false,
        callback: OplusViewSeamless.AnimationCallback? = null
    ): Bundle? {
        if (!isSupported || view == null) return null

        val bundle = Bundle()
        bundle.putBoolean(OplusViewSeamless.VIEW_SEAMLESS_OPEN, true)

        if (backgroundColor != -1) {
            bundle.putInt(OplusViewSeamless.BUNDLE_COLOR, backgroundColor)
        }
        if (cornerRadius > 0f) {
            bundle.putFloat(OplusViewSeamless.BUNDLE_RADIUS, cornerRadius)
        }
        bitmap?.let { bundle.putParcelable(OplusViewSeamless.BUNDLE_BITMAP, it) }
        rect?.let { bundle.putParcelable(OplusViewSeamless.BUNDLE_RECT, it) }
        listCover?.let { bundle.putFloatArray(OplusViewSeamless.BUNDLE_LIST_COVER, it) }
        bundle.putBoolean(OplusViewSeamless.BUNDLE_VIEW_VISIBLE, viewVisible)
        bundle.putBoolean(OplusViewSeamless.BUNDLE_FORCE_LEASH_ALPHA_OUT, forceAlphaOut)
        bundle.putBoolean(OplusViewSeamless.BUNDLE_ALPHA_OUT_ON_POSITION_CHANGE, alphaOutOnPositionChange)
        bundle.putBoolean(OplusViewSeamless.BUNDLE_VIEW_WITH_ALPHA, viewWithAlpha)

        try {
            val result = OplusViewSeamless.setSeamlessView(view, context, bundle, callback)
            if (result) {
                Log.d(TAG, "Seamless transition configured")
                return bundle
            } else {
                Log.w(TAG, "setSeamlessView returned false")
                return null
            }
        } catch (e: NoSuchMethodException) {
            Log.e(TAG, "NoSuchMethodException", e)
            isSupported = false
            return null
        } catch (e: RuntimeException) {
            Log.e(TAG, "RuntimeException", e)
            return null
        }
    }

    fun skipReturnAnimation(activity: Activity) {
        try {
            OplusViewSeamless.skipBackAnim(activity)
        } catch (e: Exception) {
            Log.e(TAG, "skipReturnAnimation error", e)
        }
    }

    fun setSkipReturnSeamless(activity: Activity) {
        try {
            OplusViewSeamless.setSkipViewSeamless(activity)
        } catch (e: Exception) {
            Log.e(TAG, "setSkipReturnSeamless error", e)
        }
    }

    fun setForceAlphaFadeOut(activity: Activity, force: Boolean) {
        try {
            OplusViewSeamless.setForceLeashAlphaOut(activity, force)
        } catch (e: Exception) {
            Log.e(TAG, "setForceAlphaFadeOut error", e)
        }
    }

    fun stopCurrentAnimation(): Boolean {
        return try {
            OplusViewSeamless.finishCurrentAnimation()
        } catch (e: Exception) {
            Log.e(TAG, "stopCurrentAnimation error", e)
            false
        }
    }

    fun getVersion(): Int {
        return try {
            OplusViewSeamless.getVersion()
        } catch (e: Exception) {
            -1
        }
    }
}
