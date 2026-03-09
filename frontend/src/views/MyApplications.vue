<template>
  <div class="page-wrap">
    <h2 style="margin-bottom:24px">
      {{ isEnterprise ? '📬 收到的求职申请' : '📝 我的求职申请' }}
    </h2>

    <!-- 人才：我提交的申请 -->
    <template v-if="isTalent">
      <el-empty v-if="!applications.length" description="暂未申请任何岗位" />
      <el-table v-else :data="applications" stripe border>
        <el-table-column label="岗位名称">
          <template #default="{ row }">
            <el-link type="primary" @click="$router.push(`/jobs/${row.job?.id}`)">
              {{ row.job?.title }}
            </el-link>
          </template>
        </el-table-column>
        <el-table-column label="企业">
          <template #default="{ row }">{{ row.job?.company?.companyName }}</template>
        </el-table-column>
        <el-table-column label="城市" width="90">
          <template #default="{ row }">{{ row.job?.city }}</template>
        </el-table-column>
        <el-table-column label="申请状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="申请时间" width="120">
          <template #default="{ row }">{{ row.applyTime?.substring(0,10) }}</template>
        </el-table-column>
      </el-table>
    </template>

    <!-- 企业：收到的申请 -->
    <template v-if="isEnterprise">
      <el-empty v-if="!applications.length" description="暂未收到求职申请" />
      <el-table v-else :data="applications" stripe border>
        <el-table-column label="求职者">
          <template #default="{ row }">
            <el-link type="primary" @click="$router.push(`/talents/${row.talent?.id}`)">
              {{ row.talent?.realName || row.talent?.user?.username }}
            </el-link>
          </template>
        </el-table-column>
        <el-table-column label="技能">
          <template #default="{ row }">
            <el-tag v-for="sk in splitSkills(row.talent?.skills)" :key="sk"
              size="small" style="margin:2px">{{ sk }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="申请岗位">
          <template #default="{ row }">{{ row.job?.title }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="申请时间" width="120">
          <template #default="{ row }">{{ row.applyTime?.substring(0,10) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <el-button v-if="row.status === 'PENDING'" size="small" type="success"
              @click="updateStatus(row.id, 'ACCEPTED')">通过</el-button>
            <el-button v-if="row.status === 'PENDING'" size="small" type="danger"
              @click="updateStatus(row.id, 'REJECTED')">拒绝</el-button>
            <span v-if="row.status !== 'PENDING'" style="color:#aaa;font-size:12px">已处理</span>
          </template>
        </el-table-column>
      </el-table>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { applicationApi } from '@/api'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const store = useUserStore()
const isTalent = computed(() => store.isTalent)
const isEnterprise = computed(() => store.isEnterprise)
const applications = ref([])

onMounted(load)

async function load() {
  if (store.isTalent) {
    const res = await applicationApi.my()
    if (res.code === 200) applications.value = res.data
  } else if (store.isEnterprise) {
    const res = await applicationApi.companyApplications()
    if (res.code === 200) applications.value = res.data
  }
}

async function updateStatus(id, status) {
  const res = await applicationApi.updateStatus(id, status)
  if (res.code === 200) {
    ElMessage.success(status === 'ACCEPTED' ? '已通过' : '已拒绝')
    load()
  }
}

const splitSkills = (s) => s ? s.split(/[,，]/).map(t => t.trim()).filter(Boolean).slice(0, 4) : []
const statusType = (s) => ({ PENDING: 'warning', ACCEPTED: 'success', REJECTED: 'danger' }[s] || '')
const statusLabel = (s) => ({ PENDING: '待处理', ACCEPTED: '已通过', REJECTED: '已拒绝' }[s] || s)
</script>

<style scoped>
.page-wrap { padding: 32px 40px; }
</style>
