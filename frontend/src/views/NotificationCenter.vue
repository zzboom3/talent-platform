<template>
  <div class="page-wrap">
    <div class="page-header">
      <h2>消息中心</h2>
      <p class="page-intro">查看全部站内通知，支持全部已读和分页</p>
    </div>

    <div class="toolbar">
      <el-button v-if="unreadCount > 0" type="primary" size="small" @click="handleMarkAllRead">
        全部已读
      </el-button>
    </div>

    <div class="notification-list" v-loading="loading">
      <template v-if="notifications.length">
        <div
          v-for="n in notifications"
          :key="n.id"
          class="notification-card"
          :class="{ unread: !n.read }"
          @click="handleItemClick(n)"
        >
          <div class="card-header">
            <span class="card-title">{{ n.title }}</span>
            <span class="card-time">{{ formatTime(n.createTime) }}</span>
          </div>
          <div class="card-content">{{ n.content }}</div>
        </div>

        <div class="pagination-wrap">
          <el-pagination
            :current-page="page + 1"
            :page-size="size"
            :total="total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            @size-change="(v) => { size = v; page = 0; fetchList() }"
            @current-change="(v) => { page = v - 1; fetchList() }"
          />
        </div>
      </template>
      <el-empty v-else description="暂无消息" />
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { useUserStore } from '@/stores/user'
import { notificationApi } from '@/api'

const store = useUserStore()
const loading = ref(false)
const notifications = ref([])
const unreadCount = ref(0)
const page = ref(0)
const size = ref(20)
const total = ref(0)

async function fetchList() {
  if (!store.isLoggedIn) return
  loading.value = true
  try {
    const res = await notificationApi.list({ page: page.value, size: size.value })
    const data = res.data
    notifications.value = data?.content ?? []
    total.value = data?.totalElements ?? 0
  } catch (_) {}
  finally {
    loading.value = false
  }
}

async function fetchUnreadCount() {
  if (!store.isLoggedIn) return
  try {
    const res = await notificationApi.unreadCount()
    unreadCount.value = res.data ?? 0
  } catch (_) {}
}

async function handleMarkAllRead() {
  try {
    await notificationApi.markAllRead()
    unreadCount.value = 0
    fetchList()
  } catch (_) {}
}

async function handleItemClick(n) {
  if (!n.read) {
    try {
      await notificationApi.markRead(n.id)
      n.read = true
      if (unreadCount.value > 0) unreadCount.value--
    } catch (_) {}
  }
}

function formatTime(t) {
  if (!t) return ''
  const d = new Date(t)
  return d.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

onMounted(() => {
  fetchList()
  fetchUnreadCount()
})

watch(() => store.isLoggedIn, (loggedIn) => {
  if (loggedIn) {
    fetchList()
    fetchUnreadCount()
  }
})
</script>

<style scoped>
.page-wrap {
  max-width: 720px;
  margin: 0 auto;
  padding: 24px 16px;
}
.page-header {
  margin-bottom: 20px;
}
.page-header h2 {
  margin: 0 0 8px 0;
  font-size: 22px;
  color: var(--el-text-color-primary);
}
.page-intro {
  margin: 0;
  font-size: 14px;
  color: var(--el-text-color-secondary);
}
.toolbar {
  margin-bottom: 16px;
}
.notification-list {
  min-height: 200px;
}
.notification-card {
  padding: 16px;
  margin-bottom: 12px;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: var(--el-border-radius-base);
  cursor: pointer;
  transition: all 0.2s;
}
.notification-card:hover {
  border-color: var(--el-color-primary-light-5);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}
.notification-card.unread {
  background: var(--el-color-primary-light-9);
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.card-title {
  font-weight: 600;
  font-size: 15px;
}
.card-time {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.card-content {
  font-size: 14px;
  color: var(--el-text-color-regular);
  line-height: 1.5;
}
.pagination-wrap {
  margin-top: 24px;
  display: flex;
  justify-content: center;
}
</style>
