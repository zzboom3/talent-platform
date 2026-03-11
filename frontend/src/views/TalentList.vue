<template>
  <div class="page-wrap" v-loading="loading">
    <div class="page-header">
      <div class="page-title-block">
        <h2>人才库</h2>
        <p class="page-intro">汇聚京州市软件人才，支持按关键词、城市、学历、技能、专业和求职方向组合筛选</p>
        <div class="page-stats">
          <el-tag type="info" effect="plain" size="large">
            共 {{ stats.talents ?? talents.length }} 条人才
            <span v-if="hasActiveFilters" class="stats-filtered"> · 当前展示 {{ filteredTalents.length }} 条</span>
          </el-tag>
        </div>
      </div>
    </div>

    <TalentSearchPanel
      :filters="filters"
      :options="filterOptions"
      @update:filters="updateFilters"
      @reset="resetFilters"
    />

    <el-row :gutter="20">
      <el-col v-for="t in filteredTalents" :key="t.id" :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" class="talent-card" @click="$router.push(`/talents/${t.id}`)">
          <div class="talent-card-main">
            <el-avatar :size="64" :src="t.avatarUrl" icon="UserFilled" class="talent-avatar" />
            <div class="talent-name-row">
              <div class="talent-name">{{ t.realName || t.user?.username || '未填写姓名' }}</div>
            </div>
            <div class="talent-role">{{ t.expectedPosition || '待补充求职方向' }}</div>
            <div class="talent-meta">{{ t.education || '未填写学历' }}<span v-if="t.major"> · {{ t.major }}</span></div>
            <div class="talent-meta">城市: {{ t.city || '未填写城市' }}<span v-if="t.workYears"> · {{ t.workYears }}</span></div>
          </div>
          <div class="talent-summary">
            {{ t.selfIntroduction || t.projectExperience || t.experience || '暂未填写个人简介' }}
          </div>
          <el-divider class="card-divider" />
          <div class="skill-tags">
            <el-tag v-for="sk in splitSkills(t.skills)" :key="sk" size="small" effect="plain" class="skill-tag">
              {{ sk }}
            </el-tag>
            <span v-if="!t.skills" class="empty-tip">暂未填写技能</span>
          </div>
        </el-card>
      </el-col>
    </el-row>
    <el-empty v-if="!filteredTalents.length" :description="hasActiveFilters ? '未找到符合条件的人才' : '暂无人才信息'">
      <el-button v-if="hasActiveFilters" @click="resetFilters">清空筛选</el-button>
      <el-button type="primary" @click="loadAll">刷新列表</el-button>
    </el-empty>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import TalentSearchPanel from '@/components/TalentSearchPanel.vue'
import { talentApi, statsApi } from '@/api'
import { buildTalentFilterOptions, filterTalents, splitTalentSkills } from '@/utils/talentSearch'

const talents = ref([])
const loading = ref(false)
const stats = ref({})
const filters = ref({
  keyword: '',
  city: '',
  education: '',
  skill: '',
})

const filteredTalents = computed(() => filterTalents(talents.value, filters.value))
const filterOptions = computed(() => buildTalentFilterOptions(talents.value))
const hasActiveFilters = computed(() => Object.values(filters.value).some(Boolean))

async function loadAll() {
  loading.value = true
  try {
    const res = await talentApi.list()
    if (res.code === 200) talents.value = res.data
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
    city: '',
    education: '',
    skill: '',
  }
}

onMounted(async () => {
  loadAll()
  const res = await statsApi.public().catch(() => null)
  if (res?.code === 200) stats.value = res.data
})

const splitSkills = splitTalentSkills
</script>

<style scoped>
.page-wrap { padding: 40px; max-width: 1280px; margin: 0 auto; }
.page-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 28px; gap: 24px; flex-wrap: wrap; }
.page-title-block { flex: 1; min-width: 200px; }
.page-header h2 { font-size: 24px; color: var(--tp-text); font-weight: 600; margin: 0 0 8px 0; }
.page-intro { color: var(--tp-text-secondary); font-size: 14px; margin: 0 0 12px 0; line-height: 1.5; }
.page-stats { margin-top: 4px; }
.page-stats .el-tag { font-size: 13px; }
.stats-filtered { font-weight: normal; opacity: 0.9; }
.talent-card {
  cursor: pointer;
  margin-bottom: 20px;
  min-height: 328px;
  border-radius: var(--tp-radius);
  transition: transform 0.2s, box-shadow 0.2s;
}
.talent-card:hover { transform: translateY(-4px); box-shadow: var(--tp-shadow-hover); }
.talent-card-main {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  min-height: 120px;
}
.talent-avatar { margin-bottom: 12px; background: var(--tp-primary) !important; }
.talent-name-row { width: 100%; }
.talent-name {
  margin-top: 12px;
  font-size: 17px;
  font-weight: 600;
  color: var(--tp-text);
  line-height: 1.4;
}
.talent-role {
  max-width: 100%;
  color: var(--tp-primary);
  font-size: 13px;
  margin-top: 8px;
  font-weight: 600;
  line-height: 1.5;
}
.talent-meta {
  color: var(--tp-text-secondary);
  font-size: 13px;
  margin-top: 4px;
  line-height: 1.5;
}
.talent-summary {
  margin-top: 14px;
  min-height: 58px;
  font-size: 13px;
  line-height: 1.6;
  color: var(--tp-text-secondary);
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.card-divider { margin: 16px 0 14px; }
.skill-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: flex-start;
  min-height: 32px;
}
.skill-tag { margin: 0; }
.empty-tip { color: var(--tp-text-secondary); font-size: 12px; opacity: 0.7; }
</style>
