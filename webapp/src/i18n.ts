import i18n from 'i18next';
import type { BackendModule, ReadCallback } from 'i18next';
import { initReactI18next } from 'react-i18next';
import LanguageDetector from 'i18next-browser-languagedetector';

const localeLoaders = {
  en: () => import('./locales/en.json'),
  si: () => import('./locales/si.json'),
  es: () => import('./locales/es.json'),
  fr: () => import('./locales/fr.json'),
  de: () => import('./locales/de.json'),
  ja: () => import('./locales/ja.json'),
  zh: () => import('./locales/zh.json'),
};

const translationBackend: BackendModule = {
  type: 'backend',
  init: () => undefined,
  read(language: string, _namespace: string, callback: ReadCallback) {
    const locale = language.split('-')[0] as keyof typeof localeLoaders;
    const loadLocale = localeLoaders[locale];

    if (!loadLocale) {
      callback(new Error(`Unsupported locale: ${language}`), false);
      return;
    }

    loadLocale()
      .then(({ default: translations }) => callback(null, translations))
      .catch((error: unknown) =>
        callback(error instanceof Error ? error : new Error(String(error)), false),
      );
  },
};

i18n
  .use(LanguageDetector)
  .use(translationBackend)
  .use(initReactI18next)
  .init({
    fallbackLng: 'en',
    supportedLngs: Object.keys(localeLoaders),
    load: 'languageOnly',
    interpolation: {
      escapeValue: false,
    }
  });

export default i18n;
