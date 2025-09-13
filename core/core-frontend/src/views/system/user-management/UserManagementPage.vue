<template>
  <div class="user-management">
    <div class="page-header">
      <h2>{{ t('user_management.title') }}</h2>
      <div class="header-actions">
        <el-button type="primary" @click="showCreateDialog">
          <el-icon><Plus /></el-icon>
          {{ t('user_management.create_user') }}
        </el-button>
        <el-button @click="refreshData">
          <el-icon><Refresh /></el-icon>
          {{ t('common.refresh') }}
        </el-button>
      </div>
    </div>

    <div class="search-bar">
      <el-input
        v-model="searchKeyword"
        :placeholder="t('user_management.search_placeholder')"
        clearable
        @input="handleSearch"
        style="width: 300px"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
    </div>

    <div class="table-container">
      <el-table :data="userList" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="account" :label="t('user_management.account')" width="150" />
        <el-table-column prop="name" :label="t('user_management.name')" width="150" />
        <el-table-column prop="email" :label="t('user_management.email')" width="200" />
        <el-table-column prop="phone" :label="t('user_management.phone')" width="150" />
        <el-table-column :label="t('user_management.status')" width="100">
          <template #default="{ row }">
            <el-tag :type="row.enable ? 'success' : 'danger'">
              {{ row.enable ? t('user_management.enabled') : t('user_management.disabled') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('user_management.roles')" width="200">
          <template #default="{ row }">
            <el-tag v-for="role in row.roles" :key="role.id" size="small" style="margin-right: 5px">
              {{ role.name }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('common.actions')" width="240">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button type="primary" size="small" @click="editUser(row)">
                {{ t('user_management.edit') }}
              </el-button>
              <el-button type="danger" size="small" @click="deleteUser(row)">
                {{ t('user_management.delete') }}
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 分页 -->
    <div class="pagination-container">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>

    <!-- 创建/编辑用户对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? t('user_management.edit_user') : t('user_management.create_user')"
      width="600px"
      @close="resetForm"
    >
      <el-form ref="userFormRef" :model="userForm" :rules="userFormRules" label-width="100px">
        <el-form-item :label="t('user_management.name')" prop="name">
          <el-input v-model="userForm.name" :placeholder="t('user_management.name_placeholder')" />
        </el-form-item>
        <el-form-item :label="t('user_management.account')" prop="account">
          <el-input
            v-model="userForm.account"
            :placeholder="t('user_management.account_placeholder')"
            :disabled="isEdit"
          />
        </el-form-item>
        <el-form-item :label="t('user_management.email')" prop="email">
          <el-input
            v-model="userForm.email"
            :placeholder="t('user_management.email_placeholder')"
          />
        </el-form-item>
        <el-form-item :label="t('user_management.phone')" prop="phone">
          <el-input
            v-model="userForm.phone"
            :placeholder="t('user_management.phone_placeholder')"
          />
        </el-form-item>
        <el-form-item :label="t('user_management.password')" prop="password">
          <el-input
            v-model="userForm.password"
            type="password"
            :placeholder="
              isEdit
                ? t('user_management.password_edit_placeholder')
                : t('user_management.password_placeholder')
            "
            show-password
          />
          <div v-if="isEdit" class="password-tip">
            {{ t('user_management.password_edit_tip') }}
          </div>
        </el-form-item>
        <el-form-item :label="t('user_management.roles')" prop="roleIds">
          <el-select
            v-model="userForm.roleIds"
            multiple
            :placeholder="t('user_management.roles_placeholder')"
            style="width: 100%"
          >
            <el-option
              v-for="role in roleList"
              :key="role.id"
              :label="role.name"
              :value="role.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('user_management.status')" prop="enable">
          <el-switch
            v-model="userForm.enable"
            :active-text="t('user_management.enabled')"
            :inactive-text="t('user_management.disabled')"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">{{ t('common.cancel') }}</el-button>
          <el-button type="primary" @click="submitForm">{{ t('common.confirm') }}</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus-secondary'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import { userApi, roleApi } from '@/api/user'

const { t } = useI18n()

// 响应式数据
const loading = ref(false)
const userList = ref([])
const roleList = ref([])
const searchKeyword = ref('')
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)

// 对话框相关
const dialogVisible = ref(false)
const isEdit = ref(false)
const userFormRef = ref<FormInstance>()

// 用户表单
const userForm = reactive({
  id: null,
  name: '',
  account: '',
  email: '',
  phone: '',
  password: '',
  roleIds: [],
  enable: true
})

// 表单验证规则
const userFormRules = computed(() => ({
  name: [{ required: true, message: t('user_management.name_required'), trigger: 'blur' }],
  account: [{ required: true, message: t('user_management.account_required'), trigger: 'blur' }],
  email: [{ type: 'email', message: t('user_management.email_invalid'), trigger: 'blur' }],
  password: [
    {
      required: !isEdit.value,
      message: t('user_management.password_required'),
      trigger: 'blur'
    },
    {
      min: 6,
      message: t('user_management.password_min_length'),
      trigger: 'blur'
    }
  ],
  roleIds: [{ required: true, message: t('user_management.roles_required'), trigger: 'change' }]
}))

// 方法
const loadUsers = async () => {
  try {
    loading.value = true
    const requestData = {
      keyword: searchKeyword.value || '',
      statusList: null,
      originList: null,
      roleIdList: null,
      timeDesc: true
    }
    const response = await userApi.pager(currentPage.value, pageSize.value, requestData)
    userList.value = response.data?.records || []
    total.value = response.data?.total || 0
  } catch (error) {
    console.error('加载用户列表失败:', error)
    ElMessage.error(t('user_management.load_failed'))
  } finally {
    loading.value = false
  }
}

const loadRoles = async () => {
  try {
    const response = await roleApi.query({})
    roleList.value = response.data || []
  } catch (error) {
    console.error('加载角色列表失败:', error)
    ElMessage.error(t('user_management.load_roles_failed'))
  }
}

const refreshData = () => {
  loadUsers()
  loadRoles()
}

const handleSearch = () => {
  // 搜索逻辑已在计算属性中处理
}

const handleSizeChange = (val: number) => {
  pageSize.value = val
  currentPage.value = 1
  loadUsers()
}

const handleCurrentChange = (val: number) => {
  currentPage.value = val
  loadUsers()
}

const showCreateDialog = () => {
  isEdit.value = false
  dialogVisible.value = true
  resetForm()
}

const editUser = (user: any) => {
  isEdit.value = true
  dialogVisible.value = true

  // 填充表单数据
  userForm.id = user.id
  userForm.name = user.name || ''
  userForm.account = user.account || ''
  userForm.email = user.email || ''
  userForm.phone = user.phone || ''
  userForm.roleIds = user.roleItems?.map((role: any) => role.id) || []
  userForm.enable = user.enable
  userForm.password = ''
}

const deleteUser = async (user: any) => {
  try {
    await ElMessageBox.confirm(
      t('user_management.delete_confirm', { name: user.name || user.account }),
      t('common.confirm'),
      {
        confirmButtonText: t('common.confirm'),
        cancelButtonText: t('common.cancel'),
        type: 'warning'
      }
    )

    await userApi.delete(user.id)
    ElMessage.success(t('user_management.delete_success'))
    loadUsers()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除用户失败:', error)
      ElMessage.error(t('user_management.delete_failed'))
    }
  }
}

const submitForm = async () => {
  if (!userFormRef.value) return

  try {
    await userFormRef.value.validate()

    // 准备提交数据
    const submitData = { ...userForm }

    // 如果是编辑模式且密码为空，则不包含密码字段
    if (isEdit.value && !submitData.password) {
      delete submitData.password
    }

    if (isEdit.value) {
      await userApi.edit(submitData)
      ElMessage.success(t('user_management.edit_success'))
    } else {
      await userApi.create(submitData)
      ElMessage.success(t('user_management.create_success'))
    }

    dialogVisible.value = false
    loadUsers()
  } catch (error) {
    console.error('提交表单失败:', error)
    ElMessage.error(t('user_management.submit_failed'))
  }
}

const resetForm = () => {
  userForm.id = null
  userForm.name = ''
  userForm.account = ''
  userForm.email = ''
  userForm.phone = ''
  userForm.password = ''
  userForm.roleIds = []
  userForm.enable = true

  if (userFormRef.value) {
    userFormRef.value.resetFields()
  }
}

// 生命周期
onMounted(() => {
  loadUsers()
  loadRoles()
})
</script>

<style lang="less" scoped>
.user-management {
  padding: 20px;
  background: #fff;
  border-radius: 4px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);

  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    padding-bottom: 15px;
    border-bottom: 1px solid #e4e7ed;

    h2 {
      margin: 0;
      color: #303133;
      font-size: 20px;
      font-weight: 500;
    }

    .header-actions {
      display: flex;
      gap: 10px;
    }
  }

  .search-bar {
    margin-bottom: 20px;
  }

  .table-container {
    margin-bottom: 20px;
  }

  .pagination-container {
    display: flex;
    justify-content: center;
    margin-top: 20px;
  }

  .action-buttons {
    display: flex;
    gap: 8px;
    flex-wrap: wrap;
    align-items: center;

    .el-button {
      min-width: 60px;
      margin: 0;
    }
  }
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.password-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
  line-height: 1.4;
}
</style>
