<!--
  权限管理页面
  负责系统权限目录的展示、搜索与增删改操作，列表与搜索均调用后端分页接口：
  全量分页 /api/permission/all/{pageNum}，模糊搜索 /api/permission/search/{pageNum}，
  新增 /api/permission/add、修改 /api/permission/update、删除 /api/permission/delete，
  访问数据依赖 permission.list 职权，增删改分别需要 permission.add / permission.update /
  permission.delete 职权，无权限时展示无访问权限提示或隐藏对应按钮，
  视觉风格与用户/角色/菜单管理页面保持一致。
-->
<script lang="ts" setup>
import {onMounted, ref} from 'vue'
import {ElAutocomplete, ElMessage, ElPagination, ElTable, ElTableColumn,} from 'element-plus'
import {getPermissionPage, type PermissionInfo, searchPermissionPage,} from '@/api/user'
import {useUserStore} from '@/store'
import {getRequestErrorMessage} from '@/utils/error'
import PageHeading from '@/components/common/PageHeading.vue'
import AddPermissionDialog from '@/components/permission/AddPermissionDialog.vue'
import UpdatePermissionDialog from '@/components/permission/UpdatePermissionDialog.vue'
import DeletePermissionDialog from '@/components/permission/DeletePermissionDialog.vue'

// 自动补全建议项：value 为回填到搜索框的文本，permission 为联想命中的完整记录
interface PermissionSuggestion {
  value: string
  permission: PermissionInfo
}

// ============ 列表与分页状态 ============
const permissions = ref<PermissionInfo[]>([])
const isLoading = ref(false)
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

// ============ 搜索状态 ============
const keyword = ref('')
const suggestions = ref<PermissionSuggestion[]>([])
const activeKeyword = ref('')

// ============ 权限状态 ============
const userStore = useUserStore()
const canListPermission = ref(false)
const canAddPermission = ref(false)
const canUpdatePermission = ref(false)
const canDeletePermission = ref(false)
const permissionChecked = ref(false)

// ============ 弹窗开关 ============
const addDialogVisible = ref(false)
const updateDialogVisible = ref(false)
const updateTarget = ref<PermissionInfo | null>(null)
const deleteDialogVisible = ref(false)
const deleteTarget = ref<PermissionInfo | null>(null)

// ============ 加载权限列表 ============
// 存在激活的搜索关键词时走模糊查询分页，否则走全量分页
async function loadPermissions() {
  isLoading.value = true
  try {
    const response = activeKeyword.value
      ? await searchPermissionPage(
          activeKeyword.value,
          pageNum.value,
          pageSize.value,
        )
      : await getPermissionPage(pageNum.value, pageSize.value)
    const page = response.data.data
    if (!page) throw new Error('权限列表为空')
    permissions.value = page.records
    total.value = page.total
  } catch (error) {
    permissions.value = []
    total.value = 0
    ElMessage.error(getRequestErrorMessage(error, '获取权限列表失败'))
  } finally {
    isLoading.value = false
  }
}

// ============ 分页处理 ============
function handlePageSizeChange() {
  pageNum.value = 1
  void loadPermissions()
}

function handlePageChange() {
  void loadPermissions()
}

// ============ 搜索联想与执行 ============
async function fetchSuggestions(
  query: string,
  callback: (items: PermissionSuggestion[]) => void,
) {
  const text = query.trim()
  if (!text) {
    suggestions.value = []
    callback([])
    return
  }

  try {
    const response = await searchPermissionPage(text, 1, 10)
    const records = (response.data.data?.records ?? []).slice(0, 10)
    suggestions.value = records.map((permission) => ({
      value:
        permission.permissionName ||
        permission.permissionCode ||
        `#${permission.permissionId}`,
      permission,
    }))
    callback(suggestions.value)
  } catch {
    suggestions.value = []
    callback([])
  }
}

function selectSuggestion(item: Record<string, unknown>) {
  const suggestion = item as unknown as PermissionSuggestion
  keyword.value =
    suggestion.permission.permissionName ||
    suggestion.permission.permissionCode ||
    `#${suggestion.permission.permissionId}`
  void executeSearch()
}

function executeSearch() {
  activeKeyword.value = keyword.value.trim()
  pageNum.value = 1
  void loadPermissions()
}

function clearSearch() {
  keyword.value = ''
  suggestions.value = []
  activeKeyword.value = ''
  pageNum.value = 1
  void loadPermissions()
}

// ============ 刷新权限列表 ============
function refreshPermissions() {
  keyword.value = ''
  suggestions.value = []
  activeKeyword.value = ''
  pageNum.value = 1
  void loadPermissions()
}

// ============ 弹窗调度 ============
function openAddDialog() {
  addDialogVisible.value = true
}

// 新增成功后回到第一页并刷新列表
function handleAddSuccess() {
  pageNum.value = 1
  void loadPermissions()
}

function openUpdateDialog(value: unknown) {
  updateTarget.value = value as PermissionInfo
  updateDialogVisible.value = true
}

// 修改成功后保持当前页并刷新列表
function handleUpdateSuccess() {
  void loadPermissions()
}

function openDeleteDialog(value: unknown) {
  deleteTarget.value = value as PermissionInfo
  deleteDialogVisible.value = true
}

// 删除成功后回到第一页并刷新列表
function handleDeleteSuccess() {
  if (permissions.value.length === 1 && pageNum.value > 1) {
    pageNum.value -= 1
  }
  void loadPermissions()
}

// ============ 初始化 ============
onMounted(() => {
  void userStore
    .loadPermissions()
    .then(() => {
      permissionChecked.value = true
      canListPermission.value = userStore.can('permission.list')
      canAddPermission.value = userStore.can('permission.add')
      canUpdatePermission.value = userStore.can('permission.update')
      canDeletePermission.value = userStore.can('permission.delete')
      if (canListPermission.value) void loadPermissions()
    })
    .catch(() => {
      permissionChecked.value = true
      canListPermission.value = false
      canAddPermission.value = false
      canUpdatePermission.value = false
      canDeletePermission.value = false
    })
})
</script>

<template>
  <div class="permission-management">
    <!-- 页面标题 -->
    <PageHeading
      badge="PERMISSION"
      description="管理系统权限定义与职权标识。"
      eyebrow="SYSTEM MANAGEMENT"
      style="cursor: pointer"
      title="权限管理"
      @click="refreshPermissions()"
    />

    <section
      v-if="!permissionChecked || canListPermission"
      class="permission-card"
    >
      <!-- 顶部工具栏：搜索与新增权限 -->
      <div class="toolbar">
        <div class="search-group">
          <ElAutocomplete
            v-model="keyword"
            :fetch-suggestions="fetchSuggestions"
            :trigger-on-focus="false"
            clearable
            placeholder="搜索权限 ID、权限标识或权限名称"
            popper-class="permission-autocomplete"
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
                  {{ (item.permission.permissionName || 'P').slice(0, 1) }}
                </span>
                <span class="suggestion-item__text">
                  <strong>{{
                    item.permission.permissionName || '未命名权限'
                  }}</strong>
                  <small
                    >{{ item.permission.permissionCode }} · ID
                    {{ item.permission.permissionId }} ·
                    {{ item.permission.permissionType }}</small
                  >
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
            v-if="canAddPermission"
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
            新增权限
          </button>
        </div>
      </div>

      <!-- 权限列表 -->
      <ElTable
        v-loading="isLoading"
        :data="permissions"
        class="permission-table"
        row-key="permissionId"
      >
        <!-- 权限 ID -->
        <ElTableColumn label="权限 ID" width="100">
          <template #default="{ row }">
            <span class="id-text">#{{ row.permissionId }}</span>
          </template>
        </ElTableColumn>
        <!-- 权限名称 -->
        <ElTableColumn label="权限名称" min-width="170">
          <template #default="{ row }">
            <span class="permission-name">{{ row.permissionName }}</span>
          </template>
        </ElTableColumn>
        <!-- 权限标识 -->
        <ElTableColumn label="权限标识" min-width="180">
          <template #default="{ row }">
            <code v-if="row.permissionCode" class="permission-code">
              {{ row.permissionCode }}
            </code>
            <span v-else class="plain-text">—</span>
          </template>
        </ElTableColumn>
        <!-- 权限描述 -->
        <ElTableColumn label="权限描述" min-width="220">
          <template #default="{ row }">
            <span class="plain-text">
              {{ row.permissionDesc || '—' }}
            </span>
          </template>
        </ElTableColumn>
        <!-- 权限类型 -->
        <ElTableColumn label="权限类型" width="130">
          <template #default="{ row }">
            <span v-if="row.permissionType" class="type-tag">
              {{ row.permissionType }}
            </span>
            <span v-else class="plain-text">—</span>
          </template>
        </ElTableColumn>
        <!-- 操作列：仅在具备修改或删除权限时展示 -->
        <ElTableColumn
          v-if="canUpdatePermission || canDeletePermission"
          :width="canUpdatePermission && canDeletePermission ? 132 : 84"
          fixed="right"
          label="操作"
        >
          <template #default="{ row }">
            <div class="row-actions">
              <!-- 修改权限：需拥有 permission.update 职权 -->
              <button
                v-if="canUpdatePermission"
                title="修改权限"
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
              <!-- 删除权限：需拥有 permission.delete 职权 -->
              <button
                v-if="canDeletePermission"
                class="row-actions__delete"
                title="删除权限"
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

      <footer class="pagination-bar">
        <p>
          共 <strong>{{ total }}</strong> 条权限
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

    <!-- 无访问权限提示 -->
    <section v-else class="permission-empty">
      <span aria-hidden="true" class="permission-empty__icon">
        <svg fill="none" viewBox="0 0 24 24">
          <path
            d="M12 10.5v5M12 7.6v.1"
            stroke="currentColor"
            stroke-linecap="round"
            stroke-width="1.6"
          />
          <path
            d="M8.5 20h7l-1.2-8.3a2.3 2.3 0 0 0-4.6 0L8.5 20Z"
            stroke="currentColor"
            stroke-linejoin="round"
            stroke-width="1.5"
          />
        </svg>
      </span>
      <h2>暂无权限管理权限</h2>
      <p>需要 permission.list 职权才能查看权限列表，请联系管理员开通。</p>
    </section>

    <!-- 新增权限弹窗 -->
    <AddPermissionDialog
      v-model="addDialogVisible"
      @success="handleAddSuccess"
    />

    <!-- 修改权限弹窗 -->
    <UpdatePermissionDialog
      v-model="updateDialogVisible"
      :permission="updateTarget"
      @success="handleUpdateSuccess"
    />

    <!-- 删除权限弹窗 -->
    <DeletePermissionDialog
      v-model="deleteDialogVisible"
      :target="deleteTarget"
      @success="handleDeleteSuccess"
    />
  </div>
</template>

<style lang="scss" scoped>
.permission-management {
  width: min(1120px, calc(100% - 48px));
  margin: 0 auto;
  padding: 42px 0 64px;
}

.permission-card {
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

  // 权限搜索框：高度、圆角、边框与其他页面一致
  :deep(.el-autocomplete) {
    width: 100%;
    flex: 1 1 280px;
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

// 自动补全下拉面板：宽度、圆角、阴影与建议项样式与其他页面一致
:global(.el-autocomplete-suggestion.permission-autocomplete) {
  width: min(420px, calc(100vw - 32px)) !important;
  overflow: hidden;
  border: 1px solid #eceef1;
  border-radius: 12px;
  box-shadow: 0 12px 34px rgb(34 42 57 / 12%);
}

:global(.permission-autocomplete .el-autocomplete-suggestion__list) {
  max-height: 280px;
  padding: 6px;
}

:global(.permission-autocomplete .el-autocomplete-suggestion__wrap) {
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
  }
}

:deep(.permission-table) {
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

.permission-name {
  color: #414958;
  font-weight: 500;
}

.plain-text {
  color: #616978;
}

.permission-code {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  padding: 3px 8px;
  border-radius: 6px;
  background: #f5f2eb;
  color: #967c56;
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.type-tag {
  display: inline-flex;
  height: 24px;
  align-items: center;
  padding: 0 9px;
  border-radius: 999px;
  background: #eef5ef;
  color: #4f8a5f;
  font-size: 11px;
  font-weight: 600;
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

.permission-empty {
  display: flex;
  min-height: 320px;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 32px;
  border: 1px solid #eceef1;
  border-radius: 16px;
  background: rgb(255 255 255 / 94%);
  box-shadow: 0 7px 22px rgb(34 42 57 / 3%);
  text-align: center;

  &__icon {
    display: grid;
    width: 58px;
    height: 58px;
    place-items: center;
    border-radius: 16px;
    background: #f5f2eb;
    color: #a68b62;

    svg {
      width: 26px;
      height: 26px;
    }
  }

  h2 {
    margin: 18px 0 0;
    color: #414958;
    font-size: 17px;
    font-weight: 600;
  }

  p {
    margin: 8px 0 0;
    color: #969ca8;
    font-size: 12px;
  }
}

@media (max-width: 760px) {
  .permission-management {
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
