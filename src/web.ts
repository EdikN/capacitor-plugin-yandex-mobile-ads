import { WebPlugin } from '@capacitor/core'
import type {
    YandexMobileAdsPlugin,
    InitializeOptions,
    AdOptions,
    BannerOptions,
    PluginListenerHandle,
    NotificationOptions,
    LaunchNotification,
} from './definitions'

export class YandexMobileAdsWeb extends WebPlugin implements YandexMobileAdsPlugin {
    async initialize(_options: InitializeOptions): Promise<void> {
        console.warn('YandexMobileAds: web platform is not supported')
    }

    async showInterstitial(_options: AdOptions): Promise<void> {
        console.warn('YandexMobileAds: web platform is not supported')
    }

    async preloadInterstitial(_options: AdOptions): Promise<void> {
        console.warn('YandexMobileAds: web platform is not supported')
    }

    async showRewarded(_options: AdOptions): Promise<void> {
        console.warn('YandexMobileAds: web platform is not supported')
    }

    async preloadRewarded(_options: AdOptions): Promise<void> {
        console.warn('YandexMobileAds: web platform is not supported')
    }

    async showBanner(_options: BannerOptions): Promise<void> {
        console.warn('YandexMobileAds: web platform is not supported')
    }

    async hideBanner(_options: AdOptions): Promise<void> {
        console.warn('YandexMobileAds: web platform is not supported')
    }

    async scheduleNotification(_options: NotificationOptions): Promise<void> {
        console.warn('YandexMobileAds: web platform is not supported')
    }

    async cancelNotification(_options: { id: string }): Promise<void> {}

    async cancelAllNotifications(): Promise<void> {}

    async requestNotificationPermission(): Promise<{ granted: boolean }> {
        return { granted: false }
    }

    async getLaunchNotification(): Promise<LaunchNotification> {
        return {}
    }

    async addListener(_eventName: string, _listenerFunc: (data?: any) => void): Promise<PluginListenerHandle> {
        return { remove: async () => {} }
    }
}
