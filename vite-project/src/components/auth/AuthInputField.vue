<!--
  认证表单输入框组件
  封装登录/注册页面的单字段输入控制：图标、标签、提示文案、
  错误态与密码可见性切换，通过 v-model 与父级表单双向绑定，
  供登录页面统一各字段的交互与样式。
-->
<script lang="ts" setup>
withDefaults(
  defineProps<{
    modelValue: string
    id: string
    name: string
    label: string
    type: 'text' | 'email' | 'password'
    placeholder: string
    autocomplete?: string
    hint?: string
    error?: string
    icon: 'user' | 'email' | 'lock'
    passwordVisible?: boolean
    hasSuggestions?: boolean
  }>(),
  {
    autocomplete: undefined,
    hint: '',
    error: '',
    passwordVisible: false,
    hasSuggestions: false,
  },
)

const emit = defineEmits<{
  'update:modelValue': [value: string]
  focus: [event: FocusEvent]
  blur: [event: FocusEvent]
  input: [event: Event]
  'toggle-password': []
}>()

function updateValue(event: Event) {
  const input = event.target as HTMLInputElement
  emit('update:modelValue', input.value)
  emit('input', event)
}
</script>

<template>
  <div
    :class="{
      'auth-field--suggestions-open': hasSuggestions,
    }"
    class="auth-field"
  >
    <label :for="id" class="auth-field__label">{{ label }}</label>
    <span
      :class="{ 'auth-field__control--invalid': error }"
      class="auth-field__control"
    >
      <svg
        v-if="icon === 'user'"
        aria-hidden="true"
        fill="none"
        viewBox="0 0 24 24"
      >
        <path
          d="M20 21a8 8 0 0 0-16 0M12 13a5 5 0 1 0 0-10 5 5 0 0 0 0 10Z"
          stroke="currentColor"
          stroke-linecap="round"
          stroke-width="1.7"
        />
      </svg>
      <svg
        v-else-if="icon === 'email'"
        aria-hidden="true"
        fill="none"
        viewBox="0 0 24 24"
      >
        <rect
          height="14"
          rx="3"
          stroke="currentColor"
          stroke-width="1.7"
          width="18"
          x="3"
          y="5"
        />
        <path
          d="m4 7 8 6 8-6"
          stroke="currentColor"
          stroke-linecap="round"
          stroke-linejoin="round"
          stroke-width="1.7"
        />
      </svg>
      <svg v-else aria-hidden="true" fill="none" viewBox="0 0 24 24">
        <rect
          height="11"
          rx="3"
          stroke="currentColor"
          stroke-width="1.7"
          width="16"
          x="4"
          y="10"
        />
        <path
          d="M8 10V7a4 4 0 0 1 8 0v3m-4 5v2"
          stroke="currentColor"
          stroke-linecap="round"
          stroke-width="1.7"
        />
      </svg>
      <input
        :id="id"
        :aria-describedby="
          error ? `${id}-error` : hint ? `${id}-hint` : undefined
        "
        :aria-invalid="Boolean(error)"
        :autocomplete="autocomplete"
        :name="name"
        :placeholder="placeholder"
        :type="type === 'password' && passwordVisible ? 'text' : type"
        :value="modelValue"
        @blur="emit('blur', $event)"
        @focus="emit('focus', $event)"
        @input="updateValue"
      />
      <button
        v-if="type === 'password'"
        :aria-label="passwordVisible ? '隐藏密码' : '显示密码'"
        class="auth-field__toggle"
        type="button"
        @click="emit('toggle-password')"
        @mousedown.prevent
      >
        {{ passwordVisible ? '隐藏' : '显示' }}
      </button>
    </span>

    <slot name="suggestions"></slot>

    <span v-if="hint && !error" :id="`${id}-hint`" class="auth-field__hint">
      {{ hint }}
    </span>
    <span v-if="error" :id="`${id}-error`" class="auth-field__error">
      {{ error }}
    </span>
  </div>
</template>

<style lang="scss" scoped>
.auth-field {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 9px;

  &--suggestions-open {
    z-index: 2;
  }

  &__label {
    color: #555b69;
    font-size: 12px;
    font-weight: 600;
  }

  &__control {
    display: flex;
    height: 50px;
    align-items: center;
    gap: 12px;
    padding: 0 15px;
    border: 1px solid #e8e9ed;
    border-radius: 9px;
    background: rgb(255 255 255 / 88%);
    transition:
      border-color 180ms ease,
      box-shadow 180ms ease;

    &:focus-within {
      border-color: #b69a70;
      box-shadow: 0 0 0 3px rgb(182 154 112 / 12%);
    }

    &--invalid {
      border-color: #d47777;

      &:focus-within {
        box-shadow: 0 0 0 3px rgb(212 119 119 / 12%);
      }
    }

    > svg {
      width: 18px;
      height: 18px;
      flex: 0 0 auto;
      color: #a2a6b0;
    }

    input {
      width: 100%;
      min-width: 0;
      height: 100%;
      padding: 0;
      border: 0;
      outline: 0;
      background: transparent;
      color: #303646;
      font: inherit;
      font-size: 13px;

      &::placeholder {
        color: #b8bbc3;
      }
    }
  }

  &__hint,
  &__error {
    margin: -2px 1px 0;
    font-size: 11px;
    line-height: 1.5;
  }

  &__hint {
    color: #9298a5;
  }

  &__error {
    color: #c45656;
  }

  &__toggle {
    flex: 0 0 auto;
    padding: 4px 0 4px 6px;
    border: 0;
    background: transparent;
    color: #979daa;
    cursor: pointer;
    font-size: 11px;

    &:hover {
      color: #806541;
    }
  }
}
</style>
