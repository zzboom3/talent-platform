<template>
  <div class="auth-wrap">
    <el-card class="auth-card" shadow="always">
      <div class="auth-title">
        <h2>注册账号</h2>
        <p>软件产业人才公共服务平台</p>
      </div>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="0">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名（4-20位）" prefix-icon="User" size="large" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码（至少6位）"
            prefix-icon="Lock" size="large" show-password />
        </el-form-item>
        <el-form-item prop="email">
          <el-input v-model="form.email" placeholder="邮箱（选填）" prefix-icon="Message" size="large" />
        </el-form-item>
        <el-form-item prop="role">
          <el-select v-model="form.role" placeholder="请选择身份" size="large" style="width:100%">
            <el-option label="求职人才" value="TALENT" />
            <el-option label="招聘企业" value="ENTERPRISE" />
          </el-select>
        </el-form-item>
        <el-button type="primary" size="large" :loading="loading"
          style="width:100%;margin-top:8px" @click="handleRegister">
          注 册
        </el-button>
      </el-form>
      <div style="text-align:center;margin-top:16px;color:#666">
        已有账号？<el-link type="primary" @click="$router.push('/login')">立即登录</el-link>
      </div>
      <div style="text-align:center;margin-top:12px;color:#aaa;font-size:12px">
        管理员账号为系统内置，请使用 admin / admin123 登录
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { authApi } from '@/api'
import { ElMessage } from 'element-plus'

const router = useRouter()
const formRef = ref()
const loading = ref(false)
const form = reactive({ username: '', password: '', email: '', role: 'TALENT' })
const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 4, max: 20, message: '4-20位字符', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '至少6位', trigger: 'blur' },
  ],
  role: [{ required: true, message: '请选择身份', trigger: 'change' }],
}

async function handleRegister() {
  await formRef.value.validate()
  loading.value = true
  try {
    const res = await authApi.register(form)
    if (res.code === 200) {
      ElMessage.success('注册成功，请登录')
      router.push('/login')
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
.auth-card { width: 440px; border-radius: 12px; }
.auth-title { text-align: center; margin-bottom: 28px; }
.auth-title h2 { font-size: 24px; color: #1a73e8; }
.auth-title p { color: #999; margin-top: 6px; }
</style>
