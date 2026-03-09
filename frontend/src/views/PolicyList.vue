<template>
  <div class="page-wrap">
    <h2 style="margin-bottom:24px">📋 政策法规 · 新闻公告</h2>
    <el-tabs v-model="tab" @tab-change="onTabChange">
      <el-tab-pane label="全部" name="" />
      <el-tab-pane label="政策法规" name="POLICY" />
      <el-tab-pane label="最新资讯" name="NEWS" />
      <el-tab-pane label="公告通知" name="ANNOUNCE" />
    </el-tabs>

    <div v-if="list.length" class="news-list">
      <div
        v-for="item in list"
        :key="item.id"
        class="news-row"
        @click="$router.push(`/policies/${item.id}`)"
      >
        <el-tag :type="tagType(item.category)" size="small" style="margin-right:12px;flex-shrink:0">
          {{ catLabel(item.category) }}
        </el-tag>
        <span class="news-title">{{ item.title }}</span>
        <span class="news-time">{{ item.publishTime?.substring(0, 10) }}</span>
      </div>
    </div>
    <el-empty v-else description="暂无内容" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { newsApi } from '@/api'

const list = ref([])
const tab = ref('')

onMounted(load)

async function load() {
  const res = await newsApi.list(tab.value || undefined)
  if (res.code === 200) list.value = res.data
}

function onTabChange() {
  load()
}

const tagType = (cat) => ({ NEWS: '', POLICY: 'success', ANNOUNCE: 'warning' }[cat] || '')
const catLabel = (cat) => ({ NEWS: '资讯', POLICY: '政策', ANNOUNCE: '公告' }[cat] || cat)
</script>

<style scoped>
.page-wrap { padding: 24px 40px; max-width: 900px; margin: 0 auto; }
.news-list { margin-top: 8px; }
.news-row {
  display: flex;
  align-items: center;
  padding: 14px 12px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  border-radius: 4px;
  transition: background .15s;
}
.news-row:hover { background: #f0f7ff; }
.news-title { flex: 1; font-size: 15px; color: #222; }
.news-time { color: #aaa; font-size: 13px; white-space: nowrap; margin-left: 16px; }
</style>
