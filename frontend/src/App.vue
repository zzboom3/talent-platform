<template>
  <el-config-provider>
    <AppHeader v-if="!hideHeader" />
    <main :style="{ minHeight: hideHeader ? '100vh' : 'calc(100vh - 60px)', background: hideHeader ? 'none' : 'var(--tp-bg)' }">
      <router-view />
    </main>
  </el-config-provider>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import AppHeader from '@/components/AppHeader.vue'

const route = useRoute()
const hideHeader = computed(() => route.meta.hideHeader === true)
</script>

