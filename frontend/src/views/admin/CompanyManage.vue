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
      <h2 style="margin-bottom:24px">企业管理</h2>
      <el-table :data="companies" stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column label="企业名称" min-width="160">
          <template #default="{ row }">
            <span class="company-link" @click="openDetail(row)">{{ row.companyName || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="industry" label="行业" width="120" />
        <el-table-column prop="scale" label="规模" width="120" />
        <el-table-column prop="contactEmail" label="联系邮箱" width="180" />
        <el-table-column label="审核状态" width="120">
          <template #default="{ row }">
            <el-tag :type="{ PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger' }[row.auditStatus]">
              {{ { PENDING: '待审核', APPROVED: '已通过', REJECTED: '已拒绝' }[row.auditStatus] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="公开状态" width="120">
          <template #default="{ row }">
            <el-switch v-model="row.visible" @change="toggleVisibility(row)" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button v-if="row.auditStatus !== 'APPROVED'" type="success" size="small" @click="audit(row.id, 'APPROVED')">通过</el-button>
            <el-button v-if="row.auditStatus !== 'REJECTED'" type="danger" size="small" @click="audit(row.id, 'REJECTED')">拒绝</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-drawer v-model="drawerVisible" :title="drawerTitle" size="520px" destroy-on-close>
        <template v-if="currentCompany">
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="企业名称">{{ currentCompany.companyName || '暂未填写' }}</el-descriptions-item>
            <el-descriptions-item label="行业">{{ currentCompany.industry || '暂未填写' }}</el-descriptions-item>
            <el-descriptions-item label="规模">{{ currentCompany.scale || '暂未填写' }}</el-descriptions-item>
            <el-descriptions-item label="联系邮箱">{{ currentCompany.contactEmail || '暂未填写' }}</el-descriptions-item>
            <el-descriptions-item label="联系电话">{{ currentCompany.contactPhone || '暂未填写' }}</el-descriptions-item>
            <el-descriptions-item label="地址">{{ currentCompany.address || '暂未填写' }}</el-descriptions-item>
            <el-descriptions-item label="官网">
              <a v-if="currentCompany.website" :href="normalizeUrl(currentCompany.website)" target="_blank" class="link">{{ currentCompany.website }}</a>
              <span v-else>暂未填写</span>
            </el-descriptions-item>
            <el-descriptions-item label="审核状态">
              <el-tag :type="{ PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger' }[currentCompany.auditStatus]" size="small">
                {{ { PENDING: '待审核', APPROVED: '已通过', REJECTED: '已拒绝' }[currentCompany.auditStatus] }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="公开状态">
              <el-tag :type="currentCompany.visible ? 'success' : 'info'" size="small">
                {{ currentCompany.visible ? '公开' : '隐藏' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="注册账号">{{ currentCompany.user?.username || '-' }}</el-descriptions-item>
          </el-descriptions>
          <div class="drawer-section" v-if="currentCompany.description">
            <h4>企业简介</h4>
            <p>{{ currentCompany.description }}</p>
          </div>
          <div class="drawer-section" v-if="currentCompany.logoUrl">
            <h4>企业 Logo</h4>
            <el-image :src="currentCompany.logoUrl" style="max-width:200px;border-radius:8px" fit="contain" />
          </div>
        </template>
      </el-drawer>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { adminApi } from '@/api'
import { ElMessage } from 'element-plus'

const companies = ref([])
const drawerVisible = ref(false)
const drawerTitle = ref('')
const currentCompany = ref(null)

onMounted(() => loadCompanies())

async function loadCompanies() {
  try {
    const res = await adminApi.companies()
    if (res?.code === 200) {
      companies.value = (Array.isArray(res.data) ? res.data : []).map(item => ({
        ...item,
        visible: item.visible !== false,
      }))
    }
  } catch (e) {
    const status = e?.response?.status
    if (status === 401) {
      ElMessage.error('当前登录状态已失效，请重新登录后再试')
    } else if (status === 403) {
      ElMessage.error('仅管理员可访问该页面')
    }
  }
}

function normalizeUrl(url) {
  if (!url) return ''
  return /^https?:\/\//i.test(url) ? url : 'https://' + url
}

function openDetail(row) {
  currentCompany.value = row
  drawerTitle.value = (row.companyName || '企业') + ' - 详细信息'
  drawerVisible.value = true
}

async function audit(id, status) {
  try {
    const res = await adminApi.auditCompany(id, status)
    if (res.code === 200) { ElMessage.success('操作成功'); loadCompanies() }
    else ElMessage.error(res.message)
  } catch (e) {
    ElMessage.error(e?.response?.status === 401 ? '当前登录状态已失效，请重新登录后再试' : '操作失败')
  }
}

async function toggleVisibility(row) {
  try {
    const res = await adminApi.toggleCompanyVisibility(row.id, row.visible)
    if (res.code === 200) ElMessage.success(row.visible ? '已公开该企业' : '已隐藏该企业')
    else {
      ElMessage.error(res.message)
      row.visible = !row.visible
    }
  } catch (e) {
    row.visible = !row.visible
    ElMessage.error(e?.response?.status === 401 ? '当前登录状态已失效，请重新登录后再试' : '操作失败')
  }
}
</script>

<style scoped>
.admin-wrap { display: flex; min-height: calc(100vh - 60px); }
.admin-sidebar { width: 200px; background: var(--tp-card); border-right: 1px solid var(--tp-border); flex-shrink: 0; }
.sidebar-title { padding: 20px 16px; font-weight: bold; font-size: 15px; color: var(--tp-primary); border-bottom: 1px solid var(--tp-border); }
.admin-content { flex: 1; padding: 32px; background: var(--tp-bg); }
.admin-content h2 { color: var(--tp-text); font-weight: 600; }
.admin-content h3 { color: var(--tp-primary); }
.admin-content :deep(.el-table) { border-radius: var(--tp-radius-sm); }
.admin-content :deep(.el-card) { border-radius: var(--tp-radius); }
.company-link { color: var(--tp-primary); cursor: pointer; font-weight: 500; }
.company-link:hover { text-decoration: underline; }
.link { color: var(--tp-primary); text-decoration: none; }
.link:hover { text-decoration: underline; }
.drawer-section { margin-top: 16px; }
.drawer-section h4 { color: var(--tp-primary); margin: 0 0 6px; font-size: 14px; }
.drawer-section p { margin: 0; white-space: pre-line; color: var(--tp-text-secondary); line-height: 1.7; font-size: 14px; }
</style>
