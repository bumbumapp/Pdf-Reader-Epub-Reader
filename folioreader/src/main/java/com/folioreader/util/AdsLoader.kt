package com.folioreader.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/**
 * Interstitials for the whole app (PDF and EPUB screens) go through [InterstitialHelper]:
 * consent gated, preloaded once, shown at most once an hour and never in the first hour after install.
 */
object AdsLoader {

    @JvmStatic
    fun displayInterstitial(context: Context) {
        InterstitialHelper.preload(context)
    }

    @JvmStatic
    fun showAds(context: Context, runMethod: () -> Unit) {
        val activity = context.findActivity()
        if (activity == null) {
            runMethod()
        } else {
            InterstitialHelper.showIfReady(activity) { runMethod() }
        }
    }

    private tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
