<template>
  <div class="header-wrap">
    <el-menu mode="horizontal" :default-active="activeIndex"
      background-color="transparent" text-color="#fff" active-text-color="#fff"
      router class="header-menu">
      <el-menu-item index="logo" class="logo-item" @click="$router.push('/')">
        人才公共服务平台
      </el-menu-item>
      <el-menu-item index="/">首页</el-menu-item>
      <el-menu-item v-if="store.canBrowseTalents" index="/talents">人才库</el-menu-item>
      <el-menu-item v-if="store.canBrowseTalents" index="/talent-showcase">人才展示</el-menu-item>
      <el-menu-item index="/jobs">岗位</el-menu-item>
      <el-menu-item index="/match">智能匹配</el-menu-item>
      <el-menu-item index="/courses">终生学习</el-menu-item>
      <el-menu-item index="/policies">资讯政策公告</el-menu-item>
      <el-menu-item index="/data-screen">数据大屏</el-menu-item>
      <el-menu-item v-if="isAdmin" index="/admin">后台</el-menu-item>
    </el-menu>
    <div class="header-right">
      <template v-if="isLoggedIn">
        <el-dropdown trigger="click" placement="bottom-end" @visible-change="onNotificationDropdownVisible">
          <el-badge :value="unreadCount > 0 ? (unreadCount > 99 ? '99+' : unreadCount) : ''" :hidden="unreadCount === 0" class="notification-badge">
            <el-icon :size="22" class="bell-icon"><Bell /></el-icon>
          </el-badge>
          <template #dropdown>
            <div class="notification-dropdown">
              <div class="notification-dropdown-header">
                <span>消息通知</span>
                <el-button v-if="unreadCount > 0" link type="primary" size="small" @click="markAllRead">全部已读</el-button>
              </div>
              <div class="notification-list">
                <template v-if="recentNotifications.length">
                  <div
                    v-for="n in recentNotifications"
                    :key="n.id"
                    class="notification-item"
                    :class="{ unread: !n.read }"
                    @click="handleNotificationClick(n)"
                  >
                    <div class="notification-title">{{ n.title }}</div>
                    <div class="notification-content">{{ n.content }}</div>
                    <div class="notification-time">{{ formatTime(n.createTime) }}</div>
                  </div>
                </template>
                <el-empty v-else description="暂无消息" :image-size="48" />
              </div>
              <div class="notification-dropdown-footer">
                <el-button text type="primary" size="small" @click="$router.push('/notifications')">查看全部</el-button>
              </div>
            </div>
          </template>
        </el-dropdown>
        <el-dropdown @command="handleCommand">
          <span class="user-info">
            <el-avatar :size="28" :src="avatarUrl" icon="UserFilled" class="user-avatar" />
            {{ username }}
            <span class="role-badge">{{ roleLabel }}</span>
            <el-icon style="margin-left:4px"><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item v-if="isTalent" command="my-profile">我的档案</el-dropdown-item>
              <el-dropdown-item v-if="isTalent" command="career-assessment">AI 能力评估</el-dropdown-item>
              <el-dropdown-item v-if="isEnterprise" command="my-company">企业信息</el-dropdown-item>
              <el-dropdown-item v-if="isEnterprise" command="company-jobs">岗位管理</el-dropdown-item>
              <el-dropdown-item v-if="isTalent || isEnterprise" command="my-applications">
                {{ isTalent ? '我的申请' : '收到的申请' }}
              </el-dropdown-item>
              <el-dropdown-item command="notifications">消息中心</el-dropdown-item>
              <el-dropdown-item command="my-certificates">我的证书</el-dropdown-item>
              <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </template>
      <template v-else>
        <el-button text style="color:#fff" @click="$router.push('/login')">登录</el-button>
        <el-button type="warning" size="small" round class="register-btn" @click="$router.push('/register')">注册</el-button>
      </template>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onBeforeUnmount, watch, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { Bell } from '@element-plus/icons-vue'
import { notificationApi } from '@/api'

const store = useUserStore()
const route = useRoute()
const router = useRouter()

const unreadCount = ref(0)
const recentNotifications = ref([])
let pollTimer = null
let authInvalidHandled = false

const activeIndex = computed(() => route.path)
const isLoggedIn = computed(() => store.isLoggedIn)
const isAdmin = computed(() => store.isAdmin)
const isTalent = computed(() => store.isTalent)
const isEnterprise = computed(() => store.isEnterprise)
const username = computed(() => store.username)
const avatarUrl = computed(() => store.avatarUrl || undefined)
const roleLabel = computed(() => ({
  ADMIN: '管理员', TALENT: '人才', ENTERPRISE: '企业'
}[store.role] || store.role))

function handleAuthFailure(error) {
  if (error?.response?.status !== 401 || authInvalidHandled) return false
  authInvalidHandled = true
  store.logout()
  stopPolling()
  ElMessage.error('登录状态已失效，请重新登录')
  router.replace({ path: '/login', query: { redirect: route.fullPath } })
  return true
}

async function fetchUnreadCount() {
  if (!store.isLoggedIn) return
  try {
    const res = await notificationApi.unreadCount()
    const n = res?.data
    unreadCount.value = typeof n === 'number' ? n : (parseInt(n, 10) || 0)
  } catch (error) {
    handleAuthFailure(error)
  }
}

async function fetchRecentNotifications() {
  if (!store.isLoggedIn) return
  try {
    const res = await notificationApi.list({ page: 0, size: 8 })
    const data = res?.data
    recentNotifications.value = Array.isArray(data) ? data : (data?.content ?? [])
  } catch (error) {
    handleAuthFailure(error)
  }
}

function onNotificationDropdownVisible(visible) {
  if (visible) {
    fetchRecentNotifications()
    fetchUnreadCount()
    if (!pollTimer) {
      pollTimer = setInterval(fetchUnreadCount, 45000)
    }
  }
}

async function markAllRead() {
  try {
    await notificationApi.markAllRead()
    unreadCount.value = 0
    fetchRecentNotifications()
  } catch (error) {
    handleAuthFailure(error)
  }
}

async function handleNotificationClick(n) {
  if (!n.read) {
    try {
      await notificationApi.markRead(n.id)
      n.read = true
      if (unreadCount.value > 0) unreadCount.value--
    } catch (error) {
      handleAuthFailure(error)
    }
  }
}

function formatTime(t) {
  if (!t) return ''
  const d = new Date(t)
  const now = new Date()
  const diff = now - d
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  return d.toLocaleDateString('zh-CN')
}

function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
  unreadCount.value = 0
}

onMounted(() => {
  if (store.isLoggedIn) fetchUnreadCount()
})

onBeforeUnmount(stopPolling)

watch(() => store.isLoggedIn, (loggedIn) => {
  authInvalidHandled = false
  if (loggedIn) fetchUnreadCount()
  else stopPolling()
})

function handleCommand(cmd) {
  if (cmd === 'logout') {
    store.logout()
    ElMessage.success('已退出登录')
    router.push('/login')
  }   else if (cmd === 'my-profile') router.push('/my-profile')
  else if (cmd === 'my-company') router.push('/company')
  else if (cmd === 'company-jobs') router.push('/company/jobs')
  else if (cmd === 'my-applications') router.push('/my-applications')
  else if (cmd === 'notifications') router.push('/notifications')
  else if (cmd === 'my-certificates') router.push('/certificates')
  else if (cmd === 'career-assessment') router.push('/career-assessment')
}
</script>

<style scoped>
.header-wrap {
  display: flex;
  align-items: center;
  background: linear-gradient(135deg, var(--tp-primary-dark) 0%, var(--tp-primary) 100%);
  height: 60px;
  position: sticky;
  top: 0;
  z-index: 999;
  padding-right: 20px;
  box-shadow: 0 2px 8px rgba(13, 71, 161, 0.2);
}
.header-menu {
  flex: 1;
  border-bottom: none !important;
  height: 60px;
}
.header-menu .el-menu-item:hover { background: rgba(255, 255, 255, 0.1) !important; }
.header-menu .el-menu-item.is-active { background: rgba(255, 255, 255, 0.2) !important; border-bottom: none !important; }
.logo-item { font-size: 17px !important; font-weight: bold !important; letter-spacing: 1px; }
.header-right { display: flex; align-items: center; gap: 8px; flex-shrink: 0; }
.user-avatar { margin-right: 6px; background: #fff !important; color: var(--tp-primary) !important; }
.user-info {
  display: flex; align-items: center; color: #fff; cursor: pointer;
  padding: 6px 12px; border-radius: var(--tp-radius-sm); font-size: 14px;
  transition: background 0.2s;
}
.user-info:hover { background: rgba(255, 255, 255, 0.15); }
.role-badge {
  background: rgba(255, 255, 255, 0.25);
  border-radius: 4px;
  padding: 2px 8px;
  font-size: 12px;
  margin-left: 8px;
}
.register-btn { border-color: #fff; background: rgba(255, 255, 255, 0.2) !important; color: #fff !important; }
.register-btn:hover { background: rgba(255, 255, 255, 0.35) !important; color: #fff !important; border-color: #fff !important; }

.notification-badge { cursor: pointer; margin-right: 8px; }
.notification-badge .el-badge__content { font-size: 11px; }
.bell-icon { color: #fff; padding: 4px; border-radius: 4px; }
.bell-icon:hover { background: rgba(255, 255, 255, 0.15); }

.notification-dropdown { min-width: 320px; max-width: 380px; }
.notification-dropdown-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 12px 16px; border-bottom: 1px solid var(--el-border-color-lighter);
  font-weight: 600; font-size: 14px;
}
.notification-list { max-height: 320px; overflow-y: auto; }
.notification-item {
  padding: 12px 16px; border-bottom: 1px solid var(--el-border-color-lighter);
  cursor: pointer; transition: background 0.2s;
}
.notification-item:hover { background: var(--el-fill-color-light); }
.notification-item.unread { background: var(--el-color-primary-light-9); }
.notification-title { font-weight: 500; font-size: 13px; margin-bottom: 4px; }
.notification-content { font-size: 12px; color: var(--el-text-color-regular); line-height: 1.4; }
.notification-time { font-size: 11px; color: var(--el-text-color-secondary); margin-top: 4px; }
.notification-dropdown-footer {
  padding: 8px 16px; border-top: 1px solid var(--el-border-color-lighter);
  text-align: center;
}
</style>
