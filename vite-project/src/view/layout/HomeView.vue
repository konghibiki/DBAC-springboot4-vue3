<!--
  首页工作台页面
  面向当前登录用户展示欢迎区域与账号概览卡片（邮箱、职能、注册时间等），
  数据来源于共享用户资料接口，作为后台管理的主入口。
-->
<script lang="ts" setup>
import {computed} from 'vue'
import {useUserStore} from '@/store'

const userStore = useUserStore()

// #region 首页展示数据
const displayName = computed(
  () => userStore.userInfo?.userName?.trim() || '欢迎回来',
)

const accountStatus = computed(() => {
  const role = userStore.userInfo?.role
  if (Array.isArray(role)) return role.join('、') || '读取中'
  return role || '读取中'
})
const accountEmail = computed(() => userStore.userInfo?.email || '读取中')

const createdAt = computed(() => {
  const value = userStore.userInfo?.createTime
  if (!value) return '读取中'

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value

  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  }).format(date)
})
// #endregion
</script>

<template>
  <div class="home-content">
    <!-- 欢迎区域为工作台提供清晰的页面层级与当前用户上下文。 -->
    <section aria-labelledby="welcome-title" class="welcome-panel">
      <div class="welcome-panel__copy">
        <p class="welcome-panel__eyebrow">
          <span aria-hidden="true"></span>
          NOVA MANAGEMENT CONSOLE
        </p>
        <h1 id="welcome-title">你好，{{ displayName }}</h1>
        <p class="welcome-panel__description">
          欢迎来到后台管理工作台，在这里查看并管理你的账号信息。
        </p>
      </div>
      <div aria-hidden="true" class="welcome-panel__art">
        <span class="welcome-panel__orbit welcome-panel__orbit--outer"></span>
        <span class="welcome-panel__orbit welcome-panel__orbit--inner"></span>
        <span class="welcome-panel__star">
          <svg fill="none" viewBox="0 0 32 32">
            <path
              d="M16 3.5 19.8 12.2 28.5 16l-8.7 3.8L16 28.5l-3.8-8.7L3.5 16l8.7-3.8L16 3.5Z"
              fill="currentColor"
            />
          </svg>
        </span>
      </div>
    </section>

    <!-- 概览卡片展示当前用户资料接口提供的核心信息。 -->
    <section aria-labelledby="account-title" class="account-section">
      <div class="section-heading">
        <div>
          <p class="section-heading__eyebrow">ACCOUNT</p>
          <h2 id="account-title">账号概览</h2>
        </div>
        <span class="section-heading__caption">个人资料摘要</span>
      </div>

      <div class="account-grid">
        <article class="account-card">
          <span class="account-card__icon account-card__icon--name">
            <svg aria-hidden="true" fill="none" viewBox="0 0 24 24">
              <circle cx="12" cy="8" r="3.5" />
              <path d="M4.5 20a7.5 7.5 0 0 1 15 0" />
            </svg>
          </span>
          <div>
            <p class="account-card__label">账号邮箱</p>
            <p class="account-card__value">{{ accountEmail }}</p>
          </div>
        </article>

        <article class="account-card">
          <span class="account-card__icon account-card__icon--status">
            <svg aria-hidden="true" fill="none" viewBox="0 0 24 24">
              <path
                d="m5 12.5 4.3 4.3L19 7"
                stroke="currentColor"
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="1.8"
              />
              <circle
                cx="12"
                cy="12"
                r="9"
                stroke="currentColor"
                stroke-width="1.5"
              />
            </svg>
          </span>
          <div>
            <p class="account-card__label">当前职能</p>
            <p class="account-card__value account-card__value--status">
              <span></span>
              {{ accountStatus }}
            </p>
          </div>
        </article>

        <article class="account-card">
          <span class="account-card__icon account-card__icon--date">
            <svg aria-hidden="true" fill="none" viewBox="0 0 24 24">
              <rect
                height="15.5"
                rx="2.5"
                stroke="currentColor"
                stroke-width="1.5"
                width="17"
                x="3.5"
                y="5"
              />
              <path
                d="M7.5 3.5v3M16.5 3.5v3M3.5 9.5h17"
                stroke="currentColor"
                stroke-linecap="round"
                stroke-width="1.5"
              />
            </svg>
          </span>
          <div>
            <p class="account-card__label">加入时间</p>
            <p class="account-card__value">{{ createdAt }}</p>
          </div>
        </article>
      </div>
    </section>

    <section aria-label="管理提示" class="home-note">
      <span aria-hidden="true" class="home-note__accent"></span>
      <div>
        <h2>管理你的个人账户</h2>
        <p>点击右上角头像，即可访问个人资料、账号绑定与密码管理入口。</p>
      </div>
      <span class="home-note__label">NOVA · ADMIN</span>
    </section>
  </div>
</template>

<style lang="scss" scoped>
.home-content {
  width: min(1120px, calc(100% - 48px));
  margin: 0 auto;
  padding: 42px 0 64px;
}

.welcome-panel {
  position: relative;
  display: flex;
  min-height: 248px;
  align-items: center;
  overflow: hidden;
  padding: 42px clamp(28px, 5vw, 58px);
  border: 1px solid rgb(255 255 255 / 12%);
  border-radius: 22px;
  background:
    radial-gradient(circle at 82% 50%, rgb(185 158 117 / 27%), transparent 24%),
    linear-gradient(118deg, #293343 0%, #354358 62%, #46546a 100%);
  box-shadow: 0 18px 42px rgb(37 48 65 / 12%);
  color: #fff;
}

.welcome-panel__copy {
  position: relative;
  z-index: 1;
  max-width: 620px;
}

.welcome-panel__eyebrow {
  display: flex;
  align-items: center;
  gap: 9px;
  margin: 0 0 17px;
  color: #d7bd93;
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.18em;

  span {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: #d7bd93;
    box-shadow: 0 0 0 4px rgb(215 189 147 / 14%);
  }
}

.welcome-panel h1 {
  margin: 0;
  color: #fff;
  font-size: clamp(27px, 4vw, 36px);
  font-weight: 600;
  letter-spacing: -0.035em;
  line-height: 1.3;
}

.welcome-panel__description {
  max-width: 500px;
  margin: 13px 0 0;
  color: rgb(255 255 255 / 68%);
  font-size: 13px;
  line-height: 1.8;
}

.welcome-panel__art {
  position: absolute;
  top: 50%;
  right: clamp(30px, 8vw, 105px);
  width: 170px;
  height: 170px;
  transform: translateY(-50%);
}

.welcome-panel__orbit {
  position: absolute;
  inset: 0;
  border: 1px solid rgb(215 189 147 / 28%);
  border-radius: 50%;

  &--inner {
    inset: 23px;
    border-color: rgb(255 255 255 / 20%);
  }
}

.welcome-panel__star {
  position: absolute;
  top: 50%;
  left: 50%;
  display: grid;
  width: 54px;
  height: 54px;
  place-items: center;
  border: 1px solid rgb(255 255 255 / 28%);
  border-radius: 17px;
  background: rgb(255 255 255 / 10%);
  box-shadow: 0 8px 26px rgb(14 22 34 / 20%);
  color: #e3cda9;
  transform: translate(-50%, -50%) rotate(-8deg);

  svg {
    width: 28px;
    height: 28px;
  }
}

.account-section {
  margin-top: 40px;
}

.section-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 17px;

  h2 {
    margin: 4px 0 0;
    color: #293343;
    font-size: 20px;
    font-weight: 600;
    letter-spacing: -0.02em;
  }

  &__eyebrow {
    margin: 0;
    color: #b3976f;
    font-size: 9px;
    font-weight: 700;
    letter-spacing: 0.19em;
  }

  &__caption {
    color: #a0a5b0;
    font-size: 11px;
  }
}

.account-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.account-card {
  display: flex;
  min-height: 118px;
  align-items: center;
  gap: 15px;
  padding: 22px;
  border: 1px solid #eceef1;
  border-radius: 14px;
  background: rgb(255 255 255 / 92%);
  box-shadow: 0 5px 16px rgb(34 42 57 / 3%);

  &__icon {
    display: grid;
    width: 42px;
    height: 42px;
    flex: 0 0 auto;
    place-items: center;
    border-radius: 12px;

    svg {
      width: 21px;
      height: 21px;
      stroke: currentColor;
      stroke-linecap: round;
    }

    &--name {
      background: #f2f0eb;
      color: #9e855f;
    }

    &--status {
      background: #edf5ef;
      color: #59846a;
    }

    &--date {
      background: #eff2f7;
      color: #71819a;
    }
  }

  &__label {
    margin: 0 0 7px;
    color: #9298a5;
    font-size: 11px;
  }

  &__value {
    max-width: 100%;
    margin: 0;
    overflow: hidden;
    color: #343c4b;
    font-size: 13px;
    font-weight: 600;
    text-overflow: ellipsis;
    white-space: nowrap;

    &--status {
      display: flex;
      align-items: center;
      gap: 7px;
      color: #59846a;

      span {
        width: 6px;
        height: 6px;
        border-radius: 50%;
        background: currentColor;
      }
    }
  }
}

.home-note {
  position: relative;
  display: flex;
  min-height: 98px;
  align-items: center;
  gap: 17px;
  margin-top: 20px;
  overflow: hidden;
  padding: 20px 24px;
  border: 1px solid #ece8df;
  border-radius: 14px;
  background: linear-gradient(100deg, #fff 0%, #fbfaf7 100%);

  &__accent {
    width: 3px;
    align-self: stretch;
    border-radius: 3px;
    background: #b3976f;
  }

  h2 {
    margin: 0;
    color: #414958;
    font-size: 13px;
    font-weight: 600;
  }

  p {
    margin: 6px 0 0;
    color: #9298a5;
    font-size: 11px;
    line-height: 1.6;
  }

  &__label {
    margin-left: auto;
    color: #c1b399;
    font-size: 9px;
    font-weight: 600;
    letter-spacing: 0.14em;
  }
}

@media (max-width: 760px) {
  .home-content {
    width: min(100% - 32px, 560px);
    padding-top: 27px;
  }

  .welcome-panel {
    min-height: 220px;
    padding: 30px 26px;
  }

  .welcome-panel__art {
    right: -54px;
    opacity: 0.45;
  }

  .welcome-panel__copy {
    max-width: 88%;
  }

  .account-section {
    margin-top: 31px;
  }

  .account-grid {
    grid-template-columns: 1fr;
    gap: 10px;
  }

  .account-card {
    min-height: 92px;
    padding: 17px;
  }
}

@media (max-width: 420px) {
  .section-heading__caption {
    display: none;
  }

  .home-note {
    align-items: flex-start;
    padding: 18px;
  }

  .home-note__label {
    display: none;
  }
}
</style>
