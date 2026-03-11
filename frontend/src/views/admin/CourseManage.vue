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
        <h2>课程管理</h2>
        <el-button type="primary" @click="openCreate">新增课程</el-button>
      </div>
      <el-table :data="list" stripe border>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="title" label="课程名称" show-overflow-tooltip />
        <el-table-column prop="teacher" label="讲师" width="100" />
        <el-table-column prop="category" label="分类" width="100" />
        <el-table-column label="时长" width="120">
          <template #default="{ row }">{{ row.duration ? formatDuration(row.duration) : '-' }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160">
          <template #default="{ row }">{{ row.createTime?.substring(0,10) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <el-button size="small" @click="openEdit(row)">编辑</el-button>
            <el-popconfirm title="确认删除该课程?" @confirm="deleteCourse(row.id)">
              <template #reference>
                <el-button type="danger" size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <el-dialog v-model="dialogVisible" :title="form.id ? '编辑课程' : '新增课程'" width="560px">
        <el-form :model="form" label-width="80px">
          <el-form-item label="课程名称"><el-input v-model="form.title" /></el-form-item>
          <el-form-item label="讲师"><el-input v-model="form.teacher" /></el-form-item>
          <el-form-item label="分类">
            <el-select v-model="form.category" style="width:100%">
              <el-option label="前端开发" value="前端开发" />
              <el-option label="后端开发" value="后端开发" />
              <el-option label="人工智能" value="人工智能" />
              <el-option label="大数据" value="大数据" />
              <el-option label="区块链" value="区块链" />
              <el-option label="职业素养" value="职业素养" />
              <el-option label="其他" value="其他" />
            </el-select>
          </el-form-item>
          <el-form-item label="课程封面">
            <div class="cover-upload-area">
              <el-upload
                :show-file-list="false"
                accept="image/*"
                :http-request="uploadCover"
                :disabled="uploadingCover"
              >
                <div v-if="form.coverUrl" class="cover-preview">
                  <img :src="form.coverUrl" alt="封面预览" />
                  <div class="cover-mask">点击更换</div>
                </div>
                <div v-else class="cover-placeholder">
                  <el-icon :size="28"><Plus /></el-icon>
                  <span>上传封面图片</span>
                </div>
              </el-upload>
              <span class="upload-tip">支持 JPG / PNG 等常见图片格式</span>
            </div>
          </el-form-item>
          <el-form-item label="视频URL"><el-input v-model="form.videoUrl" placeholder="视频链接（选填，支持OSS地址）" /></el-form-item>
          <el-form-item label="上传视频">
            <div class="upload-row">
              <el-upload
                :show-file-list="false"
                accept="video/*"
                :http-request="uploadVideo"
                :disabled="uploadingVideo"
              >
                <el-button :loading="uploadingVideo">{{ uploadingVideo ? '上传中...' : '上传课程视频' }}</el-button>
              </el-upload>
              <span class="upload-tip">支持 MP4 等常见视频格式，最大 500MB · 上传后自动识别时长</span>
            </div>
          </el-form-item>
          <el-form-item v-if="form.videoUrl" label="视频预览">
            <a :href="form.videoUrl" target="_blank" rel="noreferrer" class="video-link">{{ form.videoUrl }}</a>
          </el-form-item>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="视频时长">
                <span v-if="form.duration" class="duration-display">{{ formatDuration(form.duration) }}</span>
                <span v-else class="duration-display duration-empty">上传视频后自动识别</span>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="章节数">
                <el-input-number v-model="form.chapterCount" :min="1" style="width:100%" />
                <span class="field-auto-hint" v-if="form.chapterCount">可手动调整</span>
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="课程简介">
            <el-input v-model="form.description" type="textarea" :rows="4" />
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
import { courseApi, fileApi } from '@/api'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'

const list = ref([])
const dialogVisible = ref(false)
const uploadingVideo = ref(false)
const uploadingCover = ref(false)
const form = reactive({ id: null, title: '', teacher: '', category: '其他', coverUrl: '', videoUrl: '', duration: null, chapterCount: null, description: '' })

onMounted(load)

async function load() {
  const res = await courseApi.list()
  if (res.code === 200) list.value = res.data
}

function openCreate() {
  Object.assign(form, { id: null, title: '', teacher: '', category: '其他', coverUrl: '', videoUrl: '', duration: null, chapterCount: null, description: '' })
  dialogVisible.value = true
}

function openEdit(row) {
  Object.assign(form, { ...row })
  dialogVisible.value = true
}

async function uploadCover(option) {
  uploadingCover.value = true
  try {
    const res = await fileApi.upload(option.file, 'course-covers')
    if (res.code === 200) {
      form.coverUrl = res.data.url
      ElMessage.success('封面上传成功')
      option.onSuccess?.(res.data)
    } else {
      ElMessage.error(res.message || '封面上传失败')
      option.onError?.(new Error(res.message || '封面上传失败'))
    }
  } catch (error) {
    ElMessage.error('封面上传失败')
    option.onError?.(error)
  } finally {
    uploadingCover.value = false
  }
}

function extractVideoDuration(file) {
  return new Promise((resolve) => {
    const video = document.createElement('video')
    video.preload = 'metadata'
    const url = URL.createObjectURL(file)
    video.onloadedmetadata = () => {
      const seconds = Math.round(video.duration)
      URL.revokeObjectURL(url)
      resolve(seconds)
    }
    video.onerror = () => {
      URL.revokeObjectURL(url)
      resolve(null)
    }
    video.src = url
  })
}

function formatDuration(seconds) {
  if (!seconds || seconds <= 0) return ''
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  const s = seconds % 60
  if (h > 0) return `${h}小时${m}分${s}秒`
  if (m > 0) return `${m}分${s}秒`
  return `${s}秒`
}

async function uploadVideo(option) {
  uploadingVideo.value = true
  try {
    const durationPromise = extractVideoDuration(option.file)
    const res = await fileApi.upload(option.file, 'course-videos')
    if (res.code === 200) {
      form.videoUrl = res.data.url
      const seconds = await durationPromise
      if (seconds != null) {
        form.duration = seconds
      }
      if (!form.chapterCount || form.chapterCount < 1) {
        form.chapterCount = 1
      }
      ElMessage.success(seconds != null
        ? `视频上传成功，时长 ${formatDuration(seconds)}`
        : '视频上传成功')
      option.onSuccess?.(res.data)
    } else {
      ElMessage.error(res.message || '课程视频上传失败')
      option.onError?.(new Error(res.message || '课程视频上传失败'))
    }
  } catch (error) {
    ElMessage.error('课程视频上传失败')
    option.onError?.(error)
  } finally {
    uploadingVideo.value = false
  }
}

async function save() {
  let res
  if (form.id) res = await courseApi.update(form.id, form)
  else res = await courseApi.create(form)
  if (res.code === 200) {
    ElMessage.success('保存成功')
    dialogVisible.value = false
    load()
  } else {
    ElMessage.error(res.message)
  }
}

async function deleteCourse(id) {
  const res = await courseApi.delete(id)
  if (res.code === 200) { ElMessage.success('已删除'); load() }
}
</script>

<style scoped>
.admin-wrap { display: flex; min-height: calc(100vh - 60px); }
.admin-sidebar { width: 200px; background: var(--tp-card); border-right: 1px solid var(--tp-border); flex-shrink: 0; }
.sidebar-title { padding: 20px 16px; font-weight: bold; font-size: 15px; color: var(--tp-primary); border-bottom: 1px solid var(--tp-border); }
.admin-content { flex: 1; padding: 32px; background: var(--tp-bg); }
.admin-content h2 { color: var(--tp-text); font-weight: 600; }
.upload-row { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.upload-tip { font-size: 12px; color: var(--tp-text-secondary); }
.video-link { color: var(--tp-primary); word-break: break-all; text-decoration: none; }
.cover-upload-area { display: flex; flex-direction: column; gap: 8px; }
.cover-preview { position: relative; width: 200px; height: 120px; border-radius: 6px; overflow: hidden; cursor: pointer; }
.cover-preview img { width: 100%; height: 100%; object-fit: cover; }
.cover-mask {
  position: absolute; inset: 0; display: flex; align-items: center; justify-content: center;
  background: rgba(0,0,0,0.45); color: #fff; font-size: 14px; opacity: 0; transition: opacity .2s;
}
.cover-preview:hover .cover-mask { opacity: 1; }
.cover-placeholder {
  width: 200px; height: 120px; border: 1px dashed var(--tp-border); border-radius: 6px;
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  gap: 6px; color: var(--tp-text-secondary); cursor: pointer; transition: border-color .2s;
}
.cover-placeholder:hover { border-color: var(--tp-primary); color: var(--tp-primary); }
.field-auto-hint { font-size: 11px; color: var(--tp-text-secondary); margin-left: 6px; white-space: nowrap; }
.duration-display { font-size: 14px; color: var(--tp-text); font-weight: 500; line-height: 32px; }
.duration-empty { color: var(--tp-text-secondary); font-weight: normal; font-size: 13px; }
.admin-content :deep(.el-table) { border-radius: var(--tp-radius-sm); }
.admin-content :deep(.el-dialog) { border-radius: var(--tp-radius); }
</style>
