<template>
  <div class="page-wrap" v-loading="loading">
    <div class="page-header">
      <div class="page-title-block">
        <h2>📋 资讯政策公告</h2>
        <p class="page-intro">汇聚京州市软件产业相关资讯、政策与公告通知，方便企业和人才统一查看平台内容</p>
        <div class="page-stats">
          <el-tag type="info" effect="plain" size="large">
            当前分类共 {{ tabList.length }} 条内容
            <span v-if="hasActiveFilters" class="stats-filtered"> · 当前展示 {{ filteredList.length }} 条</span>
          </el-tag>
        </div>
      </div>
    </div>

    <PolicySearchPanel
      :filters="filters"
      :options="filterOptions"
      @update:filters="updateFilters"
      @reset="resetFilters"
    />

    <el-tabs v-model="tab">
      <el-tab-pane label="全部" name="" />
      <el-tab-pane label="政策法规" name="POLICY" />
      <el-tab-pane label="最新资讯" name="NEWS" />
      <el-tab-pane label="公告通知" name="ANNOUNCE" />
    </el-tabs>

    <div class="news-content-area">
      <el-card v-if="filteredList.length" shadow="hover" class="news-list-card">
        <div
          v-for="item in filteredList"
          :key="item.id"
          class="news-row"
          @click="$router.push(`/policies/${item.id}`)"
        >
          <el-tag :type="tagType(item.category)" size="small" style="margin-right:12px;flex-shrink:0">
            {{ catLabel(item.category) }}
          </el-tag>
          <span class="news-title">{{ item.title }}</span>
          <span class="news-time">{{ item.publishTime?.substring(0, 10) }}</span>
        </div>
      </el-card>
      <el-empty v-else :description="hasActiveFilters ? '未找到符合条件的内容' : '暂无内容'" class="empty-with-actions">
        <p class="empty-tip">{{ hasActiveFilters ? '可以尝试更换关键词或来源站点' : '该分类下暂无内容' }}</p>
        <el-button v-if="hasActiveFilters" @click="resetFilters">清空筛选</el-button>
        <el-button type="primary" @click="load">刷新</el-button>
      </el-empty>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import PolicySearchPanel from '@/components/PolicySearchPanel.vue'
import { newsApi } from '@/api'
import { buildPolicyFilterOptions, filterPolicies } from '@/utils/policySearch'

const list = ref([])
const tab = ref('')
const loading = ref(false)
const filters = ref({
  keyword: '',
  sourceSite: '',
})
const tabList = computed(() => {
  if (!tab.value) return list.value
  return list.value.filter(item => item.category === tab.value)
})
const filteredList = computed(() => filterPolicies(tabList.value, filters.value))
const filterOptions = computed(() => buildPolicyFilterOptions(tabList.value))
const hasActiveFilters = computed(() => Object.values(filters.value).some(Boolean))

onMounted(load)

async function load() {
  loading.value = true
  try {
    const res = await newsApi.list()
    if (res.code === 200) list.value = res.data
  } catch (error) {
    list.value = []
    ElMessage.error('加载资讯政策公告失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

function updateFilters(nextFilters) {
  filters.value = {
    ...filters.value,
    ...nextFilters,
  }
}

function resetFilters() {
  filters.value = {
    keyword: '',
    sourceSite: '',
  }
}

const tagType = (cat) => ({ NEWS: '', POLICY: 'success', ANNOUNCE: 'warning' }[cat] || '')
const catLabel = (cat) => ({ NEWS: '最新资讯', POLICY: '政策法规', ANNOUNCE: '公告通知' }[cat] || cat)
</script>

<style scoped>
.page-wrap { padding: 40px; max-width: 960px; margin: 0 auto; }
.page-header { margin-bottom: 24px; }
.page-title-block h2 { font-size: 24px; color: var(--tp-text); font-weight: 600; margin: 0 0 8px 0; }
.page-intro { color: var(--tp-text-secondary); font-size: 14px; margin: 0 0 12px 0; line-height: 1.5; }
.page-stats { margin-top: 4px; }
.page-stats .el-tag { font-size: 13px; }
.stats-filtered { font-weight: normal; opacity: 0.9; }
.news-content-area {
  margin-top: 20px;
  padding: 24px;
  background: linear-gradient(180deg, #f8fafc 0%, #f0f4f8 100%);
  border-radius: var(--tp-radius);
  min-height: 200px;
}
.news-list-card { margin: 0; }
.news-list-card :deep(.el-card__body) { padding: 0; }
.news-row {
  display: flex;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid var(--tp-border);
  cursor: pointer;
  transition: background 0.2s, color 0.2s;
}
.news-row:last-child { border-bottom: none; }
.news-row:hover { background: rgba(255, 255, 255, 0.8); }
.news-title { flex: 1; font-size: 15px; color: var(--tp-text); }
.news-time { color: var(--tp-text-secondary); font-size: 13px; white-space: nowrap; margin-left: 16px; opacity: 0.8; }
.empty-with-actions { padding: 40px 20px; }
.empty-with-actions .empty-tip { margin-bottom: 16px; color: var(--tp-text-secondary); font-size: 14px; }
</style>
