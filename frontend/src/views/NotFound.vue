<template>
  <div class="not-found-wrap">
    <div class="not-found-content">
      <div class="error-code">404</div>
      <p class="error-msg">页面不存在</p>
      <p class="error-desc">您访问的页面可能已被移除或暂时不可用</p>
      <el-button type="primary" size="large" @click="$router.push('/')">
        返回首页
      </el-button>
    </div>
  </div>
</template>

<style scoped>
.not-found-wrap {
  min-height: calc(100vh - 60px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}
.not-found-content {
  text-align: center;
}
.error-code {
  font-size: 120px;
  font-weight: 700;
  color: var(--tp-primary-light);
  line-height: 1;
  opacity: 0.6;
}
.error-msg {
  font-size: 24px;
  font-weight: 600;
  color: var(--tp-text);
  margin-top: 16px;
}
.error-desc {
  color: var(--tp-text-secondary);
  margin-top: 8px;
  margin-bottom: 32px;
}
</style>
