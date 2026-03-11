<template>
  <div class="page-wrap" v-loading="loading">
    <div class="page-header">
      <div>
        <h2>岗位管理</h2>
        <p class="page-intro">在此管理您企业发布的所有岗位，支持发布、编辑、开启/关闭和删除操作</p>
      </div>
      <div class="header-actions">
        <el-tag v-if="company" :type="auditTagType" size="large" effect="plain">
          企业状态：{{ auditLabel }}
        </el-tag>
        <el-tooltip v-if="!canPublish" :content="publishTip" placement="bottom">
          <span><el-button type="primary" disabled :icon="Plus">发布新岗位</el-button></span>
        </el-tooltip>
        <el-button v-else type="primary" :icon="Plus" @click="openCreate">发布新岗位</el-button>
      </div>
    </div>

    <el-alert v-if="!company" title="您还未创建企业信息，请先前往企业信息页面完善公司资料" type="warning" show-icon :closable="false" style="margin-bottom:24px">
      <template #default>
        <el-button type="primary" size="small" style="margin-top:8px" @click="$router.push('/company')">前往创建</el-button>
      </template>
    </el-alert>
    <el-alert v-else-if="company.auditStatus === 'PENDING'" title="企业信息正在等待管理员审核，审核通过后即可发布岗位" type="warning" show-icon :closable="false" style="margin-bottom:24px" />
    <el-alert v-else-if="company.auditStatus === 'REJECTED'" title="企业审核未通过，请修改企业信息后等待重新审核" type="error" show-icon :closable="false" style="margin-bottom:24px">
      <template #default>
        <el-button type="primary" size="small" style="margin-top:8px" @click="$router.push('/company')">修改企业信息</el-button>
      </template>
    </el-alert>

    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stat-row" v-if="company">
      <el-col :span="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-number">{{ myJobs.length }}</div>
          <div class="stat-label">岗位总数</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="stat-card stat-open">
          <div class="stat-number">{{ openCount }}</div>
          <div class="stat-label">招聘中</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="stat-card stat-closed">
          <div class="stat-number">{{ closedCount }}</div>
          <div class="stat-label">已关闭</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="stat-card stat-apps">
          <div class="stat-number">{{ applicationCount }}</div>
          <div class="stat-label">收到的申请</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 岗位列表 -->
    <el-card v-if="company" shadow="hover" class="jobs-table-card">
      <template #header>
        <div class="table-header">
          <span class="table-title">已发布的岗位</span>
          <el-input v-model="searchKeyword" placeholder="搜索岗位名称" clearable style="width:240px" :prefix-icon="Search" />
        </div>
      </template>
      <el-table v-if="displayJobs.length" :data="displayJobs" stripe border style="width:100%">
        <el-table-column prop="title" label="岗位名称" min-width="160">
          <template #default="{ row }">
            <router-link :to="`/jobs/${row.id}`" class="job-link">{{ row.title }}</router-link>
          </template>
        </el-table-column>
        <el-table-column prop="city" label="城市" width="100">
          <template #default="{ row }">{{ row.city || '不限' }}</template>
        </el-table-column>
        <el-table-column prop="salaryRange" label="薪资" width="120">
          <template #default="{ row }">{{ row.salaryRange || '面议' }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'OPEN' ? 'success' : 'info'" size="small">
              {{ row.status === 'OPEN' ? '招聘中' : '已关闭' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="发布时间" width="120">
          <template #default="{ row }">{{ row.createTime?.substring(0, 10) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220" align="center" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openEdit(row)">编辑</el-button>
            <el-button size="small" :type="row.status === 'OPEN' ? 'warning' : 'success'" @click="toggleStatus(row)">
              {{ row.status === 'OPEN' ? '关闭' : '开启' }}
            </el-button>
            <el-button size="small" type="danger" @click="deleteJob(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-else :description="searchKeyword ? '没有找到匹配的岗位' : '暂无发布的岗位，点击上方按钮发布第一个岗位'" />
    </el-card>

    <!-- 发布/编辑岗位弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEditing ? '编辑岗位' : '发布新岗位'" width="600px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="90px">
        <el-form-item label="岗位名称" prop="title">
          <el-input v-model="form.title" placeholder="如：Java 高级开发工程师" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="工作城市" prop="city">
          <el-input v-model="form.city" placeholder="如：京州" maxlength="50" />
        </el-form-item>
        <el-form-item label="薪资范围" prop="salaryRange">
          <el-input v-model="form.salaryRange" placeholder="如：15k-25k" maxlength="50" />
        </el-form-item>
        <el-form-item label="岗位要求" prop="requirements">
          <el-input v-model="form.requirements" type="textarea" :rows="4"
            placeholder="请详细填写技能要求、学历要求、工作经验等，有助于智能匹配" maxlength="2000" show-word-limit />
        </el-form-item>
        <el-form-item label="岗位描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="4"
            placeholder="请描述岗位职责、工作内容、福利待遇等" maxlength="2000" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">
          {{ isEditing ? '保存修改' : '确认发布' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { jobApi, companyApi, applicationApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'

const router = useRouter()
const loading = ref(false)
const company = ref(null)
const myJobs = ref([])
const applicationCount = ref(0)
const dialogVisible = ref(false)
const isEditing = ref(false)
const editingId = ref(null)
const submitting = ref(false)
const searchKeyword = ref('')
const formRef = ref(null)

const form = reactive({
  title: '',
  city: '',
  salaryRange: '',
  requirements: '',
  description: '',
})

const formRules = {
  title: [{ required: true, message: '请输入岗位名称', trigger: 'blur' }],
  city: [{ required: true, message: '请输入工作城市', trigger: 'blur' }],
  requirements: [{ required: true, message: '请填写岗位要求', trigger: 'blur' }],
}

const canPublish = computed(() => company.value?.auditStatus === 'APPROVED')
const publishTip = computed(() => {
  if (!company.value) return '请先创建企业信息'
  if (company.value.auditStatus === 'PENDING') return '企业审核通过后才可发布岗位'
  if (company.value.auditStatus === 'REJECTED') return '企业审核未通过，请修改后重新提交'
  return ''
})
const auditTagType = computed(() => ({ PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger' }[company.value?.auditStatus] || 'info'))
const auditLabel = computed(() => ({ PENDING: '待审核', APPROVED: '已通过', REJECTED: '未通过' }[company.value?.auditStatus] || '未知'))
const openCount = computed(() => myJobs.value.filter(j => j.status === 'OPEN').length)
const closedCount = computed(() => myJobs.value.filter(j => j.status !== 'OPEN').length)
const displayJobs = computed(() => {
  if (!searchKeyword.value) return myJobs.value
  const kw = searchKeyword.value.toLowerCase()
  return myJobs.value.filter(j => j.title?.toLowerCase().includes(kw))
})

onMounted(async () => {
  loading.value = true
  try {
    const [companyRes, jobsRes, appsRes] = await Promise.all([
      companyApi.getMy().catch(() => null),
      jobApi.getMy().catch(() => null),
      applicationApi.companyApplications().catch(() => null),
    ])
    if (companyRes?.code === 200) company.value = companyRes.data
    if (jobsRes?.code === 200) myJobs.value = jobsRes.data
    if (appsRes?.code === 200) {
      applicationCount.value = Array.isArray(appsRes.data) ? appsRes.data.length : 0
    }
  } finally {
    loading.value = false
  }
})

function resetForm() {
  Object.assign(form, { title: '', city: '', salaryRange: '', requirements: '', description: '' })
  editingId.value = null
  isEditing.value = false
}

function openCreate() {
  if (!company.value) {
    ElMessage.warning('请先前往"企业信息"页面创建公司资料')
    router.push('/company')
    return
  }
  resetForm()
  dialogVisible.value = true
}

function openEdit(job) {
  isEditing.value = true
  editingId.value = job.id
  Object.assign(form, {
    title: job.title || '',
    city: job.city || '',
    salaryRange: job.salaryRange || '',
    requirements: job.requirements || '',
    description: job.description || '',
  })
  dialogVisible.value = true
}

async function submitForm() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    let res
    if (isEditing.value) {
      res = await jobApi.update(editingId.value, { ...form })
    } else {
      res = await jobApi.create({ ...form })
    }
    if (res.code === 200) {
      ElMessage.success(isEditing.value ? '岗位已更新' : '岗位发布成功')
      dialogVisible.value = false
      await refreshJobs()
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } finally {
    submitting.value = false
  }
}

async function toggleStatus(job) {
  const nextStatus = job.status === 'OPEN' ? 'CLOSED' : 'OPEN'
  const label = nextStatus === 'OPEN' ? '重新开启' : '关闭'
  try {
    await ElMessageBox.confirm(`确定要${label}「${job.title}」吗？`, '确认操作', {
      confirmButtonText: label,
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch { return }

  const res = await jobApi.update(job.id, { ...job, status: nextStatus })
  if (res.code === 200) {
    ElMessage.success(`岗位已${label}`)
    await refreshJobs()
  } else {
    ElMessage.error(res.message || '操作失败')
  }
}

async function deleteJob(id) {
  try {
    await ElMessageBox.confirm('确定要删除该岗位吗？此操作不可恢复。', '确认删除', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch { return }

  const res = await jobApi.delete(id)
  if (res.code === 200) {
    ElMessage.success('岗位已删除')
    await refreshJobs()
  } else {
    ElMessage.error(res.message || '删除失败')
  }
}

async function refreshJobs() {
  const res = await jobApi.getMy().catch(() => null)
  if (res?.code === 200) myJobs.value = res.data
}
</script>

<style scoped>
.page-wrap { padding: 40px; max-width: 1200px; margin: 0 auto; }
.page-header {
  display: flex; justify-content: space-between; align-items: flex-start;
  margin-bottom: 24px; gap: 24px; flex-wrap: wrap;
}
.page-header h2 { font-size: 24px; color: var(--tp-text); font-weight: 600; margin: 0 0 8px 0; }
.page-intro { color: var(--tp-text-secondary); font-size: 14px; margin: 0; line-height: 1.5; }
.header-actions { display: flex; align-items: center; gap: 12px; flex-shrink: 0; }

.stat-row { margin-bottom: 24px; }
.stat-card {
  text-align: center; border-radius: var(--tp-radius);
  border-left: 3px solid var(--tp-primary);
}
.stat-card.stat-open { border-left-color: var(--el-color-success); }
.stat-card.stat-closed { border-left-color: var(--el-color-info); }
.stat-card.stat-apps { border-left-color: var(--el-color-warning); }
.stat-number { font-size: 28px; font-weight: 700; color: var(--tp-text); line-height: 1.2; }
.stat-label { font-size: 13px; color: var(--tp-text-secondary); margin-top: 4px; }

.jobs-table-card { border-radius: var(--tp-radius); }
.table-header { display: flex; justify-content: space-between; align-items: center; }
.table-title { font-weight: 600; font-size: 16px; color: var(--tp-text); }
.job-link { color: var(--tp-primary); text-decoration: none; font-weight: 500; }
.job-link:hover { text-decoration: underline; }

.page-wrap :deep(.el-table) { border-radius: var(--tp-radius-sm); }
.page-wrap :deep(.el-dialog) { border-radius: var(--tp-radius); }
</style>
