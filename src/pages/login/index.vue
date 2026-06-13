<script lang="ts" setup>
import type { LoginRequestData } from "./apis/type"
import ThemeSwitch from "@@/components/ThemeSwitch/index.vue"
import { useSettingsStore } from "@/pinia/stores/settings"
import { useUserStore } from "@/pinia/stores/user"
import { loginApi } from "./apis"

const router = useRouter()

const userStore = useUserStore()

const settingsStore = useSettingsStore()

/** 登录按钮 Loading */
const loading = ref(false)

/** 密码可见性 */
const showPassword = ref(false)

/** 表单错误信息 */
const errors = reactive({
  username: "",
  password: ""
})

/** 登录表单数据 */
const loginFormData: LoginRequestData = reactive({
  username: "",
  password: ""
})

function clearErrors() {
  errors.username = ""
  errors.password = ""
}

function validateForm(): boolean {
  clearErrors()
  let valid = true

  if (!loginFormData.username.trim()) {
    errors.username = "请输入用户名"
    valid = false
  }

  const password = loginFormData.password
  if (!password) {
    errors.password = "请输入密码"
    valid = false
  } else if (password.length < 6 || password.length > 16) {
    errors.password = "长度在 6 到 16 个字符"
    valid = false
  }

  return valid
}

/** 登录 */
function handleLogin() {
  if (!validateForm()) {
    ElMessage.error("表单校验不通过")
    return
  }

  loading.value = true
  loginApi(loginFormData).then(({ data }) => {
    userStore.setToken(data)
    router.push("/")
  }).catch(() => {
    loginFormData.password = ""
  }).finally(() => {
    loading.value = false
  })
}

function handleUsernameInput() {
  if (errors.username) errors.username = ""
}

function handlePasswordInput() {
  if (errors.password) errors.password = ""
}
</script>

<template>
  <div class="login-container">
    <ThemeSwitch v-if="settingsStore.showThemeSwitch" class="theme-switch" />

    <div class="login-panel">
      <div class="login-panel__header">
        <h1 class="login-panel__title">筛查管理</h1>
        <p class="login-panel__subtitle">疾病追踪与筛查数据管理平台</p>
      </div>

      <form class="login-form" @submit.prevent="handleLogin">
        <div class="form-field" :class="{ 'is-error': errors.username }">
          <label class="form-field__label" for="username">用户名</label>
          <div class="form-field__control">
            <svg class="form-field__icon" viewBox="0 0 24 24" aria-hidden="true">
              <path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z" fill="currentColor" />
            </svg>
            <input
              id="username"
              v-model.trim="loginFormData.username"
              class="form-field__input"
              type="text"
              placeholder="请输入用户名"
              tabindex="1"
              autocomplete="username"
              @input="handleUsernameInput"
            >
          </div>
          <p v-if="errors.username" class="form-field__error">{{ errors.username }}</p>
        </div>

        <div class="form-field" :class="{ 'is-error': errors.password }">
          <label class="form-field__label" for="password">密码</label>
          <div class="form-field__control">
            <svg class="form-field__icon" viewBox="0 0 24 24" aria-hidden="true">
              <path d="M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z" fill="currentColor" />
            </svg>
            <input
              id="password"
              v-model.trim="loginFormData.password"
              class="form-field__input"
              :type="showPassword ? 'text' : 'password'"
              placeholder="请输入密码"
              tabindex="2"
              autocomplete="current-password"
              @input="handlePasswordInput"
            >
            <button
              type="button"
              class="form-field__toggle"
              :aria-label="showPassword ? '隐藏密码' : '显示密码'"
              @click="showPassword = !showPassword"
            >
              <svg v-if="showPassword" viewBox="0 0 24 24" aria-hidden="true">
                <path d="M12 7c2.76 0 5 2.24 5 5 0 .65-.13 1.26-.36 1.83l2.92 2.92c1.51-1.26 2.7-2.89 3.43-4.75-1.73-4.39-6-7.5-11-7.5-1.4 0-2.74.25-3.98.7l2.16 2.16C10.74 7.13 11.35 7 12 7zM2 4.27l2.28 2.28.46.46C3.08 8.3 1.78 10.02 1 12c1.73 4.39 6 7.5 11 7.5 1.55 0 3.03-.3 4.38-.84l.42.42L19.73 22 21 20.73 3.27 3 2 4.27zM7.53 9.8l1.55 1.55c-.05.21-.08.43-.08.65 0 1.66 1.34 3 3 3 .22 0 .44-.03.65-.08l1.55 1.55c-.67.33-1.41.53-2.2.53-2.76 0-5-2.24-5-5 0-.79.2-1.53.53-2.2zm4.31-.78l3.15 3.15.02-.16c0-1.66-1.34-3-3-3l-.17.01z" fill="currentColor" />
              </svg>
              <svg v-else viewBox="0 0 24 24" aria-hidden="true">
                <path d="M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z" fill="currentColor" />
              </svg>
            </button>
          </div>
          <p v-if="errors.password" class="form-field__error">{{ errors.password }}</p>
        </div>

        <button class="login-btn" type="submit" :disabled="loading">
          <span v-if="loading" class="login-btn__spinner" aria-hidden="true" />
          <span>{{ loading ? "登录中..." : "登 录" }}</span>
        </button>
      </form>
    </div>

    <footer class="login-footer">
      <a
        href="https://beian.miit.gov.cn/"
        target="_blank"
        rel="noopener noreferrer"
      >桂ICP备2026008858号-1</a>
    </footer>
  </div>
</template>

<style lang="scss" scoped>
.login-container {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  width: 100%;
  min-height: 100%;
  padding: 48px 16px 76px;
  box-sizing: border-box;
  background: #f5f7fa url("@/static/image.png") center / cover no-repeat;

  .theme-switch {
    position: fixed;
    top: 5%;
    right: 5%;
    cursor: pointer;
    z-index: 2;
  }
}

.login-panel {
  width: min(420px, 92vw);
  padding: 36px 32px 32px;
  border-radius: 24px;
  border: 1px solid rgba(255, 255, 255, 0.42);
  background: rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(18px);
  box-shadow:
    0 24px 48px rgba(15, 45, 90, 0.18),
    inset 0 1px 0 rgba(255, 255, 255, 0.35);

  &__header {
    display: flex;
    flex-direction: column;
    align-items: center;
    margin-bottom: 28px;
    text-align: center;
  }

  &__title {
    margin: 0 0 6px;
    font-size: 28px;
    font-weight: 700;
    letter-spacing: 3px;
    color: #fff;
    text-shadow: 0 2px 12px rgba(0, 60, 120, 0.35);
  }

  &__subtitle {
    margin: 0;
    font-size: 13px;
    color: rgba(255, 255, 255, 0.82);
    letter-spacing: 1px;
  }
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.form-field {
  &__label {
    display: block;
    margin-bottom: 8px;
    font-size: 13px;
    font-weight: 500;
    color: rgba(255, 255, 255, 0.9);
    letter-spacing: 0.5px;
  }

  &__control {
    position: relative;
    display: flex;
    align-items: center;
  }

  &__icon {
    position: absolute;
    left: 14px;
    width: 18px;
    height: 18px;
    color: rgba(255, 255, 255, 0.75);
    pointer-events: none;
  }

  &__input {
    width: 100%;
    height: 48px;
    padding: 0 44px 0 42px;
    border: 1px solid rgba(255, 255, 255, 0.38);
    border-radius: 14px;
    background: transparent;
    color: #fff;
    font-size: 15px;
    outline: none;
    transition:
      border-color 0.2s ease,
      box-shadow 0.2s ease,
      background-color 0.2s ease;

    &::placeholder {
      color: rgba(255, 255, 255, 0.55);
    }

    &:focus {
      border-color: rgba(255, 255, 255, 0.72);
      box-shadow: 0 0 0 3px rgba(255, 255, 255, 0.12);
      background: rgba(255, 255, 255, 0.04);
    }
  }

  &__toggle {
    position: absolute;
    right: 8px;
    display: flex;
    align-items: center;
    justify-content: center;
    width: 32px;
    height: 32px;
    padding: 0;
    border: none;
    border-radius: 8px;
    background: transparent;
    color: rgba(255, 255, 255, 0.72);
    cursor: pointer;
    transition: color 0.2s ease, background-color 0.2s ease;

    svg {
      width: 18px;
      height: 18px;
    }

    &:hover {
      color: #fff;
      background: rgba(255, 255, 255, 0.1);
    }
  }

  &__error {
    margin: 6px 0 0;
    font-size: 12px;
    color: #ffd6d6;
    text-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
  }

  &.is-error {
    .form-field__input {
      border-color: rgba(255, 160, 160, 0.85);
    }
  }
}

.login-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  height: 48px;
  margin-top: 6px;
  border: 1px solid rgba(255, 255, 255, 0.55);
  border-radius: 14px;
  background: transparent;
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 4px;
  cursor: pointer;
  transition:
    border-color 0.2s ease,
    background-color 0.2s ease,
    transform 0.15s ease,
    box-shadow 0.2s ease;

  &:hover:not(:disabled) {
    background: rgba(255, 255, 255, 0.12);
    border-color: rgba(255, 255, 255, 0.85);
    box-shadow: 0 8px 24px rgba(15, 45, 90, 0.15);
  }

  &:active:not(:disabled) {
    transform: translateY(1px);
  }

  &:disabled {
    opacity: 0.72;
    cursor: not-allowed;
  }

  &__spinner {
    width: 16px;
    height: 16px;
    border: 2px solid rgba(255, 255, 255, 0.35);
    border-top-color: #fff;
    border-radius: 50%;
    animation: spin 0.7s linear infinite;
  }
}

.login-footer {
  position: fixed;
  bottom: 24px;
  left: 0;
  width: 100%;
  text-align: center;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.78);
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.25);

  a {
    color: inherit;
    text-decoration: none;

    &:hover {
      color: #fff;
      text-decoration: underline;
    }
  }
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
