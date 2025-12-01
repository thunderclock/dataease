import request from '@/config/axios'

export interface AccessKeyVO {
  id?: number
  accessKey?: string
  accessSecret?: string // 只在生成时返回一次
  name?: string
  creator?: number
  userId?: number // 绑定的用户ID
  userName?: string // 绑定的用户名（用于显示）
  createTime?: number
  updateTime?: number
  expireTime?: number | null
  enable?: boolean
  lastUseTime?: number
}

export interface GenerateAccessKeyRequest {
  name: string
  userId: number // 绑定的用户ID（必填，用于权限控制）
  expireTime?: number | null // 过期时间戳，null 表示永不过期
}

// AccessKey 管理 API
export const accessKeyApi = {
  // 生成 AccessKey
  generate: (data: GenerateAccessKeyRequest) =>
    request.post<AccessKeyVO>({ url: '/accessKey/generate', data }),

  // 查询 AccessKey 列表
  list: () => request.get<AccessKeyVO[]>({ url: '/accessKey/list' }),

  // 禁用 AccessKey
  disable: (id: number) => request.post({ url: `/accessKey/disable/${id}` }),

  // 删除 AccessKey
  delete: (id: number) => request.delete({ url: `/accessKey/${id}` })
}
