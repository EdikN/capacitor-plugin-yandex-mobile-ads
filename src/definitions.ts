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

export interface NotificationOptions {
    id: string
    title: string
    description: string
    delaySeconds?: number
    image?: string
    payload?: string
    smallIcon?: string
    color?: string
    channelId?: string
    channelName?: string
}

export interface LaunchNotification {
    id?: string
    payload?: string
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
    scheduleNotification(options: NotificationOptions): Promise<void>
    cancelNotification(options: { id: string }): Promise<void>
    cancelAllNotifications(): Promise<void>
    requestNotificationPermission(): Promise<{ granted: boolean }>
    getLaunchNotification(): Promise<LaunchNotification>
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
            | 'notificationOpened',
        listenerFunc: (data?: any) => void,
    ): Promise<PluginListenerHandle>
}
