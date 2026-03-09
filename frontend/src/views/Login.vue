<template>
  <div class="auth-wrap">
    <el-card class="auth-card" shadow="always">
      <div class="auth-title">
        <h2>登录平台</h2>
        <p>软件产业人才公共服务平台</p>
      </div>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="0">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" prefix-icon="User" size="large" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码"
            prefix-icon="Lock" size="large" show-password @keyup.enter="handleLogin" />
        </el-form-item>
        <el-button type="primary" size="large" :loading="loading"
          style="width:100%;margin-top:8px" @click="handleLogin">
          登 录
        </el-button>
      </el-form>
      <div style="text-align:center;margin-top:16px;color:#666">
        还没有账号？<el-link type="primary" @click="$router.push('/register')">立即注册</el-link>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { authApi } from '@/api'
import { ElMessage } from 'element-plus'

const router = useRouter()
const store = useUserStore()
const formRef = ref()
const loading = ref(false)
const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function handleLogin() {
  await formRef.value.validate()
  loading.value = true
  try {
    const res = await authApi.login(form)
    if (res.code === 200) {
      store.setUser(res.data)
      ElMessage.success('登录成功')
      router.push('/')
    } else {
      ElMessage.error(res.message)
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-wrap {
  min-height: calc(100vh - 60px);
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #e8f4f8, #d6eaf8);
}
.auth-card { width: 420px; border-radius: 12px; }
.auth-title { text-align: center; margin-bottom: 28px; }
.auth-title h2 { font-size: 24px; color: #1a73e8; }
.auth-title p { color: #999; margin-top: 6px; }
</style>
