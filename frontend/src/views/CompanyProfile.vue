<template>
  <div class="page-wrap">
    <h2 style="margin-bottom:24px">🏢 企业信息管理</h2>

    <el-card shadow="hover" style="max-width:700px">
      <template #header>
        <span style="font-weight:bold">{{ company ? '编辑企业信息' : '创建企业信息' }}</span>
      </template>
      <el-form :model="form" label-width="100px" style="max-width:560px">
        <el-form-item label="企业名称">
          <el-input v-model="form.companyName" placeholder="请输入企业全称" />
        </el-form-item>
        <el-form-item label="所属行业">
          <el-select v-model="form.industry" placeholder="选择行业" style="width:100%">
            <el-option v-for="i in industries" :key="i" :label="i" :value="i" />
          </el-select>
        </el-form-item>
        <el-form-item label="联系邮箱">
          <el-input v-model="form.contactEmail" placeholder="招聘联系邮箱" />
        </el-form-item>
        <el-form-item label="企业简介">
          <el-input v-model="form.description" type="textarea" :rows="4" placeholder="介绍公司业务、规模、文化等" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="save">
            {{ company ? '保存修改' : '创建企业信息' }}
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 企业发布的岗位 -->
    <div style="margin-top:32px" v-if="company">
      <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px">
        <h3>我发布的岗位</h3>
        <el-button type="primary" @click="$router.push('/jobs')">前往发布</el-button>
      </div>
      <el-table :data="myJobs" stripe border empty-text="暂无发布的岗位">
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
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { companyApi, jobApi } from '@/api'
import { ElMessage } from 'element-plus'

const company = ref(null)
const myJobs = ref([])
const saving = ref(false)
const form = reactive({ companyName: '', industry: '', contactEmail: '', description: '' })
const industries = ['软件开发', '互联网', '人工智能', '大数据', '云计算', '金融科技', '电子商务', '教育科技', '其他']

onMounted(async () => {
  const res = await companyApi.getMy().catch(() => null)
  if (res?.code === 200) {
    company.value = res.data
    Object.assign(form, res.data)
  }
  const jr = await jobApi.getMy().catch(() => null)
  if (jr?.code === 200) myJobs.value = jr.data
})

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
    } else {
      ElMessage.error(res.message)
    }
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.page-wrap { padding: 32px 40px; }
</style>
