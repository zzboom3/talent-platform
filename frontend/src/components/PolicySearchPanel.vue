<template>
  <div class="search-panel">
    <div class="search-fields">
      <el-input
        :model-value="filters.keyword"
        class="keyword-input"
        clearable
        placeholder="搜索标题、正文、作者、来源"
        prefix-icon="Search"
        @update:model-value="updateField('keyword', $event)"
      />

      <el-select
        :model-value="filters.sourceSite"
        class="filter-select"
        clearable
        filterable
        placeholder="选择来源站点"
        @update:model-value="updateField('sourceSite', $event)"
      >
        <el-option
          v-for="site in options.sourceSites"
          :key="site"
          :label="site"
          :value="site"
        />
      </el-select>

      <el-button @click="$emit('reset')">重置</el-button>
    </div>
  </div>
</template>

<script setup>
const props = defineProps({
  filters: {
    type: Object,
    required: true,
  },
  options: {
    type: Object,
    default: () => ({
      sourceSites: [],
      keywords: [],
    }),
  },
})

const emit = defineEmits(['update:filters', 'reset'])

function updateField(field, value) {
  emit('update:filters', {
    ...props.filters,
    [field]: value || '',
  })
}
</script>

<style scoped>
.search-panel {
  margin-bottom: 20px;
  padding: 20px 22px;
  border: 1px solid var(--tp-border);
  border-radius: 16px;
  background: linear-gradient(180deg, rgba(21, 101, 192, 0.04), rgba(255, 255, 255, 0.98));
  box-shadow: 0 8px 24px rgba(21, 101, 192, 0.08);
}

.search-fields {
  display: grid;
  grid-template-columns: minmax(320px, 2fr) minmax(220px, 1fr) auto;
  gap: 12px;
  align-items: center;
}

.keyword-input,
.filter-select {
  width: 100%;
}

@media (max-width: 820px) {
  .search-fields {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
