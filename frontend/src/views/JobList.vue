<template>
  <div class="page-wrap" v-loading="loading">
    <div class="page-header">
      <div class="page-title-block">
        <h2>岗位列表</h2>
        <p class="page-intro">京州市优质企业发布的软件岗位，支持按关键词、城市、企业和薪资范围组合筛选</p>
        <div class="page-stats">
          <el-tag type="info" effect="plain" size="large">
            共 {{ stats.jobs ?? jobs.length }} 个岗位
            <span v-if="hasActiveFilters" class="stats-filtered"> · 当前展示 {{ filteredJobs.length }} 个</span>
          </el-tag>
        </div>
      </div>
    </div>

    <JobSearchPanel
      :filters="filters"
      :options="filterOptions"
      @update:filters="updateFilters"
      @reset="resetFilters"
    >
      <template #actions>
        <el-button v-if="isEnterprise" type="success" @click="$router.push('/company/jobs')">岗位管理</el-button>
      </template>
    </JobSearchPanel>

    <el-row :gutter="20">
      <el-col :span="8" v-for="job in filteredJobs" :key="job.id">
        <el-card shadow="hover" class="job-card">
          <div class="job-title">{{ job.title }}</div>
          <div class="job-company">{{ job.company?.companyName || '未知企业' }}</div>
          <div class="job-city">📍 {{ job.city || '不限' }}</div>
          <div class="job-salary">💰 {{ job.salaryRange || '薪资面议' }}</div>
          <div style="margin-top:12px;display:flex;gap:8px">
            <el-button size="small" @click="$router.push(`/jobs/${job.id}`)">查看详情</el-button>
            <el-button v-if="isTalent" size="small" type="primary" :loading="applyingId === job.id" :disabled="applyingId != null" @click="apply(job.id)">
              申请岗位
            </el-button>
            <el-button v-if="isEnterprise && isMyJob(job)" size="small" type="danger"
              @click="deleteJob(job.id)">删除</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
    <el-empty v-if="!filteredJobs.length" :description="hasActiveFilters ? '未找到符合条件的岗位' : '暂无岗位信息'" class="empty-with-actions">
      <p class="empty-tip">{{ hasActiveFilters ? '可以调整筛选条件，查看更多岗位机会' : '当前没有可用的招聘岗位，请稍后再来' }}</p>
      <el-button v-if="hasActiveFilters" @click="resetFilters">清空筛选</el-button>
      <el-button type="primary" @click="loadAll">刷新列表</el-button>
    </el-empty>

  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import JobSearchPanel from '@/components/JobSearchPanel.vue'
import { jobApi, applicationApi, statsApi } from '@/api'
import { useUserStore } from '@/stores/user'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { buildJobFilterOptions, filterJobs } from '@/utils/jobSearch'

const store = useUserStore()
const router = useRouter()
const jobs = ref([])
const loading = ref(false)
const isTalent = computed(() => store.isTalent)
const isEnterprise = computed(() => store.isEnterprise)
const applyingId = ref(null)
const stats = ref({})
const filters = ref({
  keyword: '',
  city: '',
  salaryRange: null,
  companyName: '',
})
const filteredJobs = computed(() => filterJobs(jobs.value, filters.value))
const filterOptions = computed(() => buildJobFilterOptions(jobs.value))
const hasActiveFilters = computed(() => {
  const hasTextFilters = Boolean(filters.value.keyword || filters.value.city || filters.value.companyName)
  const bounds = filterOptions.value.salaryBounds
  const range = filters.value.salaryRange

  const hasSalaryFilter = Array.isArray(range)
    && range.length === 2
    && bounds
    && (range[0] !== bounds.min || range[1] !== bounds.max)

  return hasTextFilters || hasSalaryFilter
})

onMounted(async () => {
  loadAll()
  const statsRes = await statsApi.public().catch(() => null)
  if (statsRes?.code === 200) stats.value = statsRes.data
})

async function loadAll() {
  loading.value = true
  try {
    const res = await jobApi.list()
    if (res.code === 200) jobs.value = res.data
  } finally {
    loading.value = false
  }
}

function updateFilters(nextFilters) {
  filters.value = {
    ...filters.value,
    ...nextFilters,
  }
}

function resetFilters() {
  filters.value = {
    keyword: '',
    city: '',
    salaryRange: null,
    companyName: '',
  }
}

async function apply(jobId) {
  applyingId.value = jobId
  const res = await applicationApi.apply(jobId)
  applyingId.value = null
  if (res.code === 200) {
    ElMessage.success('申请已提交')
  } else {
    const msg = res.message || ''
    if (msg.includes('人才档案')) {
      ElMessageBox.confirm('请先创建人才档案后才能申请岗位', '提示', {
        confirmButtonText: '前往创建',
        cancelButtonText: '取消',
        type: 'warning',
      }).then(() => {
        router.push('/my-profile')
      }).catch(() => {})
    } else {
      ElMessage.error(msg)
    }
  }
}

async function deleteJob(id) {
  try {
    await ElMessageBox.confirm('确定要删除该岗位吗？此操作不可恢复。', '确认删除', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }
  const res = await jobApi.delete(id)
  if (res.code === 200) { ElMessage.success('已删除'); loadAll() }
}

const isMyJob = (job) => String(job.company?.user?.id) === String(store.userId)
</script>

<style scoped>
.page-wrap { padding: 40px; max-width: 1280px; margin: 0 auto; }
.page-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 28px; gap: 24px; flex-wrap: wrap; }
.page-title-block { flex: 1; min-width: 200px; }
.page-header h2 { font-size: 24px; color: var(--tp-text); font-weight: 600; margin: 0 0 8px 0; }
.page-intro { color: var(--tp-text-secondary); font-size: 14px; margin: 0 0 12px 0; line-height: 1.5; }
.page-stats { margin-top: 4px; }
.page-stats .el-tag { font-size: 13px; }
.stats-filtered { font-weight: normal; opacity: 0.9; }
.empty-with-actions { padding: 40px 20px; }
.empty-with-actions .empty-tip { margin-bottom: 16px; color: var(--tp-text-secondary); font-size: 14px; }
.job-card { margin-bottom: 20px; cursor: default; border-radius: var(--tp-radius); transition: box-shadow 0.2s; }
.job-card:hover { box-shadow: var(--tp-shadow-hover); }
.job-title { font-size: 16px; font-weight: bold; margin-bottom: 6px; color: var(--tp-text); }
.job-company { color: var(--tp-primary); font-size: 14px; }
.job-city, .job-salary { color: var(--tp-text-secondary); font-size: 13px; margin-top: 4px; }
.page-wrap :deep(.el-dialog) { border-radius: var(--tp-radius); }
</style>
