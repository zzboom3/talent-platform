<template>
  <div class="page-wrap">
    <div class="page-header">
      <h2>终生学习 · 课程中心</h2>
    </div>
    <el-tabs v-model="tab">
      <el-tab-pane label="全部课程" name="all">
        <el-row :gutter="20">
          <el-col :span="6" v-for="c in courses" :key="c.id">
            <el-card shadow="hover" class="course-card">
              <el-image :src="c.coverUrl || defaultCover" fit="cover" class="course-img" />
              <div class="course-info">
                <div class="course-title">{{ c.title }}</div>
                <div class="course-meta">👤 {{ c.teacher }} · {{ c.category }}</div>
                <div class="course-desc">{{ c.description }}</div>
                <el-button type="primary" size="small" style="margin-top:8px;width:100%"
                  @click="enroll(c.id)" :disabled="enrolledIds.has(c.id)">
                  {{ enrolledIds.has(c.id) ? '已报名' : '立即报名' }}
                </el-button>
              </div>
            </el-card>
          </el-col>
        </el-row>
        <el-empty v-if="!courses.length" description="暂无课程" />
      </el-tab-pane>
      <el-tab-pane label="我的课程" name="mine" v-if="isLoggedIn">
        <el-table :data="myEnrollments" stripe>
          <el-table-column prop="course.title" label="课程名称" />
          <el-table-column prop="course.teacher" label="讲师" width="120" />
          <el-table-column prop="enrollTime" label="报名时间" width="180">
            <template #default="{ row }">{{ row.enrollTime?.substring(0, 10) }}</template>
          </el-table-column>
          <el-table-column prop="progress" label="进度" width="100">
            <template #default="{ row }">
              <el-progress :percentage="row.progress || 0" />
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!myEnrollments.length" description="尚未报名任何课程" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { courseApi } from '@/api'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const store = useUserStore()
const isLoggedIn = computed(() => store.isLoggedIn)
const courses = ref([])
const myEnrollments = ref([])
const enrolledIds = ref(new Set())
const tab = ref('all')
const defaultCover = 'https://via.placeholder.com/300x160?text=Course'

onMounted(async () => {
  const res = await courseApi.list()
  if (res.code === 200) courses.value = res.data
  if (store.isLoggedIn) {
    const r = await courseApi.myEnrollments().catch(() => null)
    if (r?.code === 200) {
      myEnrollments.value = r.data
      enrolledIds.value = new Set(r.data.map(e => e.course.id))
    }
  }
})

async function enroll(courseId) {
  if (!store.isLoggedIn) { ElMessage.warning('请先登录'); return }
  const res = await courseApi.enroll(courseId)
  if (res.code === 200) {
    ElMessage.success('报名成功')
    enrolledIds.value.add(courseId)
    const r = await courseApi.myEnrollments()
    if (r.code === 200) myEnrollments.value = r.data
  } else ElMessage.error(res.message)
}
</script>

<style scoped>
.page-wrap { padding: 24px 40px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.page-header h2 { font-size: 22px; }
.course-card { margin-bottom: 20px; }
.course-img { width: 100%; height: 140px; border-radius: 6px 6px 0 0; }
.course-info { padding: 8px 0; }
.course-title { font-weight: bold; font-size: 15px; }
.course-meta { color: #888; font-size: 12px; margin: 4px 0; }
.course-desc { font-size: 13px; color: #555; overflow: hidden; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; }
</style>
