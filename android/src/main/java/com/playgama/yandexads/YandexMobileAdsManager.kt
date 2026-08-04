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

    // How much of the window the banner is taking. The banner view sizes itself
    // (WRAP_CONTENT) from the ad size the SDK picked, so its measured height is
    // the only honest answer: forcing the slot to a share of the screen made the
    // creative render clipped against the bottom edge whenever the sticky size
    // came out taller than that share — which on a short landscape window is
    // most of the time, since sticky starts at 50dp and 10% of a 1080px-tall
    // window is under 40dp. The 10% is only the guess covering the gap before
    // the first layout pass; the cap keeps a runaway creative from swallowing
    // the game.
    private fun bannerHeightPx(bannerAdView: BannerAdView, screenHeightPx: Int): Int {
        val measured = bannerAdView.height
        val height = if (measured > 0) measured else (screenHeightPx * 0.10).toInt()
        return height.coerceAtMost((screenHeightPx * 0.25).toInt())
    }

    // Split the activity between the WebView and the banner, using the CURRENT
    // size of the content view. The WebView height is plain pixels, so it
    // describes one screen geometry only: after a rotation it would keep its
    // old, portrait-sized value in a landscape window, its bottom rows falling
    // past the visible area with the banner drawn over them — the exit buttons
    // among them. Hence this is re-run on every layout change of either view for
    // as long as a banner is up, not just when the banner is created.
    private fun applyBannerLayout(bannerAdView: BannerAdView) {
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        activity.windowManager.defaultDisplay.getMetrics(metrics)

        val rootView = contentView()
        val screenHeightPx = if (rootView.height > 0) rootView.height else metrics.heightPixels
        val webViewHeightPx = screenHeightPx - bannerHeightPx(bannerAdView, screenHeightPx)

        // Only touch layoutParams when something really changed: this runs from
        // a layout listener, and an unconditional requestLayout would loop.
        val webView = plugin.bridge.webView
        val webViewParams = webView.layoutParams
        if (webViewParams.height != webViewHeightPx) {
            webViewParams.height = webViewHeightPx
            webView.layoutParams = webViewParams
        }
    }

    private fun restoreWebViewHeight() {
        val webView = plugin.bridge.webView
        val webViewParams = webView.layoutParams
        webViewParams.height = ViewGroup.LayoutParams.MATCH_PARENT
        webView.layoutParams = webViewParams
    }

    private fun stopWatchingLayout() {
        bannerLayoutListener?.let { listener ->
            contentView().removeOnLayoutChangeListener(listener)
            bannerViews.values.forEach { it.removeOnLayoutChangeListener(listener) }
        }
        bannerLayoutListener = null
    }

    fun showBanner(adUnitId: String, position: String, callback: (String?) -> Unit) {
        activity.runOnUiThread {
            try {
                val metrics = DisplayMetrics()
                @Suppress("DEPRECATION")
                activity.windowManager.defaultDisplay.getMetrics(metrics)

                val rootView = contentView()
                val widthDp = (metrics.widthPixels / metrics.density).toInt()
                val bannerAdView = BannerAdView(activity)
                // The ad rarely fills its slot to the pixel; a black backing
                // keeps whatever is left over reading as part of the frame
                // rather than as a grey seam.
                bannerAdView.setBackgroundColor(android.graphics.Color.BLACK)
                // WRAP_CONTENT, so the view is exactly as tall as the ad size
                // the SDK settled on — see bannerHeightPx.
                bannerAdView.setAdSize(BannerAdSize.sticky(activity, widthDp))
                bannerAdView.setBannerAdEventListener(object : BannerAdEventListener {
                    override fun onAdLoaded() {
                        // The view may not be measured yet; the layout listener
                        // below picks up the real height when it is.
                        bannerAdView.post { applyBannerLayout(bannerAdView) }
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
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    gravity,
                )
                rootView.addView(bannerAdView, params)

                // Two things invalidate the split, and both are watched: the
                // window changing height (rotation, a resumed activity, an
                // immersive-mode re-layout) and the banner arriving at — or
                // changing — its own height. Posted rather than applied inline:
                // a requestLayout from inside a layout pass is discarded.
                stopWatchingLayout()
                val layoutListener = android.view.View.OnLayoutChangeListener {
                    view, _, _, _, _, _, _, _, _ ->
                    view.post { applyBannerLayout(bannerAdView) }
                }
                rootView.addOnLayoutChangeListener(layoutListener)
                bannerAdView.addOnLayoutChangeListener(layoutListener)
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
