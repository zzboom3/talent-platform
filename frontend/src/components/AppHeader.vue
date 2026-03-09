<template>
  <div class="header-wrap">
    <el-menu
      mode="horizontal"
      :default-active="activeIndex"
      background-color="#1a73e8"
      text-color="#fff"
      active-text-color="#ffd04b"
      router
      class="header-menu"
    >
      <!-- Logo（不参与路由高亮） -->
      <el-menu-item index="logo" class="logo-item" @click="$router.push('/')">
        🏛 人才公共服务平台
      </el-menu-item>

      <el-menu-item index="/">首页</el-menu-item>
      <el-menu-item index="/talents">人才库</el-menu-item>
      <el-menu-item index="/jobs">岗位</el-menu-item>
      <el-menu-item index="/match">智能匹配</el-menu-item>
      <el-menu-item index="/courses">终生学习</el-menu-item>
      <el-menu-item index="/policies">政策法规</el-menu-item>
      <el-menu-item v-if="isAdmin" index="/admin">后台管理</el-menu-item>
    </el-menu>

    <!-- 右侧用户区（独立 flex 布局，不放入 el-menu） -->
    <div class="header-right">
      <template v-if="isLoggedIn">
        <el-dropdown @command="handleCommand">
          <span class="user-info">
            <el-avatar :size="28" icon="UserFilled" style="margin-right:6px;background:#fff;color:#1a73e8" />
            {{ username }}
            <span class="role-badge">{{ roleLabel }}</span>
            <el-icon style="margin-left:4px"><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item v-if="isTalent" command="my-profile">我的档案</el-dropdown-item>
              <el-dropdown-item v-if="isEnterprise" command="my-company">企业信息</el-dropdown-item>
              <el-dropdown-item v-if="isTalent || isEnterprise" command="my-applications">
                {{ isTalent ? '我的申请' : '收到的申请' }}
              </el-dropdown-item>
              <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </template>
      <template v-else>
        <el-button text style="color:#fff" @click="$router.push('/login')">登录</el-button>
        <el-button type="warning" size="small" round @click="$router.push('/register')">注册</el-button>
      </template>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const store = useUserStore()
const route = useRoute()
const router = useRouter()

const activeIndex = computed(() => route.path)
const isLoggedIn = computed(() => store.isLoggedIn)
const isAdmin = computed(() => store.isAdmin)
const isTalent = computed(() => store.isTalent)
const isEnterprise = computed(() => store.isEnterprise)
const username = computed(() => store.username)
const roleLabel = computed(() => ({
  ADMIN: '管理员', TALENT: '人才', ENTERPRISE: '企业'
}[store.role] || store.role))

function handleCommand(cmd) {
  if (cmd === 'logout') {
    store.logout()
    ElMessage.success('已退出登录')
    router.push('/login')
  } else if (cmd === 'my-profile') {
    router.push('/talents')
  } else if (cmd === 'my-company') {
    router.push('/company')
  } else if (cmd === 'my-applications') {
    router.push('/my-applications')
  }
}
</script>

<style scoped>
.header-wrap {
  display: flex;
  align-items: center;
  background: #1a73e8;
  height: 60px;
  position: sticky;
  top: 0;
  z-index: 999;
  padding-right: 20px;
}
.header-menu {
  flex: 1;
  border-bottom: none !important;
  height: 60px;
}
.logo-item {
  font-size: 17px !important;
  font-weight: bold !important;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}
.user-info {
  display: flex;
  align-items: center;
  color: #fff;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 6px;
  font-size: 14px;
}
.user-info:hover { background: rgba(255,255,255,0.15); }
.role-badge {
  background: rgba(255,255,255,0.2);
  border-radius: 4px;
  padding: 1px 6px;
  font-size: 12px;
  margin-left: 6px;
}
</style>
