<!--
  密码修改设置页面
  负责修改当前账号登录密码，提交前校验当前密码、新密码规则与二次确认一致性，
  成功后清空表单并提示重新登录其他设备。
-->
<script lang="ts" setup>
import {reactive, ref} from 'vue'
import {ElMessage, type FormInstance, type FormRules} from 'element-plus'
import SettingsSectionHeading from '@/components/common/SettingsSectionHeading.vue'
import {updatePassword} from '@/api/user'
import {getPasswordValidationError} from '@/utils/authValidation'

interface PasswordForm {
  currentPassword: string
  newPassword: string
  confirmPassword: string
}

const formRef = ref<FormInstance>()
const isSaving = ref(false)
const form = reactive<PasswordForm>({
  currentPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const rules: FormRules<PasswordForm> = {
  currentPassword: [
    { required: true, message: '请输入当前密码', trigger: 'blur' },
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    {
      validator: (_rule, value: string, callback) => {
        const error = getPasswordValidationError(value)
        callback(error ? new Error(error) : undefined)
      },
      trigger: ['blur', 'change'],
    },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule, value: string, callback) => {
        callback(
          value === form.newPassword
            ? undefined
            : new Error('两次输入的密码不一致'),
        )
      },
      trigger: ['blur', 'change'],
    },
  ],
}

// 修改密码前由服务端校验当前密码，前端同时校验新密码和确认值。
async function savePassword() {
  if (!formRef.value || isSaving.value) return

  const isValid = await formRef.value.validate().catch(() => false)
  if (!isValid) return

  isSaving.value = true
  try {
    await updatePassword({
      currentPassword: form.currentPassword,
      newPassword: form.newPassword,
    })
    form.currentPassword = ''
    form.newPassword = ''
    form.confirmPassword = ''
    formRef.value.clearValidate()
    ElMessage.success('密码已更新')
  } catch (error) {
    const message =
      error instanceof Error ? error.message : '密码更新失败，请稍后重试'
    ElMessage.error(message)
  } finally {
    isSaving.value = false
  }
}
</script>

<template>
  <div class="password-settings">
    <!-- 密码提交前同时验证当前密码、新密码规则和二次确认。 -->
    <SettingsSectionHeading
      badge="03"
      description="设置新的登录密码，提升账号安全性。"
      eyebrow="PASSWORD SECURITY"
      title="修改密码"
    />

    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      class="password-form"
      label-position="top"
    >
      <el-form-item label="当前密码" prop="currentPassword">
        <el-input
          v-model="form.currentPassword"
          autocomplete="current-password"
          placeholder="请输入当前密码"
          show-password
          type="password"
        />
      </el-form-item>

      <el-form-item label="新密码" prop="newPassword">
        <el-input
          v-model="form.newPassword"
          autocomplete="new-password"
          placeholder="请输入新密码"
          show-password
          type="password"
        />
      </el-form-item>

      <el-form-item label="确认新密码" prop="confirmPassword">
        <el-input
          v-model="form.confirmPassword"
          autocomplete="new-password"
          placeholder="请再次输入新密码"
          show-password
          type="password"
          @keyup.enter="savePassword"
        />
      </el-form-item>

      <p class="password-form__hint">
        密码为6-18位，至少包含一个英文字母和一个数字；可用符号 @$!%*?&amp;#._-。
      </p>

      <div class="password-form__footer">
        <span>修改后请使用新密码重新登录其他设备。</span>
        <el-button :loading="isSaving" type="primary" @click="savePassword">
          更新密码
        </el-button>
      </div>
    </el-form>
  </div>
</template>

<style lang="scss" scoped>
.password-form {
  max-width: 500px;

  &__hint {
    margin: -4px 0 0;
    color: #969ca8;
    font-size: 12px;
    line-height: 1.7;
  }

  &__footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 15px;
    margin-top: 22px;
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
  .password-form__footer {
    align-items: flex-start;
    flex-direction: column-reverse;
  }
}
</style>
