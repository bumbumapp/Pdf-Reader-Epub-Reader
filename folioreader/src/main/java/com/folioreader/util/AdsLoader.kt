package com.folioreader.util

import android.app.Activity
import android.content.Context
import android.util.Log
import com.folioreader.Constants.TIMER_FINISHED
import com.folioreader.R
import com.folioreader.model.Timers
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback


object AdsLoader {
    var mInterstitialAd: InterstitialAd? = null
    private var TAG = "TAG"
    fun displayInterstitial(context: Context) {
        mInterstitialAd=null
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(context,context.getString(R.string.interstitial_ads_id), adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.d(TAG, adError.message)
                mInterstitialAd = null
            }

            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                Log.d(TAG, "Ad was loaded.")
                mInterstitialAd = interstitialAd
            }
        })

    }
    inline fun showAds(context: Context, crossinline runMethod:() -> Unit) {
        if (TIMER_FINISHED && mInterstitialAd != null){
                mInterstitialAd?.show(context as Activity)
                mInterstitialAd?.fullScreenContentCallback = object: FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        TIMER_FINISHED=false
                        Timers.timer().start()
                        displayInterstitial(context)
                        runMethod()
                    }
                    override fun onAdShowedFullScreenContent() {
                        mInterstitialAd = null
                    }
                }

            } else {
                runMethod()
            }

    }
}