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
      <div style="display:flex;justify-content:space-between;margin-bottom:20px">
        <h2>课程管理</h2>
        <el-button type="primary" @click="openCreate">新增课程</el-button>
      </div>
      <el-table :data="list" stripe border>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="title" label="课程名称" show-overflow-tooltip />
        <el-table-column prop="teacher" label="讲师" width="100" />
        <el-table-column prop="category" label="分类" width="100" />
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
          <el-form-item label="封面URL"><el-input v-model="form.coverUrl" placeholder="图片链接（选填）" /></el-form-item>
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
import { courseApi } from '@/api'
import { ElMessage } from 'element-plus'

const list = ref([])
const dialogVisible = ref(false)
const form = reactive({ id: null, title: '', teacher: '', category: '其他', coverUrl: '', description: '' })

onMounted(load)

async function load() {
  const res = await courseApi.list()
  if (res.code === 200) list.value = res.data
}

function openCreate() {
  Object.assign(form, { id: null, title: '', teacher: '', category: '其他', coverUrl: '', description: '' })
  dialogVisible.value = true
}

function openEdit(row) {
  Object.assign(form, { ...row })
  dialogVisible.value = true
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
.admin-sidebar { width: 200px; background: #fff; border-right: 1px solid #eee; flex-shrink: 0; }
.sidebar-title { padding: 20px 16px; font-weight: bold; font-size: 15px; color: #1a73e8; border-bottom: 1px solid #eee; }
.admin-content { flex: 1; padding: 32px; background: #f5f7fa; }
</style>
