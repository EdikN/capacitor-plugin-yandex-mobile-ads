export interface InitializeOptions {
    appMetricaKey?: string
}

export interface AdOptions {
    adUnitId: string
}

export interface BannerOptions {
    adUnitId: string
    position: 'top' | 'bottom'
}

export interface RewardData {
    type: string
    amount: number
}

export interface ReportEventOptions {
    name: string
    params?: Record<string, unknown>
}

export interface ReportUserProfileOptions {
    attributes: Record<string, string | number | boolean | null>
}

export interface PluginListenerHandle {
    remove(): Promise<void>
}

export interface YandexMobileAdsPlugin {
    initialize(options: InitializeOptions): Promise<void>
    showInterstitial(options: AdOptions): Promise<void>
    preloadInterstitial(options: AdOptions): Promise<void>
    showRewarded(options: AdOptions): Promise<void>
    preloadRewarded(options: AdOptions): Promise<void>
    showBanner(options: BannerOptions): Promise<void>
    hideBanner(options: AdOptions): Promise<void>
    reportEvent(options: ReportEventOptions): Promise<void>
    reportUserProfile(options: ReportUserProfileOptions): Promise<void>
    sendEventsBuffer(): Promise<void>
    addListener(
        eventName:
            | 'interstitialOpened'
            | 'interstitialClosed'
            | 'interstitialFailed'
            | 'rewardedOpened'
            | 'rewardedClosed'
            | 'rewardedFailed'
            | 'userEarned'
            | 'bannerShown'
            | 'bannerHidden'
            | 'bannerFailed'
            | 'appPaused'
            | 'appResumed'
            | 'notificationOpened',
        listenerFunc: (data?: any) => void,
    ): Promise<PluginListenerHandle>
}
