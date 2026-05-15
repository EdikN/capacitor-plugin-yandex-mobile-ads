import { WebPlugin } from '@capacitor/core'
import type {
    YandexMobileAdsPlugin,
    InitializeOptions,
    AdOptions,
    BannerOptions,
    PluginListenerHandle,
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

    async addListener(_eventName: string, _listenerFunc: (data?: any) => void): Promise<PluginListenerHandle> {
        return { remove: async () => {} }
    }
}
