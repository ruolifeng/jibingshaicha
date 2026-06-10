<script lang="ts" setup>
import Notify from "@@/components/Notify/index.vue"
import Screenfull from "@@/components/Screenfull/index.vue"
import SearchMenu from "@@/components/SearchMenu/index.vue"
import ThemeSwitch from "@@/components/ThemeSwitch/index.vue"
import { useDevice } from "@@/composables/useDevice"
import { useLayoutMode } from "@@/composables/useLayoutMode"
import { resolveFileUrl } from "@@/utils/attachment"
import { Setting, UserFilled } from "@element-plus/icons-vue"
import { useAppStore } from "@/pinia/stores/app"
import { useSettingsStore } from "@/pinia/stores/settings"
import { useUserStore } from "@/pinia/stores/user"
import { Breadcrumb, Hamburger, Sidebar } from "../index"

const { isMobile } = useDevice()

const { isTop } = useLayoutMode()

const router = useRouter()

const appStore = useAppStore()

const userStore = useUserStore()

const settingsStore = useSettingsStore()

const { showThemeSwitch, showScreenfull, showSearchMenu, showSettings } = storeToRefs(settingsStore)

const avatarUrl = computed(() => resolveFileUrl(userStore.avatar))

/** 切换侧边栏 */
function toggleSidebar() {
  appStore.toggleSidebar(false)
}

/** 进入个人信息 */
function openProfile() {
  router.push("/profile")
}

/** 登出 */
function logout() {
  userStore.logout()
  router.push("/login")
}
</script>

<template>
  <div class="navigation-bar">
    <Hamburger
      v-if="!isTop || isMobile"
      :is-active="appStore.sidebar.opened"
      class="hamburger"
      @toggle-click="toggleSidebar"
    />
    <Breadcrumb v-if="!isTop || isMobile" class="breadcrumb" />
    <Sidebar v-if="isTop && !isMobile" class="sidebar" />
    <div class="right-menu">
      <SearchMenu v-if="showSearchMenu" class="right-menu-item" />
      <Screenfull v-if="showScreenfull" class="right-menu-item" />
      <ThemeSwitch v-if="showThemeSwitch" class="right-menu-item" />
      <Notify class="right-menu-item" />
      <el-tooltip v-if="showSettings" effect="dark" content="布局设置" placement="bottom">
        <el-icon class="right-menu-item settings-icon" :size="20" @click="appStore.toggleRightPanel">
          <Setting />
        </el-icon>
      </el-tooltip>
      <el-dropdown>
        <div class="right-menu-item user">
          <el-avatar :src="avatarUrl" :icon="UserFilled" :size="30" />
          <span>{{ userStore.realName || userStore.username }}</span>
          <el-tag v-if="userStore.roleName" size="small" type="info" class="ml-2">
            {{ userStore.roleName }}
          </el-tag>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item disabled>
              {{ userStore.orgName || "未设置机构" }}
            </el-dropdown-item>
            <el-dropdown-item divided @click="openProfile">
              个人信息
            </el-dropdown-item>
            <el-dropdown-item divided @click="logout">
              退出登录
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.navigation-bar {
  height: var(--v3-navigationbar-height);
  overflow: hidden;
  color: var(--v3-navigationbar-text-color);
  display: flex;
  justify-content: space-between;
  .hamburger {
    display: flex;
    align-items: center;
    height: 100%;
    padding: 0 15px;
    cursor: pointer;
  }
  .breadcrumb {
    flex: 1;
    // 参考 Bootstrap 的响应式设计将宽度设置为 576
    @media screen and (max-width: 576px) {
      display: none;
    }
  }
  .sidebar {
    flex: 1;
    // 设置 min-width 是为了让 Sidebar 里的 el-menu 宽度自适应
    min-width: 0px;
    :deep(.el-menu) {
      background-color: transparent;
    }
    :deep(.el-sub-menu) {
      &.is-active {
        .el-sub-menu__title {
          color: var(--el-color-primary);
        }
      }
    }
  }
  .right-menu {
    margin-right: 10px;
    height: 100%;
    display: flex;
    align-items: center;
    &-item {
      margin: 0 10px;
      cursor: pointer;
      &:last-child {
        margin-left: 20px;
      }
    }
    .ml-2 {
      margin-left: 8px;
    }
    .user {
      display: flex;
      align-items: center;
      .el-avatar {
        margin-right: 10px;
      }
      span {
        font-size: 16px;
      }
    }
    .settings-icon {
      cursor: pointer;
    }
  }
}
</style>
