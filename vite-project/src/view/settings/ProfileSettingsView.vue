<!--
  个人资料设置页面
  负责展示与编辑当前用户的用户名、性别与头像，
  通过 AvatarCropper 完成头像裁剪，最终以 FormData 提交个人资料，
  成功后更新共享的用户资料状态。
-->
<script lang="ts" setup>
import {onMounted, reactive, ref, watch} from 'vue'
import {ElMessage, type FormInstance, type FormRules} from 'element-plus'
import AvatarCropper from '@/components/settings/AvatarCropper.vue'
import SettingsSectionHeading from '@/components/common/SettingsSectionHeading.vue'
import {updateUserProfile} from '@/api/user'
import {useUserStore} from '@/store'
import {GENDER_OPTIONS, getGenderCode} from '@/utils/user'

interface ProfileForm {
  userName: string
  gender: number
}

const userStore = useUserStore()
const formRef = ref<FormInstance>()
const isLoadingProfile = ref(false)
const isSaving = ref(false)
const avatarFile = ref<Blob | null>(null)

const form = reactive<ProfileForm>({
  userName: '',
  gender: 0,
})

const rules: FormRules<ProfileForm> = {
  userName: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 1, max: 18, message: '用户名长度为1-18位', trigger: 'blur' },
  ],
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }],
}

watch(
  () => userStore.userInfo,
  (info) => {
    if (!info) return
    form.userName = info.userName ?? ''
    form.gender = getGenderCode(info.gender)
  },
  { immediate: true },
)

// 设置页可独立刷新访问，因此资料未缓存时主动读取。
onMounted(async () => {
  if (userStore.userInfo) return

  isLoadingProfile.value = true
  try {
    await userStore.loadUserInfo()
  } catch (error) {
    const message =
      error instanceof Error ? error.message : '获取用户信息失败，请稍后重试'
    ElMessage.error(message)
  } finally {
    isLoadingProfile.value = false
  }
})

function handleAvatarCrop(blob: Blob | null) {
  avatarFile.value = blob
}

async function saveProfile() {
  if (!formRef.value || isSaving.value) return

  const isValid = await formRef.value.validate().catch(() => false)
  if (!isValid) return

  isSaving.value = true
  try {
    const formData = new FormData()
    formData.append('userName', form.userName.trim())
    formData.append('gender', String(form.gender))
    if (avatarFile.value) {
      formData.append('avatar', avatarFile.value, 'avatar.png')
    }

    const response = await updateUserProfile(formData)
    userStore.userInfo = response.data.data
    avatarFile.value = null
    ElMessage.success('个人资料已更新')
  } catch (error) {
    const message =
      error instanceof Error ? error.message : '保存失败，请稍后重试'
    ElMessage.error(message)
  } finally {
    isSaving.value = false
  }
}
</script>

<template>
  <div class="profile-settings">
    <!-- 基础信息与头像编辑共同提交，避免页面状态与用户资料脱节。 -->
    <SettingsSectionHeading
      badge="01"
      description="更新你的个人资料和头像展示。"
      eyebrow="PROFILE"
      title="修改个人信息"
    />

    <el-skeleton v-if="isLoadingProfile" :rows="5" animated />
    <el-form
      v-else
      ref="formRef"
      :model="form"
      :rules="rules"
      class="profile-form"
      label-position="top"
      @submit.prevent="saveProfile"
    >
      <div class="profile-form__fields">
        <el-form-item label="用户名" prop="userName">
          <el-input
            v-model="form.userName"
            autocomplete="username"
            maxlength="18"
            placeholder="请输入用户名"
            show-word-limit
          />
          <p class="profile-form__hint">用户名长度为1-18位。</p>
        </el-form-item>

        <el-form-item label="性别" prop="gender">
          <el-select v-model="form.gender" class="profile-form__select">
            <el-option
              v-for="option in GENDER_OPTIONS"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="头像">
          <AvatarCropper
            :current-avatar="userStore.userInfo?.avatar"
            @crop="handleAvatarCrop"
          />
        </el-form-item>
      </div>

      <div class="profile-form__footer">
        <span>更新后刷新页面查看最新资料。</span>
        <el-button :loading="isSaving" type="primary" @click="saveProfile">
          保存修改
        </el-button>
      </div>
    </el-form>
  </div>
</template>

<style lang="scss" scoped>
.profile-settings {
  padding: 20px;
}

.profile-form {
  max-width: 760px;

  &__fields {
    display: flex;
    flex-direction: column;
    max-width: 600px;
    gap: 5px;
  }

  &__select {
    width: 100%;
    max-width: 280px;
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

@media (max-width: 600px) {
  .profile-form__footer {
    align-items: flex-start;
    flex-direction: column-reverse;
  }
}
</style>
