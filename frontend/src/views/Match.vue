<template>
  <div class="page-wrap">
    <h2 style="margin-bottom:24px">🤝 人才与岗位智能匹配</h2>
    <el-card shadow="hover" style="margin-bottom:24px">
      <div style="display:flex;gap:12px;align-items:center">
        <el-input v-model="keyword" placeholder="输入技能关键词，如：Java、Python、前端、算法"
          size="large" clearable style="flex:1" @keyup.enter="search" />
        <el-button type="primary" size="large" @click="search" :loading="loading">
          开始匹配
        </el-button>
        <el-button size="large" @click="keyword=''; search()">全部岗位</el-button>
      </div>
    </el-card>

    <div v-if="matched.length">
      <p style="color:#888;margin-bottom:16px">共找到 {{ matched.length }} 个匹配岗位</p>
      <el-row :gutter="20">
        <el-col :span="8" v-for="job in matched" :key="job.id">
          <el-card shadow="hover" class="job-card" @click="$router.push(`/jobs/${job.id}`)">
            <div class="job-title">{{ job.title }}</div>
            <div class="job-company">{{ job.company?.companyName }}</div>
            <div style="color:#666;font-size:13px;margin-top:4px">
              📍 {{ job.city || '不限' }} &nbsp;💰 {{ job.salaryRange || '面议' }}
            </div>
            <div style="margin-top:8px;font-size:12px;color:#888;overflow:hidden;max-height:40px">
              {{ job.requirements }}
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>
    <el-empty v-else-if="searched" description="未找到匹配岗位，换个关键词试试" />
    <div v-else style="text-align:center;color:#aaa;padding:60px">
      输入技能关键词开始智能匹配
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { matchApi } from '@/api'

const keyword = ref('')
const matched = ref([])
const loading = ref(false)
const searched = ref(false)

async function search() {
  loading.value = true
  searched.value = true
  const res = await matchApi.search(keyword.value)
  if (res.code === 200) matched.value = res.data
  loading.value = false
}
</script>

<style scoped>
.page-wrap { padding: 32px 40px; }
.job-card { cursor: pointer; margin-bottom: 20px; }
.job-card:hover { transform: translateY(-2px); transition: .2s; }
.job-title { font-size: 16px; font-weight: bold; margin-bottom: 6px; }
.job-company { color: #1a73e8; font-size: 14px; }
</style>
