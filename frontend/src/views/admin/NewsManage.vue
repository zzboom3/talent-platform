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
        <h2>资讯管理</h2>
        <el-button type="primary" @click="openCreate">新增资讯</el-button>
      </div>
      <el-table :data="list" stripe border>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="title" label="标题" show-overflow-tooltip />
        <el-table-column prop="category" label="分类" width="90">
          <template #default="{ row }">
            <el-tag :type="tagType(row.category)" size="small">{{ catLabel(row.category) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="author" label="作者" width="100" />
        <el-table-column prop="publishTime" label="发布时间" width="160">
          <template #default="{ row }">{{ row.publishTime?.substring(0,10) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <el-button size="small" @click="openEdit(row)">编辑</el-button>
            <el-popconfirm title="确认删除?" @confirm="deleteNews(row.id)">
              <template #reference>
                <el-button type="danger" size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <el-dialog v-model="dialogVisible" :title="form.id ? '编辑资讯' : '新增资讯'" width="640px">
        <el-form :model="form" label-width="80px">
          <el-form-item label="标题"><el-input v-model="form.title" /></el-form-item>
          <el-form-item label="分类">
            <el-select v-model="form.category" style="width:100%">
              <el-option label="资讯" value="NEWS" />
              <el-option label="政策" value="POLICY" />
              <el-option label="公告" value="ANNOUNCE" />
            </el-select>
          </el-form-item>
          <el-form-item label="作者"><el-input v-model="form.author" /></el-form-item>
          <el-form-item label="内容">
            <el-input v-model="form.content" type="textarea" :rows="8" />
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
import { newsApi } from '@/api'
import { ElMessage } from 'element-plus'

const list = ref([])
const dialogVisible = ref(false)
const form = reactive({ id: null, title: '', category: 'NEWS', author: '', content: '' })

onMounted(load)

async function load() {
  const res = await newsApi.list()
  if (res.code === 200) list.value = res.data
}

function openCreate() {
  Object.assign(form, { id: null, title: '', category: 'NEWS', author: '', content: '' })
  dialogVisible.value = true
}

function openEdit(row) {
  Object.assign(form, { ...row })
  dialogVisible.value = true
}

async function save() {
  let res
  if (form.id) res = await newsApi.update(form.id, form)
  else res = await newsApi.create(form)
  if (res.code === 200) {
    ElMessage.success('保存成功')
    dialogVisible.value = false
    load()
  }
}

async function deleteNews(id) {
  const res = await newsApi.delete(id)
  if (res.code === 200) { ElMessage.success('已删除'); load() }
}

const tagType = (cat) => ({ NEWS: '', POLICY: 'success', ANNOUNCE: 'warning' }[cat] || '')
const catLabel = (cat) => ({ NEWS: '资讯', POLICY: '政策', ANNOUNCE: '公告' }[cat] || cat)
</script>

<style scoped>
.admin-wrap { display: flex; min-height: calc(100vh - 60px); }
.admin-sidebar { width: 200px; background: #fff; border-right: 1px solid #eee; flex-shrink: 0; }
.sidebar-title { padding: 20px 16px; font-weight: bold; font-size: 15px; color: #1a73e8; border-bottom: 1px solid #eee; }
.admin-content { flex: 1; padding: 32px; background: #f5f7fa; }
</style>
