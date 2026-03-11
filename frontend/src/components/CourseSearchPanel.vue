<template>
  <div class="search-panel">
    <div class="search-fields">
      <el-input
        :model-value="filters.keyword"
        class="keyword-input"
        clearable
        placeholder="搜索课程名称、简介、讲师、分类"
        prefix-icon="Search"
        @update:model-value="updateField('keyword', $event)"
      />

      <el-select
        :model-value="filters.category"
        class="filter-select"
        clearable
        filterable
        placeholder="选择课程分类"
        @update:model-value="updateField('category', $event)"
      >
        <el-option
          v-for="category in options.categories"
          :key="category"
          :label="category"
          :value="category"
        />
      </el-select>

      <el-select
        :model-value="filters.teacher"
        class="filter-select"
        clearable
        filterable
        placeholder="选择讲师"
        @update:model-value="updateField('teacher', $event)"
      >
        <el-option
          v-for="teacher in options.teachers"
          :key="teacher"
          :label="teacher"
          :value="teacher"
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
      categories: [],
      teachers: [],
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
  margin-bottom: 24px;
  padding: 20px 22px;
  border: 1px solid var(--tp-border);
  border-radius: 16px;
  background: linear-gradient(180deg, rgba(21, 101, 192, 0.04), rgba(255, 255, 255, 0.98));
  box-shadow: 0 8px 24px rgba(21, 101, 192, 0.08);
}

.search-fields {
  display: grid;
  grid-template-columns: minmax(280px, 2fr) repeat(2, minmax(180px, 1fr)) auto;
  gap: 12px;
  align-items: center;
}

.keyword-input,
.filter-select {
  width: 100%;
}

@media (max-width: 1024px) {
  .search-fields {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .search-panel {
    padding: 16px;
  }

  .search-fields {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
