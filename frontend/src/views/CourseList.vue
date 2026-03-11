<template>
  <div class="page-wrap" v-loading="loading">
    <div class="page-header">
      <div class="page-title-block">
        <h2>终生学习 · 课程中心</h2>
        <p class="page-intro">精选软件技能课程，支持视频学习、进度跟踪，完成即可领取区块链存证证书</p>
        <div class="page-stats">
          <el-tag type="info" effect="plain" size="large">
            共 {{ stats.courses ?? courses.length }} 门课程
            <span v-if="hasActiveFilters" class="stats-filtered"> · 当前展示 {{ filteredCourses.length }} 门</span>
          </el-tag>
        </div>
      </div>
    </div>

    <CourseSearchPanel
      v-if="tab === 'all'"
      :filters="filters"
      :options="filterOptions"
      @update:filters="updateFilters"
      @reset="resetFilters"
    />

    <el-tabs v-model="tab">
      <el-tab-pane label="全部课程" name="all">
        <el-row :gutter="20">
          <el-col :span="6" v-for="c in filteredCourses" :key="c.id">
            <el-card shadow="hover" class="course-card" @click="$router.push(`/courses/${c.id}`)">
              <el-image :src="c.coverUrl || defaultCover" fit="cover" class="course-img" />
              <div class="course-info">
                <div class="course-title">{{ c.title }}</div>
                <div class="course-meta">👤 {{ c.teacher }} · {{ c.category }}</div>
                <div class="course-desc">{{ c.description }}</div>
                <el-button type="primary" size="small" style="margin-top:8px;width:100%"
                  @click.stop="enroll(c.id)" :disabled="enrolledIds.has(c.id)">
                  {{ enrolledIds.has(c.id) ? '已报名' : '立即报名' }}
                </el-button>
              </div>
            </el-card>
          </el-col>
        </el-row>
        <el-empty v-if="!filteredCourses.length" :description="hasActiveFilters ? '未找到符合条件的课程' : '暂无课程'" class="empty-with-actions">
          <p class="empty-tip">{{ hasActiveFilters ? '可以调整筛选条件，查看更多课程内容' : '课程正在筹备中，请稍后查看' }}</p>
          <el-button v-if="hasActiveFilters" @click="resetFilters">清空筛选</el-button>
          <el-button type="primary" @click="$router.push('/')">返回首页</el-button>
        </el-empty>
      </el-tab-pane>
      <el-tab-pane label="我的课程" name="mine" v-if="isLoggedIn">
        <el-card v-if="myEnrollments.length" shadow="hover" class="mine-courses-card">
          <template #header>
            <span class="mine-courses-header">我的学习记录 · 共 {{ myEnrollments.length }} 门课程</span>
          </template>
          <el-table :data="myEnrollments" stripe>
            <el-table-column prop="course.title" label="课程名称" />
            <el-table-column prop="course.teacher" label="讲师" width="120" />
            <el-table-column prop="enrollTime" label="报名时间" width="180">
              <template #default="{ row }">{{ row.enrollTime?.substring(0, 10) }}</template>
            </el-table-column>
            <el-table-column label="进度" width="140">
              <template #default="{ row }">
                <el-progress :percentage="row.progress || 0" />
              </template>
            </el-table-column>
            <el-table-column label="学习时长" width="100">
              <template #default="{ row }">{{ (row.studyHours || 0).toFixed(1) }}h</template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === 'COMPLETED' ? 'success' : 'primary'" size="small">
                  {{ row.status === 'COMPLETED' ? '已完成' : '学习中' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100">
              <template #default="{ row }">
                <el-button size="small" @click="$router.push(`/courses/${row.course.id}`)">进入学习</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
        <el-empty v-else description="尚未报名任何课程" class="empty-with-actions">
        <p class="empty-tip">快去浏览全部课程，选择心仪的课程开始学习吧</p>
        <el-button type="primary" @click="() => { tab = 'all' }">浏览课程</el-button>
      </el-empty>
      </el-tab-pane>
      <el-tab-pane label="AI 推荐" name="ai" v-if="isLoggedIn">
        <div v-loading="aiLoading" element-loading-text="AI 正在根据您的技能推荐课程，请稍候..." style="min-height:180px">
          <el-button type="success" @click="getRecommendations" :loading="aiLoading" :disabled="aiLoading" style="margin-bottom:16px">
            获取 AI 个性化推荐
          </el-button>
        <div v-if="recommendations.length">
          <el-card
            v-for="r in recommendations"
            :key="r.courseId"
            shadow="hover"
            class="recommend-card"
            @click="$router.push(`/courses/${r.courseId}`)"
          >
            <div style="display:flex;justify-content:space-between;align-items:flex-start;gap:16px">
              <div style="flex:1">
                <div class="course-title">{{ r.title || `课程#${r.courseId}` }}</div>
                <div class="course-meta">
                  <span v-if="r.teacher">👤 {{ r.teacher }}</span>
                  <span v-if="r.category"> · {{ r.category }}</span>
                  <span v-if="r.duration"> · {{ formatDuration(r.duration) }}</span>
                  <span v-if="r.chapterCount"> · {{ r.chapterCount }}章节</span>
                </div>
                <div v-if="r.description" class="course-desc" style="margin-top:8px">{{ r.description }}</div>
                <p class="recommend-reason">{{ r.reason }}</p>
              </div>
              <el-tag type="success" size="small" effect="dark">优先级 {{ r.priority }}</el-tag>
            </div>
          </el-card>
        </div>
        <el-empty v-if="aiSearched && !recommendations.length && !aiLoading" description="暂无推荐结果" />
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import CourseSearchPanel from '@/components/CourseSearchPanel.vue'
import { courseApi, aiApi, statsApi } from '@/api'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { buildCourseFilterOptions, filterCourses } from '@/utils/courseSearch'

const store = useUserStore()
const isLoggedIn = computed(() => store.isLoggedIn)

function formatDuration(seconds) {
  if (!seconds || seconds <= 0) return ''
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  const s = seconds % 60
  if (h > 0) return `${h}小时${m}分${s}秒`
  if (m > 0) return `${m}分${s}秒`
  return `${s}秒`
}
const courses = ref([])
const myEnrollments = ref([])
const enrolledIds = ref(new Set())
const tab = ref('all')
const defaultCover = 'https://via.placeholder.com/300x160?text=Course'
const recommendations = ref([])
const aiLoading = ref(false)
const aiSearched = ref(false)
const loading = ref(false)
const stats = ref({})
const filters = ref({
  keyword: '',
  category: '',
  teacher: '',
})
const filteredCourses = computed(() => filterCourses(courses.value, filters.value))
const filterOptions = computed(() => buildCourseFilterOptions(courses.value))
const hasActiveFilters = computed(() => Object.values(filters.value).some(Boolean))

onMounted(async () => {
  loading.value = true
  try {
    const statsRes = await statsApi.public().catch(() => null)
    if (statsRes?.code === 200) stats.value = statsRes.data
    const res = await courseApi.list()
    if (res.code === 200) courses.value = res.data
    if (store.isLoggedIn) {
      const r = await courseApi.myEnrollments().catch(() => null)
      if (r?.code === 200) {
        myEnrollments.value = r.data
        enrolledIds.value = new Set(r.data.map(e => e.course.id))
      }
    }
  } finally {
    loading.value = false
  }
})

async function enroll(courseId) {
  if (!store.isLoggedIn) { ElMessage.warning('请先登录'); return }
  const res = await courseApi.enroll(courseId)
  if (res.code === 200) {
    ElMessage.success('报名成功')
    enrolledIds.value = new Set([...enrolledIds.value, courseId])
    const r = await courseApi.myEnrollments()
    if (r.code === 200) {
      myEnrollments.value = r.data
      enrolledIds.value = new Set(r.data.map(e => e.course.id))
    }
  } else ElMessage.error(res.message)
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
    category: '',
    teacher: '',
  }
}

async function getRecommendations() {
  aiLoading.value = true
  aiSearched.value = true
  try {
    const res = await aiApi.recommendCourses()
    if (res.code === 200 && res.data.recommendations) {
      recommendations.value = res.data.recommendations
    } else {
      ElMessage.error(res.data?.error || '推荐失败')
    }
  } catch { ElMessage.error('AI 服务请求失败') }
  aiLoading.value = false
}
</script>

<style scoped>
.page-wrap { padding: 40px; max-width: 1280px; margin: 0 auto; }
.page-header { margin-bottom: 28px; }
.page-title-block { margin-bottom: 4px; }
.page-header h2 { font-size: 24px; color: var(--tp-text); font-weight: 600; margin: 0 0 8px 0; }
.page-intro { color: var(--tp-text-secondary); font-size: 14px; margin: 0 0 12px 0; line-height: 1.5; }
.page-stats { margin-top: 4px; }
.page-stats .el-tag { font-size: 13px; }
.stats-filtered { font-weight: normal; opacity: 0.9; }
.empty-with-actions { padding: 40px 20px; }
.empty-with-actions .empty-tip { margin-bottom: 16px; color: var(--tp-text-secondary); font-size: 14px; }
.course-card { margin-bottom: 20px; cursor: pointer; border-radius: var(--tp-radius); overflow: hidden; transition: transform 0.2s, box-shadow 0.2s; }
.course-card:hover { transform: translateY(-4px); box-shadow: var(--tp-shadow-hover); }
.recommend-card { margin-bottom: 12px; cursor: pointer; border-radius: var(--tp-radius); transition: transform 0.2s, box-shadow 0.2s; }
.recommend-card:hover { transform: translateY(-4px); box-shadow: var(--tp-shadow-hover); }
.course-img { width: 100%; height: 170px; border-radius: var(--tp-radius-sm) var(--tp-radius-sm) 0 0; }
.mine-courses-card { margin-bottom: 24px; }
.mine-courses-header { font-weight: 600; font-size: 15px; }
.course-info { padding: 12px 0; }
.course-title { font-weight: bold; font-size: 15px; color: var(--tp-text); }
.course-meta { color: var(--tp-text-secondary); font-size: 12px; margin: 4px 0; }
.course-desc { font-size: 13px; color: var(--tp-text-secondary); overflow: hidden; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; }
.recommend-reason { color: #666; margin-top: 10px; font-size: 13px; line-height: 1.6; }
.page-wrap :deep(.el-table) { border-radius: var(--tp-radius-sm); }
</style>
