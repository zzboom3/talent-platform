<template>
  <div class="page-wrap">
    <h2 style="margin-bottom:24px">区块链浏览器</h2>

    <el-row :gutter="20" style="margin-bottom:24px">
      <el-col :span="8">
        <el-card shadow="hover" class="verify-card">
          <div class="verify-status">
            <el-icon :size="32" :color="verified ? '#67c23a' : '#f56c6c'">
              <component :is="verified ? 'CircleCheckFilled' : 'CircleCloseFilled'" />
            </el-icon>
            <div>
              <div style="font-weight:bold;font-size:16px">{{ verified ? '链完整性验证通过' : '链完整性异常' }}</div>
              <div style="color:#888;font-size:13px">共 {{ blockCount }} 个区块</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="16">
        <el-input v-model="searchHash" placeholder="输入区块哈希搜索..." size="large" clearable @keyup.enter="searchBlock">
          <template #append><el-button @click="searchBlock">搜索</el-button></template>
        </el-input>
      </el-col>
    </el-row>

    <div class="chain-view">
      <div v-for="(block, i) in blocks" :key="block.id" class="block-wrapper">
        <div v-if="i > 0" class="chain-link">↓</div>
        <el-card shadow="hover" class="block-card" :class="{ genesis: block.blockIndex === 0 }">
          <div class="block-header">
            <el-tag :type="block.blockIndex === 0 ? 'warning' : 'primary'" size="large">
              # {{ block.blockIndex }}
            </el-tag>
            <span class="block-time">{{ formatTime(block.timestamp) }}</span>
          </div>
          <div class="block-field">
            <label>Hash:</label>
            <code>{{ block.hash }}</code>
          </div>
          <div class="block-field">
            <label>Prev:</label>
            <code>{{ block.previousHash }}</code>
          </div>
          <el-collapse v-if="block.blockIndex > 0">
            <el-collapse-item title="查看存证数据">
              <pre class="block-data">{{ formatData(block.data) }}</pre>
            </el-collapse-item>
          </el-collapse>
        </el-card>
      </div>
    </div>
    <el-empty v-if="!blocks.length" description="区块链为空" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { blockchainApi } from '@/api'
import { ElMessage } from 'element-plus'

const blocks = ref([])
const verified = ref(false)
const blockCount = ref(0)
const searchHash = ref('')

onMounted(async () => {
  const [chainRes, verifyRes] = await Promise.all([
    blockchainApi.getChain().catch(() => null),
    blockchainApi.verify().catch(() => null),
  ])
  if (chainRes?.code === 200) blocks.value = chainRes.data
  if (verifyRes?.code === 200) {
    verified.value = verifyRes.data.valid
    blockCount.value = verifyRes.data.blockCount
  }
})

async function searchBlock() {
  if (!searchHash.value.trim()) return
  const res = await blockchainApi.getBlock(searchHash.value.trim()).catch(() => null)
  if (res?.code === 200) {
    blocks.value = [res.data]
    ElMessage.success('找到区块')
  } else {
    ElMessage.warning('未找到该哈希对应的区块')
  }
}

function formatTime(ts) {
  return ts ? new Date(ts).toLocaleString('zh-CN') : ''
}

function formatData(data) {
  try { return JSON.stringify(JSON.parse(data), null, 2) } catch { return data }
}
</script>

<style scoped>
.page-wrap { padding: 32px 40px; }
.page-wrap h2 { color: var(--tp-text); font-weight: 600; margin-bottom: 24px; }
.verify-card { height: 100%; border-radius: var(--tp-radius); }
.verify-status { display: flex; align-items: center; gap: 12px; }
.chain-view { max-width: 800px; margin: 0 auto; }
.block-wrapper { display: flex; flex-direction: column; align-items: center; }
.chain-link { font-size: 24px; color: var(--tp-primary); margin: 4px 0; }
.block-card { width: 100%; margin-bottom: 4px; border-radius: var(--tp-radius); transition: box-shadow 0.2s; }
.block-card:hover { box-shadow: var(--tp-shadow-hover); }
.block-card.genesis { border-left: 4px solid var(--tp-warm); }
.block-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.block-time { color: var(--tp-text-secondary); font-size: 13px; }
.block-field { margin: 6px 0; font-size: 13px; }
.block-field label { color: var(--tp-text-secondary); margin-right: 8px; font-weight: bold; }
.block-field code { word-break: break-all; color: var(--tp-primary); background: #e3f2fd; padding: 2px 6px; border-radius: var(--tp-radius-sm); font-size: 12px; }
.block-data { font-size: 12px; background: var(--tp-bg); padding: 12px; border-radius: var(--tp-radius-sm); white-space: pre-wrap; word-break: break-all; }
</style>
