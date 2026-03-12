<template>
  <div class="page-wrap">
    <div class="page-header">
      <h2>我的档案</h2>
      <el-button type="primary" @click="openDialog">
        {{ myProfile ? '编辑档案' : '创建档案' }}
      </el-button>
    </div>

    <el-card v-if="myProfile && !dialogVisible" shadow="always" class="profile-card">
      <div class="profile-display">
        <div class="profile-left">
          <el-avatar :size="120" :src="myProfile.avatarUrl" icon="UserFilled" class="profile-avatar" />
        </div>
        <div class="profile-main">
          <div class="profile-head">
            <div>
              <h2>{{ myProfile.realName || store.username }}</h2>
              <p class="profile-meta">
                {{ myProfile.expectedPosition || '待补充求职方向' }}
                <span v-if="myProfile.city"> · {{ myProfile.city }}</span>
                <span v-if="myProfile.education"> · {{ myProfile.education }}</span>
              </p>
            </div>
            <el-tag v-if="myProfile.expectedSalary" type="success" effect="plain">
              {{ myProfile.expectedSalary }}
            </el-tag>
          </div>

          <div class="skill-tags">
            <el-tag v-for="sk in splitSkills(myProfile.skills)" :key="sk" type="info" style="margin:4px">
              {{ sk }}
            </el-tag>
            <span v-if="!myProfile.skills" class="empty-tip">暂未填写技能</span>
          </div>

          <el-divider />

          <el-descriptions :column="2" border class="profile-descriptions">
            <el-descriptions-item label="性别">{{ displayValue(myProfile.gender) }}</el-descriptions-item>
            <el-descriptions-item label="工作年限">{{ displayValue(myProfile.workYears) }}</el-descriptions-item>
            <el-descriptions-item label="毕业院校">{{ displayValue(myProfile.graduationSchool) }}</el-descriptions-item>
            <el-descriptions-item label="专业">{{ displayValue(myProfile.major) }}</el-descriptions-item>
            <el-descriptions-item label="所在城市">{{ displayValue(myProfile.city) }}</el-descriptions-item>
            <el-descriptions-item label="期望岗位">{{ displayValue(myProfile.expectedPosition) }}</el-descriptions-item>
          </el-descriptions>

          <div class="profile-section">
            <h3>个人简介</h3>
            <p class="section-text">{{ myProfile.selfIntroduction || '暂未填写' }}</p>
          </div>

          <div class="profile-section">
            <h3>工作经历</h3>
            <p class="section-text">{{ myProfile.experience || '暂未填写' }}</p>
          </div>

          <div class="profile-section">
            <h3>项目经历</h3>
            <p class="section-text">{{ myProfile.projectExperience || '暂未填写' }}</p>
          </div>

          <div class="profile-section">
            <h3>证书与成果</h3>
            <p class="section-text">{{ myProfile.certificates || '暂未填写' }}</p>
          </div>
        </div>
      </div>
    </el-card>

    <el-card v-if="myProfile && !dialogVisible" shadow="hover" class="quick-links-card">
      <template #header><span class="quick-links-title">快捷入口</span></template>
      <div class="quick-links">
        <el-button type="primary" @click="$router.push('/match')">
          <el-icon><Search /></el-icon>
          智能匹配岗位
        </el-button>
        <el-button type="success" @click="$router.push('/jobs')">
          <el-icon><Briefcase /></el-icon>
          浏览岗位
        </el-button>
        <el-button @click="$router.push('/career-assessment')">
          <el-icon><DataAnalysis /></el-icon>
          AI 能力评估
        </el-button>
        <el-button @click="$router.push('/my-applications')">
          <el-icon><Document /></el-icon>
          我的申请
        </el-button>
        <el-button @click="$router.push('/certificates')">
          <el-icon><DocumentCopy /></el-icon>
          我的证书
        </el-button>
      </div>
    </el-card>

    <el-empty v-else-if="!myProfile && !dialogVisible" description="您还未创建人才档案" class="empty-with-actions">
      <p class="empty-tip">创建人才档案后即可申请岗位、使用 AI 智能匹配</p>
      <el-button type="primary" @click="openDialog">立即创建档案</el-button>
    </el-empty>

    <el-dialog v-model="dialogVisible" :title="myProfile ? '编辑档案' : '创建档案'" width="760px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="头像">
          <div class="upload-field">
            <el-avatar :size="72" :src="form.avatarUrl" icon="UserFilled" class="talent-avatar" />
            <div class="upload-actions">
              <el-upload
                :show-file-list="false"
                accept="image/*"
                :http-request="uploadAvatar"
                :disabled="uploadingAvatar"
              >
                <el-button :loading="uploadingAvatar" size="small">上传头像</el-button>
              </el-upload>
              <div class="upload-tip">支持 JPG、PNG 格式</div>
            </div>
          </div>
        </el-form-item>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="真实姓名">
              <el-input v-model="form.realName" placeholder="请输入真实姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="性别">
              <el-select v-model="form.gender" style="width:100%" placeholder="请选择性别">
                <el-option v-for="item in genderOptions" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="所在城市">
              <el-input v-model="form.city" placeholder="如：太原" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="学历">
              <el-select v-model="form.education" style="width:100%" placeholder="请选择学历">
                <el-option v-for="e in educations" :key="e" :label="e" :value="e" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="毕业院校">
              <el-input v-model="form.graduationSchool" placeholder="请输入毕业院校" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="专业">
              <el-input v-model="form.major" placeholder="请输入所学专业" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="工作年限">
              <el-select v-model="form.workYears" style="width:100%" placeholder="请选择工作年限">
                <el-option v-for="item in workYearOptions" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="期望岗位">
              <el-input v-model="form.expectedPosition" placeholder="如：Java 开发工程师" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="期望薪资">
          <el-input v-model="form.expectedSalary" placeholder="如：8k-12k / 面议" />
        </el-form-item>

        <el-form-item label="技能标签">
          <el-input v-model="form.skills" placeholder="用逗号分隔，如：Java,Python,Vue" />
        </el-form-item>

        <el-form-item label="个人简介">
          <el-input
            v-model="form.selfIntroduction"
            type="textarea"
            :rows="3"
            maxlength="300"
            show-word-limit
            placeholder="简要介绍你的方向优势、求职意向和个人特点"
          />
        </el-form-item>

        <el-form-item label="工作经历">
          <el-input v-model="form.experience" type="textarea" :rows="4" placeholder="请简要描述工作经历" />
        </el-form-item>

        <el-form-item label="项目经历">
          <el-input
            v-model="form.projectExperience"
            type="textarea"
            :rows="4"
            placeholder="请描述代表项目、职责分工和项目成果"
          />
        </el-form-item>

        <el-form-item label="证书成果">
          <el-input
            v-model="form.certificates"
            type="textarea"
            :rows="3"
            placeholder="如：软考证书、技能等级证书、竞赛获奖、作品成果等"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveProfile">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { talentApi, fileApi } from '@/api'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const store = useUserStore()
const myProfile = ref(null)
const dialogVisible = ref(false)
const uploadingAvatar = ref(false)
const form = reactive({
  realName: '',
  skills: '',
  education: '',
  gender: '',
  graduationSchool: '',
  major: '',
  workYears: '',
  expectedPosition: '',
  expectedSalary: '',
  experience: '',
  projectExperience: '',
  selfIntroduction: '',
  certificates: '',
  city: '',
  avatarUrl: '',
})
const educations = ['高中/中专', '大专', '本科', '硕士', '博士']
const genderOptions = ['男', '女', '其他']
const workYearOptions = ['应届/在校', '1年以内', '1-3年', '3-5年', '5-10年', '10年以上']

onMounted(async () => {
  const r = await talentApi.getMy().catch(() => null)
  if (r?.code === 200) myProfile.value = r.data
})

function resetForm() {
  Object.assign(form, {
    realName: '',
    skills: '',
    education: '',
    gender: '',
    graduationSchool: '',
    major: '',
    workYears: '',
    expectedPosition: '',
    expectedSalary: '',
    experience: '',
    projectExperience: '',
    selfIntroduction: '',
    certificates: '',
    city: '',
    avatarUrl: '',
  })
}

function openDialog() {
  if (myProfile.value) {
    Object.assign(form, {
      ...form,
      ...myProfile.value,
    })
  } else {
    resetForm()
  }
  dialogVisible.value = true
}

async function uploadAvatar(option) {
  uploadingAvatar.value = true
  try {
    const res = await fileApi.upload(option.file, 'avatars')
    if (res.code === 200) {
      form.avatarUrl = res.data.url
      store.setAvatarUrl(res.data.url)
      ElMessage.success('头像上传成功')
      option.onSuccess?.(res.data)
    } else {
      ElMessage.error(res.message || '头像上传失败')
      option.onError?.(new Error(res.message || '头像上传失败'))
    }
  } catch (error) {
    const status = error?.response?.status
    const message = status === 401
      ? '登录状态已失效，请重新登录后再上传'
      : error?.code === 'ECONNABORTED'
        ? '头像上传超时，请稍后重试'
        : '头像上传失败'
    ElMessage.error(message)
    option.onError?.(error)
  } finally {
    uploadingAvatar.value = false
  }
}

async function saveProfile() {
  let res
  if (myProfile.value) {
    res = await talentApi.update(myProfile.value.id, form)
  } else {
    res = await talentApi.create(form)
  }
  if (res.code === 200) {
    ElMessage.success('保存成功')
    myProfile.value = res.data
    if (res.data.avatarUrl) store.setAvatarUrl(res.data.avatarUrl)
    dialogVisible.value = false
  } else {
    ElMessage.error(res.message || '保存失败')
  }
}

function displayValue(value) {
  return value || '暂未填写'
}

const splitSkills = (s) => s ? s.split(/[,，\s]+/).map(t => t.trim()).filter(Boolean) : []
</script>

<style scoped>
.page-wrap { padding: 40px; max-width: 980px; margin: 0 auto; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 28px; }
.page-header h2 { font-size: 24px; margin: 0; color: var(--tp-text); font-weight: 600; }
.quick-links-card { margin-top: 24px; }
.quick-links-title { font-weight: 600; }
.quick-links { display: flex; flex-wrap: wrap; gap: 12px; }
.quick-links .el-button { display: inline-flex; align-items: center; gap: 6px; }
.empty-with-actions { padding: 48px 20px; }
.empty-with-actions .empty-tip { margin-bottom: 20px; color: var(--tp-text-secondary); font-size: 14px; }
.profile-card { margin-top: 0; border-radius: var(--tp-radius); }
.profile-display { display: flex; gap: 32px; align-items: flex-start; }
.profile-left { flex-shrink: 0; }
.profile-avatar { background: var(--tp-primary) !important; }
.profile-main { flex: 1; min-width: 0; }
.profile-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; margin-bottom: 12px; }
.profile-main h2 { margin: 0 0 8px 0; font-size: 24px; color: var(--tp-text); }
.profile-meta { color: var(--tp-text-secondary); margin: 0; font-size: 14px; }
.skill-tags { min-height: 28px; }
.empty-tip { color: var(--tp-text-secondary); font-size: 13px; }
.profile-descriptions { margin-top: 16px; }
.profile-section { margin-top: 18px; }
.profile-section h3 { margin: 0 0 8px 0; font-size: 16px; color: var(--tp-primary); }
.section-text { margin: 0; white-space: pre-line; color: var(--tp-text-secondary); line-height: 1.7; }
.upload-field { display: flex; align-items: center; gap: 16px; }
.upload-actions { display: flex; flex-direction: column; gap: 6px; }
.upload-tip { font-size: 12px; color: var(--tp-text-secondary); }
.talent-avatar { background: var(--tp-primary) !important; }
.page-wrap :deep(.el-dialog) { border-radius: var(--tp-radius); }

@media (max-width: 768px) {
  .page-wrap { padding: 24px 16px; }
  .profile-display { flex-direction: column; }
  .profile-head { flex-direction: column; }
}
</style>
