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
      <div style="display:flex;justify-content:space-between;margin-bottom:20px">
        <h2>资讯管理</h2>
        <div style="display:flex;gap:12px">
          <el-button @click="runCrawler" :loading="runningCrawler">抓取资讯/政策</el-button>
          <el-button type="primary" @click="openCreate">新增公告</el-button>
        </div>
      </div>
      <div class="toolbar">
        <el-input
          v-model="filters.keyword"
          style="width:240px"
          clearable
          placeholder="搜索标题 / 来源 / 作者"
          @keyup.enter="load"
          @clear="load"
        />
        <el-select v-model="filters.category" style="width:160px" placeholder="分类筛选" @change="load">
          <el-option label="全部分类" value="" />
          <el-option label="资讯" value="NEWS" />
          <el-option label="政策" value="POLICY" />
          <el-option label="公告" value="ANNOUNCE" />
        </el-select>
        <el-select v-model="filters.reviewStatus" style="width:160px" placeholder="审核状态" @change="load">
          <el-option label="全部状态" value="" />
          <el-option label="待审核" value="PENDING" />
          <el-option label="已发布" value="APPROVED" />
          <el-option label="已驳回" value="REJECTED" />
        </el-select>
        <el-select v-model="filters.sourceType" style="width:160px" placeholder="来源类型" @change="load">
          <el-option label="全部来源" value="" />
          <el-option label="抓取内容" value="CRAWLED" />
          <el-option label="手工公告" value="MANUAL" />
        </el-select>
        <el-button type="primary" plain @click="load">查询</el-button>
        <el-button @click="resetFilters">重置</el-button>
      </div>
      <el-table :data="list" stripe border v-loading="listLoading">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="title" label="标题" show-overflow-tooltip />
        <el-table-column prop="category" label="分类" width="90">
          <template #default="{ row }">
            <el-tag :type="tagType(row.category)" size="small">{{ catLabel(row.category) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="来源" width="120">
          <template #default="{ row }">
            <el-tag :type="isManual(row) ? 'warning' : 'info'" size="small">
              {{ sourceLabel(row.sourceType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="审核状态" width="100">
          <template #default="{ row }">
            <el-tag :type="reviewTagType(row.reviewStatus)" size="small">{{ reviewLabel(row.reviewStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sourceSite" label="来源站点" width="140" show-overflow-tooltip />
        <el-table-column prop="author" label="作者" width="100" />
        <el-table-column prop="publishTime" label="发布时间" width="160">
          <template #default="{ row }">{{ row.publishTime?.substring(0,10) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="260">
          <template #default="{ row }">
            <el-button v-if="isManual(row)" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button
              v-if="isCrawled(row) && row.reviewStatus !== 'APPROVED'"
              size="small"
              type="success"
              @click="review(row.id, 'APPROVED')"
            >
              发布
            </el-button>
            <el-button
              v-if="isCrawled(row) && row.reviewStatus !== 'REJECTED'"
              size="small"
              type="warning"
              @click="review(row.id, 'REJECTED')"
            >
              驳回
            </el-button>
            <el-popconfirm title="确认删除?" @confirm="deleteNews(row.id)">
              <template #reference>
                <el-button type="danger" size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <el-dialog v-model="dialogVisible" :title="form.id ? '编辑公告' : '新增公告'" width="640px">
        <el-form :model="form" label-width="80px">
          <el-form-item label="标题"><el-input v-model="form.title" /></el-form-item>
          <el-form-item label="分类"><el-input value="公告通知" disabled /></el-form-item>
          <el-form-item label="作者"><el-input v-model="form.author" /></el-form-item>
          <el-form-item label="内容">
            <el-input v-model="form.content" type="textarea" :rows="8" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="dialogVisible=false">取消</el-button>
          <el-button type="primary" @click="save">保存</el-button>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { adminApi } from '@/api'
import { ElMessage } from 'element-plus'

const list = ref([])
const dialogVisible = ref(false)
const runningCrawler = ref(false)
const listLoading = ref(false)
const form = reactive({ id: null, title: '', category: 'ANNOUNCE', author: '管理员', content: '' })
const filters = reactive({ keyword: '', category: '', reviewStatus: '', sourceType: '' })

onMounted(load)

async function load() {
  listLoading.value = true
  try {
    const res = await adminApi.newsList({
      keyword: filters.keyword?.trim() || undefined,
      category: filters.category || undefined,
      reviewStatus: filters.reviewStatus || undefined,
      sourceType: filters.sourceType || undefined,
    })
    if (res.code === 200) list.value = res.data
  } catch (error) {
    console.error('load news failed', error)
    ElMessage.error('加载资讯列表失败')
  } finally {
    listLoading.value = false
  }
}

function resetFilters() {
  Object.assign(filters, { keyword: '', category: '', reviewStatus: '', sourceType: '' })
  load()
}

function openCreate() {
  Object.assign(form, { id: null, title: '', category: 'ANNOUNCE', author: '管理员', content: '' })
  dialogVisible.value = true
}

function openEdit(row) {
  Object.assign(form, { ...row })
  dialogVisible.value = true
}

async function save() {
  let res
  if (form.id) res = await adminApi.updateAnnouncement(form.id, form)
  else res = await adminApi.createAnnouncement(form)
  if (res.code === 200) {
    ElMessage.success('保存成功')
    dialogVisible.value = false
    load()
  }
}

async function review(id, reviewStatus) {
  const res = await adminApi.reviewNews(id, reviewStatus)
  if (res.code === 200) {
    ElMessage.success(reviewStatus === 'APPROVED' ? '已发布' : '已驳回')
    load()
  }
}

async function deleteNews(id) {
  const res = await adminApi.deleteNews(id)
  if (res.code === 200) { ElMessage.success('已删除'); load() }
}

async function runCrawler() {
  runningCrawler.value = true
  try {
    const res = await adminApi.crawlerRun()
    if (res.code === 200) {
      ElMessage.success(res.data?.running ? '采集任务已启动，正在后台执行' : `抓取完成，新增 ${res.data.totalInserted || 0} 条待审核内容`)
      await pollCrawlerStatus()
    }
  } finally {
    runningCrawler.value = false
  }
}

async function pollCrawlerStatus(maxRounds = 90) {
  for (let round = 0; round < maxRounds; round++) {
    await wait(2000)
    const statusRes = await adminApi.crawlerStatus().catch(() => null)
    if (statusRes?.code !== 200 || !statusRes.data) {
      continue
    }
    if (!statusRes.data.running) {
      ElMessage.success(`抓取完成，新增 ${statusRes.data.totalInserted || 0} 条待审核内容`)
      await load()
      return
    }
  }
  ElMessage.warning('采集仍在后台执行，可稍后刷新列表查看结果')
}

function wait(ms) {
  return new Promise(resolve => setTimeout(resolve, ms))
}

const tagType = (cat) => ({ NEWS: '', POLICY: 'success', ANNOUNCE: 'warning' }[cat] || '')
const catLabel = (cat) => ({ NEWS: '资讯', POLICY: '政策', ANNOUNCE: '公告' }[cat] || cat)
const reviewTagType = (status) => ({ PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger' }[status || 'APPROVED'] || '')
const reviewLabel = (status) => ({ PENDING: '待审核', APPROVED: '已发布', REJECTED: '已驳回' }[status || 'APPROVED'] || status)
const sourceLabel = (sourceType) => ({ MANUAL: '手工公告', CRAWLED: '抓取内容' }[sourceType || 'MANUAL'] || sourceType)
const isManual = (row) => (row.sourceType || 'MANUAL') === 'MANUAL'
const isCrawled = (row) => row.sourceType === 'CRAWLED'
</script>

<style scoped>
.admin-wrap { display: flex; min-height: calc(100vh - 60px); }
.admin-sidebar { width: 200px; background: var(--tp-card); border-right: 1px solid var(--tp-border); flex-shrink: 0; }
.sidebar-title { padding: 20px 16px; font-weight: bold; font-size: 15px; color: var(--tp-primary); border-bottom: 1px solid var(--tp-border); }
.admin-content { flex: 1; padding: 32px; background: var(--tp-bg); }
.admin-content h2 { color: var(--tp-text); font-weight: 600; }
.toolbar { display: flex; gap: 12px; margin-bottom: 16px; flex-wrap: wrap; }
.admin-content :deep(.el-table) { border-radius: var(--tp-radius-sm); }
.admin-content :deep(.el-dialog) { border-radius: var(--tp-radius); }
</style>
