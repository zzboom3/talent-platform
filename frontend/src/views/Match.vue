<template>
  <div class="page-wrap">
    <div class="page-header">
      <h2>{{ pageTitle }}</h2>
      <p class="page-intro">{{ pageIntro }}</p>
    </div>

    <el-card v-if="!store.isLoggedIn" class="login-card" shadow="hover">
      <h3>请先登录</h3>
      <p>登录后可根据账号身份进入对应的 AI 智能匹配界面。</p>
      <div class="action-row">
        <el-button type="primary" size="large" @click="router.push('/login')">前往登录</el-button>
        <el-button size="large" @click="router.push('/register')">注册账号</el-button>
      </div>
    </el-card>

    <template v-else>
      <section v-if="isAdmin" class="admin-switch">
        <button
          class="admin-switch-card"
          :class="{ active: adminDirection === 'job' }"
          type="button"
          @click="adminDirection = 'job'"
        >
          <div class="switch-title">管理员查看人才找岗位</div>
          <p>从人才输入的角度观察岗位匹配效果。</p>
        </button>
        <button
          class="admin-switch-card"
          :class="{ active: adminDirection === 'talent' }"
          type="button"
          @click="adminDirection = 'talent'"
        >
          <div class="switch-title">管理员查看岗位找人才</div>
          <p>从招聘需求的角度观察人才匹配效果。</p>
        </button>
      </section>

      <el-card class="workspace-card" shadow="hover">
        <div class="workspace-head">
          <div>
            <h3>{{ panelTitle }}</h3>
            <p>{{ panelIntro }}</p>
          </div>
        </div>

        <div class="form-panel">
          <template v-if="currentDirection === 'job'">
            <label class="field-label">输入你的求职描述</label>
            <el-input
              v-model="jobQuery"
              type="textarea"
              :rows="6"
              resize="none"
              placeholder="例如：我做了2年前端，熟悉 Vue3 和 TypeScript，想找太原或远程的中后台岗位。"
              @keyup.ctrl.enter="searchJobsWithAi"
            />

            <div v-if="store.isTalent" class="switch-row">
              <span>结合我的人才档案</span>
              <el-switch v-model="useTalentProfile" />
            </div>
          </template>

          <template v-else>
            <div v-if="store.isEnterprise" class="field-group">
              <label class="field-label">选择岗位</label>
              <el-select
                v-model="selectedJobId"
                clearable
                filterable
                placeholder="可直接选择已发布岗位"
                style="width: 100%"
              >
                <el-option
                  v-for="job in myJobs"
                  :key="job.id"
                  :label="job.title"
                  :value="job.id"
                />
              </el-select>
            </div>

            <div v-if="selectedJobPreview" class="job-preview">
              <div class="job-preview-title">{{ selectedJobPreview.title }}</div>
              <div class="job-preview-text">{{ selectedJobPreview.requirements || selectedJobPreview.description || '该岗位暂未填写详细要求' }}</div>
            </div>

            <label class="field-label">输入招聘需求</label>
            <el-input
              v-model="talentQuery"
              type="textarea"
              :rows="6"
              resize="none"
              placeholder="例如：想找一位有 Java 后端经验、懂 Spring Cloud、能独立推进模块交付的工程师。"
              @keyup.ctrl.enter="searchTalentsWithAi"
            />
          </template>

          <div class="action-row">
            <el-button
              type="primary"
              size="large"
              :loading="currentDirection === 'job' ? jobLoading : talentLoading"
              @click="currentDirection === 'job' ? searchJobsWithAi() : searchTalentsWithAi()"
            >
              {{ currentDirection === 'job' ? 'AI 匹配岗位' : 'AI 匹配人才' }}
            </el-button>
            <el-button size="large" @click="resetCurrentForm">清空输入</el-button>
          </div>
        </div>
      </el-card>

      <section v-if="currentLoading || currentSearched || currentResults.length" class="result-section">
        <div class="result-head">
          <div>
            <h3>{{ resultTitle }}</h3>
          </div>
        </div>

        <div v-if="currentLoading" class="skeleton-grid">
          <el-card v-for="n in 3" :key="n" class="result-card">
            <el-skeleton :rows="5" animated />
          </el-card>
        </div>

        <template v-else-if="currentResults.length">
          <el-row :gutter="20">
            <el-col
              v-for="item in currentResults"
              :key="item.jobId || item.talentId || item.id"
              :xs="24"
              :sm="12"
              :lg="8"
            >
              <el-card class="result-card" shadow="hover">
                <template v-if="currentDirection === 'job'">
                  <div class="card-head">
                    <div>
                      <div class="card-title">{{ item.title || `岗位 #${item.id}` }}</div>
                      <div class="card-sub">{{ item.companyName || '企业名称未填写' }}</div>
                    </div>
                    <el-tag v-if="item.scoreValue != null" :type="scoreTagType(item.scoreValue)" effect="dark" round>
                      {{ item.scoreValue }} 分
                    </el-tag>
                  </div>
                  <div class="card-meta">
                    <span>{{ item.city || '城市未填' }}</span>
                    <span>{{ item.salaryRange || '薪资未填' }}</span>
                  </div>
                  <div class="card-body">{{ item.reason || item.requirements || item.description || '暂无岗位描述' }}</div>
                  <div class="card-footer">
                    <el-button type="primary" plain @click="router.push(`/jobs/${item.jobId || item.id}`)">查看岗位</el-button>
                  </div>
                </template>

                <template v-else>
                  <div class="card-head">
                    <div class="talent-brief">
                      <el-avatar :size="44" :src="item.avatarUrl" icon="UserFilled" />
                      <div class="talent-brief-text">
                        <div class="card-title">{{ item.realName || item.username || '未填写姓名' }}</div>
                        <div class="card-sub">
                          {{ item.expectedPosition || '求职方向未填' }}
                          <span v-if="item.workYears"> · {{ item.workYears }}</span>
                          <span v-if="item.city"> · {{ item.city }}</span>
                        </div>
                        <div class="card-sub secondary">
                          {{ item.education || '学历未填' }}
                          <span v-if="item.major"> · {{ item.major }}</span>
                          <span v-if="item.expectedSalary"> · {{ item.expectedSalary }}</span>
                        </div>
                      </div>
                    </div>
                    <el-tag v-if="item.scoreValue != null" :type="scoreTagType(item.scoreValue)" effect="dark" round>
                      {{ item.scoreValue }} 分
                    </el-tag>
                  </div>
                  <div class="skill-list">
                    <el-tag v-for="skill in splitSkills(item.skills)" :key="skill" size="small" effect="plain">
                      {{ skill }}
                    </el-tag>
                  </div>
                  <div class="card-body">
                    {{ item.reason || item.selfIntroduction || item.projectExperience || '该人才暂无更多分析，建议进入详情查看完整档案。' }}
                  </div>
                  <div class="card-footer">
                    <el-button type="primary" plain @click="router.push(`/talents/${item.talentId || item.id}`)">查看人才</el-button>
                  </div>
                </template>
              </el-card>
            </el-col>
          </el-row>
        </template>

        <el-empty
          v-else-if="currentSearched"
          :description="currentDirection === 'job' ? '没有找到合适岗位' : '没有找到合适人才'"
          class="result-empty"
        />
      </section>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { aiApi, jobApi } from '@/api'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const store = useUserStore()
const router = useRouter()

const adminDirection = ref('job')
const jobQuery = ref('')
const talentQuery = ref('')
const useTalentProfile = ref(true)
const selectedJobId = ref(null)
const myJobs = ref([])

const matchedJobs = ref([])
const matchedTalents = ref([])
const jobLoading = ref(false)
const talentLoading = ref(false)
const jobSearched = ref(false)
const talentSearched = ref(false)

const isAdmin = computed(() => store.isAdmin)
const currentDirection = computed(() => {
  if (store.isAdmin) return adminDirection.value
  if (store.isEnterprise) return 'talent'
  return 'job'
})

const pageTitle = computed(() => {
  if (!store.isLoggedIn) return '智能匹配'
  if (store.isAdmin) return '智能匹配'
  return currentDirection.value === 'job' ? '智能匹配岗位' : '智能匹配人才'
})

const pageIntro = computed(() => {
  if (!store.isLoggedIn) return '登录后根据账号身份进入对应的 AI 智能匹配界面。'
  if (store.isAdmin) return '管理员可切换查看人才找岗位或岗位找人才的智能匹配结果。'
  return currentDirection.value === 'job'
    ? '根据你的描述和档案信息，AI 会为你匹配更合适的岗位。'
    : '根据岗位需求和已发布岗位信息，AI 会为你匹配更合适的人才。'
})

const panelTitle = computed(() => (
  currentDirection.value === 'job'
    ? 'AI 智能匹配岗位'
    : 'AI 智能匹配人才'
))

const panelIntro = computed(() => (
  currentDirection.value === 'job'
    ? '输入你的求职描述后，系统会自动完成 AI 匹配。'
    : '输入招聘需求后，系统会自动完成 AI 匹配。'
))

const selectedJobPreview = computed(() => myJobs.value.find((item) => item.id === selectedJobId.value) || null)
const currentLoading = computed(() => (currentDirection.value === 'job' ? jobLoading.value : talentLoading.value))
const currentSearched = computed(() => (currentDirection.value === 'job' ? jobSearched.value : talentSearched.value))
const normalizedJobs = computed(() => normalizeByScore(matchedJobs.value))
const normalizedTalents = computed(() => normalizeByScore(matchedTalents.value))
const currentResults = computed(() => (currentDirection.value === 'job' ? normalizedJobs.value : normalizedTalents.value))

const resultTitle = computed(() => {
  return currentDirection.value === 'job'
    ? `共找到 ${currentResults.value.length} 个岗位结果`
    : `共找到 ${currentResults.value.length} 位人才结果`
})

onMounted(async () => {
  if (store.isEnterprise) {
    const response = await jobApi.getMy().catch(() => null)
    if (response?.code === 200) myJobs.value = Array.isArray(response.data) ? response.data : []
  }
})

function normalizeByScore(list) {
  const rows = Array.isArray(list) ? [...list] : []
  return rows
    .map((item) => ({ ...item, scoreValue: parseScore(item.score) }))
    .sort((a, b) => {
      if (a.scoreValue == null && b.scoreValue == null) return 0
      if (a.scoreValue == null) return 1
      if (b.scoreValue == null) return -1
      return b.scoreValue - a.scoreValue
    })
}

function parseScore(score) {
  if (score == null || score === '') return null
  const value = Number(score)
  return Number.isFinite(value) ? Math.round(value) : null
}

function scoreTagType(score) {
  if (score >= 80) return 'success'
  if (score >= 60) return 'warning'
  return 'danger'
}

function splitSkills(skills) {
  if (!skills) return []
  return skills
    .split(/[,，/\s]+/)
    .map((item) => item.trim())
    .filter(Boolean)
    .slice(0, 6)
}

function resetCurrentForm() {
  if (currentDirection.value === 'job') {
    jobQuery.value = ''
    matchedJobs.value = []
    jobSearched.value = false
    if (store.isTalent) useTalentProfile.value = true
    return
  }

  talentQuery.value = ''
  selectedJobId.value = null
  matchedTalents.value = []
  talentSearched.value = false
}

async function searchJobsWithAi() {
  if (!store.isLoggedIn) {
    ElMessage.warning('请先登录后使用 AI 搜索')
    return
  }
  if (!jobQuery.value.trim() && !useTalentProfile.value) {
    ElMessage.warning('请输入求职描述，或开启档案增强匹配')
    return
  }

  jobLoading.value = true
  jobSearched.value = true
  try {
    const res = await aiApi.match({
      query: jobQuery.value,
      useProfile: useTalentProfile.value,
    })
    if (res.code === 200 && Array.isArray(res.data?.matches)) {
      matchedJobs.value = res.data.matches
    } else {
      matchedJobs.value = []
      ElMessage.error(res.data?.error || res.message || '匹配失败')
    }
  } catch {
    matchedJobs.value = []
    ElMessage.error('AI 服务请求失败')
  } finally {
    jobLoading.value = false
  }
}

async function searchTalentsWithAi() {
  if (!store.isLoggedIn) {
    ElMessage.warning('请先登录后使用 AI 搜索')
    return
  }
  if (!selectedJobId.value && !talentQuery.value.trim()) {
    ElMessage.warning('请选择岗位或输入招聘需求')
    return
  }

  talentLoading.value = true
  talentSearched.value = true
  try {
    const payload = { query: talentQuery.value }
    if (selectedJobId.value) payload.jobId = selectedJobId.value

    const res = await aiApi.matchTalents(payload)
    if (res.code === 200 && Array.isArray(res.data?.matches)) {
      matchedTalents.value = res.data.matches
    } else {
      matchedTalents.value = []
      ElMessage.error(res.data?.error || res.message || '匹配失败')
    }
  } catch {
    matchedTalents.value = []
    ElMessage.error('AI 服务请求失败')
  } finally {
    talentLoading.value = false
  }
}
</script>

<style scoped>
.page-wrap {
  max-width: var(--page-max-width);
  margin: 0 auto;
  padding: 32px var(--page-padding) 56px;
}

.page-header {
  margin-bottom: 24px;
}

.page-header h2 {
  margin: 0 0 8px;
  font-size: 24px;
  color: var(--tp-text);
  font-weight: 600;
}

.page-intro {
  margin: 0;
  font-size: 14px;
  line-height: 1.5;
  color: var(--tp-text-secondary);
}

.login-card,
.workspace-card {
  border-radius: 20px;
}

.login-card h3,
.workspace-head h3,
.result-head h3 {
  margin: 8px 0 10px;
  font-size: 24px;
  color: var(--tp-text);
}

.login-card p,
.workspace-head p,
.result-head p {
  margin: 0;
  line-height: 1.7;
  color: var(--tp-text-secondary);
}

.admin-switch {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.admin-switch-card {
  padding: 20px 22px;
  border-radius: 18px;
  border: 1px solid var(--tp-border);
  background: #fff;
  text-align: left;
  cursor: pointer;
  transition: all 0.2s ease;
}

.admin-switch-card.active,
.admin-switch-card:hover {
  border-color: rgba(21, 101, 192, 0.32);
  box-shadow: 0 10px 24px rgba(21, 101, 192, 0.1);
}

.switch-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--tp-text);
}

.admin-switch-card p {
  margin: 10px 0 0;
  line-height: 1.6;
  color: var(--tp-text-secondary);
}

.workspace-head,
.result-head {
  display: flex;
  align-items: flex-start;
  gap: 20px;
  margin-bottom: 24px;
}

.form-panel {
  padding: 0;
}

.field-label {
  display: block;
  margin-bottom: 10px;
  font-size: 14px;
  font-weight: 600;
  color: var(--tp-text);
}

.field-tip {
  margin-bottom: 16px;
}

.switch-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 16px;
  padding: 14px 16px;
  border-radius: 14px;
  background: #f5faff;
  color: var(--tp-text);
}

.job-preview {
  margin-bottom: 16px;
  padding: 16px;
  border-radius: 16px;
  background: #f5faff;
  border: 1px solid rgba(21, 101, 192, 0.12);
}

.job-preview-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--tp-text);
}

.job-preview-meta {
  display: none;
}

.job-preview-text {
  margin-top: 8px;
  font-size: 13px;
  line-height: 1.6;
  color: var(--tp-text-secondary);
}

.action-row {
  display: flex;
  gap: 12px;
  margin-top: 18px;
}

.result-section {
  margin-top: 6px;
}

.skeleton-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 20px;
}

.result-card,
.result-empty {
  border-radius: 18px;
}

.result-card {
  height: 100%;
}

.card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.talent-brief {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.talent-brief-text {
  min-width: 0;
}

.card-title {
  font-size: 18px;
  font-weight: 600;
  line-height: 1.4;
  color: var(--tp-text);
}

.card-sub {
  margin-top: 6px;
  font-size: 13px;
  color: var(--tp-primary);
}

.card-sub.secondary {
  color: var(--tp-text-secondary);
}

.card-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 14px;
  font-size: 13px;
  color: var(--tp-text-secondary);
}

.skill-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 14px;
  min-height: 32px;
}

.card-body {
  min-height: 72px;
  font-size: 14px;
  line-height: 1.7;
  color: var(--tp-text-secondary);
  display: -webkit-box;
  -webkit-line-clamp: 4;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-footer {
  margin-top: 18px;
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 1100px) {
  .workspace-head,
  .result-head {
    flex-direction: column;
  }

  .skeleton-grid {
    grid-template-columns: 1fr;
    min-width: 0;
  }
}

@media (max-width: 900px) {
  .page-wrap {
    padding: 24px 18px 40px;
  }

  .admin-switch,
  .skeleton-grid {
    grid-template-columns: 1fr;
  }

  .action-row {
    flex-direction: column;
  }
}

@media (max-width: 640px) {
  .page-header h2,
  .login-card h3,
  .workspace-head h3,
  .result-head h3 {
    font-size: 22px;
  }
}
</style>
