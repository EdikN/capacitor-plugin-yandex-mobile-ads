package com.playgama.yandexads

import android.app.Activity
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import com.getcapacitor.JSObject
import com.yandex.mobile.ads.banner.BannerAdEventListener
import com.yandex.mobile.ads.banner.BannerAdSize
import com.yandex.mobile.ads.banner.BannerAdView
import com.yandex.mobile.ads.common.AdRequest
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.common.ImpressionData
import com.yandex.mobile.ads.common.MobileAds
import com.yandex.mobile.ads.interstitial.InterstitialAd
import com.yandex.mobile.ads.interstitial.InterstitialAdEventListener
import com.yandex.mobile.ads.interstitial.InterstitialAdLoadListener
import com.yandex.mobile.ads.interstitial.InterstitialAdLoader
import com.yandex.mobile.ads.rewarded.Reward
import com.yandex.mobile.ads.rewarded.RewardedAd
import com.yandex.mobile.ads.rewarded.RewardedAdEventListener
import com.yandex.mobile.ads.rewarded.RewardedAdLoadListener
import com.yandex.mobile.ads.rewarded.RewardedAdLoader

class YandexMobileAdsManager(
    private val activity: Activity,
    private val plugin: YandexMobileAdsPlugin,
) {
    private val bannerViews = mutableMapOf<String, BannerAdView>()
    private val adRequest = AdRequest.Builder().build()

    fun initialize(appMetricaKey: String?) {
        MobileAds.initialize(activity) {}
    }

    fun showInterstitial(adUnitId: String, callback: (String?) -> Unit) {
        val loader = InterstitialAdLoader(activity)
        loader.setAdLoadListener(object : InterstitialAdLoadListener {
            override fun onAdLoaded(ad: InterstitialAd) {
                ad.setAdEventListener(object : InterstitialAdEventListener {
                    override fun onAdShown() {
                        plugin.emit("interstitialOpened")
                    }
                    override fun onAdFailedToShow(error: AdRequestError) {
                        plugin.emit("interstitialFailed", JSObject().put("error", error.description))
                        callback(error.description)
                    }
                    override fun onAdDismissed() {
                        plugin.emit("interstitialClosed")
                        ad.setAdEventListener(null)
                    }
                    override fun onAdClicked() {}
                    override fun onAdImpression(data: ImpressionData?) {}
                })
                activity.runOnUiThread { ad.show(activity) }
                callback(null)
            }
            override fun onAdFailedToLoad(error: AdRequestError) {
                plugin.emit("interstitialFailed", JSObject().put("error", error.description))
                callback(error.description)
            }
        })
        loader.loadAd(com.yandex.mobile.ads.interstitial.InterstitialAdRequestConfiguration.Builder(adUnitId).build())
    }

    fun preloadInterstitial(adUnitId: String) {
        val loader = InterstitialAdLoader(activity)
        loader.setAdLoadListener(object : InterstitialAdLoadListener {
            override fun onAdLoaded(ad: InterstitialAd) {}
            override fun onAdFailedToLoad(error: AdRequestError) {}
        })
        loader.loadAd(com.yandex.mobile.ads.interstitial.InterstitialAdRequestConfiguration.Builder(adUnitId).build())
    }

    fun showRewarded(adUnitId: String, callback: (String?) -> Unit) {
        val loader = RewardedAdLoader(activity)
        loader.setAdLoadListener(object : RewardedAdLoadListener {
            override fun onAdLoaded(ad: RewardedAd) {
                ad.setAdEventListener(object : RewardedAdEventListener {
                    override fun onAdShown() {
                        plugin.emit("rewardedOpened")
                    }
                    override fun onAdFailedToShow(error: AdRequestError) {
                        plugin.emit("rewardedFailed", JSObject().put("error", error.description))
                        callback(error.description)
                    }
                    override fun onAdDismissed() {
                        plugin.emit("rewardedClosed")
                        ad.setAdEventListener(null)
                    }
                    override fun onRewarded(reward: Reward) {
                        plugin.emit("userEarned", JSObject().put("type", reward.type).put("amount", reward.amount))
                    }
                    override fun onAdClicked() {}
                    override fun onAdImpression(data: ImpressionData?) {}
                })
                activity.runOnUiThread { ad.show(activity) }
                callback(null)
            }
            override fun onAdFailedToLoad(error: AdRequestError) {
                plugin.emit("rewardedFailed", JSObject().put("error", error.description))
                callback(error.description)
            }
        })
        loader.loadAd(com.yandex.mobile.ads.rewarded.RewardedAdRequestConfiguration.Builder(adUnitId).build())
    }

    fun preloadRewarded(adUnitId: String) {
        val loader = RewardedAdLoader(activity)
        loader.setAdLoadListener(object : RewardedAdLoadListener {
            override fun onAdLoaded(ad: RewardedAd) {}
            override fun onAdFailedToLoad(error: AdRequestError) {}
        })
        loader.loadAd(com.yandex.mobile.ads.rewarded.RewardedAdRequestConfiguration.Builder(adUnitId).build())
    }

    fun showBanner(adUnitId: String, position: String, callback: (String?) -> Unit) {
        activity.runOnUiThread {
            try {
                val bannerAdView = BannerAdView(activity)
                bannerAdView.setAdUnitId(adUnitId)
                bannerAdView.setAdSize(BannerAdSize.stickySize(activity, 320))
                bannerAdView.setBannerAdEventListener(object : BannerAdEventListener {
                    override fun onAdLoaded() {
                        plugin.emit("bannerShown")
                        callback(null)
                    }
                    override fun onAdFailedToLoad(error: AdRequestError) {
                        plugin.emit("bannerFailed", JSObject().put("error", error.description))
                        callback(error.description)
                    }
                    override fun onAdClicked() {}
                    override fun onLeftApplication() {}
                    override fun onReturnedToApplication() {}
                    override fun onImpression(data: ImpressionData?) {}
                })

                val rootView = activity.window.decorView.findViewById<ViewGroup>(android.R.id.content)
                val params = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    if (position == "top") Gravity.TOP else Gravity.BOTTOM,
                )
                rootView.addView(bannerAdView, params)
                bannerAdView.loadAd(adRequest)
                bannerViews[adUnitId] = bannerAdView
            } catch (e: Exception) {
                callback(e.message)
            }
        }
    }

    fun hideBanner(adUnitId: String) {
        activity.runOnUiThread {
            bannerViews.remove(adUnitId)?.let { view ->
                (view.parent as? ViewGroup)?.removeView(view)
                view.destroy()
                plugin.emit("bannerHidden")
            }
        }
    }
}
