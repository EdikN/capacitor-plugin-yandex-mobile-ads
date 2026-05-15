package com.playgama.yandexads

import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin

@CapacitorPlugin(name = "YandexMobileAds")
class YandexMobileAdsPlugin : Plugin() {
    private lateinit var implementation: YandexMobileAdsManager

    override fun load() {
        implementation = YandexMobileAdsManager(activity, this)
    }

    @PluginMethod
    fun initialize(call: PluginCall) {
        val appMetricaKey = call.getString("appMetricaKey")
        implementation.initialize(appMetricaKey)
        call.resolve()
    }

    @PluginMethod
    fun showInterstitial(call: PluginCall) {
        val adUnitId = call.getString("adUnitId") ?: run {
            call.reject("Missing adUnitId")
            return
        }
        implementation.showInterstitial(adUnitId) { error ->
            if (error != null) call.reject(error) else call.resolve()
        }
    }

    @PluginMethod
    fun preloadInterstitial(call: PluginCall) {
        val adUnitId = call.getString("adUnitId") ?: run {
            call.reject("Missing adUnitId")
            return
        }
        implementation.preloadInterstitial(adUnitId)
        call.resolve()
    }

    @PluginMethod
    fun showRewarded(call: PluginCall) {
        val adUnitId = call.getString("adUnitId") ?: run {
            call.reject("Missing adUnitId")
            return
        }
        implementation.showRewarded(adUnitId) { error ->
            if (error != null) call.reject(error) else call.resolve()
        }
    }

    @PluginMethod
    fun preloadRewarded(call: PluginCall) {
        val adUnitId = call.getString("adUnitId") ?: run {
            call.reject("Missing adUnitId")
            return
        }
        implementation.preloadRewarded(adUnitId)
        call.resolve()
    }

    @PluginMethod
    fun showBanner(call: PluginCall) {
        val adUnitId = call.getString("adUnitId") ?: run {
            call.reject("Missing adUnitId")
            return
        }
        val position = call.getString("position") ?: "bottom"
        implementation.showBanner(adUnitId, position) { error ->
            if (error != null) call.reject(error) else call.resolve()
        }
    }

    @PluginMethod
    fun hideBanner(call: PluginCall) {
        val adUnitId = call.getString("adUnitId") ?: run {
            call.reject("Missing adUnitId")
            return
        }
        implementation.hideBanner(adUnitId)
        call.resolve()
    }

    fun emit(eventName: String, data: JSObject? = null) {
        notifyListeners(eventName, data ?: JSObject())
    }
}
