<template>
  <div>
    <!-- Banner -->
    <div class="banner">
      <div class="banner-content">
        <h1>XX市软件产业人才公共服务平台</h1>
        <p>整合软件人才资源 · 对接产业发展需求 · 服务终生学习成长</p>
        <div class="banner-btns">
          <el-button type="primary" size="large" @click="$router.push('/talents')">浏览人才库</el-button>
          <el-button size="large" plain @click="$router.push('/jobs')">查看岗位</el-button>
        </div>
      </div>
    </div>

    <!-- Stats -->
    <div class="stats-row">
      <div class="stat-card" v-for="s in stats" :key="s.label">
        <div class="stat-num">{{ s.num }}</div>
        <div class="stat-label">{{ s.label }}</div>
      </div>
    </div>

    <!-- News & Announcements -->
    <div class="section-wrap">
      <el-row :gutter="24">
        <el-col :span="16">
          <el-card shadow="hover">
            <template #header>
              <div style="display:flex;justify-content:space-between;align-items:center">
                <span style="font-weight:bold;font-size:16px">📰 最新资讯</span>
                <el-link type="primary" @click="$router.push('/policies')">查看政策 →</el-link>
              </div>
            </template>
            <el-empty v-if="!newsList.length" description="暂无资讯" />
            <div v-for="item in newsList" :key="item.id" class="news-item"
              @click="$router.push(`/policies/${item.id}`)">
              <el-tag :type="tagType(item.category)" size="small" style="margin-right:8px">
                {{ catLabel(item.category) }}
              </el-tag>
              <span>{{ item.title }}</span>
              <span class="news-time">{{ formatDate(item.publishTime) }}</span>
            </div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="hover">
            <template #header>
              <span style="font-weight:bold;font-size:16px">🔔 公告通知</span>
            </template>
            <el-empty v-if="!announcements.length" description="暂无公告" />
            <div v-for="item in announcements" :key="item.id" class="news-item"
              @click="$router.push(`/policies/${item.id}`)">
              <span>{{ item.title }}</span>
              <span class="news-time">{{ formatDate(item.publishTime) }}</span>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { newsApi, adminApi } from '@/api'

const newsList = ref([])
const announcements = ref([])
const stats = ref([
  { num: '-', label: '注册用户' },
  { num: '-', label: '人才档案' },
  { num: '-', label: '岗位信息' },
  { num: '-', label: '在线课程' },
])

onMounted(async () => {
  const res = await newsApi.list()
  if (res.code === 200) {
    newsList.value = res.data.filter(n => n.category !== 'ANNOUNCE').slice(0, 8)
    announcements.value = res.data.filter(n => n.category === 'ANNOUNCE').slice(0, 5)
  }
  const statsRes = await adminApi.stats().catch(() => null)
  if (statsRes?.code === 200) {
    const d = statsRes.data
    stats.value = [
      { num: d.users, label: '注册用户' },
      { num: d.talents, label: '人才档案' },
      { num: d.jobs, label: '岗位信息' },
      { num: d.courses, label: '在线课程' },
    ]
  }
})

const tagType = (cat) => ({ NEWS: '', POLICY: 'success', ANNOUNCE: 'warning' }[cat] || '')
const catLabel = (cat) => ({ NEWS: '资讯', POLICY: '政策', ANNOUNCE: '公告' }[cat] || cat)
const formatDate = (d) => d ? d.substring(0, 10) : ''
</script>

<style scoped>
.banner {
  background: linear-gradient(135deg, #1a73e8, #0d5bd1);
  padding: 60px 40px;
  color: #fff;
  text-align: center;
}
.banner h1 { font-size: 32px; margin-bottom: 12px; }
.banner p { font-size: 16px; opacity: .85; margin-bottom: 28px; }
.banner-btns { display: flex; gap: 16px; justify-content: center; }
.stats-row {
  display: flex;
  justify-content: center;
  gap: 24px;
  padding: 32px 40px;
  background: #fff;
}
.stat-card {
  text-align: center;
  padding: 20px 40px;
  background: #f0f7ff;
  border-radius: 8px;
  min-width: 120px;
}
.stat-num { font-size: 32px; font-weight: bold; color: #1a73e8; }
.stat-label { color: #666; margin-top: 4px; }
.section-wrap { padding: 24px 40px; }
.news-item {
  display: flex;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  font-size: 14px;
}
.news-item:hover { color: #1a73e8; }
.news-time { margin-left: auto; color: #aaa; font-size: 12px; }
</style>
