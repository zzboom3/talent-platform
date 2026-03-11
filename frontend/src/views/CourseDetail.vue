<template>
  <div class="page-wrap" v-loading="loading">
    <template v-if="course">
    <el-breadcrumb separator="/" class="breadcrumb">
      <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
      <el-breadcrumb-item :to="{ path: '/courses' }">课程</el-breadcrumb-item>
      <el-breadcrumb-item>{{ course.title }}</el-breadcrumb-item>
    </el-breadcrumb>
    <!-- 视频播放器（全宽） -->
    <el-card shadow="hover" class="video-card">
      <div v-if="course.videoUrl" class="video-wrap">
        <video
          ref="videoRef"
          controls
          :src="course.videoUrl"
          class="video-player"
          @timeupdate="handleVideoTimeUpdate"
          @seeked="handleVideoSeeked"
          @pause="() => flushProgress()"
          @ended="handleVideoEnded"
        >
          您的浏览器不支持视频播放
        </video>
      </div>
      <el-image v-else :src="course.coverUrl || 'https://via.placeholder.com/800x400?text=Course'"
        fit="cover" class="cover-fallback" />
    </el-card>

    <!-- 课程信息 + 学习进度（并排） -->
    <el-row :gutter="24" class="detail-row">
      <el-col :span="14">
        <el-card shadow="hover" class="info-card">
          <h2 class="course-title">{{ course.title }}</h2>
          <div class="course-meta">
            <span>👤 {{ course.teacher }}</span>
            <span> · {{ course.category }}</span>
            <span v-if="course.duration"> · ⏱ {{ formatDuration(course.duration) }}</span>
            <span v-if="course.chapterCount"> · 📖 {{ course.chapterCount }}章节</span>
          </div>
          <el-divider />
          <div class="course-desc">{{ course.description }}</div>
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card shadow="hover" class="progress-card">
          <template #header><strong>学习进度</strong></template>
          <div v-if="enrollment">
            <el-progress :percentage="enrollment.progress || 0" :stroke-width="20" :text-inside="true" />
            <div class="progress-detail">
              <div class="progress-item">
                <span class="progress-label">学习时长</span>
                <span class="progress-value">{{ (enrollment.studyHours || 0).toFixed(1) }} 小时</span>
              </div>
              <div class="progress-item">
                <span class="progress-label">状态</span>
                <el-tag :type="enrollment.status === 'COMPLETED' ? 'success' : 'primary'" size="small">
                  {{ enrollment.status === 'COMPLETED' ? '已完成' : '学习中' }}
                </el-tag>
              </div>
              <div v-if="course.videoUrl" class="progress-tip">系统将根据视频实际播放自动记录学习进度</div>
            </div>
            <el-button v-if="enrollment.status === 'COMPLETED'" type="success" class="cert-btn"
              @click="certify">领取结业证书</el-button>
          </div>
          <div v-else class="not-enrolled">
            <p>尚未报名此课程</p>
            <el-button type="primary" class="enroll-btn" @click="enroll" :disabled="!isLoggedIn">
              {{ isLoggedIn ? '立即报名' : '请先登录' }}
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
    </template>
    <div v-else-if="!loading" class="course-not-found">
      <p>课程不存在或已被移除</p>
      <el-button type="primary" @click="$router.push('/courses')">返回课程列表</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { courseApi, blockchainApi } from '@/api'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const route = useRoute()
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
const course = ref(null)
const enrollment = ref(null)
const loading = ref(true)
const videoRef = ref(null)
const syncing = ref(false)
let lastVideoTime = 0
let pendingStudySeconds = 0
let pendingProgress = 0
let syncTimer = null

onMounted(async () => {
  loading.value = true
  try {
    const res = await courseApi.getById(route.params.id)
    if (res.code === 200) course.value = res.data
    if (store.isLoggedIn) await loadEnrollment()
  } finally {
    loading.value = false
  }
})

onUnmounted(() => {
  flushProgress(true)
  if (syncTimer) clearTimeout(syncTimer)
})

async function loadEnrollment() {
  const res = await courseApi.myEnrollments().catch(() => null)
  if (res?.code === 200) {
    enrollment.value = res.data.find(e => e.course.id == route.params.id) || null
    pendingProgress = enrollment.value?.progress || 0
    lastVideoTime = 0
    pendingStudySeconds = 0
  }
}

async function enroll() {
  const res = await courseApi.enroll(Number(route.params.id))
  if (res.code === 200) {
    ElMessage.success('报名成功')
    await loadEnrollment()
  } else ElMessage.error(res.message)
}

function queueProgressSync(progress) {
  pendingProgress = Math.max(pendingProgress, progress)
  if (syncTimer) return
  syncTimer = setTimeout(() => {
    flushProgress()
  }, 5000)
}

function handleVideoTimeUpdate() {
  if (!enrollment.value || !course.value?.videoUrl) return
  const video = videoRef.value
  if (!video || !video.duration) return

  const currentTime = video.currentTime || 0
  if (lastVideoTime > 0) {
    const delta = currentTime - lastVideoTime
    if (delta > 0 && delta <= 2) {
      pendingStudySeconds += delta
    }
  }
  lastVideoTime = currentTime

  const progress = Math.max(enrollment.value.progress || 0, Math.floor(currentTime / video.duration * 100))
  if (progress > (enrollment.value.progress || 0) || pendingStudySeconds >= 5) {
    queueProgressSync(progress)
  }
}

function handleVideoSeeked() {
  lastVideoTime = videoRef.value?.currentTime || 0
}

async function flushProgress(force = false) {
  if (syncTimer) {
    clearTimeout(syncTimer)
    syncTimer = null
  }
  if (!enrollment.value || syncing.value) return

  const nextProgress = Math.max(enrollment.value.progress || 0, pendingProgress)
  const deltaHours = Number((pendingStudySeconds / 3600).toFixed(4))
  if (!force && nextProgress <= (enrollment.value.progress || 0) && deltaHours <= 0) return
  if (force && nextProgress <= (enrollment.value.progress || 0) && deltaHours <= 0) return

  syncing.value = true
  try {
    const res = await courseApi.updateProgress(enrollment.value.id, {
      progress: nextProgress,
      studyHours: deltaHours,
    })
    if (res.code === 200) {
      enrollment.value = res.data
      pendingProgress = res.data.progress || 0
      pendingStudySeconds = 0
    }
  } finally {
    syncing.value = false
  }
}

function handleVideoEnded() {
  pendingProgress = 100
  flushProgress(true)
}

async function certify() {
  if (!enrollment.value) return
  const res = await blockchainApi.certify(enrollment.value.id)
  if (res.code === 200) ElMessage.success('结业证书已生成')
  else ElMessage.error(res.message)
}
</script>

<style scoped>
.page-wrap { padding: 40px; max-width: 1200px; margin: 0 auto; }
.breadcrumb { margin-bottom: 20px; font-size: 14px; }
.page-wrap .el-card { border-radius: var(--tp-radius); }

.video-card { margin-bottom: 24px; }
.video-card :deep(.el-card__body) { padding: 0; }
.video-wrap { background: #000; border-radius: var(--tp-radius); overflow: hidden; }
.video-player { width: 100%; display: block; max-height: 540px; background: #000; }
.cover-fallback { width: 100%; height: 400px; border-radius: var(--tp-radius); }

.detail-row { align-items: flex-start; }
.info-card { height: 100%; }
.course-title { color: var(--tp-text); font-size: 22px; font-weight: 600; margin: 0 0 12px 0; }
.course-meta { color: var(--tp-text-secondary); font-size: 14px; line-height: 1.8; }
.course-desc { line-height: 1.8; color: var(--tp-text); font-size: 14px; }

.progress-card { height: 100%; }
.progress-detail { margin-top: 20px; }
.progress-item { display: flex; justify-content: space-between; align-items: center; padding: 10px 0; border-bottom: 1px solid var(--tp-border); }
.progress-label { color: var(--tp-text-secondary); font-size: 14px; }
.progress-value { color: var(--tp-text); font-weight: 500; font-size: 14px; }
.progress-tip { margin-top: 14px; font-size: 12px; color: var(--tp-text-secondary); line-height: 1.6; }
.cert-btn { width: 100%; margin-top: 20px; }
.not-enrolled p { color: var(--tp-text-secondary); margin-bottom: 16px; text-align: center; }
.enroll-btn { width: 100%; }

.page-wrap :deep(.el-image img) { border-radius: var(--tp-radius-sm); }
.course-not-found { text-align: center; padding: 80px 24px; color: var(--tp-text-secondary); }
.course-not-found p { margin-bottom: 16px; font-size: 16px; }
</style>
