<template>
  <div>
    <!-- Banner -->
    <div class="banner">
      <div class="banner-content">
        <h1>京州市软件产业人才公共服务平台</h1>
        <p>整合软件人才资源 · 对接产业发展需求 · 服务终生学习成长</p>
        <div class="banner-btns">
          <el-button
            v-if="store.canBrowseTalents"
            class="banner-btn-primary"
            size="large"
            @click="$router.push('/talents')"
          >
            浏览人才库
          </el-button>
          <el-button
            v-else-if="store.isTalent"
            class="banner-btn-primary"
            size="large"
            @click="$router.push('/my-profile')"
          >
            我的档案
          </el-button>
          <el-button size="large" plain @click="$router.push('/jobs')">查看岗位</el-button>
        </div>
      </div>
    </div>

    <!-- Stats -->
    <div class="stats-row" v-loading="statsLoading">
      <div class="stat-card" v-for="s in stats" :key="s.label">
        <div class="stat-num">{{ statsLoading ? '...' : s.num }}</div>
        <div class="stat-label">{{ s.label }}</div>
      </div>
    </div>

    <div class="section-wrap">
      <el-card shadow="hover" class="showcase-card">
        <template #header>
          <div class="section-header">
            <div>
              <div class="section-title">精选人才推荐</div>
              <div class="section-subtitle">结合人才档案完整度、求职方向和技能标签展示平台优质人才</div>
            </div>
            <el-link type="primary" @click="$router.push('/talent-showcase')">查看全部 →</el-link>
          </div>
        </template>
        <el-empty v-if="!showcaseTalents.length" description="暂无精选人才" />
        <el-row v-else :gutter="20">
          <el-col v-for="item in showcaseTalents" :key="item.id" :xs="24" :sm="12" :lg="8">
            <div class="showcase-item" @click="$router.push(`/talents/${item.id}`)">
              <el-avatar :size="56" :src="item.avatarUrl" icon="UserFilled" class="showcase-avatar" />
              <div class="showcase-body">
                <div class="showcase-name">{{ item.realName || item.user?.username || '未填写姓名' }}</div>
                <div class="showcase-role">{{ item.expectedPosition || '待补充求职方向' }}</div>
                <div class="showcase-meta">
                  {{ item.education || '学历未填写' }}
                  <span v-if="item.major"> · {{ item.major }}</span>
                  <span v-if="item.workYears"> · {{ item.workYears }}</span>
                </div>
                <div class="showcase-meta">
                  {{ item.city || '城市未填写' }}
                  <span v-if="item.expectedSalary"> · {{ item.expectedSalary }}</span>
                </div>
                <div class="showcase-summary">
                  {{ item.selfIntroduction || item.projectExperience || item.experience || '暂未填写个人简介' }}
                </div>
                <div class="showcase-tags">
                  <el-tag v-for="skill in splitSkills(item.skills)" :key="skill" size="small" effect="plain">
                    {{ skill }}
                  </el-tag>
                </div>
              </div>
            </div>
          </el-col>
        </el-row>
      </el-card>
    </div>

    <!-- News & Announcements -->
    <div class="section-wrap">
      <el-row :gutter="24">
        <el-col :span="16">
          <el-card shadow="hover">
            <template #header>
              <div style="display:flex;justify-content:space-between;align-items:center">
                <span style="font-weight:bold;font-size:16px">📰 最新资讯</span>
                <el-link type="primary" @click="$router.push('/policies')">查看政策 →</el-link>
              </div>
            </template>
            <el-empty v-if="!newsList.length" description="暂无资讯" />
            <div v-for="item in newsList" :key="item.id" class="news-item"
              @click="$router.push(`/policies/${item.id}`)">
              <el-tag :type="tagType(item.category)" size="small" style="margin-right:8px">
                {{ catLabel(item.category) }}
              </el-tag>
              <span>{{ item.title }}</span>
              <span class="news-time">{{ formatDate(item.publishTime) }}</span>
            </div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="hover">
            <template #header>
              <span style="font-weight:bold;font-size:16px">🔔 公告通知</span>
            </template>
            <el-empty v-if="!announcements.length" description="暂无公告" />
            <div v-for="item in announcements" :key="item.id" class="news-item"
              @click="$router.push(`/policies/${item.id}`)">
              <span>{{ item.title }}</span>
              <span class="news-time">{{ formatDate(item.publishTime) }}</span>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { newsApi, statsApi, talentApi } from '@/api'
import { useUserStore } from '@/stores/user'

const store = useUserStore()

const newsList = ref([])
const announcements = ref([])
const showcaseTalents = ref([])
const stats = ref([
  { num: '-', label: '注册用户' },
  { num: '-', label: '人才档案' },
  { num: '-', label: '岗位信息' },
  { num: '-', label: '在线课程' },
])
const statsLoading = ref(true)

onMounted(async () => {
  const res = await newsApi.list()
  if (res.code === 200) {
    newsList.value = res.data.filter(n => n.category !== 'ANNOUNCE').slice(0, 8)
    announcements.value = res.data.filter(n => n.category === 'ANNOUNCE').slice(0, 5)
  }
  const showcaseRes = await talentApi.showcase().catch(() => null)
  if (showcaseRes?.code === 200) {
    showcaseTalents.value = Array.isArray(showcaseRes.data) ? showcaseRes.data.slice(0, 3) : []
  }
  const statsRes = await statsApi.public().catch(() => null)
  if (statsRes?.code === 200) {
    const d = statsRes.data
    stats.value = [
      { num: d.users, label: '注册用户' },
      { num: d.talents, label: '人才档案' },
      { num: d.jobs, label: '岗位信息' },
      { num: d.courses, label: '在线课程' },
    ]
  }
  statsLoading.value = false
})

const tagType = (cat) => ({ NEWS: '', POLICY: 'success', ANNOUNCE: 'warning' }[cat] || '')
const catLabel = (cat) => ({ NEWS: '资讯', POLICY: '政策', ANNOUNCE: '公告' }[cat] || cat)
const formatDate = (d) => d ? d.substring(0, 10) : ''
const splitSkills = (skills) => String(skills || '').split(/[,，/\s]+/).map(item => item.trim()).filter(Boolean).slice(0, 4)
</script>

<style scoped>
.banner {
  background: linear-gradient(135deg, var(--tp-primary-dark) 0%, var(--tp-primary) 50%, var(--tp-primary-light) 100%);
  padding: 64px 40px;
  color: #fff;
  text-align: center;
  box-shadow: 0 4px 20px rgba(13, 71, 161, 0.25);
}
.banner h1 { font-size: 32px; margin-bottom: 12px; font-weight: 600; letter-spacing: 2px; }
.banner p { font-size: 16px; opacity: 0.9; margin-bottom: 32px; letter-spacing: 1px; }
.banner-btns { display: flex; gap: 16px; justify-content: center; }
.banner-btns .el-button { border-radius: var(--tp-radius-sm); }
.banner-btns .banner-btn-primary {
  background: #fff;
  color: var(--tp-primary);
  border: 1px solid #fff;
}
.banner-btns .banner-btn-primary:hover {
  background: rgba(255, 255, 255, 0.9);
  color: var(--tp-primary);
  border-color: #fff;
}
.stats-row {
  display: flex;
  justify-content: center;
  gap: 24px;
  padding: 36px 40px;
  background: var(--tp-card);
  flex-wrap: wrap;
}
.stat-card {
  text-align: center;
  padding: 24px 48px;
  background: linear-gradient(135deg, #e3f2fd, #bbdefb);
  border-radius: var(--tp-radius);
  min-width: 120px;
  transition: transform 0.2s, box-shadow 0.2s;
}
.stat-card:hover { transform: translateY(-2px); box-shadow: var(--tp-shadow-hover); }
.stat-num { font-size: 32px; font-weight: bold; color: var(--tp-primary); }
.stat-label { color: var(--tp-text-secondary); margin-top: 6px; font-size: 14px; }
.section-wrap { padding: 32px 40px; }
.showcase-card { border-radius: var(--tp-radius); }
.section-header { display:flex; justify-content:space-between; align-items:center; gap:16px; }
.section-title { font-weight: 600; font-size: 18px; color: var(--tp-text); }
.section-subtitle { margin-top: 4px; font-size: 13px; color: var(--tp-text-secondary); }
.showcase-item {
  display: flex;
  gap: 14px;
  height: 100%;
  padding: 18px;
  border: 1px solid var(--tp-border);
  border-radius: var(--tp-radius);
  background: linear-gradient(180deg, rgba(21, 101, 192, 0.03), rgba(255, 255, 255, 0.98));
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s, border-color 0.2s;
}
.showcase-item:hover {
  transform: translateY(-3px);
  box-shadow: var(--tp-shadow-hover);
  border-color: rgba(21, 101, 192, 0.2);
}
.showcase-avatar { flex-shrink: 0; background: var(--tp-primary) !important; }
.showcase-body { min-width: 0; flex: 1; }
.showcase-name { font-size: 17px; font-weight: 600; color: var(--tp-text); line-height: 1.4; }
.showcase-role { margin-top: 6px; font-size: 13px; color: var(--tp-primary); font-weight: 600; }
.showcase-meta { margin-top: 4px; font-size: 13px; color: var(--tp-text-secondary); line-height: 1.5; }
.showcase-summary {
  margin-top: 10px;
  min-height: 42px;
  font-size: 13px;
  color: var(--tp-text-secondary);
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.showcase-tags { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 12px; }
.news-item {
  display: flex;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid var(--tp-border);
  cursor: pointer;
  font-size: 14px;
  border-radius: var(--tp-radius-sm);
  transition: background 0.2s, color 0.2s;
}
.news-item:hover { background: rgba(21, 101, 192, 0.04); color: var(--tp-primary); }
.news-time { margin-left: auto; color: var(--tp-text-secondary); font-size: 12px; opacity: 0.8; }

@media (max-width: 768px) {
  .banner { padding: 48px 20px; }
  .section-wrap, .stats-row { padding-left: 16px; padding-right: 16px; }
  .section-header { flex-direction: column; align-items: flex-start; }
}
</style>
