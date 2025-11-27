import CryptoJS from 'crypto-js/crypto-js'
import JSEncrypt from 'jsencrypt/bin/jsencrypt.min'
import { Base64 } from 'js-base64'
import { useCache } from '@/hooks/web/useCache'
import { useAppStoreWithOut } from '@/store/modules/app'

const appStore = useAppStoreWithOut()

const { wsCache } = useCache()

const rsaKey = '-pk_separator-'
const crypt = new JSEncrypt()

const aesDecrypt = (word, keyStr) => {
  const keyHex = CryptoJS.enc.Utf8.parse(keyStr) //
  const ivHex = CryptoJS.enc.Utf8.parse('0000000000000000')
  const decrypt = CryptoJS.AES.decrypt(word, keyHex, {
    iv: ivHex,
    mode: CryptoJS.mode.CBC,
    padding: CryptoJS.pad.Pkcs7
  })
  return decrypt.toString(CryptoJS.enc.Utf8)
}

/**
 * 将Base64 DER格式的公钥转换为PEM格式
 * JSEncrypt需要PEM格式的公钥（带BEGIN/END标记）
 * @param derKey Base64编码的DER格式公钥
 * @returns PEM格式的公钥字符串
 */
const convertDerToPem = (derKey: string): string => {
  if (!derKey) {
    throw new Error('Public key is empty')
  }

  // 如果已经是PEM格式，直接返回
  if (derKey.includes('BEGIN PUBLIC KEY') || derKey.includes('BEGIN RSA PUBLIC KEY')) {
    return derKey
  }

  // 移除所有空白字符和换行符
  const cleanKey = derKey.replace(/[\s\n\r\t]/g, '')

  // 验证是否为有效的Base64字符串
  if (!/^[A-Za-z0-9+/=]+$/.test(cleanKey)) {
    throw new Error('Invalid Base64 public key format')
  }

  // 每64个字符换行
  const chunks: string[] = []
  for (let i = 0; i < cleanKey.length; i += 64) {
    chunks.push(cleanKey.substring(i, i + 64))
  }

  // 组装PEM格式（JSEncrypt标准格式）
  return `-----BEGIN PUBLIC KEY-----\n${chunks.join('\n')}\n-----END PUBLIC KEY-----`
}

export const rsaEncryp = word => {
  try {
    const separator = Base64.encodeURI(rsaKey) + '='
    const dekey = wsCache.get(appStore.getDekey)
    if (!dekey) {
      console.error('RSA public key not found in cache')
      return null
    }

    const keyArray = dekey.split(separator)
    if (keyArray.length !== 2) {
      console.error('Invalid RSA key format')
      return null
    }

    const k1 = keyArray[0]
    const k2 = keyArray[1]
    const pkDer = aesDecrypt(k1, k2)

    // 将DER格式转换为PEM格式
    const pkPem = convertDerToPem(pkDer)

    crypt.setKey(pkPem)
    const encrypted = crypt.encrypt(word)

    if (!encrypted) {
      console.error('RSA encryption failed')
      return null
    }

    return encrypted
  } catch (error) {
    console.error('RSA encryption error:', error)
    return null
  }
}

export const symmetricDecrypt = (data, keyStr) => {
  const iv = CryptoJS.enc.Utf8.parse('0000000000000000')
  const key = CryptoJS.enc.Base64.parse(keyStr)
  const decodedCiphertext = CryptoJS.enc.Base64.parse(data)
  const decrypted = CryptoJS.AES.decrypt({ ciphertext: decodedCiphertext }, key, {
    iv: iv,
    mode: CryptoJS.mode.CBC,
    padding: CryptoJS.pad.Pkcs7
  })
  return decrypted.toString(CryptoJS.enc.Utf8)
}
