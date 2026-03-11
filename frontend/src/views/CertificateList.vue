<template>
  <div class="page-wrap">
    <h2 style="margin-bottom:24px">我的学习证书</h2>
    <el-row :gutter="20">
      <el-col :span="8" v-for="cert in certificates" :key="cert.id">
        <el-card shadow="hover" class="cert-card" @click="$router.push(`/certificates/${cert.id}`)">
          <div class="cert-header">
            <el-icon :size="36" class="cert-icon"><Medal /></el-icon>
            <div class="cert-tags">
              <el-tag :type="cert.status === 'VALID' ? 'success' : 'danger'" size="small">
                {{ cert.status === 'VALID' ? '有效' : '已撤销' }}
              </el-tag>
              <el-tag v-if="cert.blockHash" type="success" size="small">可信认证</el-tag>
            </div>
          </div>
          <div class="cert-no">{{ cert.certificateNo }}</div>
          <div class="cert-course">{{ cert.course?.title }}</div>
          <div class="cert-time">颁发时间: {{ cert.issueTime?.substring(0, 10) }}</div>
        </el-card>
      </el-col>
    </el-row>
    <el-empty v-if="!certificates.length" description="暂无证书，完成课程学习后自动颁发" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { blockchainApi } from '@/api'

const certificates = ref([])

onMounted(async () => {
  const res = await blockchainApi.myCertificates().catch(() => null)
  if (res?.code === 200) certificates.value = res.data
})
</script>

<style scoped>
.page-wrap { padding: 32px 40px; }
.page-wrap h2 { color: var(--tp-text); font-weight: 600; margin-bottom: 24px; }
.cert-card { cursor: pointer; margin-bottom: 20px; text-align: center; border-radius: var(--tp-radius); transition: transform 0.2s, box-shadow 0.2s; }
.cert-card:hover { transform: translateY(-4px); box-shadow: var(--tp-shadow-hover); }
.cert-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.cert-icon { color: var(--tp-primary); }
.cert-tags { display: flex; gap: 6px; }
.cert-no { font-size: 13px; color: var(--tp-text-secondary); font-family: monospace; }
.cert-course { font-size: 18px; font-weight: bold; margin: 8px 0; color: var(--tp-text); }
.cert-time { font-size: 13px; color: var(--tp-text-secondary); }
</style>
