<template>
  <div class="search-panel">
    <div class="search-fields">
      <el-input
        :model-value="filters.keyword"
        class="keyword-input"
        clearable
        placeholder="搜索岗位名称、要求、描述、企业"
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
        :model-value="filters.companyName"
        class="filter-select"
        clearable
        filterable
        placeholder="选择企业"
        @update:model-value="updateField('companyName', $event)"
      >
        <el-option
          v-for="company in options.companies"
          :key="company"
          :label="company"
          :value="company"
        />
      </el-select>

      <el-popover
        placement="bottom"
        trigger="click"
        :width="320"
        popper-class="salary-popover"
      >
        <template #reference>
          <div class="salary-trigger">
            <span class="salary-trigger-value is-placeholder">薪资范围</span>
            <el-icon class="salary-trigger-arrow"><ArrowDown /></el-icon>
          </div>
        </template>

        <div v-if="hasSalaryBounds" class="salary-popover-content">
          <div class="salary-slider-head">
            <span class="salary-label">拖动选择区间</span>
            <span class="salary-value">{{ salaryRangeText }}</span>
          </div>
          <el-slider
            :model-value="salarySliderValue"
            range
            :min="options.salaryBounds.min"
            :max="options.salaryBounds.max"
            :step="1"
            show-stops
            @update:model-value="updateSalaryRange"
          />
        </div>
        <div v-else class="salary-popover-empty">暂无可用薪资数据</div>
      </el-popover>

      <div class="panel-actions">
        <el-button @click="$emit('reset')">重置</el-button>
        <slot name="actions" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { formatSalaryK } from '@/utils/jobSearch'

const props = defineProps({
  filters: {
    type: Object,
    required: true,
  },
  options: {
    type: Object,
    default: () => ({
      cities: [],
      companies: [],
      salaryBounds: null,
    }),
  },
})

const emit = defineEmits(['update:filters', 'reset'])
const hasSalaryBounds = computed(() => {
  const bounds = props.options.salaryBounds
  return bounds && Number.isFinite(bounds.min) && Number.isFinite(bounds.max) && bounds.max > bounds.min
})

const salarySliderValue = computed(() => {
  if (!hasSalaryBounds.value) return [0, 0]
  return Array.isArray(props.filters.salaryRange) && props.filters.salaryRange.length === 2
    ? props.filters.salaryRange
    : [props.options.salaryBounds.min, props.options.salaryBounds.max]
})

const salaryRangeText = computed(() => {
  if (!hasSalaryBounds.value) return '暂无可用薪资数据'
  const [min, max] = salarySliderValue.value
  return `${formatSalaryK(min)} - ${formatSalaryK(max)}`
})

function updateField(field, value) {
  emit('update:filters', {
    ...props.filters,
    [field]: value || '',
  })
}

function updateSalaryRange(value) {
  emit('update:filters', {
    ...props.filters,
    salaryRange: Array.isArray(value) ? value : null,
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

.salary-trigger {
  width: 100%;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border: 1px solid var(--tp-border);
  border-radius: 8px;
  background: #fff;
  color: var(--tp-text);
  padding: 0 14px;
  cursor: pointer;
  transition: border-color 0.2s ease;
}

.salary-trigger:hover {
  border-color: #c0c4cc;
}

.salary-trigger-value {
  flex: 1;
  min-width: 0;
  color: var(--tp-text);
  font-size: 14px;
  line-height: 1;
}

.salary-trigger-value.is-placeholder {
  color: #909399;
  font-size: 13px;
}

.salary-trigger-arrow {
  color: #a8abb2;
  font-size: 14px;
  margin-left: 10px;
}

.salary-slider-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  margin-bottom: 10px;
}

.salary-label {
  font-size: 14px;
  color: var(--tp-text);
  font-weight: 600;
}

.salary-value {
  font-size: 13px;
  color: var(--tp-primary);
}

.salary-popover-content {
  padding: 4px 4px 0;
}

.salary-popover-empty {
  color: var(--tp-text-secondary);
  font-size: 13px;
}

.panel-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  flex-wrap: wrap;
}

@media (max-width: 1200px) {
  .search-fields {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .panel-actions {
    justify-content: flex-start;
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
