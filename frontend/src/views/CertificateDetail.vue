<template>
  <div class="page-wrap">
    <el-button @click="$router.back()" style="margin-bottom:16px">← 返回</el-button>
    <div v-if="cert" class="cert-detail">
      <el-card shadow="hover" class="cert-paper">
        <div class="cert-border">
          <div class="cert-title">学习完成证书</div>
          <div class="cert-subtitle">CERTIFICATE OF COMPLETION</div>
          <el-divider />
          <p class="cert-body">
            兹证明 <strong>{{ cert.user?.username }}</strong> 已完成
            <strong>「{{ cert.course?.title }}」</strong> 全部课程学习
          </p>
          <div class="cert-info">
            <div>证书编号: <code>{{ cert.certificateNo }}</code></div>
            <div>颁发日期: {{ cert.issueTime?.substring(0, 10) }}</div>
            <div>状态: <el-tag :type="cert.status === 'VALID' ? 'success' : 'danger'" size="small">{{ cert.status }}</el-tag></div>
          </div>
        </div>
      </el-card>

      <el-card shadow="hover" style="margin-top:20px" v-if="cert.blockHash">
        <template #header><strong>认证信息</strong></template>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="认证状态">
            <el-tag type="success">已完成可信存证</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="存证编号">
            <code>{{ cert.certificateNo }}</code>
          </el-descriptions-item>
          <el-descriptions-item label="颁发说明">
            该证书已完成平台认证，可作为课程学习完成凭证使用。
          </el-descriptions-item>
        </el-descriptions>
      </el-card>
    </div>
    <el-empty v-if="!cert && !loading" description="证书不存在" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { blockchainApi } from '@/api'

const route = useRoute()
const cert = ref(null)
const loading = ref(true)

onMounted(async () => {
  const res = await blockchainApi.getCertificate(route.params.id).catch(() => null)
  if (res?.code === 200) {
    cert.value = res.data.certificate
  }
  loading.value = false
})
</script>

<style scoped>
.page-wrap { padding: 32px 40px; max-width: 900px; margin: 0 auto; }
.page-wrap .el-card { border-radius: var(--tp-radius); }
.cert-paper { background: #fffdf5; }
.cert-border { border: 3px double #c0a060; padding: 40px; text-align: center; }
.cert-title { font-size: 28px; font-weight: bold; color: var(--tp-primary); }
.cert-subtitle { font-size: 14px; color: var(--tp-text-secondary); letter-spacing: 4px; margin-top: 4px; }
.cert-body { font-size: 16px; line-height: 2; margin: 24px 0; color: var(--tp-text); }
.cert-info { text-align: left; font-size: 14px; color: var(--tp-text-secondary); }
.cert-info > div { margin: 8px 0; }
</style>
