<template>
  <div class="admin-wrap">
    <div class="admin-sidebar">
      <div class="sidebar-title">🛠 后台管理</div>
      <el-menu :default-active="$route.path" router>
        <el-menu-item index="/admin"><el-icon><DataAnalysis /></el-icon>数据概览</el-menu-item>
        <el-menu-item index="/admin/users"><el-icon><User /></el-icon>用户管理</el-menu-item>
        <el-menu-item index="/admin/news"><el-icon><Document /></el-icon>资讯管理</el-menu-item>
        <el-menu-item index="/admin/courses"><el-icon><Reading /></el-icon>课程管理</el-menu-item>
      </el-menu>
    </div>
    <div class="admin-content">
      <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:20px">
        <h2>用户管理</h2>
        <el-input v-model="search" placeholder="搜索用户名" prefix-icon="Search"
          style="width:200px" clearable />
      </div>
      <el-table :data="filteredUsers" stripe border>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="username" label="用户名" />
        <el-table-column prop="email" label="邮箱" />
        <el-table-column prop="phone" label="手机" width="130" />
        <el-table-column prop="role" label="角色" width="120">
          <template #default="{ row }">
            <el-select v-model="row.role" size="small" @change="updateRole(row)">
              <el-option label="管理员" value="ADMIN" />
              <el-option label="人才" value="TALENT" />
              <el-option label="企业" value="ENTERPRISE" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="注册时间" width="160">
          <template #default="{ row }">{{ row.createTime?.substring(0,10) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="80">
          <template #default="{ row }">
            <el-popconfirm title="确认删除该用户?" @confirm="deleteUser(row.id)">
              <template #reference>
                <el-button type="danger" size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { adminApi } from '@/api'
import { ElMessage } from 'element-plus'

const users = ref([])
const search = ref('')
const filteredUsers = computed(() =>
  users.value.filter(u => u.username.includes(search.value))
)

onMounted(load)

async function load() {
  const res = await adminApi.users()
  if (res.code === 200) users.value = res.data
}

async function deleteUser(id) {
  const res = await adminApi.deleteUser(id)
  if (res.code === 200) { ElMessage.success('已删除'); load() }
}

async function updateRole(row) {
  const res = await adminApi.updateRole(row.id, row.role)
  if (res.code === 200) ElMessage.success('角色已更新')
  else ElMessage.error(res.message)
}
</script>

<style scoped>
.admin-wrap { display: flex; min-height: calc(100vh - 60px); }
.admin-sidebar { width: 200px; background: #fff; border-right: 1px solid #eee; flex-shrink: 0; }
.sidebar-title { padding: 20px 16px; font-weight: bold; font-size: 15px; color: #1a73e8; border-bottom: 1px solid #eee; }
.admin-content { flex: 1; padding: 32px; background: #f5f7fa; }
</style>
