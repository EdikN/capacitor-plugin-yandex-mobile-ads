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
            | 'bannerFailed',
        listenerFunc: (data?: any) => void,
    ): Promise<PluginListenerHandle>
}
