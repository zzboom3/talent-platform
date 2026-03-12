<template>
  <div class="page-wrap">
    <el-breadcrumb separator="/" class="breadcrumb">
      <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
      <el-breadcrumb-item :to="{ path: '/jobs' }">岗位</el-breadcrumb-item>
      <el-breadcrumb-item v-if="job">{{ job.title }}</el-breadcrumb-item>
    </el-breadcrumb>
    <el-card v-if="job" shadow="always">
      <div style="display:flex;justify-content:space-between;align-items:flex-start">
        <div>
          <h2>{{ job.title }}</h2>
          <button type="button" class="company-trigger" @click="openCompanyDetail(job.company)">
            {{ job.company?.companyName || '未知企业' }}
          </button>
          <p>📍 {{ job.city || '不限' }} &nbsp;|&nbsp; 💰 {{ job.salaryRange || '薪资面议' }}</p>
        </div>
        <el-button v-if="isTalent" type="primary" size="large" :loading="applying" @click="apply">申请该岗位</el-button>
      </div>
      <el-divider />
      <h3>岗位要求</h3>
      <p style="margin-top:8px;white-space:pre-line;color:#555">{{ job.requirements || '暂无' }}</p>
      <el-divider />
      <h3>岗位描述</h3>
      <p style="margin-top:8px;white-space:pre-line;color:#555">{{ job.description || '暂无' }}</p>
    </el-card>
    <el-empty v-else description="岗位不存在" />

    <CompanyDetailDialog
      v-model="companyDialogVisible"
      :company-id="activeCompany?.id"
      :company-name="activeCompany?.companyName"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import CompanyDetailDialog from '@/components/CompanyDetailDialog.vue'
import { jobApi, applicationApi } from '@/api'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const job = ref(null)
const applying = ref(false)
const store = useUserStore()
const isTalent = computed(() => store.isTalent)
const companyDialogVisible = ref(false)
const activeCompany = ref(null)

onMounted(async () => {
  const res = await jobApi.getById(route.params.id)
  if (res.code === 200) job.value = res.data
})

async function apply() {
  applying.value = true
  const res = await applicationApi.apply(job.value.id)
  applying.value = false
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

function openCompanyDetail(company) {
  if (!company?.id) {
    ElMessage.warning('该企业暂无可查看的详情')
    return
  }
  activeCompany.value = company
  companyDialogVisible.value = true
}
</script>

<style scoped>
.page-wrap { padding: 40px 60px; max-width: 900px; margin: 0 auto; }
.breadcrumb { margin-bottom: 20px; font-size: 14px; }
.page-wrap .el-card { border-radius: var(--tp-radius); }
.page-wrap h2 { color: var(--tp-text); }
.page-wrap h3 { color: var(--tp-primary); margin: 16px 0 8px; font-size: 16px; }
.page-wrap p { color: var(--tp-text-secondary); }
.company-trigger {
  padding: 0;
  margin: 8px 0;
  border: 0;
  background: transparent;
  color: var(--tp-primary);
  cursor: pointer;
  font: inherit;
}
.company-trigger:hover { text-decoration: underline; }
</style>
