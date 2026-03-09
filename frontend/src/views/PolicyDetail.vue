<template>
  <div class="page-wrap">
    <el-button @click="$router.back()" style="margin-bottom:16px">← 返回列表</el-button>
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
.page-wrap { padding: 32px 60px; max-width: 860px; margin: 0 auto; }
.content { font-size: 15px; line-height: 1.8; color: #333; }
</style>
