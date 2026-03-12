<template>
  <div class="page-wrap">
    <div class="page-header">
      <h2>{{ isEnterprise ? '收到的求职申请' : '我的求职申请' }}</h2>
      <p v-if="isEnterprise" class="page-intro">查看并处理人才投递的求职申请，通过或拒绝后将通知对方</p>
      <p v-else class="page-intro">查看您投递的岗位申请状态，企业处理后将更新为已通过或已拒绝</p>
    </div>

    <!-- 统计条 -->
    <div v-if="applications.length" class="stats-row">
      <div class="stat-item">
        <span class="stat-num">{{ stats.pending }}</span>
        <span class="stat-label">待处理</span>
      </div>
      <div class="stat-item stat-success">
        <span class="stat-num">{{ stats.accepted }}</span>
        <span class="stat-label">已通过</span>
      </div>
      <div class="stat-item stat-danger">
        <span class="stat-num">{{ stats.rejected }}</span>
        <span class="stat-label">已拒绝</span>
      </div>
    </div>

    <!-- 人才：我提交的申请 -->
    <template v-if="isTalent">
      <el-empty v-if="!applications.length" description="暂未申请任何岗位" class="empty-with-actions">
      <p class="empty-tip">浏览岗位并提交申请，企业处理后会在此展示状态</p>
      <el-button type="primary" @click="$router.push('/jobs')">去浏览岗位</el-button>
    </el-empty>
      <el-table v-else :data="applications" stripe border class="applications-table">
        <el-table-column label="岗位名称">
          <template #default="{ row }">
            <el-link type="primary" @click="$router.push(`/jobs/${row.job?.id}`)">
              {{ row.job?.title }}
            </el-link>
          </template>
        </el-table-column>
        <el-table-column label="企业">
          <template #default="{ row }">
            <el-link
              type="primary"
              :underline="false"
              @click="openCompanyDetail(row.job?.company)"
            >
              {{ row.job?.company?.companyName || '未知企业' }}
            </el-link>
          </template>
        </el-table-column>
        <el-table-column label="城市" width="90">
          <template #default="{ row }">{{ row.job?.city }}</template>
        </el-table-column>
        <el-table-column label="申请状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="申请时间" width="120">
          <template #default="{ row }">{{ row.applyTime?.substring(0,10) }}</template>
        </el-table-column>
      </el-table>
    </template>

    <!-- 企业：收到的申请 -->
    <template v-if="isEnterprise">
      <el-empty v-if="!applications.length" description="暂未收到求职申请" class="empty-with-actions">
      <p class="empty-tip">发布岗位后，人才投递的申请将在此展示</p>
      <el-button type="primary" @click="$router.push('/jobs')">去发布岗位</el-button>
    </el-empty>
      <el-table v-else :data="applications" stripe border class="applications-table">
        <el-table-column label="求职者">
          <template #default="{ row }">
            <el-link type="primary" @click="$router.push(`/talents/${row.talent?.id}`)">
              {{ row.talent?.realName || row.talent?.user?.username }}
            </el-link>
          </template>
        </el-table-column>
        <el-table-column label="求职意向" min-width="180">
          <template #default="{ row }">
            <div class="intent-title">{{ row.talent?.expectedPosition || '未填写岗位方向' }}</div>
            <div class="muted-line">
              {{ row.talent?.education || '学历未填' }}
              <span v-if="row.talent?.major"> · {{ row.talent?.major }}</span>
              <span v-if="row.talent?.workYears"> · {{ row.talent?.workYears }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="技能">
          <template #default="{ row }">
            <el-tag v-for="sk in splitSkills(row.talent?.skills)" :key="sk"
              size="small" style="margin:2px">{{ sk }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="申请岗位">
          <template #default="{ row }">{{ row.job?.title }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="申请时间" width="120">
          <template #default="{ row }">{{ row.applyTime?.substring(0,10) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <el-button v-if="row.status === 'PENDING'" size="small" type="success"
              @click="updateStatus(row.id, 'ACCEPTED')">通过</el-button>
            <el-button v-if="row.status === 'PENDING'" size="small" type="danger"
              @click="updateStatus(row.id, 'REJECTED')">拒绝</el-button>
            <span v-if="row.status !== 'PENDING'" style="color:#aaa;font-size:12px">已处理</span>
          </template>
        </el-table-column>
      </el-table>
    </template>

    <CompanyDetailDialog
      v-model="companyDialogVisible"
      :company-id="activeCompany?.id"
      :company-name="activeCompany?.companyName"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import CompanyDetailDialog from '@/components/CompanyDetailDialog.vue'
import { applicationApi } from '@/api'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const store = useUserStore()
const isTalent = computed(() => store.isTalent)
const isEnterprise = computed(() => store.isEnterprise)
const applications = ref([])
const companyDialogVisible = ref(false)
const activeCompany = ref(null)

onMounted(load)

async function load() {
  if (store.isTalent) {
    const res = await applicationApi.my()
    if (res.code === 200) applications.value = res.data
  } else if (store.isEnterprise) {
    const res = await applicationApi.companyApplications()
    if (res.code === 200) applications.value = res.data
  }
}

async function updateStatus(id, status) {
  const res = await applicationApi.updateStatus(id, status)
  if (res.code === 200) {
    ElMessage.success(status === 'ACCEPTED' ? '已通过' : '已拒绝')
    load()
  }
}

function openCompanyDetail(company) {
  if (!company?.id) {
    ElMessage.warning('该企业暂无可查看的详情')
    return
  }
  activeCompany.value = company
  companyDialogVisible.value = true
}

const splitSkills = (s) => s ? s.split(/[,，]/).map(t => t.trim()).filter(Boolean).slice(0, 4) : []
const statusType = (s) => ({ PENDING: 'warning', ACCEPTED: 'success', REJECTED: 'danger' }[s] || '')
const statusLabel = (s) => ({ PENDING: '待处理', ACCEPTED: '已通过', REJECTED: '已拒绝' }[s] || s)

const stats = computed(() => {
  const p = applications.value.filter(a => a.status === 'PENDING').length
  const a = applications.value.filter(a => a.status === 'ACCEPTED').length
  const r = applications.value.filter(a => a.status === 'REJECTED').length
  return { pending: p, accepted: a, rejected: r }
})
</script>

<style scoped>
.page-wrap { padding: 40px; max-width: 1000px; margin: 0 auto; }
.page-header { margin-bottom: 20px; }
.page-wrap h2 { font-size: 24px; color: var(--tp-text); font-weight: 600; margin: 0 0 8px 0; }
.page-intro { color: var(--tp-text-secondary); font-size: 14px; margin: 0; line-height: 1.5; }
.stats-row { display: flex; gap: 24px; margin-bottom: 24px; flex-wrap: wrap; }
.stat-item { padding: 12px 20px; background: linear-gradient(135deg, #f0f4f8, #e8eef4); border-radius: var(--tp-radius-sm); display: flex; flex-direction: column; align-items: center; min-width: 88px; }
.stat-item .stat-num { font-size: 24px; font-weight: bold; color: var(--tp-primary); }
.stat-item .stat-label { font-size: 12px; color: var(--tp-text-secondary); margin-top: 4px; }
.stat-item.stat-success .stat-num { color: #67c23a; }
.stat-item.stat-danger .stat-num { color: #f56c6c; }
.empty-with-actions { padding: 40px 20px; }
.empty-with-actions .empty-tip { margin-bottom: 16px; color: var(--tp-text-secondary); font-size: 14px; }
.page-wrap :deep(.el-table) { border-radius: var(--tp-radius-sm); }
.applications-table :deep(.el-table__cell) { vertical-align: top; }
.intent-title { font-weight: 600; color: var(--tp-text); line-height: 1.5; }
.muted-line { margin-top: 4px; color: var(--tp-text-secondary); font-size: 12px; }
</style>
