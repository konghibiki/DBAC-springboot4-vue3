<!--
  登录/注册页面
  负责账号的登录、注册与登录后选择职能流程，
  通过 useAuthForm 组合函数统一管理表单状态与校验，
  成功后写入用户状态并跳转到首页；同时处理会话过期重新登录。
-->
<script lang="ts" setup>
import {ref} from 'vue'
import AuthBrand from '@/components/auth/AuthBrand.vue'
import AuthInputField from '@/components/auth/AuthInputField.vue'
import AuthVisualPanel from '@/components/auth/AuthVisualPanel.vue'
import {login, loginWithRole, logout, register} from '@/api/login'
import {getUserRoleList, type UserRole} from '@/api/user'
import router from '@/router'
import {useUserStore} from '@/store'
import {type AuthFieldName, useAuthForm} from '@/composables/useAuthForm'

const activeMode = ref<'login' | 'register'>('login')
const userStore = useUserStore()
const showPassword = ref(false)
const isSubmitting = ref(false)
const rememberMe = ref(false)
const roleDialogVisible = ref(false)
const isLoadingRoles = ref(false)
const roles = ref<UserRole[]>([])
const selectedRoleId = ref<number | null>(null)
const roleError = ref('')
const currentYear = new Date().getFullYear()
const passwordHint =
  '6-18位，至少包含一个英文字母和一个数字；可用符号 @$!%*?&#._-。'

const {
  values: form,
  fieldState,
  submitError,
  emailSuggestions,
  getError,
  markFocused,
  markBlurred,
  clearServerError,
  resetValidation,
  validateFields,
  applyRequestError,
  selectEmailSuggestion,
} = useAuthForm()

function selectMode(mode: 'login' | 'register') {
  activeMode.value = mode
  showPassword.value = false
  resetValidation()
}

function getRoleRequestErrorMessage(error: unknown) {
  return error instanceof Error ? error.message : '请求失败，请稍后重试'
}

async function loadRoles() {
  isLoadingRoles.value = true
  roleError.value = ''

  try {
    const response = await getUserRoleList()
    roles.value = response.data.data
    if (roles.value.length === 0) {
      roleError.value = '暂未获取到可选择的职能，请重试或联系管理员。'
    }
  } catch (error) {
    roleError.value = getRoleRequestErrorMessage(error)
  } finally {
    isLoadingRoles.value = false
  }
}

async function handleRoleSubmit() {
  if (isSubmitting.value || selectedRoleId.value === null) return

  isSubmitting.value = true
  roleError.value = ''
  try {
    await loginWithRole(selectedRoleId.value)
    await userStore.loadPermissions(true)
    userStore.markSessionReady() // 会话就绪，路由守卫据此短路鉴权，保证跳转不被偶发网络请求拦截
    roleDialogVisible.value = false
    await router.replace({ name: 'home' })
  } catch (error) {
    roleError.value = getRoleRequestErrorMessage(error)
  } finally {
    isSubmitting.value = false
  }
}

async function handleRoleLogout() {
  if (isSubmitting.value) return

  isSubmitting.value = true
  roleError.value = ''
  try {
    await logout()
    userStore.clearUserInfo()
    roleDialogVisible.value = false
    roles.value = []
    selectedRoleId.value = null
  } catch (error) {
    roleError.value = getRoleRequestErrorMessage(error)
  } finally {
    isSubmitting.value = false
  }
}

async function handleSubmit(event: SubmitEvent) {
  if (isSubmitting.value) return

  const fields: AuthFieldName[] =
    activeMode.value === 'login'
      ? ['email', 'password']
      : ['username', 'email', 'password', 'confirmPassword']

  if (event.currentTarget instanceof HTMLFormElement) {
    const submittedValues = new FormData(event.currentTarget)
    const username = submittedValues.get('username')
    const email = submittedValues.get('email')
    const password = submittedValues.get('password')
    const confirmPassword = submittedValues.get('password-confirmation')

    if (typeof username === 'string') form.username = username
    if (typeof email === 'string') form.email = email
    if (typeof password === 'string') form.password = password
    if (typeof confirmPassword === 'string') {
      form.confirmPassword = confirmPassword
    }
  }

  fields.forEach(markBlurred)
  if (!validateFields(fields)) return

  isSubmitting.value = true
  try {
    if (activeMode.value === 'login') {
      const response = await login({
        email: form.email.trim(),
        password: form.password,
        rememberMe: rememberMe.value,
      })

      if (response.data.data) {
        await userStore.loadPermissions(true)
        userStore.markSessionReady() // 会话就绪，路由守卫据此短路鉴权，保证跳转不被偶发网络请求拦截
        await router.replace({ name: 'home' })
      } else {
        roles.value = []
        selectedRoleId.value = null
        roleDialogVisible.value = true
        await loadRoles()
      }
      return
    }

    await register({
      email: form.email.trim(),
      password: form.password,
      userName: form.username.trim(),
    })
    selectMode('login')
  } catch (error) {
    applyRequestError(error)
  } finally {
    isSubmitting.value = false
  }
}
</script>

<template>
  <main class="auth-page">
    <AuthVisualPanel />

    <section aria-label="账号登录与注册" class="form-panel">
      <div class="form-panel__content">
        <AuthBrand class="form-panel__mobile-brand" compact dark />

        <div aria-label="选择登录或注册" class="mode-switch" role="tablist">
          <span
            :aria-selected="activeMode === 'login'"
            :class="{ 'mode-switch__item--active': activeMode === 'login' }"
            class="mode-switch__item"
            role="tab"
            tabindex="0"
            @click="selectMode('login')"
            @keydown.enter.prevent="selectMode('login')"
            @keydown.space.prevent="selectMode('login')"
          >
            去登录
          </span>
          <span
            :aria-selected="activeMode === 'register'"
            :class="{ 'mode-switch__item--active': activeMode === 'register' }"
            class="mode-switch__item"
            role="tab"
            tabindex="0"
            @click="selectMode('register')"
            @keydown.enter.prevent="selectMode('register')"
            @keydown.space.prevent="selectMode('register')"
          >
            去注册
          </span>
        </div>

        <div :key="activeMode" class="auth-form">
          <div class="auth-form__heading">
            <p class="auth-form__welcome">
              {{ activeMode === 'login' ? '欢迎回来' : '加入 NOVA' }}
            </p>
            <h2>
              {{ activeMode === 'login' ? '登录你的账号' : '创建一个新账号' }}
            </h2>
            <p class="auth-form__description">
              {{
                activeMode === 'login'
                  ? '输入账号信息，继续你的探索。'
                  : '填写以下信息，开启新的旅程。'
              }}
            </p>
          </div>

          <p v-if="submitError" class="auth-form__error" role="alert">
            {{ submitError }}
          </p>

          <form
            class="auth-form__fields"
            novalidate
            @submit.prevent="handleSubmit"
          >
            <AuthInputField
              v-if="activeMode === 'register'"
              id="username"
              v-model="form.username"
              :error="getError('username')"
              :hint="fieldState.username.focused ? '用户名长度为1-18位。' : ''"
              autocomplete="username"
              icon="user"
              label="用户名"
              name="username"
              placeholder="请输入用户名"
              type="text"
              @blur="markBlurred('username')"
              @focus="markFocused('username')"
              @input="clearServerError('username')"
            />

            <AuthInputField
              id="email"
              v-model="form.email"
              :error="getError('email')"
              :has-suggestions="emailSuggestions.length > 0"
              :hint="
                fieldState.email.focused
                  ? '请输入有效邮箱，输入 @ 后可选择常用后缀。'
                  : ''
              "
              autocomplete="email"
              icon="email"
              label="邮箱地址"
              name="email"
              placeholder="name@example.com"
              type="email"
              @blur="markBlurred('email')"
              @focus="markFocused('email')"
              @input="clearServerError('email')"
            >
              <template #suggestions>
                <div
                  v-if="emailSuggestions.length"
                  aria-label="常用邮箱后缀"
                  class="email-suggestions"
                  role="listbox"
                >
                  <button
                    v-for="suggestion in emailSuggestions"
                    :key="suggestion"
                    class="email-suggestion"
                    role="option"
                    type="button"
                    @click="selectEmailSuggestion(suggestion)"
                    @mousedown.prevent
                  >
                    {{ suggestion }}
                  </button>
                </div>
              </template>
            </AuthInputField>

            <AuthInputField
              id="password"
              v-model="form.password"
              :autocomplete="
                activeMode === 'login' ? 'current-password' : 'new-password'
              "
              :error="getError('password')"
              :hint="fieldState.password.focused ? passwordHint : ''"
              :password-visible="showPassword"
              icon="lock"
              label="密码"
              name="password"
              placeholder="请输入密码"
              type="password"
              @blur="markBlurred('password')"
              @focus="markFocused('password')"
              @input="clearServerError('password')"
              @toggle-password="showPassword = !showPassword"
            />

            <AuthInputField
              v-if="activeMode === 'register'"
              id="confirm-password"
              v-model="form.confirmPassword"
              :error="getError('confirmPassword')"
              :hint="
                fieldState.confirmPassword.focused
                  ? '请再次输入密码，确保与上方密码一致。'
                  : ''
              "
              :password-visible="showPassword"
              autocomplete="new-password"
              icon="lock"
              label="确认密码"
              name="password-confirmation"
              placeholder="请再次输入密码"
              type="password"
              @blur="markBlurred('confirmPassword')"
              @focus="markFocused('confirmPassword')"
              @input="clearServerError('confirmPassword')"
              @toggle-password="showPassword = !showPassword"
            />

            <div v-if="activeMode === 'login'" class="form-options">
              <label class="remember-option">
                <input v-model="rememberMe" name="remember" type="checkbox" />
                <span class="remember-option__checkbox"></span>
                <span>记住我</span>
              </label>
              <span class="form-options__hint">请妥善保管你的账号信息</span>
            </div>
            <p v-else class="register-note">
              注册即表示你已阅读并同意相关服务条款。
            </p>

            <button
              :disabled="isSubmitting"
              class="submit-button"
              type="submit"
            >
              {{
                isSubmitting
                  ? '处理中…'
                  : activeMode === 'login'
                    ? '登录'
                    : '创建账号'
              }}
              <svg aria-hidden="true" fill="none" viewBox="0 0 24 24">
                <path
                  d="M5 12h14m-6-6 6 6-6 6"
                  stroke="currentColor"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="1.8"
                />
              </svg>
            </button>
          </form>

          <p class="auth-form__switch-copy">
            {{ activeMode === 'login' ? '还没有账号？' : '已有账号？' }}
            <span
              role="button"
              tabindex="0"
              @click="selectMode(activeMode === 'login' ? 'register' : 'login')"
              @keydown.enter.prevent="
                selectMode(activeMode === 'login' ? 'register' : 'login')
              "
            >
              {{ activeMode === 'login' ? '去注册' : '去登录' }}
            </span>
          </p>
        </div>

        <p class="form-panel__copyright">
          © {{ currentYear }} NOVA. 用心连接每一个新开始。
        </p>
      </div>
    </section>

    <el-dialog
      v-model="roleDialogVisible"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      :show-close="false"
      aria-label="选择登录职能"
      class="role-dialog"
      title="选择登录职能"
      width="420px"
    >
      <p class="role-dialog__description">
        该账号拥有多个职能，请选择本次登录使用的职能。
      </p>
      <p v-if="isLoadingRoles" class="role-dialog__message">
        正在获取职能列表…
      </p>
      <el-radio-group
        v-else-if="roles.length"
        v-model="selectedRoleId"
        aria-label="可选职能"
        class="role-dialog__options"
      >
        <el-radio
          v-for="role in roles"
          :key="role.roleId"
          :value="role.roleId"
          class="role-dialog__option"
        >
          {{ role.roleName }}
        </el-radio>
      </el-radio-group>
      <p v-if="roleError" class="role-dialog__error" role="alert">
        {{ roleError }}
      </p>
      <template #footer>
        <el-button
          :disabled="isLoadingRoles || isSubmitting"
          @click="handleRoleLogout"
        >
          退出登录
        </el-button>
        <el-button
          v-if="roleError"
          :disabled="isLoadingRoles || isSubmitting"
          @click="loadRoles"
        >
          重新获取
        </el-button>
        <el-button
          :disabled="selectedRoleId === null || isSubmitting || isLoadingRoles"
          type="primary"
          @click="handleRoleSubmit"
        >
          {{ isSubmitting ? '登录中…' : '确认并登录' }}
        </el-button>
      </template>
    </el-dialog>
  </main>
</template>

<style lang="scss" scoped>
:global(*) {
  box-sizing: border-box;
}

:global(html),
:global(body),
:global(#app) {
  width: 100%;
  min-width: 320px;
  min-height: 100%;
  margin: 0;
}

:global(body) {
  font-family:
    Inter,
    -apple-system,
    BlinkMacSystemFont,
    'Segoe UI',
    'Microsoft YaHei',
    sans-serif;
  color: #242a3a;
  -webkit-font-smoothing: antialiased;
}

.auth-page {
  display: grid;
  grid-template-columns: minmax(0, 1.08fr) minmax(420px, 0.92fr);
  min-height: 100vh;
  min-height: 100svh;
  background: #fff;
}

.form-panel {
  display: flex;
  min-height: 100vh;
  min-height: 100svh;
  align-items: center;
  justify-content: center;
  padding: 48px clamp(32px, 6vw, 96px);
  background:
    radial-gradient(
      ellipse at 100% 0%,
      rgb(236 231 221 / 38%),
      transparent 37%
    ),
    #fff;

  &__content {
    width: 100%;
    max-width: 410px;
  }

  &__mobile-brand {
    display: none;
  }

  &__copyright {
    margin: 54px 0 0;
    color: #a4a8b2;
    font-size: 11px;
    letter-spacing: 0.02em;
    text-align: center;
  }
}

.mode-switch {
  display: inline-flex;
  align-items: center;
  gap: 28px;
  margin-bottom: 36px;
  border-bottom: 1px solid #eceef1;

  &__item {
    position: relative;
    padding: 0 0 12px;
    color: #989daa;
    cursor: pointer;
    font-size: 14px;
    font-weight: 500;
    transition: color 180ms ease;

    &::after {
      position: absolute;
      right: 0;
      bottom: -1px;
      left: 0;
      height: 2px;
      background: #b69a70;
      content: '';
      transform: scaleX(0);
      transition: transform 180ms ease;
    }

    &--active {
      color: #252b3a;

      &::after {
        transform: scaleX(1);
      }
    }

    &:focus-visible {
      outline: 2px solid #b69a70;
      outline-offset: 4px;
    }
  }
}

.auth-form {
  &__heading {
    margin-bottom: 32px;
  }

  &__welcome {
    margin: 0 0 10px;
    color: #b3976f;
    font-size: 12px;
    font-weight: 600;
    letter-spacing: 0.14em;
  }

  h2 {
    margin: 0;
    color: #242a3a;
    font-size: 30px;
    font-weight: 600;
    letter-spacing: -0.035em;
    line-height: 1.3;
  }

  &__description {
    margin: 10px 0 0;
    color: #9298a5;
    font-size: 13px;
    line-height: 1.7;
  }

  &__error {
    margin: -18px 0 20px;
    padding: 11px 13px;
    border: 1px solid #f0d7d5;
    border-radius: 8px;
    background: #fff8f7;
    color: #b94a48;
    font-size: 12px;
    line-height: 1.6;
  }

  &__fields {
    display: flex;
    flex-direction: column;
    gap: 20px;
  }

  &__switch-copy {
    margin: 28px 0 0;
    color: #9298a5;
    font-size: 13px;
    text-align: center;

    span {
      margin-left: 4px;
      color: #a98d65;
      cursor: pointer;
      font-weight: 600;

      &:hover {
        color: #806541;
      }

      &:focus-visible {
        outline: 2px solid #b69a70;
        outline-offset: 3px;
      }
    }
  }
}

.email-suggestions {
  position: absolute;
  z-index: 5;
  top: 73px;
  right: 0;
  left: 0;
  display: flex;
  max-height: 190px;
  flex-direction: column;
  overflow-y: auto;
  padding: 6px;
  border: 1px solid #ece9e2;
  border-radius: 9px;
  background: #fff;
  box-shadow: 0 12px 28px rgb(34 42 57 / 12%);
}

.email-suggestion {
  padding: 10px 11px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: #555b69;
  cursor: pointer;
  font: inherit;
  font-size: 12px;
  text-align: left;
  transition:
    background-color 150ms ease,
    color 150ms ease;

  &:hover,
  &:focus-visible {
    outline: 0;
    background: #f7f4ee;
    color: #806541;
  }
}

.form-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: -2px;

  &__hint {
    color: #a5a9b2;
    font-size: 11px;
  }
}

.remember-option {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #777d8a;
  cursor: pointer;
  font-size: 12px;

  input {
    position: absolute;
    width: 1px;
    height: 1px;
    opacity: 0;

    &:checked + .remember-option__checkbox {
      border-color: #a98d65;
      background: #a98d65;

      &::after {
        opacity: 1;
      }
    }

    &:focus-visible + .remember-option__checkbox {
      outline: 2px solid #b69a70;
      outline-offset: 2px;
    }
  }

  &__checkbox {
    position: relative;
    width: 15px;
    height: 15px;
    border: 1px solid #d5d7dc;
    border-radius: 4px;
    background: #fff;
    transition: all 160ms ease;

    &::after {
      position: absolute;
      top: 2px;
      left: 4px;
      width: 4px;
      height: 7px;
      border: solid #fff;
      border-width: 0 1.5px 1.5px 0;
      content: '';
      opacity: 0;
      transform: rotate(45deg);
    }
  }
}

.register-note {
  margin: -1px 0 0;
  color: #9da2ad;
  font-size: 11px;
  line-height: 1.6;
}

.role-dialog {
  &__description,
  &__message {
    margin: 0 0 18px;
    color: #777d8a;
    font-size: 13px;
    line-height: 1.6;
  }

  &__options {
    display: flex;
    width: 100%;
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
  }

  &__option {
    margin-right: 0;
  }

  &__error {
    margin: 16px 0 0;
    color: #b94a48;
    font-size: 12px;
    line-height: 1.6;
  }
}

.submit-button {
  display: flex;
  width: 100%;
  height: 51px;
  align-items: center;
  justify-content: center;
  gap: 10px;
  margin-top: 3px;
  border: 0;
  border-radius: 9px;
  background: #293343;
  box-shadow: 0 7px 16px rgb(41 51 67 / 14%);
  color: #fff;
  cursor: pointer;
  font: inherit;
  font-size: 14px;
  font-weight: 600;
  transition:
    background-color 180ms ease,
    transform 180ms ease,
    box-shadow 180ms ease;

  svg {
    width: 17px;
    height: 17px;
    transition: transform 180ms ease;
  }

  &:hover:not(:disabled) {
    background: #39475b;
    box-shadow: 0 9px 20px rgb(41 51 67 / 19%);

    svg {
      transform: translateX(3px);
    }
  }

  &:disabled {
    cursor: wait;
    opacity: 0.72;
  }

  &:active:not(:disabled) {
    transform: translateY(1px);
  }

  &:focus-visible {
    outline: 3px solid rgb(182 154 112 / 45%);
    outline-offset: 3px;
  }
}

@media (max-width: 1020px) {
  .auth-page {
    grid-template-columns: minmax(0, 0.92fr) minmax(390px, 1.08fr);
  }
}

@media (max-width: 760px) {
  .auth-page {
    display: flex;
    flex-direction: column;
  }

  .form-panel {
    min-height: auto;
    flex: 1;
    padding: 42px 28px 32px;

    &__content {
      max-width: 430px;
    }

    &__mobile-brand {
      display: flex;
      margin-bottom: 30px;
    }

    &__copyright {
      margin-top: 38px;
    }
  }

  .mode-switch {
    margin-bottom: 28px;
  }
}

@media (max-width: 420px) {
  .form-panel {
    padding: 34px 22px 28px;
  }

  .auth-form h2 {
    font-size: 27px;
  }

  .form-options__hint {
    font-size: 10px;
  }
}

@media (prefers-reduced-motion: reduce) {
  *,
  *::before,
  *::after {
    transition-duration: 0.01ms !important;
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
  }
}
</style>
