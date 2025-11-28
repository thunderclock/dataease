import request from '@/config/axios'

export interface AccessTokenVO {
  id?: number
  accessToken?: string
  accessSecret?: string // 只在生成时返回一次
  name?: string
  creator?: number
  createTime?: number
  updateTime?: number
  expireTime?: number | null
  enable?: boolean
  lastUseTime?: number
}

export interface GenerateAccessTokenRequest {
  name: string
  expireTime?: number | null // 过期时间戳，null 表示永不过期
}

// AccessToken 管理 API
export const accessTokenApi = {
  // 生成 AccessToken
  generate: (data: GenerateAccessTokenRequest) =>
    request.post<AccessTokenVO>({ url: '/accessToken/generate', data }),

  // 查询 AccessToken 列表
  list: () => request.get<AccessTokenVO[]>({ url: '/accessToken/list' }),

  // 禁用 AccessToken
  disable: (id: number) => request.post({ url: `/accessToken/disable/${id}` }),

  // 删除 AccessToken
  delete: (id: number) => request.delete({ url: `/accessToken/${id}` })
}
