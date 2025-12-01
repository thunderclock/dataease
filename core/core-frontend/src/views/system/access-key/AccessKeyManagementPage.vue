<template>
  <div class="access-key-management">
    <div class="page-header">
      <h2>{{ t('access_key_management.title') }}</h2>
      <div class="header-actions">
        <el-button type="primary" @click="showCreateDialog">
          <el-icon><Plus /></el-icon>
          {{ t('access_key_management.generate_key') }}
        </el-button>
        <el-button @click="refreshData">
          <el-icon><Refresh /></el-icon>
          {{ t('common.refresh') }}
        </el-button>
      </div>
    </div>

    <div class="table-container">
      <el-table :data="keyList" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" :label="t('access_key_management.name')" width="200" />
        <el-table-column :label="t('access_key_management.bound_user')" width="150">
          <template #default="{ row }">
            <span v-if="row.userName">{{ row.userName }}</span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column
          prop="accessKey"
          :label="t('access_key_management.access_key')"
          min-width="250"
        >
          <template #default="{ row }">
            <el-input :value="row.accessKey" readonly style="width: 100%">
              <template #append>
                <el-button @click="copyToClipboard(row.accessKey)" size="small">
                  <el-icon><DocumentCopy /></el-icon>
                </el-button>
              </template>
            </el-input>
          </template>
        </el-table-column>
        <el-table-column :label="t('access_key_management.status')" width="100">
          <template #default="{ row }">
            <el-tag :type="row.enable ? 'success' : 'danger'">
              {{
                row.enable
                  ? t('access_key_management.enabled')
                  : t('access_key_management.disabled')
              }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('access_key_management.expire_time')" width="180">
          <template #default="{ row }">
            <span v-if="row.expireTime">
              {{ formatTime(row.expireTime) }}
            </span>
            <span v-else>{{ t('access_key_management.never_expires') }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('access_key_management.create_time')" width="180">
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column :label="t('access_key_management.last_use_time')" width="180">
          <template #default="{ row }">
            <span v-if="row.lastUseTime">{{ formatTime(row.lastUseTime) }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('common.actions')" width="200" fixed="right">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button v-if="row.enable" type="warning" size="small" @click="disableKey(row)">
                {{ t('access_key_management.disable') }}
              </el-button>
              <el-button type="danger" size="small" @click="deleteKey(row)">
                {{ t('access_key_management.delete') }}
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 生成 AccessKey 对话框 -->
    <el-dialog
      v-model="createDialogVisible"
      :title="t('access_key_management.generate_key')"
      width="600px"
      @close="resetForm"
    >
      <el-form ref="keyFormRef" :model="keyForm" :rules="keyFormRules" label-width="120px">
        <el-form-item :label="t('access_key_management.name')" prop="name">
          <el-input
            v-model="keyForm.name"
            :placeholder="t('access_key_management.name_placeholder')"
          />
        </el-form-item>
        <el-form-item :label="t('access_key_management.bound_user')" prop="userId">
          <el-select
            v-model="keyForm.userId"
            :placeholder="t('access_key_management.select_user_placeholder')"
            filterable
            remote
            :remote-method="searchUsers"
            :loading="userSearchLoading"
            style="width: 100%"
            @focus="loadUserOptions"
          >
            <el-option
              v-for="user in userOptions"
              :key="user.id"
              :label="user.username || user.account"
              :value="user.id"
            >
              <span>{{ user.username || user.account }}</span>
              <span
                v-if="user.account && user.username"
                class="text-muted"
                style="margin-left: 10px; font-size: 12px"
              >
                ({{ user.account }})
              </span>
            </el-option>
          </el-select>
          <div class="form-tip">
            {{ t('access_key_management.bound_user_tip') }}
          </div>
        </el-form-item>
        <el-form-item :label="t('access_key_management.expire_time')" prop="expireTime">
          <el-radio-group v-model="expireType">
            <el-radio value="never">{{ t('access_key_management.never_expires') }}</el-radio>
            <el-radio value="custom">{{ t('access_key_management.custom_expire') }}</el-radio>
          </el-radio-group>
          <el-date-picker
            v-if="expireType === 'custom'"
            v-model="keyForm.expireTime"
            type="datetime"
            :placeholder="t('access_key_management.select_expire_time')"
            style="width: 100%; margin-top: 10px"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="x"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" @click="generateKey" :loading="generating">
          {{ t('common.confirm') }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 显示生成的 Key 和 Secret 对话框 -->
    <el-dialog
      v-model="resultDialogVisible"
      :title="t('access_key_management.key_generated')"
      width="700px"
    >
      <el-alert
        :title="t('access_key_management.secret_warning')"
        type="warning"
        :closable="false"
        show-icon
        style="margin-bottom: 20px"
      />
      <el-form label-width="120px">
        <el-form-item :label="t('access_key_management.access_key')">
          <el-input :value="generatedKey?.accessKey" readonly style="width: 100%">
            <template #append>
              <el-button @click="copyToClipboard(generatedKey?.accessKey)" size="small">
                <el-icon><DocumentCopy /></el-icon>
              </el-button>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item :label="t('access_key_management.access_secret')">
          <el-input
            :value="generatedKey?.accessSecret"
            readonly
            type="password"
            show-password
            style="width: 100%"
          >
            <template #append>
              <el-button @click="copyToClipboard(generatedKey?.accessSecret)" size="small">
                <el-icon><DocumentCopy /></el-icon>
              </el-button>
            </template>
          </el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="resultDialogVisible = false">
          {{ t('common.confirm') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script lang="ts" setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus-secondary'
import { Plus, Refresh, DocumentCopy } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import { accessKeyApi, type AccessKeyVO, type GenerateAccessKeyRequest } from '@/api/accessKey'
import { userApi } from '@/api/user'

const { t } = useI18n()

// 响应式数据
const loading = ref(false)
const keyList = ref<AccessKeyVO[]>([])
const generating = ref(false)
const userSearchLoading = ref(false)
const userOptions = ref<Array<{ id: number; username: string; account: string }>>([])

// 对话框相关
const createDialogVisible = ref(false)
const resultDialogVisible = ref(false)
const keyFormRef = ref<FormInstance>()
const expireType = ref<'never' | 'custom'>('never')
const generatedKey = ref<AccessKeyVO | null>(null)

// Key 表单
const keyForm = reactive<GenerateAccessKeyRequest>({
  name: '',
  userId: 0, // 初始值设为 0，表单验证会确保必须选择用户
  expireTime: null
})

// 表单验证规则
const keyFormRules = computed(() => ({
  name: [{ required: true, message: t('access_key_management.name_required'), trigger: 'blur' }],
  userId: [
    { required: true, message: t('access_key_management.user_required'), trigger: 'change' },
    {
      validator: (_rule: any, value: number, callback: any) => {
        if (!value || value === 0) {
          callback(new Error(t('access_key_management.user_required')))
        } else {
          callback()
        }
      },
      trigger: 'change'
    }
  ]
}))

// 方法
const loadKeys = async () => {
  try {
    loading.value = true
    const response = await accessKeyApi.list()
    keyList.value = response.data || []
  } catch (error: any) {
    console.error('加载 AccessKey 列表失败:', error)
    ElMessage.error(error.msg || t('access_key_management.load_failed'))
  } finally {
    loading.value = false
  }
}

const refreshData = () => {
  loadKeys()
}

const showCreateDialog = () => {
  createDialogVisible.value = true
  resetForm()
}

const resetForm = () => {
  keyForm.name = ''
  keyForm.userId = 0
  keyForm.expireTime = null
  expireType.value = 'never'
  userOptions.value = []
  keyFormRef.value?.clearValidate()
}

// 加载用户选项
const loadUserOptions = async () => {
  if (userOptions.value.length > 0) {
    return // 已经加载过
  }
  try {
    userSearchLoading.value = true
    const response = await userApi.pager(1, 100, { keyword: '' })
    if (response.data?.records) {
      userOptions.value = response.data.records.map((user: any) => ({
        id: user.id,
        username: user.username,
        account: user.account
      }))
    }
  } catch (error) {
    console.error('加载用户列表失败:', error)
  } finally {
    userSearchLoading.value = false
  }
}

// 搜索用户
const searchUsers = async (query: string) => {
  if (!query) {
    loadUserOptions()
    return
  }
  try {
    userSearchLoading.value = true
    const response = await userApi.pager(1, 50, { keyword: query })
    if (response.data?.records) {
      userOptions.value = response.data.records.map((user: any) => ({
        id: user.id,
        username: user.username,
        account: user.account
      }))
    }
  } catch (error) {
    console.error('搜索用户失败:', error)
  } finally {
    userSearchLoading.value = false
  }
}

const generateKey = async () => {
  if (!keyFormRef.value) return

  try {
    await keyFormRef.value.validate()
    generating.value = true

    if (!keyForm.userId || keyForm.userId === 0) {
      ElMessage.error(t('access_key_management.user_required'))
      return
    }

    const requestData: GenerateAccessKeyRequest = {
      name: keyForm.name,
      userId: keyForm.userId,
      expireTime: expireType.value === 'never' ? null : keyForm.expireTime || undefined
    }

    const response = await accessKeyApi.generate(requestData)
    generatedKey.value = response.data || null
    createDialogVisible.value = false
    resultDialogVisible.value = true

    ElMessage.success(t('access_key_management.generate_success'))
    loadKeys()
  } catch (error: any) {
    if (error.message) {
      // 表单验证错误
      return
    }
    console.error('生成 AccessKey 失败:', error)
    ElMessage.error(error.msg || t('access_key_management.generate_failed'))
  } finally {
    generating.value = false
  }
}

const disableKey = async (key: AccessKeyVO) => {
  try {
    await ElMessageBox.confirm(
      t('access_key_management.disable_confirm', { name: key.name }),
      t('common.warning'),
      {
        confirmButtonText: t('common.confirm'),
        cancelButtonText: t('common.cancel'),
        type: 'warning'
      }
    )

    if (!key.id) return
    await accessKeyApi.disable(key.id)
    ElMessage.success(t('access_key_management.disable_success'))
    loadKeys()
  } catch (error: any) {
    if (error === 'cancel') {
      return
    }
    console.error('禁用 AccessKey 失败:', error)
    ElMessage.error(error.msg || t('access_key_management.disable_failed'))
  }
}

const deleteKey = async (key: AccessKeyVO) => {
  try {
    await ElMessageBox.confirm(
      t('access_key_management.delete_confirm', { name: key.name }),
      t('common.warning'),
      {
        confirmButtonText: t('common.confirm'),
        cancelButtonText: t('common.cancel'),
        type: 'warning'
      }
    )

    if (!key.id) return
    await accessKeyApi.delete(key.id)
    ElMessage.success(t('access_key_management.delete_success'))
    loadKeys()
  } catch (error: any) {
    if (error === 'cancel') {
      return
    }
    console.error('删除 AccessKey 失败:', error)
    ElMessage.error(error.msg || t('access_key_management.delete_failed'))
  }
}

const copyToClipboard = async (text?: string) => {
  if (!text) return
  try {
    // 检查是否支持 Clipboard API
    if (navigator.clipboard && navigator.clipboard.writeText) {
      await navigator.clipboard.writeText(text)
      ElMessage.success(t('access_key_management.copy_success'))
    } else {
      // 降级方案：使用 document.execCommand
      const textarea = document.createElement('textarea')
      textarea.style.position = 'fixed'
      textarea.style.opacity = '0'
      textarea.style.left = '-999999px'
      textarea.value = text
      document.body.appendChild(textarea)
      textarea.select()
      document.execCommand('copy')
      document.body.removeChild(textarea)
      ElMessage.success(t('access_key_management.copy_success'))
    }
  } catch (error) {
    console.error('复制失败:', error)
    ElMessage.error(t('access_key_management.copy_failed'))
  }
}

const formatTime = (timestamp?: number) => {
  if (!timestamp) return '-'
  return new Date(timestamp).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

onMounted(() => {
  loadKeys()
})
</script>

<style lang="less" scoped>
.access-key-management {
  padding: 20px;

  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;

    h2 {
      margin: 0;
      font-size: 20px;
      font-weight: 500;
    }

    .header-actions {
      display: flex;
      gap: 10px;
    }
  }

  .table-container {
    margin-bottom: 20px;
  }

  .action-buttons {
    display: flex;
    gap: 8px;
  }

  .text-muted {
    color: #909399;
  }

  .form-tip {
    font-size: 12px;
    color: #909399;
    margin-top: 4px;
  }
}
</style>
