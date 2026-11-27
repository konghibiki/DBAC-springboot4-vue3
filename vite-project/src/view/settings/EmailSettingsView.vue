<!--
  邮箱绑定设置页面
  负责展示当前绑定邮箱并修改登录邮箱，
  前端校验新邮箱格式、后端校验唯一性，成功后同步更新共享用户资料中的邮箱。
-->
<script lang="ts" setup>
import {onMounted, reactive, ref} from 'vue'
import {ElMessage, type FormInstance, type FormRules} from 'element-plus'
import SettingsSectionHeading from '@/components/common/SettingsSectionHeading.vue'
import {updateBoundEmail} from '@/api/user'
import {useUserStore} from '@/store'
import {getEmailValidationError} from '@/utils/authValidation'

interface EmailForm {
  email: string
}

const userStore = useUserStore()
const formRef = ref<FormInstance>()
const isLoadingProfile = ref(false)
const isSaving = ref(false)
const form = reactive<EmailForm>({ email: '' })

const rules: FormRules<EmailForm> = {
  email: [
    { required: true, message: '请输入新的邮箱地址', trigger: 'blur' },
    {
      validator: (_rule, value: string, callback) => {
        const error = getEmailValidationError(value)
        callback(error ? new Error(error) : undefined)
      },
      trigger: ['blur', 'change'],
    },
  ],
}

// 绑定邮箱从共享用户资料回显，修改成功后同步更新页面状态。
onMounted(async () => {
  isLoadingProfile.value = !userStore.userInfo
  try {
    const info = await userStore.loadUserInfo()
    form.email = info.email
  } catch (error) {
    const message =
      error instanceof Error ? error.message : '获取用户信息失败，请稍后重试'
    ElMessage.error(message)
  } finally {
    isLoadingProfile.value = false
  }
})

async function saveEmail() {
  if (!formRef.value || isSaving.value) return

  const isValid = await formRef.value.validate().catch(() => false)
  if (!isValid) return

  const email = form.email.trim()
  if (email === userStore.userInfo?.email) {
    ElMessage.info('新邮箱与当前绑定邮箱相同')
    return
  }

  isSaving.value = true
  try {
    await updateBoundEmail(email)
    if (userStore.userInfo) userStore.userInfo.email = email
    ElMessage.success('绑定邮箱已更新')
  } catch (error) {
    const message =
      error instanceof Error ? error.message : '邮箱更新失败，请稍后重试'
    ElMessage.error(message)
  } finally {
    isSaving.value = false
  }
}
</script>

<template>
  <div class="email-settings">
    <!-- 邮箱更改仅在服务端确认唯一且格式有效后写入共享用户资料。 -->
    <SettingsSectionHeading
      badge="02"
      description="更新登录邮箱，请确认新邮箱地址准确无误。"
      eyebrow="EMAIL BINDING"
      title="更改绑定邮箱"
    />

    <el-skeleton v-if="isLoadingProfile" :rows="4" animated />
    <el-form
      v-else
      ref="formRef"
      :model="form"
      :rules="rules"
      class="email-form"
      label-position="top"
    >
      <div class="email-form__current">
        <span aria-hidden="true" class="email-form__current-icon">@</span>
        <div>
          <p>当前绑定邮箱</p>
          <strong>{{ userStore.userInfo?.email || '尚未获取' }}</strong>
        </div>
        <span class="email-form__current-status">当前绑定</span>
      </div>

      <el-form-item label="新的邮箱地址" prop="email">
        <el-input
          v-model="form.email"
          autocomplete="email"
          maxlength="254"
          placeholder="请输入新的邮箱地址"
          type="email"
        />
        <p class="email-form__hint">
          请输入有效邮箱地址，邮箱更新后将用于登录。
        </p>
      </el-form-item>

      <div class="email-form__footer">
        <span>为保证账号安全，请确保该邮箱可以正常使用。</span>
        <el-button :loading="isSaving" type="primary" @click="saveEmail">
          保存新邮箱
        </el-button>
      </div>
    </el-form>
  </div>
</template>

<style lang="scss" scoped>
.email-form {
  max-width: 560px;

  &__current {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 26px;
    padding: 14px;
    border: 1px solid #eceef1;
    border-radius: 11px;
    background: #fafbfc;

    > div {
      min-width: 0;
    }

    p {
      margin: 0 0 5px;
      color: #969ca8;
      font-size: 10px;
    }

    strong {
      display: block;
      overflow: hidden;
      color: #404858;
      font-size: 12px;
      text-overflow: ellipsis;
    }
  }

  &__current-icon {
    display: grid;
    width: 38px;
    height: 38px;
    flex: 0 0 auto;
    place-items: center;
    border-radius: 10px;
    background: #f2efe8;
    color: #a68b62;
    font-size: 17px;
    font-weight: 600;
  }

  &__current-status {
    margin-left: auto;
    padding: 5px 8px;
    border-radius: 20px;
    background: #edf5ef;
    color: #59846a;
    font-size: 9px;
    white-space: nowrap;
  }

  &__hint {
    margin: 6px 0 0;
    color: #a0a5b0;
    font-size: 12px;
  }

  &__footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 15px;
    margin-top: 21px;
    padding-top: 18px;
    border-top: 1px solid #f0f1f3;

    > span {
      color: #999eaa;
      font-size: 12px;
      line-height: 1.6;
    }
  }
}

@media (max-width: 560px) {
  .email-form__footer {
    align-items: flex-start;
    flex-direction: column-reverse;
  }
}
</style>
