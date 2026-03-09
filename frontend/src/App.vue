<template>
  <el-config-provider>
    <AppHeader />
    <main style="min-height: calc(100vh - 60px); background: #f5f7fa;">
      <router-view />
    </main>
  </el-config-provider>
</template>

<script setup>
import AppHeader from '@/components/AppHeader.vue'
</script>

<style>
* { margin: 0; padding: 0; box-sizing: border-box; }
body { font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif; }
</style>
