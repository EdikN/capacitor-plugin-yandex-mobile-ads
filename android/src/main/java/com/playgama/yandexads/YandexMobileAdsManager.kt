package com.playgama.yandexads

import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.getcapacitor.JSObject
import com.yandex.mobile.ads.banner.BannerAdEventListener
import com.yandex.mobile.ads.banner.BannerAdSize
import com.yandex.mobile.ads.banner.BannerAdView
import com.yandex.mobile.ads.common.AdError
import com.yandex.mobile.ads.common.AdRequest
import com.yandex.mobile.ads.common.AdRequestConfiguration
import com.yandex.mobile.ads.common.ImpressionData
import com.yandex.mobile.ads.common.MobileAds
import com.yandex.mobile.ads.interstitial.InterstitialAd
import com.yandex.mobile.ads.interstitial.InterstitialAdEventListener
import com.yandex.mobile.ads.interstitial.InterstitialAdLoader
import com.yandex.mobile.ads.rewarded.Reward
import com.yandex.mobile.ads.rewarded.RewardedAd
import com.yandex.mobile.ads.rewarded.RewardedAdEventListener
import com.yandex.mobile.ads.rewarded.RewardedAdLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class YandexMobileAdsManager(
    private val activity: AppCompatActivity,
    private val plugin: YandexMobileAdsPlugin,
) {
    private val bannerViews = mutableMapOf<String, BannerAdView>()
    private val scope = CoroutineScope(Dispatchers.Main)

    fun initialize(appMetricaKey: String?) {
        MobileAds.initialize(activity) {}
    }

    fun showInterstitial(adUnitId: String, callback: (String?) -> Unit) {
        scope.launch {
            try {
                val loader = InterstitialAdLoader(activity)
                val ad = loader.loadAd(AdRequestConfiguration.Builder(adUnitId).build())
                ad.setAdEventListener(object : InterstitialAdEventListener {
                    override fun onAdShown() {
                        plugin.emit("interstitialOpened")
                    }
                    override fun onAdFailedToShow(adError: AdError) {
                        plugin.emit("interstitialFailed", JSObject().put("error", adError.description))
                        callback(adError.description)
                    }
                    override fun onAdDismissed() {
                        plugin.emit("interstitialClosed")
                        ad.setAdEventListener(null)
                    }
                    override fun onAdClicked() {}
                    override fun onAdImpression(data: ImpressionData?) {}
                })
                ad.show(activity)
                callback(null)
            } catch (e: Exception) {
                plugin.emit("interstitialFailed", JSObject().put("error", e.message))
                callback(e.message)
            }
        }
    }

    fun preloadInterstitial(adUnitId: String) {
        scope.launch {
            try {
                InterstitialAdLoader(activity).loadAd(AdRequestConfiguration.Builder(adUnitId).build())
            } catch (_: Exception) {}
        }
    }

    fun showRewarded(adUnitId: String, callback: (String?) -> Unit) {
        scope.launch {
            try {
                val loader = RewardedAdLoader(activity)
                val ad = loader.loadAd(AdRequestConfiguration.Builder(adUnitId).build())
                ad.setAdEventListener(object : RewardedAdEventListener {
                    override fun onAdShown() {
                        plugin.emit("rewardedOpened")
                    }
                    override fun onAdFailedToShow(adError: AdError) {
                        plugin.emit("rewardedFailed", JSObject().put("error", adError.description))
                        callback(adError.description)
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
                ad.show(activity)
                callback(null)
            } catch (e: Exception) {
                plugin.emit("rewardedFailed", JSObject().put("error", e.message))
                callback(e.message)
            }
        }
    }

    fun preloadRewarded(adUnitId: String) {
        scope.launch {
            try {
                RewardedAdLoader(activity).loadAd(AdRequestConfiguration.Builder(adUnitId).build())
            } catch (_: Exception) {}
        }
    }

    fun showBanner(adUnitId: String, position: String, callback: (String?) -> Unit) {
        activity.runOnUiThread {
            try {
                val bannerAdView = BannerAdView(activity)
                bannerAdView.adSize = BannerAdSize.fixedSize(activity, 320, 50)
                bannerAdView.setBannerAdEventListener(object : BannerAdEventListener {
                    override fun onAdLoaded() {
                        plugin.emit("bannerShown")
                        callback(null)
                    }
                    override fun onAdFailedToLoad(adError: AdError) {
                        plugin.emit("bannerFailed", JSObject().put("error", adError.description))
                        callback(adError.description)
                    }
                    override fun onAdClicked() {}
                    override fun onImpression(data: ImpressionData?) {}
                })

                val rootView = activity.window.decorView.findViewById<ViewGroup>(android.R.id.content)
                val params = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    if (position == "top") Gravity.TOP else Gravity.BOTTOM,
                )
                rootView.addView(bannerAdView, params)
                bannerAdView.loadAd(adUnitId, AdRequest.Builder().build())
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
