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
      <div class="page-head">
        <div>
          <h2>人才管理</h2>
          <p class="page-intro">
        管理平台人才档案的公开状态与精选展示。点击人才姓名可查看详细信息。
          </p>
        </div>
      </div>
      <div class="toolbar">
        <el-button type="primary" :loading="aiLoading" @click="aiRecommend" :disabled="!talents.length">
          ✨ AI 智能推荐
        </el-button>
      </div>
      <el-table :data="talents" stripe v-loading="loading" class="talent-table">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column label="人才画像" min-width="220">
          <template #default="{ row }">
            <div class="talent-cell">
              <div class="talent-name clickable" @click="openDetail(row)">
                {{ row.realName || row.user?.username || '-' }}
              </div>
              <div class="talent-role">{{ row.expectedPosition || '未填写求职方向' }}</div>
              <div class="talent-meta">
                {{ row.education || '学历未填' }}
                <span v-if="row.major"> · {{ row.major }}</span>
                <span v-if="row.workYears"> · {{ row.workYears }}</span>
              </div>
              <div class="talent-meta">
                {{ row.city || '城市未填' }}
                <span v-if="row.expectedSalary"> · {{ row.expectedSalary }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="档案亮点" min-width="240">
          <template #default="{ row }">
            <div class="summary-cell">
              {{ row.selfIntroduction || row.projectExperience || row.experience || '暂无简介信息' }}
            </div>
          </template>
        </el-table-column>
        <el-table-column label="完整度" width="100">
          <template #default="{ row }">
            <el-tag :type="completeness(row) >= 6 ? 'success' : completeness(row) >= 3 ? 'warning' : 'info'" size="small">
              {{ completeness(row) }}/8 项
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="city" label="城市" width="100" />
        <el-table-column label="公开状态" width="120">
          <template #default="{ row }">
            <el-switch v-model="row.visible" @change="toggleVisibility(row)" />
          </template>
        </el-table-column>
        <el-table-column label="精选" width="100">
          <template #default="{ row }">
            <el-switch v-model="row.featured" :disabled="row.visible === false" @change="toggleFeatured(row)" />
          </template>
        </el-table-column>
        <el-table-column label="展示顺序" width="120">
          <template #default="{ row }">
            <el-input-number v-model="row.featuredOrder" :min="0" :max="999" size="small" style="width:90px"
              :disabled="row.visible === false"
              @change="updateOrder(row)" />
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!talents.length && !loading" description="暂无人才档案" />

      <el-dialog v-model="aiDialogVisible" title="AI 智能推荐" width="600px" destroy-on-close>
        <div v-if="aiError" class="ai-error">{{ aiError }}</div>
        <el-table v-else :data="aiRecommendations" stripe max-height="360">
          <el-table-column prop="talentId" label="人才ID" width="80" />
          <el-table-column label="姓名" width="100">
            <template #default="{ row }">{{ talentName(row.talentId) }}</template>
          </el-table-column>
          <el-table-column prop="score" label="评分" width="70" />
          <el-table-column prop="reason" label="推荐理由" />
        </el-table>
        <template #footer>
          <el-button @click="aiDialogVisible = false">关闭</el-button>
          <el-button type="primary" :loading="adoptLoading" @click="adoptRecommendations" :disabled="!aiRecommendations.length">
            一键采纳
          </el-button>
        </template>
      </el-dialog>

      <el-drawer v-model="drawerVisible" :title="drawerTitle" size="520px" destroy-on-close>
        <template v-if="currentTalent">
          <div class="drawer-header">
            <el-avatar :size="72" :src="currentTalent.avatarUrl" icon="UserFilled" class="drawer-avatar" />
            <div class="drawer-basic">
              <h3>{{ currentTalent.realName || currentTalent.user?.username || '-' }}</h3>
              <p>{{ currentTalent.expectedPosition || '未填写求职方向' }}</p>
              <div class="drawer-tags">
                <el-tag v-for="sk in splitSkills(currentTalent.skills)" :key="sk" size="small" style="margin:2px">{{ sk }}</el-tag>
                <span v-if="!splitSkills(currentTalent.skills).length" class="empty-tip">暂未填写技能</span>
              </div>
            </div>
          </div>
          <el-divider />
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="性别">{{ currentTalent.gender || '暂未填写' }}</el-descriptions-item>
            <el-descriptions-item label="工作年限">{{ currentTalent.workYears || '暂未填写' }}</el-descriptions-item>
            <el-descriptions-item label="学历">{{ currentTalent.education || '暂未填写' }}</el-descriptions-item>
            <el-descriptions-item label="专业">{{ currentTalent.major || '暂未填写' }}</el-descriptions-item>
            <el-descriptions-item label="毕业院校">{{ currentTalent.graduationSchool || '暂未填写' }}</el-descriptions-item>
            <el-descriptions-item label="所在城市">{{ currentTalent.city || '暂未填写' }}</el-descriptions-item>
            <el-descriptions-item label="期望岗位">{{ currentTalent.expectedPosition || '暂未填写' }}</el-descriptions-item>
            <el-descriptions-item label="期望薪资">{{ currentTalent.expectedSalary || '暂未填写' }}</el-descriptions-item>
          </el-descriptions>
          <div class="drawer-section">
            <h4>个人简介</h4>
            <p>{{ currentTalent.selfIntroduction || '暂未填写' }}</p>
          </div>
          <div class="drawer-section">
            <h4>工作经历</h4>
            <p>{{ currentTalent.experience || '暂未填写' }}</p>
          </div>
          <div class="drawer-section">
            <h4>项目经历</h4>
            <p>{{ currentTalent.projectExperience || '暂未填写' }}</p>
          </div>
          <div class="drawer-section">
            <h4>证书与成果</h4>
            <p>{{ currentTalent.certificates || '暂未填写' }}</p>
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

const talents = ref([])
const loading = ref(false)
const aiLoading = ref(false)
const aiDialogVisible = ref(false)
const aiRecommendations = ref([])
const aiError = ref('')
const adoptLoading = ref(false)
const drawerVisible = ref(false)
const drawerTitle = ref('')
const currentTalent = ref(null)

onMounted(load)

async function load() {
  loading.value = true
  try {
    const res = await adminApi.talents()
    if (res.code === 200) {
      talents.value = (Array.isArray(res.data) ? res.data : []).map(item => ({
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
  } finally {
    loading.value = false
  }
}

function openDetail(row) {
  currentTalent.value = row
  drawerTitle.value = (row.realName || row.user?.username || '人才') + ' - 详细信息'
  drawerVisible.value = true
}

const splitSkills = (s) => s ? s.split(/[,，]/).map(t => t.trim()).filter(Boolean) : []

function skillCount(skills) {
  if (!skills) return 0
  return skills.split(/[,，\s]+/).filter(Boolean).length
}

function completeness(row) {
  let n = 0
  if (row.realName?.trim()) n++
  if (row.skills?.trim()) n++
  if (row.education?.trim()) n++
  if (row.expectedPosition?.trim()) n++
  if (row.major?.trim()) n++
  if (row.experience?.trim()) n++
  if (row.projectExperience?.trim()) n++
  if (row.selfIntroduction?.trim()) n++
  return n
}

async function toggleVisibility(row) {
  try {
    const res = await adminApi.toggleTalentVisibility(row.id, row.visible)
    if (res.code === 200) {
      if (row.visible === false) row.featured = false
      ElMessage.success(row.visible ? '已公开该人才' : '已隐藏该人才')
    } else {
      ElMessage.error(res.message)
      row.visible = !row.visible
    }
  } catch (e) {
    row.visible = !row.visible
    ElMessage.error(e?.response?.status === 401 ? '当前登录状态已失效，请重新登录后再试' : '操作失败')
  }
}

async function toggleFeatured(row) {
  try {
    const res = await adminApi.toggleFeatured(row.id, { featured: row.featured })
    if (res.code === 200) ElMessage.success(row.featured ? '已设为精选' : '已取消精选')
    else { ElMessage.error(res.message); row.featured = !row.featured }
  } catch (e) {
    row.featured = !row.featured
    ElMessage.error(e?.response?.status === 401 ? '当前登录状态已失效，请重新登录后再试' : '操作失败')
  }
}

async function updateOrder(row) {
  try {
    const res = await adminApi.toggleFeatured(row.id, { featuredOrder: row.featuredOrder })
    if (res.code === 200) ElMessage.success('顺序已更新')
    else ElMessage.error(res.message)
  } catch (e) {
    ElMessage.error(e?.response?.status === 401 ? '当前登录状态已失效，请重新登录后再试' : '操作失败')
  }
}

function talentName(talentId) {
  if (!Array.isArray(talents.value)) return '-'
  const t = talents.value.find(x => String(x.id) === String(talentId))
  return t ? (t.realName || t.user?.username || '-') : '-'
}

async function aiRecommend() {
  aiLoading.value = true
  aiError.value = ''
  aiRecommendations.value = []
  try {
    const res = await adminApi.recommendShowcase(10)
    aiDialogVisible.value = true
    if (res?.code === 200 && res.data) {
      const recs = res.data.recommendations
      aiRecommendations.value = Array.isArray(recs) ? recs : []
      aiError.value = res.data.error || ''
    } else {
      aiError.value = res?.data?.error || res?.message || 'AI 推荐失败'
    }
  } catch (e) {
    aiDialogVisible.value = true
    const status = e?.response?.status
    if (status === 401) {
      aiError.value = '当前登录状态已失效，请重新登录后再试'
    } else if (status === 403) {
      aiError.value = '仅管理员可使用 AI 智能推荐'
    } else {
      aiError.value = e?.response?.data?.message || e?.message || '请求失败'
    }
  } finally {
    aiLoading.value = false
  }
}

async function adoptRecommendations() {
  if (!aiRecommendations.value.length) return
  adoptLoading.value = true
  try {
    for (let i = 0; i < aiRecommendations.value.length; i++) {
      const r = aiRecommendations.value[i]
      await adminApi.toggleFeatured(r.talentId, { featured: true, featuredOrder: i })
    }
    for (let i = 0; i < aiRecommendations.value.length; i++) {
      const r = aiRecommendations.value[i]
      const row = talents.value.find(t => String(t.id) === String(r.talentId))
      if (row) {
        row.featured = true
        row.featuredOrder = i
      }
    }
    ElMessage.success('已采纳 AI 推荐，人才已设为精选并排序')
    aiDialogVisible.value = false
  } catch (e) {
    ElMessage.error(e.message || '采纳失败')
  } finally {
    adoptLoading.value = false
  }
}
</script>

<style scoped>
.admin-wrap { display: flex; min-height: calc(100vh - 60px); }
.admin-sidebar { width: 200px; background: var(--tp-card); border-right: 1px solid var(--tp-border); flex-shrink: 0; }
.sidebar-title { padding: 20px 16px; font-weight: bold; font-size: 15px; color: var(--tp-primary); border-bottom: 1px solid var(--tp-border); }
.admin-content { flex: 1; padding: 32px; background: var(--tp-bg); }
.admin-content h2 { color: var(--tp-text); font-weight: 600; }
.page-head { margin-bottom: 16px; }
.page-intro { margin: 8px 0 0; color: var(--tp-text-secondary); font-size: 14px; line-height: 1.6; }
.toolbar { margin-bottom: 16px; display: flex; justify-content: flex-start; }
.admin-content :deep(.el-table) { border-radius: var(--tp-radius-sm); }
.talent-table :deep(.el-table__cell) { vertical-align: top; }
.talent-cell { line-height: 1.6; }
.talent-name { font-size: 15px; font-weight: 600; color: var(--tp-text); }
.talent-name.clickable { color: var(--tp-primary); cursor: pointer; }
.talent-name.clickable:hover { text-decoration: underline; }
.talent-role { font-size: 13px; color: var(--tp-primary); font-weight: 600; margin-top: 2px; }
.talent-meta { font-size: 12px; color: var(--tp-text-secondary); }
.summary-cell {
  font-size: 13px;
  color: var(--tp-text-secondary);
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.ai-error { color: var(--el-color-danger); padding: 8px 0; }
.drawer-header { display: flex; gap: 20px; align-items: flex-start; }
.drawer-avatar { background: var(--tp-primary) !important; flex-shrink: 0; }
.drawer-basic { flex: 1; min-width: 0; }
.drawer-basic h3 { margin: 0 0 4px; color: var(--tp-text); }
.drawer-basic p { margin: 0 0 8px; color: var(--tp-text-secondary); font-size: 14px; }
.drawer-tags { min-height: 24px; }
.empty-tip { color: var(--tp-text-secondary); font-size: 12px; }
.drawer-section { margin-top: 16px; }
.drawer-section h4 { color: var(--tp-primary); margin: 0 0 6px; font-size: 14px; }
.drawer-section p { margin: 0; white-space: pre-line; color: var(--tp-text-secondary); line-height: 1.7; font-size: 14px; }
</style>
