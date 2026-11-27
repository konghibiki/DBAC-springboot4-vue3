<!--
  用户管理页面
  负责用户列表的展示、搜索联想、分页、批量操作入口与权限控制，
  通过组合 AddUserDialog / UpdateUserDialog / DeleteUserDialog 完成
  新增、修改、删除三个弹窗的调度，并处理各弹窗成功后的列表刷新。
-->
<script lang="ts" setup>
import {onMounted, ref} from 'vue'
import {ElAutocomplete, ElMessage, ElOption, ElPagination, ElSelect, ElTable, ElTableColumn,} from 'element-plus'
import {getUserPage, searchUserPage, type UserInfo} from '@/api/user'
import {useUserStore} from '@/store'
import {getRequestErrorMessage} from '@/utils/error'
import {formatDateTime} from '@/utils/format'
import {getAvatarText} from '@/utils/user'
import {useRoleList} from '@/composables/useRoleList'
import PageHeading from '@/components/common/PageHeading.vue'
import UserCell from '@/components/common/UserCell.vue'
import UserRoleTags from '@/components/common/UserRoleTags.vue'
import AddUserDialog from '@/components/user/AddUserDialog.vue'
import UpdateUserDialog from '@/components/user/UpdateUserDialog.vue'
import DeleteUserDialog from '@/components/user/DeleteUserDialog.vue'

interface UserSuggestion {
  value: string
  user: UserInfo
}

// ============ 列表与分页状态 ============
const users = ref<UserInfo[]>([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const selectedIds = ref<number[]>([])
const isLoading = ref(false)

// ============ 搜索状态 ============
const keyword = ref('')
const suggestions = ref<UserSuggestion[]>([])
const activeKeyword = ref('')
// 按角色筛选：roleName 为角色名称下拉选择，activeRoleName 为生效中的筛选
const { roles, loadRoles: loadRoleOptions } = useRoleList()
const roleName = ref('')
const activeRoleName = ref('')

// ============ 权限状态 ============
const userStore = useUserStore()
const canListUsers = ref(false)
const permissionChecked = ref(false)
const canAddUser = ref(false)
const canUpdateUser = ref(false)
const canDeleteUser = ref(false)
const currentUserId = ref<number | null>(null)

// ============ 弹窗开关 ============
const addDialogVisible = ref(false)
const updateDialogVisible = ref(false)
const updateTarget = ref<UserInfo | null>(null)
const deleteDialogVisible = ref(false)
const deleteTarget = ref<UserInfo | null>(null)

// ============ 加载用户列表 ============
// 根据当前激活的关键词与角色筛选走模糊查询或全量分页
async function loadUsers() {
  if (!canListUsers.value) return
  isLoading.value = true
  try {
    const response =
      activeKeyword.value || activeRoleName.value
        ? await searchUserPage(
            activeKeyword.value,
            pageNum.value,
            pageSize.value,
            activeRoleName.value,
          )
        : await getUserPage(pageNum.value, pageSize.value)
    const page = response.data.data
    if (!page) throw new Error('用户列表为空')
    users.value = page.records
    total.value = page.total
  } catch (error) {
    users.value = []
    total.value = 0
    ElMessage.error(getRequestErrorMessage(error, '获取用户列表失败'))
  } finally {
    isLoading.value = false
  }
}

// ============ 表格选择与分页 ============
function handleSelectionChange(rows: unknown[]) {
  selectedIds.value = rows.map((row) => (row as UserInfo).userId)
}

function handlePageSizeChange() {
  pageNum.value = 1
  selectedIds.value = []
  void loadUsers()
}

function handlePageChange() {
  selectedIds.value = []
  void loadUsers()
}

// ============ 搜索联想与执行 ============
async function fetchSuggestions(
  query: string,
  callback: (items: Array<{ value: string; user: UserInfo }>) => void,
) {
  const text = query.trim()
  if (!text) {
    suggestions.value = []
    callback([])
    return
  }

  try {
    const response = await searchUserPage(text, 1, 10, activeRoleName.value)
    const records = (response.data.data?.records ?? []).slice(0, 10)
    suggestions.value = records.map((user) => ({
      value: user.userName || user.email,
      user,
    }))
    callback(suggestions.value)
  } catch {
    suggestions.value = []
    callback([])
  }
}

function selectSuggestion(item: Record<string, unknown>) {
  const suggestion = item as unknown as UserSuggestion
  keyword.value = suggestion.user.userName || suggestion.user.email
  void executeSearch()
}

function executeSearch() {
  activeKeyword.value = keyword.value.trim()
  activeRoleName.value = roleName.value
  pageNum.value = 1
  selectedIds.value = []
  void loadUsers()
}

// 切换角色筛选：立即生效，回到第一页重新加载
function handleRoleFilterChange() {
  activeRoleName.value = roleName.value
  pageNum.value = 1
  selectedIds.value = []
  void loadUsers()
}

function clearSearch() {
  keyword.value = ''
  roleName.value = ''
  activeKeyword.value = ''
  activeRoleName.value = ''
  pageNum.value = 1
  selectedIds.value = []
  void loadUsers()
}

// ============ 弹窗调度 ============
function openAddDialog() {
  addDialogVisible.value = true
}

function openUpdateDialog(value: unknown) {
  updateTarget.value = value as UserInfo
  updateDialogVisible.value = true
}

function openDeleteDialog(value: unknown) {
  deleteTarget.value = value as UserInfo
  deleteDialogVisible.value = true
}

function getUserRow(value: unknown): UserInfo {
  return value as UserInfo
}

// 不允许删除自己
function canDeleteRow(value: unknown) {
  const user = value as UserInfo
  return canDeleteUser.value && user.userId !== currentUserId.value
}

// ============ 弹窗成功回调 ============
function handleAddSuccess() {
  pageNum.value = 1
  void loadUsers()
}

function handleUpdateSuccess() {
  void loadUsers()
}

function handleDeleteSuccess() {
  if (users.value.length === 1 && pageNum.value > 1) {
    pageNum.value -= 1
  }
  void loadUsers()
}

function showAction(message: string) {
  ElMessage.info(message)
}

// ============ 刷新用户列表 ============
function refreshUsers() {
  activeKeyword.value = ''
  activeRoleName.value = ''
  roleName.value = ''
  suggestions.value = []
  pageNum.value = 1
  void loadUsers()
}

// ============ 初始化 ============
onMounted(() => {
  void userStore
    .loadPermissions()
    .then(() => {
      canListUsers.value = userStore.can('user.list')
      canAddUser.value = userStore.can('user.add')
      canUpdateUser.value = userStore.can('user.update')
      canDeleteUser.value = userStore.can('user.delete')
      permissionChecked.value = true
      if (canListUsers.value) {
        void loadUsers()
        void loadRoleOptions()
      }
    })
    .catch(() => {
      canListUsers.value = false
      permissionChecked.value = true
      canAddUser.value = false
      canUpdateUser.value = false
      canDeleteUser.value = false
    })

  void userStore
    .loadUserInfo()
    .then((info) => {
      currentUserId.value = info?.userId ?? null
    })
    .catch(() => {
      currentUserId.value = null
    })
})
</script>

<template>
  <div class="user-management">
    <!-- 页面标题 -->
    <PageHeading
      badge="USER"
      description="管理系统中的用户账号、职能与基础资料。"
      eyebrow="SYSTEM MANAGEMENT"
      style="cursor: pointer"
      title="用户管理"
      @click="refreshUsers()"
    />

    <section v-if="canListUsers" class="user-card">
      <!-- 顶部工具栏：搜索与操作按钮 -->
      <div class="toolbar">
        <div class="search-group">
          <ElAutocomplete
            v-model="keyword"
            :fetch-suggestions="fetchSuggestions"
            :trigger-on-focus="false"
            clearable
            placeholder="搜索姓名、邮箱或用户 ID"
            popper-class="user-autocomplete"
            @clear="clearSearch"
            @select="selectSuggestion"
            @keyup.enter="executeSearch"
          >
            <template #prefix>
              <svg aria-hidden="true" fill="none" viewBox="0 0 20 20">
                <circle
                  cx="8.5"
                  cy="8.5"
                  r="5"
                  stroke="currentColor"
                  stroke-width="1.5"
                />
                <path
                  d="m12.4 12.4 4 4"
                  stroke="currentColor"
                  stroke-linecap="round"
                  stroke-width="1.5"
                />
              </svg>
            </template>
            <template #default="{ item }">
              <div class="suggestion-item">
                <span class="suggestion-item__avatar">
                  {{ getAvatarText(item.user) }}
                </span>
                <span class="suggestion-item__text">
                  <strong>{{ item.user.userName || '未设置昵称' }}</strong>
                  <small
                    >{{ item.user.email }} · ID {{ item.user.userId }}</small
                  >
                </span>
              </div>
            </template>
          </ElAutocomplete>
          <!-- 按角色筛选 -->
          <ElSelect
            v-model="roleName"
            class="role-filter"
            clearable
            placeholder="按角色筛选"
            @change="handleRoleFilterChange"
            @clear="handleRoleFilterChange"
          >
            <ElOption
              v-for="role in roles"
              :key="role.roleId"
              :label="role.roleName"
              :value="role.roleName"
            />
          </ElSelect>
          <button class="search-button" type="button" @click="executeSearch">
            查询
          </button>
        </div>
        <div class="toolbar__actions">
          <button
            :disabled="selectedIds.length === 0"
            class="action-button action-button--muted"
            type="button"
            @click="
              showAction(
                `已选择 ${selectedIds.length} 个用户（批量操作界面入口）`,
              )
            "
          >
            批量操作
          </button>
          <button
            v-if="canAddUser"
            class="action-button action-button--primary"
            type="button"
            @click="openAddDialog"
          >
            <svg aria-hidden="true" fill="none" viewBox="0 0 20 20">
              <path
                d="M10 4v12M4 10h12"
                stroke="currentColor"
                stroke-linecap="round"
                stroke-width="1.6"
              />
            </svg>
            新增用户
          </button>
        </div>
      </div>

      <!-- 用户列表 -->
      <ElTable
        v-loading="isLoading"
        :data="users"
        class="user-table"
        row-key="userId"
        @selection-change="handleSelectionChange"
      >
        <ElTableColumn type="selection" width="48" />
        <!-- 用户信息列 -->
        <ElTableColumn label="用户" min-width="220">
          <template #default="{ row }">
            <UserCell :user="getUserRow(row)" />
          </template>
        </ElTableColumn>
        <ElTableColumn label="邮箱" min-width="220">
          <template #default="{ row }">
            <span class="email-text">{{ row.email }}</span>
          </template>
        </ElTableColumn>
        <!-- 职能标签列 -->
        <ElTableColumn label="职能" min-width="180">
          <template #default="{ row }">
            <UserRoleTags :role="getUserRow(row).role" />
          </template>
        </ElTableColumn>
        <ElTableColumn label="性别" width="90">
          <template #default="{ row }">
            {{ row.gender || '保密' }}
          </template>
        </ElTableColumn>
        <ElTableColumn label="创建时间" min-width="170">
          <template #default="{ row }">
            <span class="time-text">{{ formatDateTime(row.createTime) }}</span>
          </template>
        </ElTableColumn>
        <!-- 操作列：编辑与删除 -->
        <ElTableColumn
          v-if="canUpdateUser || canDeleteUser"
          :width="canUpdateUser && canDeleteUser ? 132 : 84"
          fixed="right"
          label="操作"
        >
          <template #default="{ row }">
            <div class="row-actions">
              <button
                v-if="canUpdateUser"
                title="编辑用户"
                type="button"
                @click="openUpdateDialog(row)"
              >
                <svg aria-hidden="true" fill="none" viewBox="0 0 20 20">
                  <path
                    d="m12.8 3.5 3.7 3.7-8.7 8.7-4.3.6.6-4.3 8.7-8.7Z"
                    stroke="currentColor"
                    stroke-linejoin="round"
                    stroke-width="1.4"
                  />
                </svg>
              </button>
              <button
                v-if="canDeleteRow(row)"
                class="row-actions__delete"
                title="删除用户"
                type="button"
                @click="openDeleteDialog(row)"
              >
                <svg aria-hidden="true" fill="none" viewBox="0 0 20 20">
                  <path
                    d="M4 6h12M8 3.5h4M6.5 6l.7 10h5.6l.7-10M8.5 9v4.5M11.5 9v4.5"
                    stroke="currentColor"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    stroke-width="1.4"
                  />
                </svg>
              </button>
            </div>
          </template>
        </ElTableColumn>
      </ElTable>

      <!-- 分页栏 -->
      <footer class="pagination-bar">
        <p>
          共 <strong>{{ total }}</strong> 条用户
        </p>
        <div class="pagination-controls">
          <ElPagination
            v-model:current-page="pageNum"
            v-model:page-size="pageSize"
            :page-sizes="[10, 20, 30, 50]"
            :total="total"
            background
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handlePageSizeChange"
            @current-change="handlePageChange"
          />
        </div>
      </footer>
    </section>

    <!-- 新增用户弹窗 -->
    <AddUserDialog v-model="addDialogVisible" @success="handleAddSuccess" />

    <!-- 修改用户弹窗 -->
    <UpdateUserDialog
      v-model="updateDialogVisible"
      :user="updateTarget"
      @success="handleUpdateSuccess"
    />

    <!-- 删除用户弹窗 -->
    <DeleteUserDialog
      v-model="deleteDialogVisible"
      :target="deleteTarget"
      @success="handleDeleteSuccess"
    />
    <p
      v-if="permissionChecked && !canListUsers"
      class="user-card user-card--denied"
    >
      当前账号没有查看用户列表的权限。
    </p>
  </div>
</template>

<style lang="scss" scoped>
.user-management {
  width: min(1120px, calc(100% - 48px));
  margin: 0 auto;
  padding: 42px 0 64px;
}

.user-card {
  overflow: hidden;
  border: 1px solid #eceef1;
  border-radius: 16px;
  background: rgb(255 255 255 / 94%);
  box-shadow: 0 7px 22px rgb(34 42 57 / 3%);
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 18px;
  border-bottom: 1px solid #eef0f3;

  &__actions {
    display: flex;
    gap: 8px;
  }
}

.search-group {
  display: flex;
  width: min(600px, 100%);
  flex-wrap: wrap;
  gap: 8px;

  :deep(.el-autocomplete) {
    width: 100%;
    flex: 1 1 240px;
  }

  // 角色筛选下拉：高度、圆角、边框与搜索框一致，宽度略窄
  :deep(.role-filter) {
    width: 160px;
    flex: 0 1 160px;

    .el-select__wrapper {
      min-height: 38px;
      border-radius: 9px;
      box-shadow: 0 0 0 1px #e4e7ec inset;
      font-size: 12px;
    }

    .el-select__wrapper.is-hovering:not(.is-focused) {
      box-shadow: 0 0 0 1px #b3976f inset;
    }

    .el-select__wrapper.is-focused {
      box-shadow: 0 0 0 1px #b3976f inset;
    }

    .el-select__selected-item {
      color: #414958;
      font-size: 12px;
    }

    .el-select__placeholder {
      color: #adb2bb;
      font-size: 12px;
    }

    .el-select__caret {
      color: #a0a5af;
    }
  }

  :deep(.el-input__wrapper) {
    min-height: 38px;
    border-radius: 9px;
    box-shadow: 0 0 0 1px #e4e7ec inset;
  }

  :deep(.el-input__wrapper.is-focus) {
    box-shadow: 0 0 0 1px #b3976f inset;
  }

  :deep(.el-input__prefix-inner) {
    color: #a0a5af;
  }

  :deep(.el-input__prefix svg) {
    width: 17px;
    height: 17px;
  }

  :deep(.el-input__inner) {
    color: #414958;
    font-size: 12px;
  }

  :deep(.el-input__inner::placeholder) {
    color: #adb2bb;
  }
}

.search-button {
  height: 38px;
  flex: 0 0 auto;
  padding: 0 16px;
  border: 1px solid #293343;
  border-radius: 9px;
  background: #293343;
  color: #fff;
  cursor: pointer;
  font: inherit;
  font-size: 12px;
  font-weight: 600;
  transition:
    background-color 160ms ease,
    transform 160ms ease;

  &:hover {
    background: #39475b;
    transform: translateY(-1px);
  }
}

:global(.el-autocomplete-suggestion.user-autocomplete) {
  width: min(420px, calc(100vw - 32px)) !important;
  overflow: hidden;
  border: 1px solid #eceef1;
  border-radius: 12px;
  box-shadow: 0 12px 34px rgb(34 42 57 / 12%);
}

:global(.user-autocomplete .el-autocomplete-suggestion__list) {
  max-height: 280px;
  padding: 6px;
}

:global(.user-autocomplete .el-autocomplete-suggestion__wrap) {
  padding: 0;
}

.suggestion-item {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 7px 8px;

  &__avatar {
    display: grid;
    width: 32px;
    height: 32px;
    flex: 0 0 auto;
    place-items: center;
    border-radius: 8px;
    background: #f2eee5;
    color: #967c56;
    font-size: 12px;
    font-weight: 700;
  }

  &__text {
    display: flex;
    min-width: 0;
    flex-direction: column;
    gap: 3px;

    strong {
      color: #414958;
      font-size: 12px;
    }

    small {
      overflow: hidden;
      color: #969ca8;
      font-size: 10px;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }
}

.action-button {
  display: inline-flex;
  height: 38px;
  align-items: center;
  justify-content: center;
  gap: 7px;
  padding: 0 13px;
  border-radius: 9px;
  cursor: pointer;
  font: inherit;
  font-size: 12px;
  font-weight: 600;

  svg {
    width: 16px;
    height: 16px;
  }

  &--primary {
    border: 1px solid #9d845e;
    background: #9d845e;
    color: #fff;
  }

  &--muted {
    border: 1px solid #e3e5e9;
    background: #fff;
    color: #747c8a;

    &:disabled {
      cursor: not-allowed;
      opacity: 0.45;
    }
  }
}

:deep(.user-table) {
  --el-table-border-color: #eef0f3;
  --el-table-header-bg-color: #fafbfc;
  --el-table-row-hover-bg-color: #fbfaf7;
  font-size: 12px;

  th.el-table__cell {
    height: 48px;
    color: #7d8491;
    font-weight: 600;
  }

  td.el-table__cell {
    height: 68px;
  }
}

.email-text,
.time-text {
  color: #687080;
}

.row-actions {
  display: flex;
  gap: 6px;

  button {
    display: grid;
    width: 32px;
    height: 32px;
    place-items: center;
    border: 1px solid #e7e9ed;
    border-radius: 8px;
    background: #fff;
    color: #7d8491;
    cursor: pointer;

    svg {
      width: 15px;
      height: 15px;
    }
  }

  &__delete {
    color: #b46a6a !important;
  }
}

.pagination-bar {
  display: flex;
  min-height: 68px;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 18px;
  border-top: 1px solid #eef0f3;

  p {
    margin: 0;
    color: #969ca8;
    font-size: 12px;

    strong {
      color: #616978;
    }
  }
}

:deep(.el-pagination) {
  --el-pagination-button-bg-color: #fff;
  --el-pagination-hover-color: #987d56;
  --el-pagination-font-size: 12px;
  font-size: 12px;

  .el-pager li.is-active {
    background: #9d845e;
  }

  .el-pagination__total,
  .el-pagination__jump {
    color: #969ca8;
  }

  .el-select .el-input__wrapper {
    min-height: 32px;
    border-radius: 8px;
  }

  .el-pagination__editor.el-input {
    width: 46px;
  }

  .el-pagination__editor .el-input__wrapper {
    min-height: 32px;
    border-radius: 8px;
  }
}

@media (max-width: 760px) {
  .user-management {
    width: min(100% - 32px, 560px);
    padding-top: 27px;
  }

  .toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .search-group {
    width: 100%;
  }

  .toolbar__actions {
    justify-content: flex-end;
  }

  .pagination-bar {
    align-items: flex-start;
    flex-direction: column;
  }

  .pagination-controls {
    width: 100%;
    overflow-x: auto;
  }
}
</style>
