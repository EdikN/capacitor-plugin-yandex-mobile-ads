import { registerPlugin } from '@capacitor/core'
import type { YandexMobileAdsPlugin } from './definitions'

const YandexMobileAds = registerPlugin<YandexMobileAdsPlugin>('YandexMobileAds', {
    web: () => import('./web').then((m) => new m.YandexMobileAdsWeb()),
})

export * from './definitions'
export { YandexMobileAds }
