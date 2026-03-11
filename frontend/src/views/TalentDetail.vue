<template>
  <div class="page-wrap">
    <el-breadcrumb separator="/" class="breadcrumb">
      <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
      <el-breadcrumb-item :to="{ path: '/talents' }">人才库</el-breadcrumb-item>
      <el-breadcrumb-item v-if="profile">{{ profile.realName || profile.user?.username || '人才详情' }}</el-breadcrumb-item>
    </el-breadcrumb>
    <el-card v-if="profile" shadow="always" class="detail-card">
      <div class="detail-header">
        <el-avatar :size="100" :src="profile.avatarUrl" icon="UserFilled" class="talent-avatar" />
        <div class="detail-main">
          <div class="name-row">
            <h2>{{ profile.realName || profile.user?.username }}</h2>
            <el-tag v-if="profile.expectedSalary" type="success" effect="plain">{{ profile.expectedSalary }}</el-tag>
          </div>
          <p class="meta-line">
            {{ profile.expectedPosition || '待补充求职方向' }}
            <span v-if="profile.city"> · {{ profile.city }}</span>
            <span v-if="profile.education"> · {{ profile.education }}</span>
          </p>
          <div class="skill-tags">
            <el-tag v-for="sk in splitSkills(profile.skills)" :key="sk" style="margin:3px">{{ sk }}</el-tag>
            <span v-if="!splitSkills(profile.skills).length" class="empty-tip">暂未填写技能</span>
          </div>
        </div>
      </div>

      <el-divider />

      <el-descriptions :column="2" border class="detail-descriptions">
        <el-descriptions-item label="性别">{{ displayValue(profile.gender) }}</el-descriptions-item>
        <el-descriptions-item label="工作年限">{{ displayValue(profile.workYears) }}</el-descriptions-item>
        <el-descriptions-item label="毕业院校">{{ displayValue(profile.graduationSchool) }}</el-descriptions-item>
        <el-descriptions-item label="专业">{{ displayValue(profile.major) }}</el-descriptions-item>
        <el-descriptions-item label="所在城市">{{ displayValue(profile.city) }}</el-descriptions-item>
        <el-descriptions-item label="期望岗位">{{ displayValue(profile.expectedPosition) }}</el-descriptions-item>
      </el-descriptions>

      <div class="detail-section">
        <h3>个人简介</h3>
        <p class="section-text">{{ profile.selfIntroduction || '暂未填写' }}</p>
      </div>

      <div class="detail-section">
        <h3>工作经历</h3>
        <p class="section-text">{{ profile.experience || '暂未填写' }}</p>
      </div>

      <div class="detail-section">
        <h3>项目经历</h3>
        <p class="section-text">{{ profile.projectExperience || '暂未填写' }}</p>
      </div>

      <div class="detail-section">
        <h3>证书与成果</h3>
        <p class="section-text">{{ profile.certificates || '暂未填写' }}</p>
      </div>
    </el-card>
    <el-empty v-else description="人才档案不存在" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { talentApi } from '@/api'

const route = useRoute()
const profile = ref(null)

onMounted(async () => {
  const res = await talentApi.getById(route.params.id)
  if (res.code === 200) profile.value = res.data
})

function displayValue(value) {
  return value || '暂未填写'
}

const splitSkills = (s) => s ? s.split(/[,，]/).map(t => t.trim()).filter(Boolean) : []
</script>

<style scoped>
.page-wrap { padding: 40px 60px; max-width: 900px; margin: 0 auto; }
.breadcrumb { margin-bottom: 20px; font-size: 14px; }
.detail-card { border-radius: var(--tp-radius); }
.detail-header { display: flex; gap: 32px; align-items: flex-start; }
.talent-avatar { background: var(--tp-primary) !important; }
.detail-main { flex: 1; min-width: 0; }
.name-row { display: flex; justify-content: space-between; gap: 12px; align-items: flex-start; }
.page-wrap h2 { color: var(--tp-text); margin: 0; }
.meta-line { color: var(--tp-text-secondary); margin: 8px 0 12px; }
.skill-tags { min-height: 28px; }
.empty-tip { color: var(--tp-text-secondary); font-size: 12px; }
.detail-descriptions { margin-top: 12px; }
.detail-section { margin-top: 18px; }
.page-wrap h3 { color: var(--tp-primary); margin: 0 0 8px; font-size: 16px; }
.section-text { margin: 0; white-space: pre-line; color: var(--tp-text-secondary); line-height: 1.7; }

@media (max-width: 768px) {
  .page-wrap { padding: 24px 16px; }
  .detail-header { flex-direction: column; }
  .name-row { flex-direction: column; }
}
</style>
