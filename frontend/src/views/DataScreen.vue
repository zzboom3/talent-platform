<template>
  <div class="data-screen">
    <div class="screen-header">
      <el-button class="back-btn" text @click="$router.push('/')">← 返回首页</el-button>
      <h1>京州市软件产业人才公共服务平台 · 服务总览</h1>
      <p class="lead">整合软件人才资源 · 对接产业发展需求 · 服务终生学习成长</p>
      <span class="time">{{ currentTime }}</span>
    </div>
    <div class="screen-body">
      <!-- 1. 顶部：平台服务生态总览 -->
      <div class="overview-row">
        <div class="metric-card" v-for="m in overviewMetrics" :key="m.label">
          <div class="metric-value">{{ m.value }}</div>
          <div class="metric-label">{{ m.label }}</div>
        </div>
      </div>

      <div class="content-row">
        <!-- 2. 左侧：人才供给画像 -->
        <div class="col col-left">
          <div class="panel">
            <div class="panel-title">人才分布态势</div>
            <div ref="cityChartRef" class="chart-box"></div>
          </div>
          <div class="panel">
            <div class="panel-title">热门技能方向</div>
            <div ref="skillChartRef" class="chart-box"></div>
          </div>
        </div>

        <!-- 3. 中部：平台服务资源 + 月度趋势 -->
        <div class="col col-center">
          <div class="panel">
            <div class="panel-title">平台服务资源</div>
            <div class="resource-cards">
              <div class="resource-item">
                <span>在招岗位</span>
                <strong>{{ resourceStats.openJobs }}</strong>
              </div>
              <div class="resource-item">
                <span>在线课程</span>
                <strong>{{ resourceStats.courses }}</strong>
              </div>
              <div class="resource-item">
                <span>政策法规</span>
                <strong>{{ resourceStats.policies }}</strong>
              </div>
              <div class="resource-item">
                <span>资讯公告</span>
                <strong>{{ (resourceStats.news || 0) + (resourceStats.announcements || 0) }}</strong>
              </div>
            </div>
          </div>
          <div class="panel panel-trend">
            <div class="panel-title">平台活跃趋势</div>
            <div ref="trendChartRef" class="chart-box chart-box-trend"></div>
          </div>
        </div>

        <!-- 4. 右侧：服务结果与对接效果 -->
        <div class="col col-right">
          <div class="panel">
            <div class="panel-title">求职对接结果</div>
            <div ref="statusChartRef" class="chart-box"></div>
          </div>
          <div class="panel">
            <div class="panel-title">服务成果与人才画像</div>
            <div class="service-results">
              <div class="result-item">
                <span>累计申请</span>
                <strong>{{ serviceResults.applications }}</strong>
              </div>
              <div class="result-item">
                <span>已录用</span>
                <strong>{{ serviceResults.acceptedApplications }}</strong>
              </div>
              <div class="result-item">
                <span>结业证书</span>
                <strong>{{ serviceResults.certificates }}</strong>
              </div>
              <div class="result-item">
                <span>学习人次</span>
                <strong>{{ serviceResults.learningParticipants }}</strong>
              </div>
              <div class="result-item">
                <span>覆盖城市</span>
                <strong>{{ serviceResults.coveredCities }}</strong>
              </div>
            </div>
            <div class="profile-insights">
              <div class="insight-block">
                <div class="insight-title">热门求职方向</div>
                <div class="insight-tags">
                  <span v-for="item in expectedPositionSummary" :key="item.label" class="insight-tag">
                    {{ item.label }} · {{ item.value }}
                  </span>
                </div>
              </div>
              <div class="insight-block">
                <div class="insight-title">热门专业背景</div>
                <div class="insight-tags">
                  <span v-for="item in majorSummary" :key="item.label" class="insight-tag muted">
                    {{ item.label }} · {{ item.value }}
                  </span>
                </div>
              </div>
              <div class="insight-block">
                <div class="insight-title">工作年限分布</div>
                <div class="insight-tags">
                  <span v-for="item in workYearSummary" :key="item.label" class="insight-tag muted">
                    {{ item.label }} · {{ item.value }}
                  </span>
                </div>
              </div>
              <div class="insight-block">
                <div class="insight-title">档案完善度</div>
                <div class="insight-bars">
                  <div v-for="item in profileCompletenessSummary" :key="item.label" class="insight-bar-row">
                    <span>{{ item.label }}</span>
                    <div class="insight-bar-track">
                      <div class="insight-bar-fill" :style="{ width: `${item.percent}%` }"></div>
                    </div>
                    <strong>{{ item.value }}</strong>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { statsApi } from '@/api'

const currentTime = ref('')
const overviewMetrics = ref([
  { label: '人才总量', value: '-' },
  { label: '入驻企业', value: '-' },
  { label: '在招岗位', value: '-' },
  { label: '在线课程', value: '-' },
  { label: '资讯政策公告', value: '-' },
  { label: '累计申请', value: '-' },
])
const resourceStats = ref({
  openJobs: 0,
  courses: 0,
  policies: 0,
  news: 0,
  announcements: 0,
})
const serviceResults = ref({
  applications: 0,
  acceptedApplications: 0,
  certificates: 0,
  learningParticipants: 0,
  coveredCities: 0,
})
const majorSummary = ref([])
const expectedPositionSummary = ref([])
const workYearSummary = ref([])
const profileCompletenessSummary = ref([])

const cityChartRef = ref(null)
const skillChartRef = ref(null)
const trendChartRef = ref(null)
const statusChartRef = ref(null)
let charts = []
let timer = null

function updateTime() {
  currentTime.value = new Date().toLocaleString('zh-CN')
}

onMounted(async () => {
  updateTime()
  timer = setInterval(updateTime, 1000)

  const dashboardRes = await statsApi.dashboard().catch(() => null)

  if (dashboardRes?.code === 200) {
    const d = dashboardRes.data
    overviewMetrics.value = [
      { label: '人才总量', value: d.talents ?? 0 },
      { label: '入驻企业', value: d.companies ?? 0 },
      { label: '在招岗位', value: d.openJobs ?? d.jobs ?? 0 },
      { label: '在线课程', value: d.courses ?? 0 },
      { label: '资讯政策公告', value: d.news ?? 0 },
      { label: '累计申请', value: d.applications ?? 0 },
    ]
    const rs = d.resourceStats || {}
    resourceStats.value = {
      openJobs: rs.openJobs ?? d.openJobs ?? d.jobs ?? 0,
      courses: rs.courses ?? d.courses ?? 0,
      policies: rs.policies ?? d.policyCount ?? 0,
      news: rs.news ?? 0,
      announcements: rs.announcements ?? d.announcementCount ?? 0,
    }
    const sr = d.serviceResults || {}
    serviceResults.value = {
      applications: sr.applications ?? d.applications ?? 0,
      acceptedApplications: sr.acceptedApplications ?? 0,
      certificates: sr.certificates ?? d.certificateCount ?? 0,
      learningParticipants: sr.learningParticipants ?? 0,
      coveredCities: sr.coveredCities ?? (d.coveredCities ?? Object.keys(d.talentsByCity || {}).length),
    }
    majorSummary.value = toSummaryItems(d.majorsFrequency, 4)
    expectedPositionSummary.value = toSummaryItems(d.expectedPositions, 4)
    workYearSummary.value = toSummaryItems(d.workYearsDistribution, 4)
    profileCompletenessSummary.value = toProgressItems(d.profileCompleteness)
    await nextTick()
    initCharts(d)
    await nextTick()
    charts.forEach(c => c.resize())
  }

  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
  window.removeEventListener('resize', handleResize)
  charts.forEach(chart => chart.dispose())
  charts = []
})

function handleResize() {
  charts.forEach(c => c.resize())
}

function toSummaryItems(source, limit = 4) {
  return Object.entries(source || {})
    .slice(0, limit)
    .map(([label, value]) => ({ label, value }))
}

function toProgressItems(source) {
  const items = toSummaryItems(source, 3)
  const total = items.reduce((sum, item) => sum + Number(item.value || 0), 0) || 1
  return items.map(item => ({
    ...item,
    percent: Math.round((Number(item.value || 0) / total) * 100),
  }))
}

function initCharts(d) {
  charts.forEach(chart => chart.dispose())
  charts = []

  const trendData = d.monthlyTrend || {}
  const statusData = d.applicationStatusStats || {}
  const cityData = d.talentsByCity || {}
  const skillData = d.skillsFrequency || {}

  const textColor = '#5a6c7d'
  const lineColor = '#e8ebf0'

  // 人才城市分布
  if (cityChartRef.value) {
    const cityChart = echarts.init(cityChartRef.value)
    charts.push(cityChart)
    cityChart.setOption({
      tooltip: {},
      grid: { left: 80, right: 20, top: 20, bottom: 30 },
      xAxis: { type: 'value', axisLabel: { color: textColor }, splitLine: { lineStyle: { color: lineColor, type: 'dashed' } }, axisLine: { show: false } },
      yAxis: { type: 'category', data: Object.keys(cityData), axisLabel: { color: textColor }, axisLine: { lineStyle: { color: lineColor } } },
      series: [{ type: 'bar', data: Object.values(cityData), itemStyle: { color: '#1565c0' } }],
    })
  }

  // 技能热度 TOP10
  if (skillChartRef.value) {
    const skillKeys = Object.keys(skillData).slice(0, 10)
    const skillChart = echarts.init(skillChartRef.value)
    charts.push(skillChart)
    skillChart.setOption({
      tooltip: {},
      grid: { left: 20, right: 20, top: 20, bottom: 50 },
      xAxis: { type: 'category', data: skillKeys, axisLabel: { color: textColor, rotate: 30 }, axisLine: { lineStyle: { color: lineColor } } },
      yAxis: { type: 'value', axisLabel: { color: textColor }, splitLine: { lineStyle: { color: lineColor, type: 'dashed' } }, axisLine: { show: false } },
      series: [{
        type: 'bar',
        data: skillKeys.map(k => skillData[k]),
        itemStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: '#42a5f5' }, { offset: 1, color: '#1565c0' }]) },
      }],
    })
  }

  // 月度趋势
  if (trendChartRef.value) {
    const months = trendData.months && trendData.months.length > 0
      ? trendData.months
      : ['10月', '11月', '12月', '1月', '2月', '3月']
    const regData = trendData.registrations || []
    const appData = trendData.applications || []
    const enrollData = trendData.enrollments || []
    const pad = (arr, len) => [...arr, ...Array(Math.max(0, len - arr.length)).fill(0)].slice(0, len)
    const trendChart = echarts.init(trendChartRef.value)
    charts.push(trendChart)
    trendChart.setOption({
      tooltip: { trigger: 'axis' },
      legend: {
        data: ['用户增长', '岗位申请', '学习参与'],
        top: 8,
        right: 16,
        itemGap: 20,
        textStyle: { color: '#5a6c7d', fontSize: 12 },
        itemWidth: 18,
        itemHeight: 10,
      },
      grid: { left: 50, right: 20, top: 50, bottom: 45 },
      xAxis: {
        type: 'category',
        data: months,
        boundaryGap: false,
        axisLabel: { color: '#5a6c7d', fontSize: 11 },
        axisLine: { lineStyle: { color: '#e8ebf0' } },
        axisTick: { show: false },
      },
      yAxis: {
        type: 'value',
        axisLabel: { color: '#5a6c7d', fontSize: 11 },
        splitLine: { lineStyle: { color: '#e8ebf0', type: 'dashed' } },
        axisLine: { show: false },
        axisTick: { show: false },
      },
      series: [
        { name: '用户增长', type: 'line', data: pad(regData, months.length), smooth: true, symbol: 'circle', symbolSize: 6, lineStyle: { width: 2, color: '#1565c0' }, itemStyle: { color: '#1565c0' } },
        { name: '岗位申请', type: 'line', data: pad(appData, months.length), smooth: true, symbol: 'circle', symbolSize: 6, lineStyle: { width: 2, color: '#ff8f00' }, itemStyle: { color: '#ff8f00' } },
        { name: '学习参与', type: 'line', data: pad(enrollData, months.length), smooth: true, symbol: 'circle', symbolSize: 6, lineStyle: { width: 2, color: '#2e7d32' }, itemStyle: { color: '#2e7d32' } },
      ],
    })
  }

  // 求职状态分布
  const statusLabelMap = { PENDING: '待处理', ACCEPTED: '已接受', REJECTED: '已拒绝' }
  if (statusChartRef.value) {
    const statusChart = echarts.init(statusChartRef.value)
    charts.push(statusChart)
    statusChart.setOption({
      tooltip: { trigger: 'item' },
      legend: { bottom: 0, textStyle: { color: textColor, fontSize: 11 } },
      series: [{
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['50%', '45%'],
        data: Object.entries(statusData).map(([key, value]) => ({
          value,
          name: statusLabelMap[key] || key,
          itemStyle: {
            color: { PENDING: '#42a5f5', ACCEPTED: '#2e7d32', REJECTED: '#c62828' }[key] || '#1565c0',
          },
        })),
        label: { color: textColor, fontSize: 11 },
      }],
    })
  }
}
</script>

<style scoped>
.data-screen {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: var(--tp-bg);
  color: var(--tp-text);
  overflow: hidden;
  font-family: 'PingFang SC', 'Microsoft YaHei', 'Noto Sans SC', sans-serif;
}
.screen-header {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  padding: 16px 24px;
  background: linear-gradient(135deg, var(--tp-primary-dark), var(--tp-primary));
  color: #fff;
  position: relative;
  box-shadow: 0 2px 12px rgba(21, 101, 192, 0.2);
}
.screen-header h1 {
  font-size: 20px;
  color: #fff;
  letter-spacing: 2px;
  margin: 0 0 4px 0;
  font-weight: 600;
}
.screen-header .lead {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.9);
  margin: 0;
}
.back-btn {
  position: absolute;
  left: 16px;
  color: rgba(255, 255, 255, 0.95) !important;
}
.time {
  position: absolute;
  right: 24px;
  top: 50%;
  transform: translateY(-50%);
  color: rgba(255, 255, 255, 0.9);
  font-size: 13px;
}
.screen-body {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 72px);
  padding: 20px 24px;
  gap: 16px;
}
.overview-row {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 12px;
  flex-shrink: 0;
}
.metric-card {
  text-align: center;
  padding: 20px 16px;
  background: var(--tp-card);
  border: 1px solid var(--tp-border);
  border-radius: var(--tp-radius);
  box-shadow: var(--tp-shadow);
  animation: rise-in 0.6s ease;
}
.metric-card .metric-value {
  font-size: 26px;
  font-weight: bold;
  color: var(--tp-primary);
}
.metric-card .metric-label {
  font-size: 13px;
  color: var(--tp-text-secondary);
  margin-top: 6px;
}
.content-row {
  display: flex;
  flex: 1;
  gap: 16px;
  min-height: 0;
}
.col {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-width: 0;
}
.panel {
  flex: 1;
  background: var(--tp-card);
  border: 1px solid var(--tp-border);
  border-radius: var(--tp-radius);
  padding: 18px 20px;
  display: flex;
  flex-direction: column;
  min-height: 0;
  box-shadow: var(--tp-shadow);
}
.panel-title {
  font-size: 15px;
  color: var(--tp-text);
  font-weight: 600;
  margin-bottom: 12px;
  border-left: 4px solid var(--tp-primary);
  padding-left: 12px;
}
.chart-box {
  flex: 1;
  min-height: 140px;
}
.panel-trend {
  min-height: 280px;
}
.chart-box-trend {
  min-height: 220px;
  width: 100%;
}
.resource-cards,
.service-results {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 12px 0;
}
.resource-item,
.result-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
  padding: 10px 0;
  border-bottom: 1px solid var(--tp-border);
  color: var(--tp-text-secondary);
}
.resource-item:last-child,
.result-item:last-child {
  border-bottom: none;
}
.resource-item strong,
.result-item strong {
  color: var(--tp-primary);
  font-size: 20px;
}
.profile-insights {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-top: 10px;
  padding-top: 14px;
  border-top: 1px solid var(--tp-border);
}
.insight-block {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.insight-title {
  font-size: 13px;
  color: var(--tp-text);
  font-weight: 600;
}
.insight-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.insight-tag {
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(21, 101, 192, 0.08);
  color: var(--tp-primary);
  font-size: 12px;
  line-height: 1.4;
}
.insight-tag.muted {
  background: rgba(90, 108, 125, 0.08);
  color: var(--tp-text-secondary);
}
.insight-bars {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.insight-bar-row {
  display: grid;
  grid-template-columns: 60px 1fr 28px;
  gap: 8px;
  align-items: center;
  font-size: 12px;
  color: var(--tp-text-secondary);
}
.insight-bar-track {
  height: 8px;
  border-radius: 999px;
  background: rgba(21, 101, 192, 0.08);
  overflow: hidden;
}
.insight-bar-fill {
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, #42a5f5, #1565c0);
}
@keyframes rise-in {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
