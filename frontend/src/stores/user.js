import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

function parseJwtPayload(token) {
  if (!token) return null
  try {
    const [, payload] = token.split('.')
    if (!payload) return null
    const base64 = payload.replace(/-/g, '+').replace(/_/g, '/')
    const normalized = base64.padEnd(base64.length + ((4 - base64.length % 4) % 4), '=')
    return JSON.parse(atob(normalized))
  } catch {
    return null
  }
}

function isTokenExpired(token) {
  const payload = parseJwtPayload(token)
  if (!payload?.exp) return false
  return payload.exp * 1000 <= Date.now()
}

function readPersistedUser() {
  const savedToken = localStorage.getItem('token') || ''
  if (savedToken && isTokenExpired(savedToken)) {
    localStorage.clear()
    return {
      token: '',
      username: '',
      role: '',
      userId: '',
      avatarUrl: '',
    }
  }
  return {
    token: savedToken,
    username: localStorage.getItem('username') || '',
    role: localStorage.getItem('role') || '',
    userId: localStorage.getItem('userId') || '',
    avatarUrl: localStorage.getItem('avatarUrl') || '',
  }
}

export const useUserStore = defineStore('user', () => {
  const persisted = readPersistedUser()
  const token = ref(persisted.token)
  const username = ref(persisted.username)
  const role = ref(persisted.role)
  const userId = ref(persisted.userId)
  const avatarUrl = ref(persisted.avatarUrl)

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => role.value === 'ADMIN')
  const isTalent = computed(() => role.value === 'TALENT')
  const isEnterprise = computed(() => role.value === 'ENTERPRISE')
  const canBrowseTalents = computed(() => isEnterprise.value || isAdmin.value)

  function setUser(data) {
    token.value = data.token
    username.value = data.username
    role.value = data.role
    userId.value = String(data.userId)
    avatarUrl.value = data.avatarUrl || ''
    localStorage.setItem('token', data.token)
    localStorage.setItem('username', data.username)
    localStorage.setItem('role', data.role)
    localStorage.setItem('userId', String(data.userId))
    localStorage.setItem('avatarUrl', avatarUrl.value)
  }

  function setAvatarUrl(url) {
    avatarUrl.value = url || ''
    localStorage.setItem('avatarUrl', avatarUrl.value)
  }

  function logout() {
    token.value = ''
    username.value = ''
    role.value = ''
    userId.value = ''
    avatarUrl.value = ''
    localStorage.clear()
  }

  return {
    token,
    username,
    role,
    userId,
    avatarUrl,
    isLoggedIn,
    isAdmin,
    isTalent,
    isEnterprise,
    canBrowseTalents,
    setUser,
    setAvatarUrl,
    logout,
  }
})
