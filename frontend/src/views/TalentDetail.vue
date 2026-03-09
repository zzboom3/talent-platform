<template>
  <div class="page-wrap">
    <el-button @click="$router.back()" style="margin-bottom:16px">← 返回</el-button>
    <el-card v-if="profile" shadow="always">
      <div style="display:flex;gap:32px;align-items:flex-start">
        <el-avatar :size="100" :src="profile.avatarUrl" icon="UserFilled" />
        <div>
          <h2>{{ profile.realName || profile.user?.username }}</h2>
          <p style="color:#888;margin:8px 0">{{ profile.city }} · {{ profile.education }}</p>
          <div>
            <el-tag v-for="sk in splitSkills(profile.skills)" :key="sk" style="margin:3px">{{ sk }}</el-tag>
          </div>
        </div>
      </div>
      <el-divider />
      <h3>工作经历</h3>
      <p style="margin-top:8px;white-space:pre-line;color:#555">
        {{ profile.experience || '暂未填写' }}
      </p>
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

const splitSkills = (s) => s ? s.split(/[,，]/).map(t => t.trim()).filter(Boolean) : []
</script>

<style scoped>
.page-wrap { padding: 32px 60px; max-width: 800px; margin: 0 auto; }
</style>
