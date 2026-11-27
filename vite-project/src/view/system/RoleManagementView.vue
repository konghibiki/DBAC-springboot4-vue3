<!--
  角色管理页面
  负责角色列表的分页展示与搜索，展示角色 ID、角色名称与权限等级，
  搜索支持按角色名称或角色 ID 模糊查询，并带有弹出框自动补全联想，
  提供新增、修改、删除等操作入口：修改功能已接入后端（角色名称与职权分配），
  且仅拥有 role.update 职权、且角色等级为 2 时才可修改，
  视觉风格与用户管理页面保持一致。
-->
<script lang="ts" setup>
import {onMounted, onUnmounted, ref} from 'vue'
import {ElAutocomplete, ElMessage, ElPagination, ElTable, ElTableColumn,} from 'element-plus'
import {getRolePage, type RoleInfo, searchRolePage} from '@/api/user'
import {useUserStore} from '@/store'
import {getRequestErrorMessage} from '@/utils/error'
import PageHeading from '@/components/common/PageHeading.vue'
import UpdateRoleDialog from '@/components/role/UpdateRoleDialog.vue'
import AddRoleDialog from '@/components/role/AddRoleDialog.vue'
import DeleteRoleDialog from '@/components/role/DeleteRoleDialog.vue'

interface RoleSuggestion {
  value: string
  role: RoleInfo
}

// ============ 权限等级展示 ============
// 等级文案：1 最高(仅限超级管理员) / 2 管理员等 / 3 普通用户
function levelText(level: number): string {
  switch (level) {
    case 1:
      return '最高等级'
    case 2:
      return '管理员'
    case 3:
      return '普通用户'
    default:
      return `等级 ${level}`
  }
}

function levelClass(level: number): string {
  return `level-tag--${Math.min(Math.max(level, 1), 3)}`
}

// ============ 列表与分页状态 ============
const roles = ref<RoleInfo[]>([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const isLoading = ref(false)

// ============ 权限与角色弹窗状态 ============
const userStore = useUserStore()
// 是否拥有新增角色的职权（决定「新增角色」按钮可见性）
const canAddRole = ref(false)
// 是否拥有修改角色的职权（决定修改按钮可见性与修改入口是否开放）
const canUpdateRole = ref(false)
// 是否拥有删除角色的职权（决定删除按钮可见性与删除入口是否开放）
const canDeleteRole = ref(false)
const addDialogVisible = ref(false)
const updateDialogVisible = ref(false)
const deleteDialogVisible = ref(false)
const updateTarget = ref<RoleInfo | null>(null)
const deleteTarget = ref<RoleInfo | null>(null)

// 当前用户是否可修改该角色：需同时拥有 role.update 职权，且角色等级为 2
function canUpdateRow(value: unknown) {
  const role = value as RoleInfo
  return canUpdateRole.value && role.roleLevel === 2
}

// 当前用户是否可删除该角色：需同时拥有 role.delete 职权，且角色等级为 2
// （等级 1 超级管理员与等级 3 普通用户为系统内置角色，不可删除）
function canDeleteRow(value: unknown) {
  const role = value as RoleInfo
  return canDeleteRole.value && role.roleLevel === 2
}

// ============ 搜索状态 ============
const keyword = ref('')
const suggestions = ref<RoleSuggestion[]>([])
const activeKeyword = ref('')

// ============ 加载角色列表 ============
// 根据当前激活的关键词，走模糊搜索接口或全量分页接口
async function loadRoles() {
  isLoading.value = true
  try {
    const response = activeKeyword.value
      ? await searchRolePage(activeKeyword.value, pageNum.value, pageSize.value)
      : await getRolePage(pageNum.value, pageSize.value)
    const page = response.data.data
    if (!page) throw new Error('角色列表为空')
    roles.value = page.records
    total.value = page.total
  } catch (error) {
    roles.value = []
    total.value = 0
    ElMessage.error(getRequestErrorMessage(error, '获取角色列表失败'))
  } finally {
    isLoading.value = false
  }
}

// ============ 分页处理 ============
function handlePageSizeChange() {
  pageNum.value = 1
  void loadRoles()
}

function handlePageChange() {
  void loadRoles()
}

// ============ 搜索联想 ============
// 通过防抖与请求序号双重保护，避免快速输入时并发请求
// 导致旧响应覆盖新结果、联想弹窗反复闪烁。
let suggestionTimer: ReturnType<typeof setTimeout> | undefined
let suggestionRequestId = 0

// 输入关键词时调用搜索接口，将匹配到的角色作为弹出框候选
function fetchSuggestions(
  query: string,
  callback: (items: Array<{ value: string; role: RoleInfo }>) => void,
) {
  // 防抖：仅在用户停顿后才发起请求
  if (suggestionTimer) clearTimeout(suggestionTimer)

  suggestionTimer = setTimeout(async () => {
    suggestionTimer = undefined
    const text = query.trim()
    if (!text) {
      suggestions.value = []
      callback([])
      return
    }

    // 记录本次请求序号，丢弃过期请求结果
    const requestId = ++suggestionRequestId
    try {
      const response = await searchRolePage(text, 1, 10)
      if (requestId !== suggestionRequestId) return
      const records = (response.data.data?.records ?? []).slice(0, 10)
      suggestions.value = records.map((role) => ({
        value: role.roleName,
        role,
      }))
      callback(suggestions.value)
    } catch {
      if (requestId !== suggestionRequestId) return
      suggestions.value = []
      callback([])
    }
  }, 300)
}

// ============ 搜索执行 ============
function executeSearch() {
  activeKeyword.value = keyword.value.trim()
  pageNum.value = 1
  void loadRoles()
}

// 选中联想项后直接执行该角色的搜索
function selectSuggestion(item: Record<string, unknown>) {
  const suggestion = item as unknown as RoleSuggestion
  keyword.value = suggestion.role.roleName
  executeSearch()
}

function clearSearch() {
  keyword.value = ''
  activeKeyword.value = ''
  suggestions.value = []
  suggestionRequestId += 1
  pageNum.value = 1
  void loadRoles()
}

// ============ 角色弹窗调度 ============
function openAddDialog() {
  addDialogVisible.value = true
}

// 打开修改弹窗并回显目标角色信息
function openUpdateDialog(value: unknown) {
  updateTarget.value = value as RoleInfo
  updateDialogVisible.value = true
}

// 打开删除弹窗并确认目标角色信息
function openDeleteDialog(value: unknown) {
  deleteTarget.value = value as RoleInfo
  deleteDialogVisible.value = true
}

// 新增成功后回到第一页并刷新列表
function handleAddSuccess() {
  pageNum.value = 1
  void loadRoles()
}

// 修改成功后刷新当前列表（保留搜索与分页状态）
function handleUpdateSuccess() {
  void loadRoles()
}

// 删除成功后刷新列表：当前页仅剩一条时回退一页
function handleDeleteSuccess() {
  if (roles.value.length === 1 && pageNum.value > 1) {
    pageNum.value -= 1
  }
  void loadRoles()
}

// ============ 刷新角色列表 ============
function refreshRoles() {
  activeKeyword.value = ''
  suggestions.value = []
  suggestionRequestId += 1
  pageNum.value = 1
  void loadRoles()
}

// ============ 初始化 ============
onMounted(() => {
  void loadRoles()
  // 加载当前用户职权，决定角色增删改功能是否可见可用
  void userStore
    .loadPermissions()
    .then(() => {
      canAddRole.value = userStore.can('role.add')
      canUpdateRole.value = userStore.can('role.update')
      canDeleteRole.value = userStore.can('role.delete')
    })
    .catch(() => {
      canAddRole.value = false
      canUpdateRole.value = false
      canDeleteRole.value = false
    })
})

// 组件卸载时清理联想防抖定时器
onUnmounted(() => {
  if (suggestionTimer) clearTimeout(suggestionTimer)
  suggestionTimer = undefined
})
</script>

<template>
  <div class="role-management">
    <!-- 页面标题 -->

    <PageHeading
      badge="ROLE"
      description="管理角色定义与系统权限分配。"
      eyebrow="SYSTEM MANAGEMENT"
      style="cursor: pointer"
      title="角色管理"
      @click="refreshRoles()"
    />

    <section class="role-card">
      <!-- 顶部工具栏：搜索与操作按钮 -->
      <div class="toolbar">
        <div class="search-group">
          <ElAutocomplete
            v-model="keyword"
            :fetch-suggestions="fetchSuggestions"
            :trigger-on-focus="false"
            clearable
            placeholder="搜索角色名称或角色 ID"
            popper-class="role-autocomplete"
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
            <!-- 联想项：角色首字头像、名称与角色 ID -->
            <template #default="{ item }">
              <div class="suggestion-item">
                <span class="suggestion-item__avatar">
                  {{ item.role.roleName.slice(0, 1) }}
                </span>
                <span class="suggestion-item__text">
                  <strong>{{ item.role.roleName }}</strong>
                  <small>角色 ID {{ item.role.roleId }}</small>
                </span>
              </div>
            </template>
          </ElAutocomplete>
          <button class="search-button" type="button" @click="executeSearch">
            查询
          </button>
        </div>
        <div class="toolbar__actions">
          <button
            v-if="canAddRole"
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
            新增角色
          </button>
        </div>
      </div>

      <!-- 角色列表 -->
      <ElTable
        v-loading="isLoading"
        :data="roles"
        class="role-table"
        row-key="roleId"
      >
        <!-- 角色 ID -->
        <ElTableColumn label="角色 ID" width="120">
          <template #default="{ row }">
            <span class="id-text">#{{ row.roleId }}</span>
          </template>
        </ElTableColumn>
        <!-- 角色名称 -->
        <ElTableColumn label="角色名称" min-width="240">
          <template #default="{ row }">
            <div class="role-name">
              <span class="role-name__avatar">
                {{ row.roleName.slice(0, 1) }}
              </span>
              <span>{{ row.roleName }}</span>
            </div>
          </template>
        </ElTableColumn>
        <!-- 权限等级 -->
        <ElTableColumn label="权限等级" min-width="180">
          <template #default="{ row }">
            <span :class="['level-tag', levelClass(row.roleLevel)]">
              <span class="level-tag__dot"></span>
              {{ levelText(row.roleLevel) }}
            </span>
          </template>
        </ElTableColumn>
        <!-- 操作列：修改与删除（无修改亦无删除职权时整列隐藏） -->
        <ElTableColumn
          v-if="canUpdateRole || canDeleteRole"
          :width="canUpdateRole && canDeleteRole ? 132 : 84"
          fixed="right"
          label="操作"
        >
          <template #default="{ row }">
            <div class="row-actions">
              <!-- 修改角色：需拥有 role.update 职权且角色等级为 2 -->
              <button
                v-if="canUpdateRow(row)"
                title="修改角色"
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
              <!-- 删除角色：需拥有 role.delete 职权且角色等级为 2 -->
              <button
                v-if="canDeleteRow(row)"
                class="row-actions__delete"
                title="删除角色"
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
          共 <strong>{{ total }}</strong> 条角色
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
  </div>

  <!-- 新增角色弹窗 -->
  <AddRoleDialog v-model="addDialogVisible" @success="handleAddSuccess" />

  <!-- 修改角色弹窗 -->
  <UpdateRoleDialog
    v-model="updateDialogVisible"
    :role="updateTarget"
    @success="handleUpdateSuccess"
  />

  <!-- 删除角色弹窗 -->
  <DeleteRoleDialog
    v-model="deleteDialogVisible"
    :target="deleteTarget"
    @success="handleDeleteSuccess"
  />
</template>

<style lang="scss" scoped>
.role-management {
  width: min(1120px, calc(100% - 48px));
  margin: 0 auto;
  padding: 42px 0 64px;
}

.role-card {
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
  width: min(480px, 100%);
  gap: 8px;

  :deep(.el-autocomplete) {
    width: 100%;
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

// 搜索联想弹出框样式
:global(.el-autocomplete-suggestion.role-autocomplete) {
  width: min(420px, calc(100vw - 32px)) !important;
  overflow: hidden;
  border: 1px solid #eceef1;
  border-radius: 12px;
  box-shadow: 0 12px 34px rgb(34 42 57 / 12%);
}

:global(.role-autocomplete .el-autocomplete-suggestion__list) {
  max-height: 280px;
  padding: 6px;
}

:global(.role-autocomplete .el-autocomplete-suggestion__wrap) {
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

    &:hover {
      background: #8d7350;
    }
  }
}

:deep(.role-table) {
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

.id-text {
  color: #a0a5af;
}

.role-name {
  display: flex;
  align-items: center;
  gap: 9px;
  color: #414958;

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
}

.level-tag {
  display: inline-flex;
  height: 26px;
  align-items: center;
  gap: 6px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 600;

  &__dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
  }

  &--1 {
    background: #fbecea;
    color: #b5544a;

    .level-tag__dot {
      background: #b5544a;
    }
  }

  &--2 {
    background: #f5f2eb;
    color: #967c56;

    .level-tag__dot {
      background: #967c56;
    }
  }

  &--3 {
    background: #eef5ef;
    color: #4f8a5f;

    .level-tag__dot {
      background: #4f8a5f;
    }
  }
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
  .role-management {
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
