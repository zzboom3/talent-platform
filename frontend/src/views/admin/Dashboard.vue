<template>
  <div class="admin-wrap">
    <div class="admin-sidebar">
      <div class="sidebar-title">🛠 后台管理</div>
      <el-menu :default-active="$route.path" router>
        <el-menu-item index="/admin"><el-icon><DataAnalysis /></el-icon>数据概览</el-menu-item>
        <el-menu-item index="/admin/users"><el-icon><User /></el-icon>用户管理</el-menu-item>
        <el-menu-item index="/admin/news"><el-icon><Document /></el-icon>资讯管理</el-menu-item>
        <el-menu-item index="/admin/courses"><el-icon><Reading /></el-icon>课程管理</el-menu-item>
      </el-menu>
    </div>
    <div class="admin-content">
      <h2 style="margin-bottom:24px">数据概览</h2>
      <el-row :gutter="20">
        <el-col :span="8" v-for="s in statCards" :key="s.label">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-icon">{{ s.icon }}</div>
            <div class="stat-num">{{ s.value }}</div>
            <div class="stat-label">{{ s.label }}</div>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { adminApi } from '@/api'

const statCards = ref([
  { label: '注册用户', value: '-', icon: '👤' },
  { label: '人才档案', value: '-', icon: '📋' },
  { label: '发布岗位', value: '-', icon: '💼' },
  { label: '新闻资讯', value: '-', icon: '📰' },
  { label: '在线课程', value: '-', icon: '📚' },
  { label: '求职申请', value: '-', icon: '✉️' },
])

onMounted(async () => {
  const res = await adminApi.stats()
  if (res.code === 200) {
    const d = res.data
    statCards.value[0].value = d.users
    statCards.value[1].value = d.talents
    statCards.value[2].value = d.jobs
    statCards.value[3].value = d.news
    statCards.value[4].value = d.courses
    statCards.value[5].value = d.applications
  }
})
</script>

<style scoped>
.admin-wrap { display: flex; min-height: calc(100vh - 60px); }
.admin-sidebar {
  width: 200px;
  background: #fff;
  border-right: 1px solid #eee;
  flex-shrink: 0;
}
.sidebar-title {
  padding: 20px 16px;
  font-weight: bold;
  font-size: 15px;
  color: #1a73e8;
  border-bottom: 1px solid #eee;
}
.admin-content { flex: 1; padding: 32px; background: #f5f7fa; }
.stat-card { text-align: center; padding: 16px 0; }
.stat-icon { font-size: 32px; margin-bottom: 8px; }
.stat-num { font-size: 36px; font-weight: bold; color: #1a73e8; }
.stat-label { color: #666; margin-top: 4px; }
</style>
