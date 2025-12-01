import axios, {
  AxiosInstance,
  AxiosRequestHeaders,
  InternalAxiosRequestConfig,
  AxiosResponse,
  AxiosError,
  AxiosRequestConfig,
  AxiosHeaders
} from 'axios'
import { tryShowLoading, tryHideLoading } from '@/utils/loading'
import qs from 'qs'
import { usePermissionStoreWithOut } from '@/store/modules/permission'
import { useEmbedded } from '@/store/modules/embedded'
import { useLinkStoreWithOut } from '@/store/modules/link'
import { config } from './config'
import { configHandler } from './refresh'
import { isMobile, getLocale } from '@/utils/utils'
import { useRequestStoreWithOut } from '@/store/modules/request'
type AxiosErrorWidthLoading<T> = T & {
  config: {
    loading?: boolean
  }
}

type InternalAxiosRequestConfigWidthLoading<T> = T & {
  loading?: boolean
}

import { ElMessage, ElMessageBox } from 'element-plus-secondary'
import router from '@/router'

const { result_code } = config
import { useCache } from '@/hooks/web/useCache'
const { wsCache } = useCache()
const requestStore = useRequestStoreWithOut()
const embeddedStore = useEmbedded()
const basePath = import.meta.env.VITE_API_BASEPATH

const embeddedBasePath =
  basePath.startsWith('./') && basePath.length > 2 ? basePath.substring(2) : basePath
// 确保路径以 / 开头，避免相对路径问题
const normalizedBasePath = embeddedBasePath.startsWith('/')
  ? embeddedBasePath
  : '/' + embeddedBasePath

/**
 * 获取部署的基础路径（用于子路径部署）
 * 例如：如果部署在 /reports/ 下，返回 /reports
 */
const getDeploymentBasePath = (): string => {
  // 优先使用 Vite 的 BASE_URL 环境变量
  if (import.meta.env.BASE_URL && import.meta.env.BASE_URL !== '/') {
    const base = import.meta.env.BASE_URL
    // 移除末尾的斜杠
    return base.endsWith('/') ? base.slice(0, -1) : base
  }

  // 从 window.location.pathname 自动检测（仅浏览器环境）
  if (typeof window !== 'undefined' && window.location) {
    const pathname = window.location.pathname
    // 如果路径包含 /#/，说明是 hash 路由，需要提取基础路径
    if (pathname.includes('/#')) {
      const hashIndex = pathname.indexOf('/#')
      const base = pathname.substring(0, hashIndex)
      // 如果基础路径不是根路径，返回它
      if (base && base !== '/') {
        return base.endsWith('/') ? base.slice(0, -1) : base
      }
    } else if (pathname !== '/') {
      // 非 hash 路由，且不是根路径
      // 尝试提取第一个路径段作为基础路径
      const parts = pathname.split('/').filter(p => p)
      if (parts.length > 0) {
        // 检查是否是已知的应用路由（这些路由不应该作为基础路径）
        const knownRoutes = [
          'login',
          'admin-login',
          '401',
          'dvCanvas',
          'dashboard',
          'dashboardPreview',
          'chart',
          'previewShow',
          'workbranch',
          'copilot',
          'de-link'
        ]
        // 如果第一个路径段不是已知路由，可能是部署基础路径
        if (!knownRoutes.includes(parts[0])) {
          return '/' + parts[0]
        }
      }
    }
  }

  return ''
}

// 获取部署基础路径
const deploymentBasePath = getDeploymentBasePath()

// 构建完整的 API 路径
let finalBasePath = normalizedBasePath
if (deploymentBasePath && !embeddedStore.baseUrl) {
  // 只有在没有设置 embeddedStore.baseUrl 时才添加部署基础路径
  // 确保路径正确拼接（移除重复的斜杠）
  const apiPath = normalizedBasePath.startsWith('/') ? normalizedBasePath : '/' + normalizedBasePath
  finalBasePath = deploymentBasePath + apiPath
}

export const PATH_URL = embeddedStore.baseUrl
  ? embeddedStore?.baseUrl + normalizedBasePath
  : finalBasePath

export interface AxiosInstanceWithLoading extends AxiosInstance {
  <T = any, R = AxiosResponse<T>, D = any>(
    config: AxiosRequestConfig<D> & { loading?: boolean }
  ): Promise<R>
}

const getTimeOut = () => {
  let time = 100
  const url = PATH_URL + '/sysParameter/requestTimeOut'
  const xhr = new XMLHttpRequest()
  xhr.onreadystatechange = () => {
    if (xhr.readyState === 4 && xhr.status === 200) {
      if (xhr.responseText) {
        try {
          const response = JSON.parse(xhr.responseText)
          if (response.code === 0) {
            time = response.data
          } else {
            ElMessage.error('系统异常，请联系管理员')
          }
        } catch (e) {
          ElMessage.error('系统异常，请联系管理员')
        }
      } else {
        ElMessage.error('网络异常，请联系网管')
      }
    }
  }

  xhr.open('get', url, false)
  xhr.send()
  return time
}

// 创建axios实例
const time = getTimeOut()
window._de_get_time_out = time
const service: AxiosInstanceWithLoading = axios.create({
  baseURL: PATH_URL, // api 的 base_url
  timeout: time ? time * 1000 : config.request_timeout // 请求超时时间
})
const mapping = {
  'zh-CN': 'zh-CN',
  en: 'en-US',
  tw: 'zh-TW'
}
const permissionStore = usePermissionStoreWithOut()
const linkStore = useLinkStoreWithOut()
const CancelToken = axios.CancelToken
const cancelMap = {}

// request拦截器
service.interceptors.request.use(
  async (c: InternalAxiosRequestConfigWidthLoading<InternalAxiosRequestConfig>) => {
    // 在 configHandler 之前先保护 AccessKey 相关的 headers
    const accessKeyHeaders: Record<string, string> = {}
    const originalHeaders = c.headers as AxiosRequestHeaders
    if (originalHeaders['X-ACCESS-KEY']) {
      accessKeyHeaders['X-ACCESS-KEY'] = originalHeaders['X-ACCESS-KEY'] as string
    }
    if (originalHeaders['X-TIMESTAMP']) {
      accessKeyHeaders['X-TIMESTAMP'] = originalHeaders['X-TIMESTAMP'] as string
    }
    if (originalHeaders['X-SIGNATURE']) {
      accessKeyHeaders['X-SIGNATURE'] = originalHeaders['X-SIGNATURE'] as string
    }

    let config = configHandler(c)
    if (config instanceof Promise) {
      config = await config
    }

    if (
      config.method === 'post' &&
      (config.headers as AxiosRequestHeaders)['Content-Type'] ===
        'application/x-www-form-urlencoded'
    ) {
      config.data = qs.stringify(config.data)
    }
    if (embeddedStore.baseUrl) {
      config.baseURL = PATH_URL
    }

    if (isMobile()) {
      ;(config.headers as AxiosRequestHeaders)['X-DE-MOBILE'] = true
    }
    if (linkStore.getLinkToken) {
      ;(config.headers as AxiosRequestHeaders)['X-DE-LINK-TOKEN'] = linkStore.getLinkToken
    } else if (embeddedStore.token) {
      ;(config.headers as AxiosRequestHeaders)['X-EMBEDDED-TOKEN'] = embeddedStore.token
    }
    const locale = getLocale()
    if (locale) {
      const val = mapping[locale] || locale
      ;(config.headers as AxiosRequestHeaders)['Accept-Language'] = val
    }

    if (config.method === 'get' && config.params) {
      let url = config.url as string
      url += '?'
      const keys = Object.keys(config.params)
      for (const key of keys) {
        if (config.params[key] !== void 0 && config.params[key] !== null) {
          url += `${key}=${encodeURIComponent(config.params[key])}&`
        }
      }
      url = url.substring(0, url.length - 1)
      config.params = {}
      config.url = url
    }
    config.cancelToken = new CancelToken(function executor(c) {
      cancelMap[config.url] = c
    })
    config.loading && tryShowLoading(permissionStore.getCurrentPath)

    // 恢复 AccessKey 相关的 headers（确保它们被正确设置）
    if (Object.keys(accessKeyHeaders).length > 0) {
      // 确保 headers 对象存在
      if (!config.headers) {
        config.headers = {} as AxiosRequestHeaders
      }
      // 强制设置 AccessKey headers，确保它们不会被覆盖
      Object.keys(accessKeyHeaders).forEach(key => {
        const headerValue = accessKeyHeaders[key]
        if (headerValue) {
          ;(config.headers as AxiosRequestHeaders)[key] = headerValue
          // 同时设置小写版本，确保兼容性
          ;(config.headers as AxiosRequestHeaders)[key.toLowerCase()] = headerValue
        }
      })
    }

    return config
  },
  (error: AxiosErrorWidthLoading<AxiosError>) => {
    error.config.loading && tryHideLoading(permissionStore.getCurrentPath)
    Promise.reject(error)
  }
)

// response 拦截器
service.interceptors.response.use(
  (
    response: AxiosResponse<any> & { config: InternalAxiosRequestConfig & { loading?: boolean } }
  ) => {
    executeVersionHandler(response)
    /* if (response.headers['x-de-refresh-token']) {
      wsCache.set('user.token', response.headers['x-de-refresh-token'])
      wsCache.set('user.exp', new Date().getTime() + 90000)
    } */
    if (response.headers['x-de-link-token']) {
      linkStore.setLinkToken(response.headers['x-de-link-token'])
    }
    response.config.loading && tryHideLoading(permissionStore.getCurrentPath)

    if (response.config.responseType === 'blob') {
      // 如果是文件流，直接过
      return response
    } else if (response.data.code === result_code || response.data.code === 50002) {
      return response.data
    } else if (response.config.url.match(/^\/map|geo\/\d{3}\/\d+\.json$/)) {
      //   TODO 处理静态文件
      return response
    } else if (
      response.config.url.includes('DEXPack.umd.js') ||
      response.config.url.includes('/i18n/custom_')
    ) {
      return response
    } else if (response.config.url.startsWith('/xpackComponent/pluginStaticInfo/extensions-')) {
      return response
    } else {
      if (
        !response?.config?.url.startsWith('/xpackComponent/content') &&
        response?.data?.code !== 60003
      ) {
        ElMessage({
          type: 'error',
          message: response.data.msg,
          showClose: true
        })
        if (response.data.code === 80001) {
          localStorage.clear()
          let queryRedirectPath = '/workbranch/index'
          if (router.currentRoute.value.fullPath) {
            queryRedirectPath = router.currentRoute.value.fullPath as string
          }
          router.push(`/login?redirect=${queryRedirectPath}`)
        }
      } else if (response?.config?.url.startsWith('/xpackComponent/content')) {
        console.error(
          "never mind this error about '/xpackComponent/content', just a reminder to support the official license"
        )
      }

      return Promise.reject(response.data.msg)
    }
  },
  (error: AxiosErrorWidthLoading<AxiosError>) => {
    if (error.message?.includes('timeout of')) {
      requestStore.resetLoadingMap()
      ElMessage({
        type: 'error',
        message: '请求超时，请稍后再试',
        showClose: true
      })
    }

    if (!error?.response) {
      return Promise.reject(error)
    }

    if (error?.response.status === 413) {
      ElMessage({
        type: 'error',
        message: '文件大小超出限制, 请修改相关配置文件',
        showClose: true
      })
      return
    }
    const header = error.response?.headers as AxiosHeaders
    if (
      !error.config.url.startsWith('/xpackComponent/content') &&
      !header.has('DE-FORBIDDEN-FLAG') &&
      !header.has('DE-GATEWAY-FLAG')
    ) {
      ElMessage({
        type: 'error',
        message: error.response?.data?.msg ? error.response?.data?.msg : error.message,
        showClose: true
      })
    } else if (error?.config?.url.startsWith('/xpackComponent/content')) {
      console.error(
        "never mind this error about '/xpackComponent/content', just a reminder to support the official license"
      )
    }

    error.config.loading && tryHideLoading(permissionStore.getCurrentPath)
    if (header.has('DE-GATEWAY-FLAG')) {
      localStorage.clear()
      const flag = header.get('DE-GATEWAY-FLAG')
      localStorage.setItem('DE-GATEWAY-FLAG', flag.toString())
      let queryRedirectPath = '/workbranch/index'
      if (router.currentRoute.value.fullPath) {
        queryRedirectPath = router.currentRoute.value.fullPath as string
      }
      router.push(`/login?redirect=${queryRedirectPath}`)
    }
    if (header.has('DE-FORBIDDEN-FLAG')) {
      showMsg('当前用户权限配置已变更，请刷新页面', '-changed-')
    }
    if (error?.response.status === 400) {
      return Promise.reject(error)
    }
    return Promise.resolve()
  }
)

const showMsg = (msg: string, id: string) => {
  if (window['cross-permission-' + id]) {
    return
  }
  window['cross-permission-' + id] = ElMessageBox.confirm(msg, {
    confirmButtonType: 'primary',
    type: 'warning',
    confirmButtonText: '刷新',
    cancelButtonText: '取消',
    autofocus: false,
    showClose: false
  })
    .then(() => {
      window['cross-permission-' + id]
      window.location.reload()
    })
    .catch(() => {
      window['cross-permission-' + id] = null
    })
}

const executeVersionHandler = (response: AxiosResponse) => {
  const key = 'x-de-execute-version'
  const executeVersion = response.headers[key]
  const cacheVal = wsCache.get(key)
  if (!cacheVal) {
    wsCache.set(key, executeVersion)
    return
  }
  if (executeVersion && executeVersion !== cacheVal) {
    wsCache.clear()
    wsCache.set(key, executeVersion)
    showMsg('系统有升级，请点击刷新页面', '-sys-upgrade-')
  }
}

const cancelRequestBatch = cancelKey => {
  if (cancelKey) {
    if (cancelKey.indexOf('/**') > -1) {
      const cancelKeyPre = cancelKey.split('/**')[0]
      Object.keys(cancelMap).forEach(key => {
        if (key.indexOf(cancelKeyPre) > -1) {
          cancelMap[key]?.(() => {
            console.warn('Operation canceled by the user,url:' + key)
          })
        }
      })
    } else {
      cancelMap[cancelKey]?.(() => {
        console.warn('Operation canceled by the user,url:' + cancelKey)
      })
    }
  }
}
export { service, cancelMap, cancelRequestBatch }
