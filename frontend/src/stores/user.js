import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const username = ref(localStorage.getItem('username') || '')
  const role = ref(localStorage.getItem('role') || '')
  const userId = ref(localStorage.getItem('userId') || '')

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => role.value === 'ADMIN')
  const isTalent = computed(() => role.value === 'TALENT')
  const isEnterprise = computed(() => role.value === 'ENTERPRISE')

  function setUser(data) {
    token.value = data.token
    username.value = data.username
    role.value = data.role
    userId.value = String(data.userId)
    localStorage.setItem('token', data.token)
    localStorage.setItem('username', data.username)
    localStorage.setItem('role', data.role)
    localStorage.setItem('userId', String(data.userId))
  }

  function logout() {
    token.value = ''
    username.value = ''
    role.value = ''
    userId.value = ''
    localStorage.clear()
  }

  return { token, username, role, userId, isLoggedIn, isAdmin, isTalent, isEnterprise, setUser, logout }
})
