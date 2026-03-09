<template>
  <div class="page-wrap">
    <div class="page-header">
      <h2>岗位列表</h2>
      <div style="display:flex;gap:12px;align-items:center">
        <el-input v-model="keyword" placeholder="搜索岗位" prefix-icon="Search"
          clearable style="width:220px" @keyup.enter="doSearch" @clear="loadAll" />
        <el-button type="primary" @click="doSearch">搜索</el-button>
        <el-button v-if="isEnterprise" type="success" @click="openCreate">发布岗位</el-button>
      </div>
    </div>

    <el-row :gutter="20">
      <el-col :span="8" v-for="job in list" :key="job.id">
        <el-card shadow="hover" class="job-card">
          <div class="job-title">{{ job.title }}</div>
          <div class="job-company">{{ job.company?.companyName || '未知企业' }}</div>
          <div class="job-city">📍 {{ job.city || '不限' }}</div>
          <div class="job-salary">💰 {{ job.salaryRange || '薪资面议' }}</div>
          <div style="margin-top:12px;display:flex;gap:8px">
            <el-button size="small" @click="$router.push(`/jobs/${job.id}`)">查看详情</el-button>
            <el-button v-if="isTalent" size="small" type="primary" @click="apply(job.id)">
              申请岗位
            </el-button>
            <el-button v-if="isEnterprise && isMyJob(job)" size="small" type="danger"
              @click="deleteJob(job.id)">删除</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
    <el-empty v-if="!list.length" description="暂无岗位信息" />

    <!-- 发布/编辑岗位弹窗 -->
    <el-dialog v-model="dialogVisible" title="发布岗位" width="520px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="岗位名称"><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="城市"><el-input v-model="form.city" /></el-form-item>
        <el-form-item label="薪资范围"><el-input v-model="form.salaryRange" placeholder="如：10k-20k" /></el-form-item>
        <el-form-item label="岗位要求">
          <el-input v-model="form.requirements" type="textarea" :rows="3"
            placeholder="填写技能要求，便于智能匹配" />
        </el-form-item>
        <el-form-item label="岗位描述">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveJob">发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { jobApi, applicationApi } from '@/api'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const store = useUserStore()
const list = ref([])
const keyword = ref('')
const dialogVisible = ref(false)
const form = reactive({ title: '', city: '', salaryRange: '', requirements: '', description: '' })
const isTalent = computed(() => store.isTalent)
const isEnterprise = computed(() => store.isEnterprise)

onMounted(loadAll)

async function loadAll() {
  const res = await jobApi.list()
  if (res.code === 200) list.value = res.data
}

async function doSearch() {
  if (!keyword.value.trim()) { loadAll(); return }
  const res = await jobApi.list()
  if (res.code === 200) {
    list.value = res.data.filter(j =>
      j.title.includes(keyword.value) ||
      (j.requirements || '').includes(keyword.value)
    )
  }
}

function openCreate() { Object.assign(form, { title: '', city: '', salaryRange: '', requirements: '', description: '' }); dialogVisible.value = true }

async function saveJob() {
  const res = await jobApi.create(form)
  if (res.code === 200) {
    ElMessage.success('发布成功')
    dialogVisible.value = false
    loadAll()
  } else ElMessage.error(res.message)
}

async function apply(jobId) {
  const res = await applicationApi.apply(jobId)
  if (res.code === 200) ElMessage.success('申请已提交')
  else ElMessage.error(res.message)
}

async function deleteJob(id) {
  const res = await jobApi.delete(id)
  if (res.code === 200) { ElMessage.success('已删除'); loadAll() }
}

const isMyJob = (job) => job.company?.user?.id == store.userId
</script>

<style scoped>
.page-wrap { padding: 24px 40px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.page-header h2 { font-size: 22px; }
.job-card { margin-bottom: 20px; cursor: default; }
.job-card:hover { box-shadow: 0 4px 16px rgba(0,0,0,.12); }
.job-title { font-size: 16px; font-weight: bold; margin-bottom: 6px; }
.job-company { color: #1a73e8; font-size: 14px; }
.job-city, .job-salary { color: #666; font-size: 13px; margin-top: 4px; }
</style>
