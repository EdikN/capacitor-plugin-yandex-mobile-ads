package com.playgama.yandexads

import android.util.DisplayMetrics
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.getcapacitor.JSObject
import io.appmetrica.analytics.AppMetrica
import io.appmetrica.analytics.AppMetricaConfig
import com.yandex.mobile.ads.banner.BannerAdEventListener
import com.yandex.mobile.ads.banner.BannerAdSize
import com.yandex.mobile.ads.banner.BannerAdView
import com.yandex.mobile.ads.common.AdError
import com.yandex.mobile.ads.common.AdRequest
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.common.ImpressionData
import com.yandex.mobile.ads.common.YandexAds
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
    private val activity: AppCompatActivity,
    private val plugin: YandexMobileAdsPlugin,
) {
    private val bannerViews = mutableMapOf<String, BannerAdView>()

    // Set while a banner is on screen — see applyBannerLayout.
    private var bannerLayoutListener: android.view.View.OnLayoutChangeListener? = null

    // AppMetrica is optional: without a key in playgama-bridge-config.json the
    // ads still work, there is simply no analytics. Activation goes first
    // because the ads SDK reports through AppMetrica when it is present.
    fun initialize(appMetricaKey: String?) {
        if (!appMetricaKey.isNullOrBlank()) {
            try {
                val config = AppMetricaConfig.newConfigBuilder(appMetricaKey).build()
                AppMetrica.activate(activity.applicationContext, config)
                // Sessions are counted from activity transitions; without this
                // every launch looks like one endless session.
                AppMetrica.enableActivityAutoTracking(activity.application)
            } catch (e: Exception) {
                // Analytics must never be the reason a game fails to start.
            }
        }
        YandexAds.initialize(activity) {}
    }

    fun showInterstitial(adUnitId: String, callback: (String?) -> Unit) {
        val loader = InterstitialAdLoader(activity)
        loader.loadAd(AdRequest.Builder(adUnitId).build(), object : InterstitialAdLoadListener {
            override fun onAdLoaded(ad: InterstitialAd) {
                ad.setAdEventListener(object : InterstitialAdEventListener {
                    override fun onAdShown() { plugin.emit("interstitialOpened") }
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
                activity.runOnUiThread { ad.show(activity) }
                callback(null)
            }
            override fun onAdFailedToLoad(error: AdRequestError) {
                plugin.emit("interstitialFailed", JSObject().put("error", error.description))
                callback(error.description)
            }
        })
    }

    fun preloadInterstitial(adUnitId: String) {
        val loader = InterstitialAdLoader(activity)
        loader.loadAd(AdRequest.Builder(adUnitId).build(), object : InterstitialAdLoadListener {
            override fun onAdLoaded(ad: InterstitialAd) {}
            override fun onAdFailedToLoad(error: AdRequestError) {}
        })
    }

    fun showRewarded(adUnitId: String, callback: (String?) -> Unit) {
        val loader = RewardedAdLoader(activity)
        loader.loadAd(AdRequest.Builder(adUnitId).build(), object : RewardedAdLoadListener {
            override fun onAdLoaded(ad: RewardedAd) {
                ad.setAdEventListener(object : RewardedAdEventListener {
                    override fun onAdShown() { plugin.emit("rewardedOpened") }
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
                activity.runOnUiThread { ad.show(activity) }
                callback(null)
            }
            override fun onAdFailedToLoad(error: AdRequestError) {
                plugin.emit("rewardedFailed", JSObject().put("error", error.description))
                callback(error.description)
            }
        })
    }

    fun preloadRewarded(adUnitId: String) {
        val loader = RewardedAdLoader(activity)
        loader.loadAd(AdRequest.Builder(adUnitId).build(), object : RewardedAdLoadListener {
            override fun onAdLoaded(ad: RewardedAd) {}
            override fun onAdFailedToLoad(error: AdRequestError) {}
        })
    }

    // Measure the container the banner is actually placed in, not the display.
    // getMetrics() reports the screen MINUS the system bars, while a fullscreen
    // activity's content view spans the whole panel — so sizing the WebView by
    // the former left a strip of bare activity background between the game and
    // the banner, exactly as tall as the navigation bar that isn't there.
    private fun contentView(): ViewGroup =
        activity.window.decorView.findViewById(android.R.id.content)

    // Split the activity between the WebView and the banner, using the CURRENT
    // size of the content view. Both heights are plain pixels, so they describe
    // one screen geometry only: after a rotation the WebView keeps its old,
    // portrait-sized height in a landscape window, its bottom rows fall past the
    // visible area and the banner ends up drawn over them — the exit buttons
    // among them. Hence applyBannerLayout is re-run on every layout change for
    // as long as a banner is up, not just when it is created.
    private fun applyBannerLayout(bannerAdView: BannerAdView) {
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        activity.windowManager.defaultDisplay.getMetrics(metrics)

        val rootView = contentView()
        val screenHeightPx = if (rootView.height > 0) rootView.height else metrics.heightPixels
        val bannerHeightPx = (screenHeightPx * 0.10).toInt()
        val webViewHeightPx = screenHeightPx - bannerHeightPx

        // Only touch layoutParams when something really changed: this runs from
        // a layout listener, and an unconditional requestLayout would loop.
        val webView = plugin.bridge.webView
        val webViewParams = webView.layoutParams
        if (webViewParams.height != webViewHeightPx) {
            webViewParams.height = webViewHeightPx
            webView.layoutParams = webViewParams
        }

        val bannerParams = bannerAdView.layoutParams as? FrameLayout.LayoutParams
        if (bannerParams != null && bannerParams.height != bannerHeightPx) {
            bannerParams.height = bannerHeightPx
            bannerAdView.layoutParams = bannerParams
        }
    }

    private fun restoreWebViewHeight() {
        val webView = plugin.bridge.webView
        val webViewParams = webView.layoutParams
        webViewParams.height = ViewGroup.LayoutParams.MATCH_PARENT
        webView.layoutParams = webViewParams
    }

    private fun stopWatchingLayout() {
        bannerLayoutListener?.let { contentView().removeOnLayoutChangeListener(it) }
        bannerLayoutListener = null
    }

    fun showBanner(adUnitId: String, position: String, callback: (String?) -> Unit) {
        activity.runOnUiThread {
            try {
                val metrics = DisplayMetrics()
                @Suppress("DEPRECATION")
                activity.windowManager.defaultDisplay.getMetrics(metrics)

                val rootView = contentView()
                val screenHeightPx = if (rootView.height > 0) rootView.height else metrics.heightPixels
                val bannerHeightPx = (screenHeightPx * 0.10).toInt()
                val webViewHeightPx = screenHeightPx - bannerHeightPx

                val webView = plugin.bridge.webView
                val webViewParams = webView.layoutParams
                webViewParams.height = webViewHeightPx
                webView.layoutParams = webViewParams

                val widthDp = (metrics.widthPixels / metrics.density).toInt()
                val bannerAdView = BannerAdView(activity)
                // The ad rarely fills its slot to the pixel; a black backing
                // keeps whatever is left over reading as part of the frame
                // rather than as a grey seam.
                bannerAdView.setBackgroundColor(android.graphics.Color.BLACK)
                bannerAdView.setAdSize(BannerAdSize.sticky(activity, widthDp))
                bannerAdView.setBannerAdEventListener(object : BannerAdEventListener {
                    override fun onAdLoaded() {
                        plugin.emit("bannerShown")
                        callback(null)
                    }
                    override fun onAdFailedToLoad(error: AdRequestError) {
                        stopWatchingLayout()
                        restoreWebViewHeight()
                        plugin.emit("bannerFailed", JSObject().put("error", error.description))
                        callback(error.description)
                    }
                    override fun onAdClicked() {}
                    override fun onImpression(data: ImpressionData?) {}
                })

                val gravity = if (position == "top") Gravity.TOP else Gravity.BOTTOM
                val params = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    bannerHeightPx,
                    gravity,
                )
                rootView.addView(bannerAdView, params)

                // Rotation, a resumed activity, an immersive-mode re-layout —
                // anything that changes the window height invalidates the pixel
                // split above, so redo it. Posted rather than applied inline: a
                // requestLayout from inside a layout pass is discarded.
                stopWatchingLayout()
                val layoutListener = android.view.View.OnLayoutChangeListener {
                    view, _, top, _, bottom, _, oldTop, _, oldBottom ->
                    if (bottom - top != oldBottom - oldTop) {
                        view.post { applyBannerLayout(bannerAdView) }
                    }
                }
                rootView.addOnLayoutChangeListener(layoutListener)
                bannerLayoutListener = layoutListener

                // Since SDK 8 the ad unit travels in the request rather than on
                // the view — setAdUnitId no longer exists.
                bannerAdView.loadAd(AdRequest.Builder(adUnitId).build())
                bannerViews[adUnitId] = bannerAdView
            } catch (e: Exception) {
                callback(e.message)
            }
        }
    }

    fun hideBanner(adUnitId: String) {
        activity.runOnUiThread {
            stopWatchingLayout()
            bannerViews.remove(adUnitId)?.let { view ->
                (view.parent as? ViewGroup)?.removeView(view)
                view.destroy()
                plugin.emit("bannerHidden")
            }
            restoreWebViewHeight()
        }
    }
}
