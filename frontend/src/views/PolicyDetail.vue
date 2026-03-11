<template>
  <div class="page-wrap">
    <el-breadcrumb separator="/" class="breadcrumb">
      <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
      <el-breadcrumb-item :to="{ path: '/policies' }">资讯政策公告</el-breadcrumb-item>
      <el-breadcrumb-item v-if="item">{{ item.title }}</el-breadcrumb-item>
    </el-breadcrumb>
    <el-card v-if="item" shadow="always">
      <div style="margin-bottom:8px">
        <el-tag :type="tagType(item.category)">{{ catLabel(item.category) }}</el-tag>
      </div>
      <h2 style="margin-bottom:16px">{{ item.title }}</h2>
      <p style="color:#aaa;font-size:13px;margin-bottom:24px">
        发布者：{{ item.author || '平台管理员' }} &nbsp;|&nbsp; {{ item.publishTime?.substring(0, 10) }}
      </p>
      <div class="content" v-html="htmlContent" />
    </el-card>
    <el-empty v-else description="内容不存在" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { newsApi } from '@/api'

const route = useRoute()
const item = ref(null)

onMounted(async () => {
  const res = await newsApi.getById(route.params.id)
  if (res.code === 200) item.value = res.data
})

const htmlContent = computed(() =>
  item.value?.content?.replace(/\n/g, '<br>') || ''
)

const tagType = (cat) => ({ NEWS: '', POLICY: 'success', ANNOUNCE: 'warning' }[cat] || '')
const catLabel = (cat) => ({ NEWS: '资讯', POLICY: '政策', ANNOUNCE: '公告' }[cat] || cat)
</script>

<style scoped>
.page-wrap { padding: 40px 60px; max-width: 900px; margin: 0 auto; }
.breadcrumb { margin-bottom: 20px; font-size: 14px; }
.page-wrap .el-card { border-radius: var(--tp-radius); }
.page-wrap h2 { color: var(--tp-text); }
.content { font-size: 15px; line-height: 1.8; color: var(--tp-text); }
</style>
