<!--
  登录页装饰视觉面板
  展示品牌 Logo 与自动轮播的风景大图、标语文案，
  为登录/注册页面提供统一的视觉氛围与背景动效。
-->
<script lang="ts" setup>
import {onBeforeUnmount, onMounted, ref} from 'vue'
import AuthBrand from './AuthBrand.vue'

const slides = [
  {
    image:
      'https://images.unsplash.com/photo-1470770841072-f978cf4d019e?auto=format&fit=crop&w=1800&q=85',
    alt: '湖光山色间的日出',
    eyebrow: '开启全新的可能',
    title: '每一次出发，\n都是新的故事。',
  },
  {
    image:
      'https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format&fit=crop&w=1800&q=85',
    alt: '阳光照亮远处的山脉',
    eyebrow: '发现生活的灵感',
    title: '去看更远的风景，\n遇见更好的自己。',
  },
  {
    image:
      'https://images.unsplash.com/photo-1470252649378-9c29740c9fa8?auto=format&fit=crop&w=1800&q=85',
    alt: '晨光洒落在山谷与草地',
    eyebrow: '让美好如期发生',
    title: '心怀热爱，\n日子自有回响。',
  },
]

const activeSlide = ref(0)
let slideTimer: ReturnType<typeof setInterval> | undefined

onMounted(() => {
  slideTimer = setInterval(() => {
    activeSlide.value = (activeSlide.value + 1) % slides.length
  }, 6000)
})

onBeforeUnmount(() => {
  if (slideTimer) clearInterval(slideTimer)
})
</script>

<template>
  <section aria-label="介绍" class="auth-visual">
    <Transition name="visual-fade">
      <img
        :key="slides[activeSlide].image"
        :alt="slides[activeSlide].alt"
        :src="slides[activeSlide].image"
        class="auth-visual__image"
      />
    </Transition>
    <div class="auth-visual__shade"></div>

    <AuthBrand />

    <div class="auth-visual__copy">
      <Transition mode="out-in" name="copy-fade">
        <div :key="activeSlide" class="auth-visual__message">
          <p class="auth-visual__eyebrow" style="user-select: none">
            <span></span>
            {{ slides[activeSlide].eyebrow }}
          </p>
          <h1 style="user-select: none">{{ slides[activeSlide].title }}</h1>
        </div>
      </Transition>

      <div aria-label="选择展示图片" class="slide-controls">
        <button
          v-for="(_, index) in slides"
          :key="index"
          :aria-label="`显示第 ${index + 1} 张图片`"
          :aria-pressed="activeSlide === index"
          :class="{ 'slide-controls__dot--active': activeSlide === index }"
          class="slide-controls__dot"
          type="button"
          @click="activeSlide = index"
        ></button>
      </div>
    </div>

    <p class="auth-visual__footer" style="user-select: none">
      让灵感与每一个新的开始相遇
    </p>
  </section>
</template>

<style lang="scss" scoped>
.auth-visual {
  position: relative;
  display: flex;
  min-height: 100vh;
  min-height: 100svh;
  flex-direction: column;
  justify-content: space-between;
  overflow: hidden;
  padding: 46px clamp(36px, 5vw, 78px) 36px;
  color: #fff;
  background: #273842;

  &__image,
  &__shade {
    position: absolute;
    inset: 0;
    width: 100%;
    height: 100%;
  }

  &__image {
    object-fit: cover;
    object-position: center;
  }

  &__shade {
    background:
      linear-gradient(180deg, rgb(9 20 29 / 30%) 0%, transparent 32%),
      linear-gradient(0deg, rgb(9 20 29 / 62%) 0%, rgb(9 20 29 / 8%) 75%);
  }

  &__copy,
  &__footer {
    position: relative;
    z-index: 1;
  }

  &__copy {
    margin-bottom: clamp(32px, 8vh, 82px);
  }

  &__message {
    min-height: 210px;
  }

  &__eyebrow {
    display: flex;
    align-items: center;
    gap: 10px;
    margin: 0 0 20px;
    color: rgb(255 255 255 / 82%);
    font-size: 13px;
    font-weight: 500;
    letter-spacing: 0.16em;

    span {
      width: 22px;
      height: 1px;
      background: #d7bd93;
    }
  }

  h1 {
    margin: 0;
    color: #fff;
    font-family: Georgia, 'Songti SC', 'SimSun', serif;
    font-size: clamp(40px, 5.2vw, 70px);
    font-weight: 400;
    letter-spacing: 0.025em;
    line-height: 1.28;
    white-space: pre-line;
  }

  &__footer {
    margin: 0;
    color: rgb(255 255 255 / 70%);
    font-size: 12px;
    letter-spacing: 0.1em;
  }
}

.slide-controls {
  display: flex;
  gap: 8px;
  margin-top: 30px;

  &__dot {
    width: 24px;
    height: 3px;
    padding: 0;
    border: 0;
    border-radius: 2px;
    background: rgb(255 255 255 / 42%);
    cursor: pointer;
    transition:
      width 220ms ease,
      background-color 220ms ease;

    &--active {
      width: 42px;
      background: #d7bd93;
    }

    &:focus-visible {
      outline: 2px solid #fff;
      outline-offset: 4px;
    }
  }
}

.visual-fade-enter-active,
.visual-fade-leave-active {
  transition: opacity 700ms ease;
}

.visual-fade-enter-from,
.visual-fade-leave-to {
  opacity: 0;
}

.copy-fade-enter-active,
.copy-fade-leave-active {
  transition:
    opacity 180ms ease,
    transform 180ms ease;
}

.copy-fade-enter-from,
.copy-fade-leave-to {
  opacity: 0;
  transform: translateY(5px);
}

@media (max-width: 1020px) {
  .auth-visual {
    padding-right: 36px;
    padding-left: 36px;
  }
}

@media (max-width: 760px) {
  .auth-visual {
    min-height: 300px;
    height: 36vh;
    height: 36svh;
    max-height: 420px;
    padding: 24px 26px 26px;

    &__copy {
      margin-bottom: 0;
    }

    &__message {
      min-height: 0;
    }

    &__eyebrow {
      margin-bottom: 10px;
      font-size: 10px;
    }

    h1 {
      font-size: clamp(32px, 8vw, 48px);
    }

    &__footer {
      display: none;
    }
  }

  .slide-controls {
    margin-top: 18px;
  }
}

@media (max-width: 420px) {
  .auth-visual {
    min-height: 270px;
    padding: 20px 21px 22px;
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
