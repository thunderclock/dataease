<template>
  <div class="access-token-management">
    <div class="page-header">
      <h2>{{ t('access_token_management.title') }}</h2>
      <div class="header-actions">
        <el-button type="primary" @click="showCreateDialog">
          <el-icon><Plus /></el-icon>
          {{ t('access_token_management.generate_token') }}
        </el-button>
        <el-button @click="refreshData">
          <el-icon><Refresh /></el-icon>
          {{ t('common.refresh') }}
        </el-button>
      </div>
    </div>

    <div class="table-container">
      <el-table :data="tokenList" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" :label="t('access_token_management.name')" width="200" />
        <el-table-column
          prop="accessToken"
          :label="t('access_token_management.access_token')"
          min-width="250"
        >
          <template #default="{ row }">
            <el-input :value="row.accessToken" readonly style="width: 100%">
              <template #append>
                <el-button @click="copyToClipboard(row.accessToken)" size="small">
                  <el-icon><DocumentCopy /></el-icon>
                </el-button>
              </template>
            </el-input>
          </template>
        </el-table-column>
        <el-table-column :label="t('access_token_management.status')" width="100">
          <template #default="{ row }">
            <el-tag :type="row.enable ? 'success' : 'danger'">
              {{
                row.enable
                  ? t('access_token_management.enabled')
                  : t('access_token_management.disabled')
              }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('access_token_management.expire_time')" width="180">
          <template #default="{ row }">
            <span v-if="row.expireTime">
              {{ formatTime(row.expireTime) }}
            </span>
            <span v-else>{{ t('access_token_management.never_expires') }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('access_token_management.create_time')" width="180">
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column :label="t('access_token_management.last_use_time')" width="180">
          <template #default="{ row }">
            <span v-if="row.lastUseTime">{{ formatTime(row.lastUseTime) }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('common.actions')" width="200" fixed="right">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button v-if="row.enable" type="warning" size="small" @click="disableToken(row)">
                {{ t('access_token_management.disable') }}
              </el-button>
              <el-button type="danger" size="small" @click="deleteToken(row)">
                {{ t('access_token_management.delete') }}
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 生成 AccessToken 对话框 -->
    <el-dialog
      v-model="createDialogVisible"
      :title="t('access_token_management.generate_token')"
      width="600px"
      @close="resetForm"
    >
      <el-form ref="tokenFormRef" :model="tokenForm" :rules="tokenFormRules" label-width="120px">
        <el-form-item :label="t('access_token_management.name')" prop="name">
          <el-input
            v-model="tokenForm.name"
            :placeholder="t('access_token_management.name_placeholder')"
          />
        </el-form-item>
        <el-form-item :label="t('access_token_management.expire_time')" prop="expireTime">
          <el-radio-group v-model="expireType">
            <el-radio value="never">{{ t('access_token_management.never_expires') }}</el-radio>
            <el-radio value="custom">{{ t('access_token_management.custom_expire') }}</el-radio>
          </el-radio-group>
          <el-date-picker
            v-if="expireType === 'custom'"
            v-model="tokenForm.expireTime"
            type="datetime"
            :placeholder="t('access_token_management.select_expire_time')"
            style="width: 100%; margin-top: 10px"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="x"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" @click="generateToken" :loading="generating">
          {{ t('common.confirm') }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 显示生成的 Token 和 Secret 对话框 -->
    <el-dialog
      v-model="resultDialogVisible"
      :title="t('access_token_management.token_generated')"
      width="700px"
    >
      <el-alert
        :title="t('access_token_management.secret_warning')"
        type="warning"
        :closable="false"
        show-icon
        style="margin-bottom: 20px"
      />
      <el-form label-width="120px">
        <el-form-item :label="t('access_token_management.access_token')">
          <el-input :value="generatedToken?.accessToken" readonly style="width: 100%">
            <template #append>
              <el-button @click="copyToClipboard(generatedToken?.accessToken)" size="small">
                <el-icon><DocumentCopy /></el-icon>
              </el-button>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item :label="t('access_token_management.access_secret')">
          <el-input
            :value="generatedToken?.accessSecret"
            readonly
            type="password"
            show-password
            style="width: 100%"
          >
            <template #append>
              <el-button @click="copyToClipboard(generatedToken?.accessSecret)" size="small">
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
import {
  accessTokenApi,
  type AccessTokenVO,
  type GenerateAccessTokenRequest
} from '@/api/accessToken'

const { t } = useI18n()

// 响应式数据
const loading = ref(false)
const tokenList = ref<AccessTokenVO[]>([])
const generating = ref(false)

// 对话框相关
const createDialogVisible = ref(false)
const resultDialogVisible = ref(false)
const tokenFormRef = ref<FormInstance>()
const expireType = ref<'never' | 'custom'>('never')
const generatedToken = ref<AccessTokenVO | null>(null)

// Token 表单
const tokenForm = reactive<GenerateAccessTokenRequest>({
  name: '',
  expireTime: null
})

// 表单验证规则
const tokenFormRules = computed(() => ({
  name: [{ required: true, message: t('access_token_management.name_required'), trigger: 'blur' }]
}))

// 方法
const loadTokens = async () => {
  try {
    loading.value = true
    const response = await accessTokenApi.list()
    tokenList.value = response.data || []
  } catch (error: any) {
    console.error('加载 AccessToken 列表失败:', error)
    ElMessage.error(error.msg || t('access_token_management.load_failed'))
  } finally {
    loading.value = false
  }
}

const refreshData = () => {
  loadTokens()
}

const showCreateDialog = () => {
  createDialogVisible.value = true
  resetForm()
}

const resetForm = () => {
  tokenForm.name = ''
  tokenForm.expireTime = null
  expireType.value = 'never'
  tokenFormRef.value?.clearValidate()
}

const generateToken = async () => {
  if (!tokenFormRef.value) return

  try {
    await tokenFormRef.value.validate()
    generating.value = true

    const requestData: GenerateAccessTokenRequest = {
      name: tokenForm.name,
      expireTime: expireType.value === 'never' ? null : tokenForm.expireTime || undefined
    }

    const response = await accessTokenApi.generate(requestData)
    generatedToken.value = response.data || null
    createDialogVisible.value = false
    resultDialogVisible.value = true

    ElMessage.success(t('access_token_management.generate_success'))
    loadTokens()
  } catch (error: any) {
    if (error.message) {
      // 表单验证错误
      return
    }
    console.error('生成 AccessToken 失败:', error)
    ElMessage.error(error.msg || t('access_token_management.generate_failed'))
  } finally {
    generating.value = false
  }
}

const disableToken = async (token: AccessTokenVO) => {
  try {
    await ElMessageBox.confirm(
      t('access_token_management.disable_confirm', { name: token.name }),
      t('common.warning'),
      {
        confirmButtonText: t('common.confirm'),
        cancelButtonText: t('common.cancel'),
        type: 'warning'
      }
    )

    if (!token.id) return
    await accessTokenApi.disable(token.id)
    ElMessage.success(t('access_token_management.disable_success'))
    loadTokens()
  } catch (error: any) {
    if (error === 'cancel') {
      return
    }
    console.error('禁用 AccessToken 失败:', error)
    ElMessage.error(error.msg || t('access_token_management.disable_failed'))
  }
}

const deleteToken = async (token: AccessTokenVO) => {
  try {
    await ElMessageBox.confirm(
      t('access_token_management.delete_confirm', { name: token.name }),
      t('common.warning'),
      {
        confirmButtonText: t('common.confirm'),
        cancelButtonText: t('common.cancel'),
        type: 'warning'
      }
    )

    if (!token.id) return
    await accessTokenApi.delete(token.id)
    ElMessage.success(t('access_token_management.delete_success'))
    loadTokens()
  } catch (error: any) {
    if (error === 'cancel') {
      return
    }
    console.error('删除 AccessToken 失败:', error)
    ElMessage.error(error.msg || t('access_token_management.delete_failed'))
  }
}

const copyToClipboard = async (text?: string) => {
  if (!text) return
  try {
    // 检查是否支持 Clipboard API
    if (navigator.clipboard && navigator.clipboard.writeText) {
      await navigator.clipboard.writeText(text)
      ElMessage.success(t('access_token_management.copy_success'))
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
      ElMessage.success(t('access_token_management.copy_success'))
    }
  } catch (error) {
    console.error('复制失败:', error)
    ElMessage.error(t('access_token_management.copy_failed'))
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
  loadTokens()
})
</script>

<style lang="less" scoped>
.access-token-management {
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
}
</style>
