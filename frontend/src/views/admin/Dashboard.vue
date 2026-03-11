<template>
  <div class="admin-wrap">
    <div class="admin-sidebar">
      <div class="sidebar-title">🛠 后台管理</div>
      <el-menu :default-active="$route.path" router>
        <el-menu-item index="/admin">📊 数据概览</el-menu-item>
        <el-menu-item index="/admin/users">👤 用户管理</el-menu-item>
        <el-menu-item index="/admin/news">📰 资讯管理</el-menu-item>
        <el-menu-item index="/admin/courses">📚 课程管理</el-menu-item>
        <el-menu-item index="/admin/companies">🏢 企业管理</el-menu-item>
        <el-menu-item index="/admin/showcase">⭐ 人才管理</el-menu-item>
        <el-menu-item index="/admin/monitor">📈 系统监控</el-menu-item>
        <el-menu-item index="/admin/blockchain">🔗 区块链管理</el-menu-item>
      </el-menu>
    </div>
    <div class="admin-content">
      <h2 style="margin-bottom:24px">数据概览</h2>
      <el-row :gutter="16">
        <el-col :span="4" v-for="s in statCards" :key="s.label">
          <el-card shadow="hover" class="stat-card" :style="{ borderTop: `3px solid ${s.color}` }">
            <div class="stat-icon">{{ s.icon }}</div>
            <div class="stat-num">{{ s.value }}</div>
            <div class="stat-label">{{ s.label }}</div>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="20" style="margin-top:24px">
        <el-col :span="12">
          <el-card shadow="hover">
            <template #header><strong>技能热度排行</strong></template>
            <div ref="skillChartRef" style="height:300px"></div>
          </el-card>
        </el-col>
        <el-col :span="12">
          <el-card shadow="hover">
            <template #header><strong>人才城市分布</strong></template>
            <div ref="cityChartRef" style="height:300px"></div>
          </el-card>
        </el-col>
      </el-row>

      <el-card shadow="hover" style="margin-top:24px">
        <template #header><strong>系统监控概要</strong></template>
        <el-row :gutter="16">
          <el-col :span="6" v-for="m in monitorCards" :key="m.label">
            <div class="monitor-item">
              <div class="monitor-value">{{ m.value }}</div>
              <div class="monitor-label">{{ m.label }}</div>
            </div>
          </el-col>
        </el-row>
      </el-card>

      <el-card shadow="hover" style="margin-top:24px">
        <template #header>
          <div class="crawler-header">
            <strong>数据采集</strong>
            <el-button type="primary" :loading="runningCrawler" @click="runCrawler">立即采集</el-button>
          </div>
        </template>
        <div class="crawler-sections">
          <div class="crawler-section">
            <div class="crawler-section-title">数据库真实内容统计</div>
            <div class="crawler-grid">
              <div class="crawler-item">
                <div class="crawler-value">{{ dbContentStats.totalJobs }}</div>
                <div class="crawler-label">岗位总数</div>
              </div>
              <div class="crawler-item">
                <div class="crawler-value">{{ dbContentStats.totalContent }}</div>
                <div class="crawler-label">资讯政策公告总数</div>
              </div>
              <div class="crawler-item">
                <div class="crawler-value">{{ dbContentStats.newsCount }}</div>
                <div class="crawler-label">最新资讯</div>
              </div>
              <div class="crawler-item">
                <div class="crawler-value">{{ dbContentStats.policyCount }}</div>
                <div class="crawler-label">政策法规</div>
              </div>
              <div class="crawler-item">
                <div class="crawler-value">{{ dbContentStats.announcementCount }}</div>
                <div class="crawler-label">公告通知</div>
              </div>
            </div>
          </div>
          <div class="crawler-section">
            <div class="crawler-section-title">最近一次采集执行</div>
            <div class="crawler-grid">
              <div class="crawler-item">
                <div class="crawler-value">{{ formatTime(crawlerStatus.finishedAt) }}</div>
                <div class="crawler-label">执行时间</div>
              </div>
              <div class="crawler-item">
                <div class="crawler-value">{{ crawlerStatus.jobsInserted ?? 0 }}</div>
                <div class="crawler-label">本次新增岗位</div>
              </div>
              <div class="crawler-item">
                <div class="crawler-value">{{ crawlerStatus.newsInserted ?? 0 }}</div>
                <div class="crawler-label">本次新增资讯/政策</div>
              </div>
              <div class="crawler-item">
                <div class="crawler-value">{{ crawlerStatus.totalInserted ?? 0 }}</div>
                <div class="crawler-label">本次总新增</div>
              </div>
            </div>
          </div>
        </div>
        <el-alert
          :title="crawlerStatus.success ? '最近一次采集执行完成' : '最近一次采集尚未成功执行'"
          :type="crawlerStatus.success ? 'success' : 'warning'"
          :closable="false"
          style="margin-top:16px"
        />
        <div class="crawler-messages">
          <el-tag v-for="msg in crawlerStatus.messages || []" :key="msg" class="crawler-tag" effect="plain">
            {{ msg }}
          </el-tag>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import { statsApi, adminApi } from '@/api'

const statCards = ref([
  { label: '注册用户', value: '-', icon: '👤', color: 'var(--tp-primary)' },
  { label: '人才档案', value: '-', icon: '📋', color: '#67c23a' },
  { label: '发布岗位', value: '-', icon: '💼', color: '#e6a23c' },
  { label: '资讯政策公告', value: '-', icon: '📰', color: '#909399' },
  { label: '在线课程', value: '-', icon: '📚', color: '#f56c6c' },
  { label: '求职申请', value: '-', icon: '✉️', color: '#409eff' },
])
const dbContentStats = ref({
  totalJobs: 0,
  totalContent: 0,
  newsCount: 0,
  policyCount: 0,
  announcementCount: 0,
})
const monitorCards = ref([
  { label: '总 API 调用', value: '-' },
  { label: '今日调用', value: '-' },
  { label: '平均响应', value: '-' },
  { label: '今日错误', value: '-' },
])
const crawlerStatus = ref({
  success: false,
  finishedAt: null,
  totalInserted: 0,
  jobsInserted: 0,
  newsInserted: 0,
  messages: [],
  crawledJobsTotal: 0,
  crawledNewsTotal: 0,
  crawledPolicyCount: 0,
  crawledIndustryNewsCount: 0,
})
const runningCrawler = ref(false)
const skillChartRef = ref(null)
const cityChartRef = ref(null)

onMounted(async () => {
  const [dashRes, monRes, crawlerRes] = await Promise.all([
    statsApi.dashboard().catch(() => null),
    adminApi.apiStats().catch(() => null),
    adminApi.crawlerStatus().catch(() => null),
  ])

  if (dashRes?.code === 200) {
    const d = dashRes.data
    const totalContent = d.news ?? 0
    const policyCount = d.policyCount ?? 0
    const announcementCount = d.announcementCount ?? 0
    statCards.value[0].value = d.users
    statCards.value[1].value = d.talents
    statCards.value[2].value = d.jobs
    statCards.value[3].value = d.news
    statCards.value[4].value = d.courses
    statCards.value[5].value = d.applications
    dbContentStats.value = {
      totalJobs: d.jobs ?? 0,
      totalContent,
      newsCount: Math.max(0, totalContent - policyCount - announcementCount),
      policyCount,
      announcementCount,
    }

    await nextTick()
    if (skillChartRef.value && d.skillsFrequency) {
      const keys = Object.keys(d.skillsFrequency).slice(0, 10)
      echarts.init(skillChartRef.value).setOption({
        tooltip: {}, grid: { left: 20, right: 20, top: 10, bottom: 40 },
        xAxis: { type: 'category', data: keys, axisLabel: { rotate: 30 } },
        yAxis: { type: 'value' },
        series: [{ type: 'bar', data: keys.map(k => d.skillsFrequency[k]), itemStyle: { color: '#1565c0' } }]
      })
    }
    if (cityChartRef.value && d.talentsByCity) {
      const entries = Object.entries(d.talentsByCity)
      echarts.init(cityChartRef.value).setOption({
        tooltip: { trigger: 'item' },
        series: [{
          type: 'pie', radius: ['35%', '65%'],
          data: entries.map(([name, value]) => ({ name, value })),
          label: { fontSize: 12 }
        }]
      })
    }
  }

  if (monRes?.code === 200) {
    const m = monRes.data
    monitorCards.value = [
      { label: '总 API 调用', value: m.totalCalls },
      { label: '今日调用', value: m.todayCalls },
      { label: '平均响应', value: Math.round(m.avgResponseTime) + 'ms' },
      { label: '今日错误', value: m.errorCount },
    ]
  }

  if (crawlerRes?.code === 200 && crawlerRes.data) {
    crawlerStatus.value = { ...crawlerStatus.value, ...crawlerRes.data }
  }
})

function formatTime(value) {
  return value ? String(value).replace('T', ' ').slice(0, 19) : '未执行'
}

async function runCrawler() {
  runningCrawler.value = true
  try {
    const res = await adminApi.crawlerRun()
    if (res.code === 200) {
      crawlerStatus.value = { ...crawlerStatus.value, ...res.data }
      ElMessage.success(res.data?.running ? '采集任务已启动，正在后台执行' : `采集完成，本次新增 ${res.data.totalInserted || 0} 条记录`)
      await pollCrawlerStatus()
    } else {
      ElMessage.error(res.message || '数据采集失败')
    }
  } finally {
    runningCrawler.value = false
  }
}

async function pollCrawlerStatus(maxRounds = 90) {
  for (let round = 0; round < maxRounds; round++) {
    await wait(2000)
    const statusRes = await adminApi.crawlerStatus().catch(() => null)
    if (statusRes?.code !== 200 || !statusRes.data) {
      continue
    }
    crawlerStatus.value = { ...crawlerStatus.value, ...statusRes.data }
    if (!statusRes.data.running) {
      ElMessage.success(`采集完成，本次新增 ${statusRes.data.totalInserted || 0} 条记录`)
      return
    }
  }
  ElMessage.warning('采集仍在后台执行，可稍后查看状态面板')
}

function wait(ms) {
  return new Promise(resolve => setTimeout(resolve, ms))
}
</script>

<style scoped>
.admin-wrap { display: flex; min-height: calc(100vh - 60px); }
.admin-sidebar { width: 200px; background: var(--tp-card); border-right: 1px solid var(--tp-border); flex-shrink: 0; }
.sidebar-title { padding: 20px 16px; font-weight: bold; font-size: 15px; color: var(--tp-primary); border-bottom: 1px solid var(--tp-border); }
.admin-content { flex: 1; padding: 32px; background: var(--tp-bg); }
.admin-content h2 { color: var(--tp-text); font-weight: 600; }
.stat-card { text-align: center; padding: 12px 0; border-radius: var(--tp-radius); }
.stat-icon { font-size: 28px; margin-bottom: 4px; }
.stat-num { font-size: 28px; font-weight: bold; color: var(--tp-text); }
.stat-label { color: var(--tp-text-secondary); font-size: 13px; margin-top: 2px; }
.monitor-item { text-align: center; padding: 16px; }
.monitor-value { font-size: 28px; font-weight: bold; color: var(--tp-primary); }
.monitor-label { color: var(--tp-text-secondary); font-size: 13px; margin-top: 4px; }
.crawler-header { display: flex; justify-content: space-between; align-items: center; }
.crawler-sections { display: flex; flex-direction: column; gap: 20px; }
.crawler-section { }
.crawler-section-title { font-size: 14px; color: var(--tp-text-secondary); margin-bottom: 12px; font-weight: 500; }
.crawler-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(140px, 1fr)); gap: 16px; }
.crawler-item { text-align: center; padding: 18px 12px; background: rgba(21, 101, 192, 0.04); border-radius: var(--tp-radius-sm); }
.crawler-value { font-size: 20px; font-weight: bold; color: var(--tp-primary); word-break: break-word; }
.crawler-label { color: var(--tp-text-secondary); font-size: 13px; margin-top: 6px; }
.crawler-messages { margin-top: 16px; display: flex; gap: 8px; flex-wrap: wrap; }
.crawler-tag { margin-right: 0; }
.admin-content :deep(.el-card) { border-radius: var(--tp-radius); }
</style>
