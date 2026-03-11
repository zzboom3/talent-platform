<template>
  <div class="admin-wrap">
    <div class="admin-sidebar">
      <div class="sidebar-title">🛠 后台管理</div>
      <el-menu :default-active="$route.path" router>
        <el-menu-item index="/admin">📊 数据概览</el-menu-item>
        <el-menu-item index="/admin/users">👤 用户管理</el-menu-item>
        <el-menu-item index="/admin/news">📰 资讯管理</el-menu-item>
        <el-menu-item index="/admin/courses">📚 课程管理</el-menu-item>
        <el-menu-item index="/admin/companies">🏢 企业管理</el-menu-item>
        <el-menu-item index="/admin/showcase">⭐ 人才管理</el-menu-item>
        <el-menu-item index="/admin/monitor">📈 系统监控</el-menu-item>
        <el-menu-item index="/admin/blockchain">🔗 区块链管理</el-menu-item>
      </el-menu>
    </div>
    <div class="admin-content">
      <h2 style="margin-bottom:24px">区块链管理</h2>

      <!-- 统计卡片 -->
      <el-row :gutter="16" style="margin-bottom:24px">
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-icon">📦</div>
            <div class="stat-num">{{ stats.blockCount ?? '-' }}</div>
            <div class="stat-label">区块总数</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-icon">📜</div>
            <div class="stat-num">{{ stats.certificateCount ?? '-' }}</div>
            <div class="stat-label">存证证书数</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-icon">📏</div>
            <div class="stat-num">{{ stats.chainHeight ?? '-' }}</div>
            <div class="stat-label">链高度</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card" :class="{ danger: !stats.chainValid }">
            <div class="stat-icon">{{ stats.chainValid ? '✓' : '✗' }}</div>
            <div class="stat-num">{{ stats.chainValid ? '正常' : '异常' }}</div>
            <div class="stat-label">链完整性</div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 链状态概览 -->
      <el-card shadow="hover" style="margin-bottom:24px">
        <template #header>
          <span><strong>链健康状态</strong></span>
          <el-button size="small" style="float:right" @click="refreshAll">刷新</el-button>
        </template>
        <el-row :gutter="24" align="middle">
          <el-col :span="8">
            <div class="verify-status">
              <el-icon :size="40" :color="stats.chainValid ? '#67c23a' : '#f56c6c'">
                <component :is="stats.chainValid ? 'CircleCheckFilled' : 'CircleCloseFilled'" />
              </el-icon>
              <div>
                <div style="font-weight:bold;font-size:16px">{{ stats.chainValid ? '链完整性验证通过' : '链完整性异常，请检查' }}</div>
                <div style="color:#888;font-size:13px">基于 SHA-256 哈希链校验</div>
              </div>
            </div>
          </el-col>
          <el-col :span="16">
            <div v-if="stats.latestBlockHash" class="latest-info">
              <div class="info-row"><span class="label">最新区块 #:</span> {{ stats.latestBlockIndex }}</div>
              <div class="info-row"><span class="label">最新区块哈希:</span>
                <code>{{ stats.latestBlockHash }}</code>
              </div>
              <div class="info-row"><span class="label">最新出块时间:</span>
                {{ stats.latestBlockTime ? new Date(stats.latestBlockTime).toLocaleString('zh-CN') : '-' }}
              </div>
            </div>
            <el-empty v-else description="暂无区块数据" :image-size="60" />
          </el-col>
        </el-row>
      </el-card>

      <!-- 标签页 -->
      <el-tabs v-model="activeTab">
        <!-- 区块列表 -->
        <el-tab-pane label="区块列表" name="blocks">
          <el-card shadow="hover">
            <div style="margin-bottom:16px;display:flex;gap:12px;flex-wrap:wrap">
              <el-input v-model="blockSearch" placeholder="按哈希搜索区块..." clearable style="width:320px" @keyup.enter="searchBlockByHash">
                <template #append><el-button @click="searchBlockByHash">搜索</el-button></template>
              </el-input>
              <el-button @click="clearBlockSearch">显示全部</el-button>
              <el-select v-model="blockTypeFilter" placeholder="数据类型" clearable style="width:120px">
                <el-option label="全部" value="" />
                <el-option label="创世块" value="genesis" />
                <el-option label="证书存证" value="certificate" />
              </el-select>
            </div>
            <el-table :data="filteredBlocks" stripe border>
              <el-table-column prop="blockIndex" label="区块#" width="80" />
              <el-table-column label="类型" width="100">
                <template #default="{ row }">
                  <el-tag :type="row.blockIndex === 0 ? 'warning' : 'success'" size="small">
                    {{ row.blockIndex === 0 ? '创世块' : '证书存证' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="区块哈希" min-width="200">
                <template #default="{ row }">
                  <code class="hash-cell">{{ row.hash }}</code>
                </template>
              </el-table-column>
              <el-table-column label="前序哈希" min-width="200">
                <template #default="{ row }">
                  <code class="hash-cell">{{ row.previousHash }}</code>
                </template>
              </el-table-column>
              <el-table-column label="时间戳" width="180">
                <template #default="{ row }">{{ new Date(row.timestamp).toLocaleString('zh-CN') }}</template>
              </el-table-column>
              <el-table-column label="存证概要" min-width="180">
                <template #default="{ row }">{{ getBlockSummary(row) }}</template>
              </el-table-column>
              <el-table-column label="操作" width="90" fixed="right">
                <template #default="{ row }">
                  <el-button size="small" link type="primary" @click="openBlockDetail(row)">详情</el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-if="!filteredBlocks.length" description="暂无区块数据" style="padding:40px 0" />
          </el-card>
        </el-tab-pane>

        <!-- 证书存证 -->
        <el-tab-pane label="证书存证" name="certificates">
          <el-card shadow="hover">
            <div style="margin-bottom:16px">
              <el-input v-model="certSearch" placeholder="搜索证书号/用户名/课程名..." clearable style="width:320px" />
            </div>
            <el-table :data="filteredCertificates" stripe border>
              <el-table-column prop="certificateNo" label="证书编号" width="160" />
              <el-table-column prop="username" label="持有人" width="110" />
              <el-table-column prop="courseName" label="课程名称" show-overflow-tooltip min-width="160" />
              <el-table-column prop="blockIndex" label="区块#" width="80" />
              <el-table-column label="区块哈希" min-width="200">
                <template #default="{ row }">
                  <code class="hash-cell">{{ row.blockHash }}</code>
                </template>
              </el-table-column>
              <el-table-column label="上链时间" width="180">
                <template #default="{ row }">{{ formatCertTime(row.issueTime) }}</template>
              </el-table-column>
              <el-table-column prop="status" label="状态" width="90">
                <template #default="{ row }">
                  <el-tag :type="row.status === 'VALID' ? 'success' : 'info'" size="small">{{ row.status === 'VALID' ? '有效' : row.status }}</el-tag>
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-if="!filteredCertificates.length" description="暂无存证证书" style="padding:40px 0" />
          </el-card>
        </el-tab-pane>

        <!-- 链健康诊断 -->
        <el-tab-pane label="链健康诊断" name="diagnosis">
          <el-card shadow="hover">
            <div class="diagnosis-section">
              <h4>🔗 链结构说明</h4>
              <p>本平台采用模拟区块链技术，使用 SHA-256 哈希链实现学习证书的不可篡改存证。每个区块包含：</p>
              <ul>
                <li><strong>创世区块 (#0)</strong>：链的起点，包含平台标识信息</li>
                <li><strong>证书区块 (#1+)</strong>：用户完成课程后颁发的证书数据，包含用户ID、课程信息、证书编号、完成时间等</li>
              </ul>
            </div>
            <div class="diagnosis-section">
              <h4>🔐 完整性校验</h4>
              <el-descriptions :column="1" border>
                <el-descriptions-item label="校验算法">SHA-256</el-descriptions-item>
                <el-descriptions-item label="校验内容">区块索引 + 前序哈希 + 时间戳 + 数据 + Nonce</el-descriptions-item>
                <el-descriptions-item label="链式关系">每个区块的 previousHash 必须等于前一区块的 hash</el-descriptions-item>
                <el-descriptions-item label="当前状态">
                  <el-tag :type="stats.chainValid ? 'success' : 'danger'">
                    {{ stats.chainValid ? '通过' : '失败' }}
                  </el-tag>
                </el-descriptions-item>
              </el-descriptions>
            </div>
            <div class="diagnosis-section">
              <h4>📊 诊断结果</h4>
              <el-result
                :icon="stats.chainValid ? 'success' : 'error'"
                :title="stats.chainValid ? '链健康，无异常' : '链存在异常，请排查'"
              >
                <template #sub-title>
                  <span v-if="stats.chainValid">区块链运行正常，所有 {{ stats.blockCount }} 个区块已验证通过。</span>
                  <span v-else>哈希链校验未通过，可能存在数据篡改或链断裂。</span>
                </template>
              </el-result>
            </div>
          </el-card>
        </el-tab-pane>
      </el-tabs>

      <!-- 区块详情抽屉 -->
      <el-drawer v-model="detailDrawerVisible" title="区块详情" size="500" direction="rtl">
        <template v-if="selectedBlock">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="区块索引">{{ selectedBlock.blockIndex }}</el-descriptions-item>
            <el-descriptions-item label="类型">{{ selectedBlock.blockIndex === 0 ? '创世块' : '证书存证' }}</el-descriptions-item>
            <el-descriptions-item label="区块哈希"><code class="hash-cell">{{ selectedBlock.hash }}</code></el-descriptions-item>
            <el-descriptions-item label="前序哈希"><code class="hash-cell">{{ selectedBlock.previousHash }}</code></el-descriptions-item>
            <el-descriptions-item label="时间戳">{{ new Date(selectedBlock.timestamp).toLocaleString('zh-CN') }}</el-descriptions-item>
            <el-descriptions-item label="Nonce">{{ selectedBlock.nonce }}</el-descriptions-item>
          </el-descriptions>
          <div style="margin-top:16px">
            <div style="font-weight:bold;margin-bottom:8px">原始数据</div>
            <pre class="block-data">{{ formatBlockData(selectedBlock.data) }}</pre>
          </div>
        </template>
      </el-drawer>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { blockchainApi } from '@/api'
import { ElMessage } from 'element-plus'

const activeTab = ref('blocks')
const blocks = ref([])
const certificates = ref([])
const stats = ref({})
const blockSearch = ref('')
const certSearch = ref('')
const blockTypeFilter = ref('')
const detailDrawerVisible = ref(false)
const selectedBlock = ref(null)

const filteredBlocks = computed(() => {
  let list = blockSearch.value
    ? blocks.value.filter(b => (b.hash || '').toLowerCase().includes(blockSearch.value.toLowerCase()))
    : [...blocks.value]
  if (blockTypeFilter.value === 'genesis') list = list.filter(b => b.blockIndex === 0)
  if (blockTypeFilter.value === 'certificate') list = list.filter(b => b.blockIndex > 0)
  return list
})

const filteredCertificates = computed(() => {
  if (!certSearch.value) return certificates.value
  const q = certSearch.value.toLowerCase()
  return certificates.value.filter(c =>
    (c.certificateNo || '').toLowerCase().includes(q) ||
    (c.username || '').toLowerCase().includes(q) ||
    (c.courseName || '').toLowerCase().includes(q)
  )
})

onMounted(() => refreshAll())

async function refreshAll() {
  await Promise.all([loadStats(), loadBlocks(), loadCertificates()])
}

async function loadStats() {
  const res = await blockchainApi.adminStats().catch(() => null)
  if (res?.code === 200) stats.value = res.data
}

async function loadBlocks() {
  const res = await blockchainApi.getChain().catch(() => null)
  if (res?.code === 200) blocks.value = res.data || []
}

async function loadCertificates() {
  const res = await blockchainApi.adminCertificates().catch(() => null)
  if (res?.code === 200) certificates.value = res.data || []
}

function searchBlockByHash() {
  if (!blockSearch.value.trim()) return
  blockchainApi.getBlock(blockSearch.value.trim()).then(res => {
    if (res?.code === 200) blocks.value = [res.data]
    else ElMessage.warning('未找到该哈希对应的区块')
  }).catch(() => ElMessage.warning('查询失败'))
}

function clearBlockSearch() {
  blockSearch.value = ''
  blockTypeFilter.value = ''
  loadBlocks()
}

function getBlockSummary(row) {
  if (row.blockIndex === 0) return '创世区块'
  try {
    const d = JSON.parse(row.data || '{}')
    if (d.courseName) return `证书: ${d.courseName} - ${d.username || ''}`
    return d.certificateNo || row.data?.substring(0, 40) + '...' || '-'
  } catch {
    return row.data?.substring(0, 50) + (row.data?.length > 50 ? '...' : '') || '-'
  }
}

function openBlockDetail(row) {
  selectedBlock.value = row
  detailDrawerVisible.value = true
}

function formatBlockData(data) {
  try {
    return JSON.stringify(JSON.parse(data), null, 2)
  } catch {
    return data || '-'
  }
}

function formatCertTime(t) {
  if (!t) return '-'
  return new Date(t).toLocaleString('zh-CN')
}
</script>

<style scoped>
.admin-wrap { display: flex; min-height: calc(100vh - 60px); }
.admin-sidebar { width: 200px; background: var(--tp-card); border-right: 1px solid var(--tp-border); flex-shrink: 0; }
.sidebar-title { padding: 20px 16px; font-weight: bold; font-size: 15px; color: var(--tp-primary); border-bottom: 1px solid var(--tp-border); }
.admin-content { flex: 1; padding: 32px; background: var(--tp-bg); }
.admin-content h2 { color: var(--tp-text); font-weight: 600; margin-bottom: 24px; }

.stat-card { text-align: center; border-radius: var(--tp-radius); }
.stat-card .stat-icon { font-size: 28px; margin-bottom: 8px; }
.stat-card .stat-num { font-size: 24px; font-weight: bold; color: var(--tp-primary); }
.stat-card .stat-label { font-size: 13px; color: var(--tp-text-secondary); }
.stat-card.danger .stat-num { color: #f56c6c; }

.verify-status { display: flex; align-items: center; gap: 12px; }
.latest-info .info-row { margin: 6px 0; font-size: 13px; }
.latest-info .label { color: var(--tp-text-secondary); margin-right: 8px; }
.hash-cell { font-size: 11px; word-break: break-all; color: var(--tp-primary); background: #e3f2fd; padding: 2px 6px; border-radius: var(--tp-radius-sm); }
.block-data { font-size: 12px; background: var(--tp-bg); padding: 12px; border-radius: var(--tp-radius-sm); white-space: pre-wrap; word-break: break-all; max-height: 400px; overflow: auto; }

.diagnosis-section { margin-bottom: 24px; }
.diagnosis-section h4 { margin-bottom: 12px; font-size: 15px; color: var(--tp-text); }
.diagnosis-section p, .diagnosis-section ul { color: var(--tp-text-secondary); font-size: 14px; line-height: 1.6; }
.diagnosis-section ul { margin: 8px 0 0 20px; }
.admin-content :deep(.el-card) { border-radius: var(--tp-radius); }
.admin-content :deep(.el-table) { border-radius: var(--tp-radius-sm); }
</style>
