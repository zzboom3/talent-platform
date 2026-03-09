<template>
  <div class="page-wrap">
    <el-button @click="$router.back()" style="margin-bottom:16px">← 返回</el-button>
    <el-card v-if="job" shadow="always">
      <div style="display:flex;justify-content:space-between;align-items:flex-start">
        <div>
          <h2>{{ job.title }}</h2>
          <p style="color:#1a73e8;margin:8px 0">{{ job.company?.companyName }}</p>
          <p>📍 {{ job.city || '不限' }} &nbsp;|&nbsp; 💰 {{ job.salaryRange || '薪资面议' }}</p>
        </div>
        <el-button v-if="isTalent" type="primary" size="large" @click="apply">申请该岗位</el-button>
      </div>
      <el-divider />
      <h3>岗位要求</h3>
      <p style="margin-top:8px;white-space:pre-line;color:#555">{{ job.requirements || '暂无' }}</p>
      <el-divider />
      <h3>岗位描述</h3>
      <p style="margin-top:8px;white-space:pre-line;color:#555">{{ job.description || '暂无' }}</p>
    </el-card>
    <el-empty v-else description="岗位不存在" />
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { jobApi, applicationApi } from '@/api'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const route = useRoute()
const job = ref(null)
const store = useUserStore()
const isTalent = computed(() => store.isTalent)

onMounted(async () => {
  const res = await jobApi.getById(route.params.id)
  if (res.code === 200) job.value = res.data
})

async function apply() {
  const res = await applicationApi.apply(job.value.id)
  if (res.code === 200) ElMessage.success('申请已提交')
  else ElMessage.error(res.message)
}
</script>

<style scoped>
.page-wrap { padding: 32px 60px; max-width: 860px; margin: 0 auto; }
</style>
