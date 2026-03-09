import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000,
})

request.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  res => res.data,
  err => {
    const status = err.response?.status
    // 401：未登录，跳转登录页（静默处理，不弹 toast）
    if (status === 401) {
      localStorage.clear()
      window.location.href = '/login'
      return Promise.reject(err)
    }
    // 403：权限不足，静默处理（由业务层自行决定是否提示）
    if (status === 403) {
      return Promise.reject(err)
    }
    // 其他错误才弹 toast
    const msg = err.response?.data?.message || err.message || '请求失败'
    ElMessage.error(msg)
    return Promise.reject(err)
  }
)

export default request
