<!--
  菜单管理页面
  负责菜单列表的分页展示，展示菜单 ID、菜单名称、路由路径、组件路径、
  权限标识、图标与缓存标记，访问数据依赖 menu.list 职权，
  无权限时展示无访问权限提示，视觉风格与用户/角色管理页面保持一致。
-->
<script lang="ts" setup>
import {onMounted, ref} from 'vue'
import {ElMessage, ElPagination, ElTable, ElTableColumn} from 'element-plus'
import {getMenuPage, type MenuInfo} from '@/api/menu'
import {useUserStore} from '@/store'
import {getRequestErrorMessage} from '@/utils/error'
import PageHeading from '@/components/common/PageHeading.vue'
import AddMenuDialog from '@/components/menu/AddMenuDialog.vue'
import UpdateMenuDialog from '@/components/menu/UpdateMenuDialog.vue'
import DeleteMenuDialog from '@/components/menu/DeleteMenuDialog.vue'

// ============ 列表与分页状态 ============
const menus = ref<MenuInfo[]>([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const isLoading = ref(false)

// ============ 权限状态 ============
const userStore = useUserStore()
const canListMenu = ref(false)
// 是否拥有新增菜单的职权（决定「新增菜单」按钮可见性）
const canAddMenu = ref(false)
// 是否拥有修改/删除菜单的职权（决定操作列按钮可见性）
const canUpdateMenu = ref(false)
const canDeleteMenu = ref(false)
const permissionChecked = ref(false)
const addDialogVisible = ref(false)
const updateDialogVisible = ref(false)
const deleteDialogVisible = ref(false)
const updateTarget = ref<MenuInfo | null>(null)
const deleteTarget = ref<MenuInfo | null>(null)

// 是否缓存展示：keepAlive 为 1 表示缓存，其余视为不缓存
function keepAliveText(value: number | null): string {
  return value === 1 ? '缓存' : '不缓存'
}

function keepAliveClass(value: number | null): string {
  return value === 1 ? 'keepalive-tag--on' : 'keepalive-tag--off'
}

// ============ 加载菜单列表 ============
async function loadMenus() {
  isLoading.value = true
  try {
    const response = await getMenuPage(pageNum.value, pageSize.value)
    const page = response.data.data
    if (!page) throw new Error('菜单列表为空')
    menus.value = page.records
    total.value = page.total
  } catch (error) {
    menus.value = []
    total.value = 0
    ElMessage.error(getRequestErrorMessage(error, '获取菜单列表失败'))
  } finally {
    isLoading.value = false
  }
}

// ============ 分页处理 ============
function handlePageSizeChange() {
  pageNum.value = 1
  void loadMenus()
}

function handlePageChange() {
  void loadMenus()
}

// ============ 刷新菜单列表 ============
function refreshMenus() {
  pageNum.value = 1
  void loadMenus()
}

// ============ 新增菜单弹窗 ============
function openAddDialog() {
  addDialogVisible.value = true
}

// 新增成功后回到第一页并刷新列表
function handleAddSuccess() {
  pageNum.value = 1
  void loadMenus()
}

// ============ 修改菜单弹窗 ============
function openUpdateDialog(value: unknown) {
  const menu = value as MenuInfo
  updateTarget.value = menu
  updateDialogVisible.value = true
}

// 修改成功后保持当前页并刷新列表
function handleUpdateSuccess() {
  void loadMenus()
}

// ============ 删除菜单弹窗 ============
function openDeleteDialog(value: unknown) {
  const menu = value as MenuInfo
  deleteTarget.value = menu
  deleteDialogVisible.value = true
}

// 删除成功后回到第一页并刷新列表
function handleDeleteSuccess() {
  pageNum.value = 1
  void loadMenus()
}

// ============ 初始化 ============
onMounted(() => {
  void userStore
    .loadPermissions()
    .then(() => {
      permissionChecked.value = true
      canListMenu.value = userStore.can('menu.list')
      canAddMenu.value = userStore.can('menu.add')
      canUpdateMenu.value = userStore.can('menu.update')
      canDeleteMenu.value = userStore.can('menu.delete')
      if (canListMenu.value) void loadMenus()
    })
    .catch(() => {
      permissionChecked.value = true
      canListMenu.value = false
      canAddMenu.value = false
      canUpdateMenu.value = false
      canDeleteMenu.value = false
    })
})
</script>

<template>
  <div class="menu-management">
    <!-- 页面标题 -->
    <PageHeading
      badge="MENU"
      description="管理后台菜单结构与导航配置。"
      eyebrow="SYSTEM MANAGEMENT"
      style="cursor: pointer"
      title="菜单管理"
      @click="refreshMenus()"
    />

    <section v-if="!permissionChecked || canListMenu" class="menu-card">
      <!-- 顶部工具栏：操作按钮 -->
      <div class="toolbar">
        <div class="toolbar__hint">配置后台菜单结构、路由与组件信息。</div>
        <div class="toolbar__actions">
          <button
            v-if="canAddMenu"
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
            新增菜单
          </button>
        </div>
      </div>

      <!-- 菜单列表 -->
      <ElTable
        v-loading="isLoading"
        :data="menus"
        class="menu-table"
        row-key="menuId"
      >
        <!-- 菜单 ID -->
        <ElTableColumn label="菜单 ID" width="110">
          <template #default="{ row }">
            <span class="id-text">#{{ row.menuId }}</span>
          </template>
        </ElTableColumn>
        <!-- 菜单名称 -->
        <ElTableColumn label="菜单名称" min-width="180">
          <template #default="{ row }">
            <div class="menu-name">
              <span class="menu-name__avatar">
                {{ row.menuName.slice(0, 1) }}
              </span>
              <span>{{ row.menuName }}</span>
            </div>
          </template>
        </ElTableColumn>
        <!-- 路由路径 -->
        <ElTableColumn label="路由路径" min-width="180">
          <template #default="{ row }">
            <code class="path-text">{{ row.routePath || '—' }}</code>
          </template>
        </ElTableColumn>
        <!-- 路由名称 -->
        <ElTableColumn label="路由名称" min-width="170">
          <template #default="{ row }">
            <span class="plain-text">{{ row.routeName || '—' }}</span>
          </template>
        </ElTableColumn>
        <!-- 组件路径 -->
        <ElTableColumn label="组件路径" min-width="200">
          <template #default="{ row }">
            <code class="path-text">{{ row.componentPath || '—' }}</code>
          </template>
        </ElTableColumn>
        <!-- 菜单图标 -->
        <ElTableColumn label="图标" min-width="150">
          <template #default="{ row }">
            <span class="plain-text">{{ row.icon || '—' }}</span>
          </template>
        </ElTableColumn>
        <!-- 权限标识 -->
        <ElTableColumn label="权限标识" min-width="170">
          <template #default="{ row }">
            <code v-if="row.permissionCode" class="permission-code">
              {{ row.permissionCode }}
            </code>
            <span v-else class="plain-text">—</span>
          </template>
        </ElTableColumn>
        <!-- 是否缓存 -->
        <ElTableColumn label="路由缓存" width="110">
          <template #default="{ row }">
            <span :class="['keepalive-tag', keepAliveClass(row.keepAlive)]">
              {{ keepAliveText(row.keepAlive) }}
            </span>
          </template>
        </ElTableColumn>
        <!-- 操作：无 menu.update 亦无 menu.delete 职权时整列隐藏 -->
        <ElTableColumn
          v-if="canUpdateMenu || canDeleteMenu"
          :width="canUpdateMenu && canDeleteMenu ? 132 : 84"
          fixed="right"
          label="操作"
        >
          <template #default="{ row }">
            <div class="row-actions">
              <!-- 修改菜单：需拥有 menu.update 职权 -->
              <button
                v-if="canUpdateMenu"
                title="修改菜单"
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
              <!-- 删除菜单：需拥有 menu.delete 职权 -->
              <button
                v-if="canDeleteMenu"
                class="row-actions__delete"
                title="删除菜单"
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
          共 <strong>{{ total }}</strong> 条菜单
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
      <h2>暂无菜单管理权限</h2>
      <p>需要 menu.list 职权才能查看菜单列表，请联系管理员开通。</p>
    </section>
  </div>

  <!-- 新增菜单弹窗 -->
  <AddMenuDialog v-model="addDialogVisible" @success="handleAddSuccess" />

  <!-- 修改菜单弹窗 -->
  <UpdateMenuDialog
    v-model="updateDialogVisible"
    :menu="updateTarget"
    @success="handleUpdateSuccess"
  />

  <!-- 删除菜单弹窗 -->
  <DeleteMenuDialog
    v-model="deleteDialogVisible"
    :target="deleteTarget"
    @success="handleDeleteSuccess"
  />
</template>

<style lang="scss" scoped>
.menu-management {
  width: min(1120px, calc(100% - 48px));
  margin: 0 auto;
  padding: 42px 0 64px;
}

.menu-card {
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

  &__hint {
    color: #969ca8;
    font-size: 12px;
  }

  &__actions {
    display: flex;
    gap: 8px;
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

:deep(.menu-table) {
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

.menu-name {
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

.path-text {
  max-width: 100%;
  overflow: hidden;
  color: #616978;
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
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

.keepalive-tag {
  display: inline-flex;
  height: 24px;
  align-items: center;
  padding: 0 9px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 600;

  &--on {
    background: #eef5ef;
    color: #4f8a5f;
  }

  &--off {
    background: #f2f3f5;
    color: #8a919d;
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
  .menu-management {
    width: min(100% - 32px, 560px);
    padding-top: 27px;
  }

  .toolbar {
    align-items: stretch;
    flex-direction: column;

    &__actions {
      justify-content: flex-end;
    }
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
