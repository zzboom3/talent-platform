<template>
  <div class="page-wrap">
    <h2 style="margin-bottom:24px">🏢 企业信息管理</h2>

    <el-card shadow="hover" style="max-width:700px">
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span style="font-weight:bold">{{ company ? '编辑企业信息' : '创建企业信息' }}</span>
          <el-tag v-if="company" :type="{ PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger' }[company.auditStatus]" size="large">
            {{ { PENDING: '待审核', APPROVED: '已通过', REJECTED: '审核未通过' }[company.auditStatus] || '未知' }}
          </el-tag>
        </div>
      </template>
      <el-alert v-if="company && company.auditStatus === 'PENDING'" title="您的企业信息正在等待管理员审核，审核通过后可发布岗位" type="warning" :closable="false" style="margin-bottom:16px" />
      <el-alert v-if="company && company.auditStatus === 'REJECTED'" title="您的企业信息未通过审核，请修改后等待重新审核" type="error" :closable="false" style="margin-bottom:16px" />
      <el-form :model="form" label-width="100px" style="max-width:560px">
        <el-form-item label="企业 Logo">
          <div class="logo-field">
            <el-avatar :size="72" :src="form.logoUrl" shape="square" class="company-logo">
              {{ (form.companyName || '企').slice(0, 1) }}
            </el-avatar>
            <div class="upload-actions">
              <el-upload
                :show-file-list="false"
                accept="image/*"
                :http-request="uploadLogo"
                :disabled="uploadingLogo"
              >
                <el-button :loading="uploadingLogo">上传 Logo</el-button>
              </el-upload>
              <div class="upload-tip">支持 JPG、PNG，上传后自动回填企业 Logo 地址</div>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="企业名称">
          <el-input v-model="form.companyName" placeholder="请输入企业全称" />
        </el-form-item>
        <el-form-item label="所属行业">
          <el-select v-model="form.industry" placeholder="选择行业" style="width:100%">
            <el-option v-for="i in industries" :key="i" :label="i" :value="i" />
          </el-select>
        </el-form-item>
        <el-form-item label="企业规模">
          <el-select v-model="form.scale" placeholder="选择企业规模" style="width:100%">
            <el-option v-for="s in scales" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item label="联系邮箱">
          <el-input v-model="form.contactEmail" placeholder="招聘联系邮箱" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="form.contactPhone" placeholder="招聘联系电话" />
        </el-form-item>
        <el-form-item label="企业地址">
          <el-input v-model="form.address" placeholder="公司办公地址" />
        </el-form-item>
        <el-form-item label="企业官网">
          <el-input v-model="form.website" placeholder="https://example.com" />
        </el-form-item>
        <el-form-item label="企业简介">
          <el-input v-model="form.description" type="textarea" :rows="4" placeholder="介绍公司业务、文化等" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="save">
            {{ company ? '保存修改' : '创建企业信息' }}
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 企业发布的岗位 -->
    <el-card v-if="company" shadow="hover" class="jobs-card">
      <template #header>
        <div class="jobs-header">
          <div>
            <h3>我发布的岗位</h3>
            <p class="jobs-intro">审核通过后可发布岗位，岗位将展示在岗位列表中供人才浏览申请</p>
          </div>
          <el-button type="primary" @click="$router.push('/company/jobs')">岗位管理</el-button>
        </div>
      </template>
      <el-table v-if="myJobs.length" :data="myJobs" stripe border>
        <el-table-column prop="title" label="岗位名称" />
        <el-table-column prop="city" label="城市" width="100" />
        <el-table-column prop="salaryRange" label="薪资" width="120" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'OPEN' ? 'success' : 'info'" size="small">
              {{ row.status === 'OPEN' ? '招聘中' : '已关闭' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="发布时间" width="120">
          <template #default="{ row }">{{ row.createTime?.substring(0,10) }}</template>
        </el-table-column>
      </el-table>
      <el-empty v-else description="暂无发布的岗位">
        <el-button type="primary" @click="$router.push('/company/jobs')">去发布岗位</el-button>
      </el-empty>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { companyApi, fileApi, jobApi } from '@/api'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const store = useUserStore()
const company = ref(null)
const myJobs = ref([])
const saving = ref(false)
const uploadingLogo = ref(false)
const form = reactive({ companyName: '', industry: '', contactEmail: '', contactPhone: '', scale: '', address: '', website: '', description: '', logoUrl: '' })
const industries = ['软件开发', '互联网', '人工智能', '大数据', '云计算', '金融科技', '电子商务', '教育科技', '其他']
const scales = ['1-50人', '50-200人', '200-500人', '500-1000人', '1000-5000人', '5000人以上']

onMounted(async () => {
  const res = await companyApi.getMy().catch(() => null)
  if (res?.code === 200) {
    company.value = res.data
    Object.assign(form, res.data)
  }
  const jr = await jobApi.getMy().catch(() => null)
  if (jr?.code === 200) myJobs.value = jr.data
})

async function uploadLogo(option) {
  uploadingLogo.value = true
  try {
    const res = await fileApi.upload(option.file, 'company-logos')
    if (res.code === 200) {
      form.logoUrl = res.data.url
      store.setAvatarUrl(res.data.url)
      ElMessage.success('Logo 上传成功')
      option.onSuccess?.(res.data)
    } else {
      ElMessage.error(res.message || 'Logo 上传失败')
      option.onError?.(new Error(res.message || 'Logo 上传失败'))
    }
  } catch (error) {
    const status = error?.response?.status
    const message = status === 401
      ? '登录状态已失效，请重新登录后再上传'
      : error?.code === 'ECONNABORTED'
        ? 'Logo 上传超时，请稍后重试'
        : 'Logo 上传失败'
    ElMessage.error(message)
    option.onError?.(error)
  } finally {
    uploadingLogo.value = false
  }
}

async function save() {
  if (!form.companyName.trim()) { ElMessage.warning('请填写企业名称'); return }
  saving.value = true
  try {
    let res
    if (company.value) {
      res = await companyApi.update(company.value.id, form)
    } else {
      res = await companyApi.create(form)
    }
    if (res.code === 200) {
      ElMessage.success(company.value ? '修改成功' : '创建成功')
      company.value = res.data
      if (res.data.logoUrl) store.setAvatarUrl(res.data.logoUrl)
    } else {
      ElMessage.error(res.message)
    }
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.page-wrap { padding: 40px; max-width: 900px; margin: 0 auto; }
.page-wrap h2 { font-size: 24px; color: var(--tp-text); font-weight: 600; margin-bottom: 24px; }
.jobs-card { margin-top: 32px; }
.jobs-header { display: flex; justify-content: space-between; align-items: flex-start; flex-wrap: wrap; gap: 16px; }
.jobs-header h3 { margin: 0 0 4px 0; font-size: 16px; color: var(--tp-primary); }
.jobs-intro { margin: 0; font-size: 13px; color: var(--tp-text-secondary); line-height: 1.5; }
.page-wrap .el-card { border-radius: var(--tp-radius); }
.logo-field { display: flex; align-items: center; gap: 16px; }
.company-logo { background: var(--tp-primary) !important; color: #fff !important; font-size: 24px; }
.upload-actions { display: flex; flex-direction: column; gap: 8px; }
.upload-tip { font-size: 12px; color: var(--tp-text-secondary); }
.page-wrap :deep(.el-table) { border-radius: var(--tp-radius-sm); }
</style>
