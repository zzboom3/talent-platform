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
      <div class="monitor-header">
        <h2>系统监控</h2>
        <el-button type="primary" :icon="Refresh" @click="load" :loading="loading">刷新数据</el-button>
      </div>

      <!-- 核心指标卡片 -->
      <el-row :gutter="16">
        <el-col :span="6" v-for="m in metrics" :key="m.label">
          <el-card shadow="hover" class="metric-card">
            <div class="metric-val">{{ m.value }}</div>
            <div class="metric-label">{{ m.label }}</div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 图表区域 -->
      <el-row :gutter="16" style="margin-top:24px">
        <el-col :span="12">
          <el-card shadow="hover">
            <template #header><strong>今日 API 调用趋势（按小时）</strong></template>
            <div class="chart-wrap">
              <div ref="hourlyChartRef" class="chart-box"></div>
              <div v-if="!hasHourlyData" class="chart-overlay"><el-empty description="今日暂无调用记录" :image-size="60" /></div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="12">
          <el-card shadow="hover">
            <template #header><strong>调用成功率分布</strong></template>
            <div class="chart-wrap">
              <div ref="pieChartRef" class="chart-box"></div>
              <div v-if="!hasPieData" class="chart-overlay"><el-empty description="今日暂无调用记录" :image-size="60" /></div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="16" style="margin-top:24px">
        <el-col :span="12">
          <el-card shadow="hover">
            <template #header><strong>近 7 天 API 调用趋势</strong></template>
            <div class="chart-wrap">
              <div ref="dailyChartRef" class="chart-box"></div>
              <div v-if="!hasDailyData" class="chart-overlay"><el-empty description="近7日暂无调用记录" :image-size="60" /></div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="12">
          <el-card shadow="hover">
            <template #header><strong>热门接口 Top 10（调用量）</strong></template>
            <div class="chart-wrap">
              <div ref="barChartRef" class="chart-box"></div>
              <div v-if="!topUris.length" class="chart-overlay"><el-empty description="暂无数据" :image-size="60" /></div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 热门接口明细表格 -->
      <el-card shadow="hover" style="margin-top:24px">
        <template #header><strong>热门接口明细</strong></template>
        <el-table :data="topUris" stripe size="small">
          <el-table-column type="index" label="#" width="50" />
          <el-table-column prop="uri" label="接口路径" show-overflow-tooltip />
          <el-table-column prop="count" label="调用次数" width="120">
            <template #default="{ row }">{{ row.count?.toLocaleString?.() ?? row.count }}</template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!topUris.length" description="暂无数据" />
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { adminApi } from '@/api'

const loading = ref(false)
const metrics = ref([
  { label: '总 API 调用', value: '-' },
  { label: '今日调用', value: '-' },
  { label: '平均响应时间', value: '-' },
  { label: '今日错误数', value: '-' },
])
const topUris = ref([])
const hourlyChartRef = ref(null)
const dailyChartRef = ref(null)
const pieChartRef = ref(null)
const barChartRef = ref(null)
let charts = []
const hasHourlyData = ref(false)
const hasDailyData = ref(false)
const hasPieData = ref(false)

async function load() {
  loading.value = true
  try {
    const res = await adminApi.apiStats().catch(() => null)
    if (res?.code === 200) {
      const d = res.data
      metrics.value = [
        { label: '总 API 调用', value: formatNum(d.totalCalls) },
        { label: '今日调用', value: formatNum(d.todayCalls) },
        { label: '平均响应时间', value: Math.round(d.avgResponseTime || 0) + 'ms' },
        { label: '今日错误数', value: formatNum(d.errorCount) },
      ]
      topUris.value = d.topUris || []

      // 渲染图表（等待 DOM 更新）
      await nextTick()
      renderCharts(d)
    }
  } finally {
    loading.value = false
  }
}

function formatNum(n) {
  if (n == null) return '-'
  return Number(n).toLocaleString()
}

function renderCharts(d) {
  charts.forEach(c => c?.dispose?.())
  charts = []

  const primaryColor = '#1565c0'
  const successColor = '#67c23a'
  const errorColor = '#f56c6c'

  // 今日每小时趋势
  const hourly = d.hourlyTrend || {}
  const hourlyLabels = hourly.labels || []
  const hourlyValues = hourly.values || []
  hasHourlyData.value = hourlyValues.some(v => v > 0)
  if (hourlyChartRef.value) {
    const chart = echarts.init(hourlyChartRef.value)
    charts.push(chart)
    chart.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: 40, right: 20, top: 20, bottom: 30 },
      xAxis: { type: 'category', data: hourlyLabels, axisLabel: { fontSize: 10 } },
      yAxis: { type: 'value', axisLabel: { fontSize: 10 } },
      series: [{
        type: 'line',
        data: hourlyValues,
        smooth: true,
        areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(21,101,192,0.4)' },
          { offset: 1, color: 'rgba(21,101,192,0.05)' }
        ]) },
        lineStyle: { color: primaryColor },
        itemStyle: { color: primaryColor }
      }]
    })
  }

  // 成功率饼图
  const successCount = d.successCount ?? 0
  const errorCount = d.errorCount ?? 0
  hasPieData.value = successCount > 0 || errorCount > 0
  if (pieChartRef.value && hasPieData.value) {
    const chart = echarts.init(pieChartRef.value)
    charts.push(chart)
    chart.setOption({
      tooltip: { trigger: 'item' },
      legend: { bottom: 0 },
      series: [{
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['50%', '45%'],
        data: [
          { value: successCount, name: '成功', itemStyle: { color: successColor } },
          { value: errorCount, name: '失败', itemStyle: { color: errorColor } }
        ],
        label: { fontSize: 12 }
      }]
    })
  }

  // 近7天趋势
  const daily = d.dailyTrend || {}
  const dailyLabels = daily.labels || []
  const dailyValues = daily.values || []
  hasDailyData.value = dailyValues.length > 0
  if (dailyChartRef.value && hasDailyData.value) {
    const chart = echarts.init(dailyChartRef.value)
    charts.push(chart)
    chart.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: 50, right: 20, top: 20, bottom: 30 },
      xAxis: { type: 'category', data: dailyLabels, axisLabel: { fontSize: 10, rotate: 25 } },
      yAxis: { type: 'value', axisLabel: { fontSize: 10 } },
      series: [{
        type: 'bar',
        data: dailyValues,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: primaryColor },
            { offset: 1, color: '#90caf9' }
          ])
        }
      }]
    })
  }

  // 热门接口 Top 10 柱状图
  if (barChartRef.value && topUris.value.length) {
    const chart = echarts.init(barChartRef.value)
    charts.push(chart)
    const uris = topUris.value.map(u => (u.uri || '').replace(/^\/api/, '') || u.uri).slice(0, 10)
    const counts = topUris.value.map(u => u.count || 0).slice(0, 10)
    chart.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: 80, right: 20, top: 10, bottom: 80 },
      xAxis: { type: 'category', data: uris, axisLabel: { fontSize: 9, rotate: 35, interval: 0 } },
      yAxis: { type: 'value', axisLabel: { fontSize: 10 } },
      series: [{
        type: 'bar',
        data: counts,
        itemStyle: { color: primaryColor }
      }]
    })
  }
}

onMounted(() => {
  load()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  charts.forEach(c => c?.dispose?.())
  charts = []
})

function handleResize() {
  charts.forEach(c => c?.resize?.())
}
</script>

<style scoped>
.admin-wrap { display: flex; min-height: calc(100vh - 60px); }
.admin-sidebar { width: 200px; background: var(--tp-card); border-right: 1px solid var(--tp-border); flex-shrink: 0; }
.sidebar-title { padding: 20px 16px; font-weight: bold; font-size: 15px; color: var(--tp-primary); border-bottom: 1px solid var(--tp-border); }
.admin-content { flex: 1; padding: 32px; background: var(--tp-bg); }
.monitor-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.admin-content h2 { color: var(--tp-text); font-weight: 600; margin: 0; }
.metric-card { text-align: center; padding: 20px 0; border-radius: var(--tp-radius); }
.metric-val { font-size: 32px; font-weight: bold; color: var(--tp-primary); }
.metric-label { color: var(--tp-text-secondary); margin-top: 4px; }
.chart-wrap { position: relative; height: 260px; }
.chart-box { height: 260px; width: 100%; }
.chart-overlay { position: absolute; inset: 0; display: flex; align-items: center; justify-content: center; background: rgba(255,255,255,0.9); }
.admin-content :deep(.el-card) { border-radius: var(--tp-radius); }
.admin-content :deep(.el-table) { border-radius: var(--tp-radius-sm); }
</style>
