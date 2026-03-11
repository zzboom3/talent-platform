<template>
  <div class="page-wrap">
    <h2>AI 职业能力智能评估</h2>
    <p style="color:#888;margin:8px 0 24px">基于 AI 大模型，对您的技能、经验、学历进行全面分析评估</p>

    <el-row :gutter="20">
      <el-col :span="12">
        <el-card shadow="hover" v-loading="loading" element-loading-text="AI 正在分析您的技能与经验，请稍候...">
          <el-button type="primary" size="large" @click="doAssess" :loading="loading" :disabled="loading" style="width:100%">
            开始 AI 评估
          </el-button>
          <div v-if="result" style="margin-top:20px">
            <div style="text-align:center;margin-bottom:16px">
              <div class="score-num">{{ result.overallScore }}</div>
              <div style="color:#888">综合评分</div>
            </div>
            <div ref="radarRef" style="height:300px"></div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card v-if="result" shadow="hover">
          <template #header><strong>评估报告</strong></template>
          <p style="line-height:1.8;color:#333">{{ result.report }}</p>
          <el-divider />
          <strong>发展建议</strong>
          <ul style="margin-top:8px">
            <li v-for="(s, i) in (result.suggestions || [])" :key="i" style="margin:6px 0;color:#555">{{ s }}</li>
          </ul>
          <el-divider />
          <el-button @click="printReport" size="small">导出/打印报告</el-button>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="hover" style="margin-top:24px">
      <template #header><strong>评估历史</strong></template>
      <el-table :data="history" stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="overallScore" label="综合评分" width="100">
          <template #default="{ row }">
            <el-tag :type="row.overallScore >= 70 ? 'success' : row.overallScore >= 40 ? 'warning' : 'danger'">
              {{ row.overallScore }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="assessmentReport" label="报告摘要" show-overflow-tooltip />
        <el-table-column prop="createTime" label="评估时间" width="180" />
      </el-table>
      <el-empty v-if="!history.length" description="暂无评估记录" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { aiApi } from '@/api'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const result = ref(null)
const history = ref([])
const radarRef = ref(null)

onMounted(async () => {
  const res = await aiApi.assessmentHistory().catch(() => null)
  if (res?.code === 200) history.value = res.data
})

async function doAssess() {
  loading.value = true
  try {
    const res = await aiApi.careerAssessment()
    if (res.code === 200) {
      result.value = res.data
      ElMessage.success('评估完成')
      await nextTick()
      renderRadar(res.data.radarData)
      const hRes = await aiApi.assessmentHistory()
      if (hRes.code === 200) history.value = hRes.data
    } else {
      ElMessage.error(res.message || '评估失败')
    }
  } catch (e) {
    ElMessage.error('评估请求失败')
  }
  loading.value = false
}

function renderRadar(data) {
  if (!radarRef.value || !data) return
  const chart = echarts.init(radarRef.value)
  const keys = Object.keys(data)
  const values = Object.values(data)
  chart.setOption({
    radar: {
      indicator: keys.map(k => ({ name: k, max: 100 })),
      shape: 'polygon',
      axisName: { color: '#333' },
    },
    series: [{
      type: 'radar',
      data: [{ value: values, name: '能力评分',       areaStyle: { color: 'rgba(21,101,192,0.25)' } }],
      itemStyle: { color: '#1565c0' },
    }]
  })
}

function printReport() { window.print() }
</script>

<style scoped>
.page-wrap { padding: 32px 40px; }
.page-wrap h2 { color: var(--tp-text); font-weight: 600; }
.page-wrap .el-card { border-radius: var(--tp-radius); }
.page-wrap :deep(.el-table) { border-radius: var(--tp-radius-sm); }
.score-num { font-size: 48px; font-weight: bold; color: var(--tp-primary); }
</style>
