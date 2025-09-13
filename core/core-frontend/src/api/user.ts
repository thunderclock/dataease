import request from '@/config/axios'

export const mountedOrg = (keyword?: string) =>
  request.post({ url: '/org/mounted', data: { keyword } })

export const switchOrg = (id: number | string) => request.post({ url: `/user/switch/${id}` })

export const userInfo = () => request.get({ url: '/user/info' })

export const searchRoleApi = (keyword: string) =>
  request.post({ url: '/role/query', data: { keyword } })

export const userOptionForRoleApi = data => request.post({ url: '/user/role/option', data })

export const userSelectedForRoleApi = (page: number, limit: number, data) =>
  request.post({ url: `/user/role/selected/${page}/${limit}`, data })

export const userPageApi = (page: number, limit: number, data) =>
  request.post({ url: `/user/pager/${page}/${limit}`, data })

export const personSysVariableInfoApi = uid =>
  request.get({ url: `/user/personSysVariableInfo/${uid}` })

export const userCreateApi = data => request.post({ url: '/user/create', data })

export const userEditApi = data => request.post({ url: '/user/edit', data })

export const personEditApi = data => request.post({ url: '/user/personEdit', data })

export const roleOptionForUserApi = data => request.post({ url: '/role/user/option', data })

export const userDelApi = uid => request.post({ url: `/user/delete/${uid}` })

export const queryFormApi = uid => request.get({ url: `/user/queryById/${uid}` })

export const personInfoApi = () => request.get({ url: `/user/personInfo` })

export const ipInfoApi = () => request.get({ url: `/user/ipInfo` })

export const roleCreateApi = data => request.post({ url: '/role/create', data })

export const roleEditApi = data => request.post({ url: '/role/edit', data })

export const roleDetailApi = rid => request.get({ url: `/role/detail/${rid}` })

export const roleDelApi = rid => request.post({ url: `/role/delete/${rid}` })

export const beforeUnmountInfoApi = data => request.post({ url: '/role/beforeUnmountInfo', data })

export const unMountUserApi = data => request.post({ url: '/role/unMountUser', data })

export const mountUserApi = data => request.post({ url: '/role/mountUser', data })

export const searchExternalUserApi = keyword =>
  request.get({ url: '/role/searchExternalUser/' + keyword })

export const mountExternalUserApi = data => request.post({ url: '/role/mountExternalUser', data })

export const switchLangApi = data => request.post({ url: '/user/switchLanguage', data })

export const downExcelTemplateApi = () =>
  request.post({ url: '/user/excelTemplate', responseType: 'blob' })

export const importUserApi = data =>
  request.post({
    url: '/user/batchImport',
    headersType: 'multipart/form-data',
    data
  })

export const downErrorRecordApi = (key: string) =>
  request.get({ url: `/user/errorRecord/${key}`, responseType: 'blob' })

export const clearErrorApi = (key: string) => {
  request.get({ url: `/user/clearErrorRecord/${key}` })
}

export const batchDelApi = data => request.post({ url: '/user/batchDel', data })

export const defaultPwdApi = () => request.get({ url: '/user/defaultPwd' })

export const resetPwdApi = uid => request.post({ url: `/user/resetPwd/${uid}` })

export const switchEnableApi = data => request.post({ url: '/user/enable', data })

// 用户管理相关API - 增强版本
export const userApi = {
  // 分页查询用户
  pager: (page: number, size: number, data: any = {}) =>
    request.post({ url: `/user/pager/${page}/${size}`, data }),

  // 创建用户
  create: (data: any) => request.post({ url: '/user/create', data }),

  // 编辑用户
  edit: (data: any) => request.post({ url: '/user/edit', data }),

  // 删除用户
  delete: (id: number) => request.post({ url: `/user/delete/${id}` }),

  // 切换用户状态
  enable: (data: any) => request.post({ url: '/user/enable', data }),

  // 获取角色选项
  getRoleOptions: () => request.post({ url: '/role/query', data: {} }),

  // 获取用户详情
  getById: (id: number) => request.get({ url: `/user/queryById/${id}` }),

  // 批量删除
  batchDelete: (ids: number[]) => request.post({ url: '/user/batchDel', data: ids }),

  // 重置密码
  resetPassword: (id: number) => request.post({ url: `/user/resetPwd/${id}` }),

  // 获取默认密码
  getDefaultPassword: () => request.get({ url: '/user/defaultPwd' })
}

// 角色管理相关API
export const roleApi = {
  // 查询角色列表
  query: (data: any = {}) => request.post({ url: '/role/query', data }),

  // 创建角色
  create: (data: any) => request.post({ url: '/role/create', data }),

  // 编辑角色
  edit: (data: any) => request.post({ url: '/role/edit', data }),

  // 删除角色
  delete: (id: number) => request.post({ url: `/role/delete/${id}` }),

  // 获取角色详情
  detail: (id: number) => request.get({ url: `/role/detail/${id}` }),

  // 获取角色选项
  getOptions: () => request.post({ url: '/role/query', data: {} })
}
