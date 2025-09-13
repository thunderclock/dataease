import { i18n } from '@/plugins/vue-i18n'

type I18nGlobalTranslation = {
  (key: string): string
  (key: string, locale: string): string
  (key: string, locale: string, list: unknown[]): string
  (key: string, locale: string, named: Record<string, unknown>): string
  (key: string, list: unknown[]): string
  (key: string, named: Record<string, unknown>): string
}

type I18nTranslationRestParameters = [string, any]

const getKey = (namespace: string | undefined, key: string) => {
  if (!namespace) {
    return key
  }
  if (key.startsWith(namespace)) {
    return key
  }
  return `${namespace}.${key}`
}

export const useI18n = (
  namespace?: string
): {
  t: I18nGlobalTranslation
} => {
  const normalFn = {
    t: (key: string) => {
      return getKey(namespace, key)
    }
  }

  if (!i18n) {
    console.warn('i18n not initialized, returning key as fallback')
    return normalFn
  }

  const { t, ...methods } = i18n.global

  const tFn: I18nGlobalTranslation = (key: string, ...arg: any[]) => {
    if (!key) return ''
    if (!key.includes('.') && !namespace) return key

    try {
      const result = (t as any)(getKey(namespace, key), ...(arg as I18nTranslationRestParameters))
      // 如果结果是键名本身，说明翻译未找到
      if (result === getKey(namespace, key)) {
        console.warn(`Translation not found for key: ${getKey(namespace, key)}`)
      }
      return result
    } catch (error) {
      // 如果翻译出错，返回键名
      console.warn(`Translation error for key: ${getKey(namespace, key)}`, error)
      return getKey(namespace, key)
    }
  }
  return {
    ...methods,
    t: tFn
  }
}

export const t = (key: string) => key
