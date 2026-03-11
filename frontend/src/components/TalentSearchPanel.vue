<template>
  <div class="search-panel">
    <div class="search-fields">
      <el-input
        :model-value="filters.keyword"
        class="keyword-input"
        clearable
        placeholder="搜索姓名、技能、经历、项目、专业、期望岗位、学历、城市"
        prefix-icon="Search"
        @update:model-value="updateField('keyword', $event)"
      />

      <el-select
        :model-value="filters.city"
        class="filter-select"
        clearable
        filterable
        placeholder="选择城市"
        @update:model-value="updateField('city', $event)"
      >
        <el-option
          v-for="city in options.cities"
          :key="city"
          :label="city"
          :value="city"
        />
      </el-select>

      <el-select
        :model-value="filters.education"
        class="filter-select"
        clearable
        filterable
        placeholder="选择学历"
        @update:model-value="updateField('education', $event)"
      >
        <el-option
          v-for="education in options.educations"
          :key="education"
          :label="education"
          :value="education"
        />
      </el-select>

      <el-select
        :model-value="filters.skill"
        class="filter-select"
        clearable
        filterable
        allow-create
        default-first-option
        placeholder="选择或输入技能"
        @update:model-value="updateField('skill', $event)"
      >
        <el-option
          v-for="skill in options.skills"
          :key="skill"
          :label="skill"
          :value="skill"
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
      cities: [],
      educations: [],
      skills: [],
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
  grid-template-columns: minmax(260px, 2fr) repeat(3, minmax(160px, 1fr)) auto;
  gap: 12px;
  align-items: center;
}

.keyword-input,
.filter-select {
  width: 100%;
}

@media (max-width: 1100px) {
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
