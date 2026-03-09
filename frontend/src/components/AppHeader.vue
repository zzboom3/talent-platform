<template>
  <el-menu
    mode="horizontal"
    :default-active="activeIndex"
    background-color="#1a73e8"
    text-color="#fff"
    active-text-color="#ffd04b"
    style="height:60px;position:sticky;top:0;z-index:999;"
    router
  >
    <el-menu-item index="/" style="font-size:18px;font-weight:bold;">
      🏛 人才公共服务平台
    </el-menu-item>
    <el-menu-item index="/">首页</el-menu-item>
    <el-menu-item index="/talents">人才库</el-menu-item>
    <el-menu-item index="/jobs">岗位</el-menu-item>
    <el-menu-item index="/match">智能匹配</el-menu-item>
    <el-menu-item index="/courses">终生学习</el-menu-item>
    <el-menu-item index="/policies">政策法规</el-menu-item>
    <el-menu-item v-if="isAdmin" index="/admin">后台管理</el-menu-item>

    <div style="flex:1;" />

    <template v-if="isLoggedIn">
      <el-menu-item disabled style="color:#fff !important;">
        {{ username }} ({{ roleLabel }})
      </el-menu-item>
      <el-menu-item @click="handleLogout">退出</el-menu-item>
    </template>
    <template v-else>
      <el-menu-item index="/login">登录</el-menu-item>
      <el-menu-item index="/register">注册</el-menu-item>
    </template>
  </el-menu>
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
const username = computed(() => store.username)
const roleLabel = computed(() => ({
  ADMIN: '管理员', TALENT: '人才', ENTERPRISE: '企业'
}[store.role] || store.role))

function handleLogout() {
  store.logout()
  ElMessage.success('已退出登录')
  router.push('/login')
}
</script>
