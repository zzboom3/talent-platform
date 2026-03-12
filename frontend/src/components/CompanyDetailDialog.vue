<template>
  <el-dialog
    :model-value="modelValue"
    :title="dialogTitle"
    width="640px"
    destroy-on-close
    @close="emit('update:modelValue', false)"
  >
    <div v-loading="loading">
      <template v-if="company">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="企业名称">{{ company.companyName || '暂未填写' }}</el-descriptions-item>
          <el-descriptions-item label="行业">{{ company.industry || '暂未填写' }}</el-descriptions-item>
          <el-descriptions-item label="规模">{{ company.scale || '暂未填写' }}</el-descriptions-item>
          <el-descriptions-item label="联系邮箱">{{ company.contactEmail || '暂未填写' }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ company.contactPhone || '暂未填写' }}</el-descriptions-item>
          <el-descriptions-item label="地址">{{ company.address || '暂未填写' }}</el-descriptions-item>
          <el-descriptions-item label="官网">
            <a v-if="company.website" :href="normalizeUrl(company.website)" target="_blank" rel="noopener noreferrer" class="company-link">
              {{ company.website }}
            </a>
            <span v-else>暂未填写</span>
          </el-descriptions-item>
        </el-descriptions>

        <div v-if="company.description" class="section-block">
          <h4>企业简介</h4>
          <p>{{ company.description }}</p>
        </div>

        <div v-if="company.logoUrl" class="section-block">
          <h4>企业 Logo</h4>
          <el-image :src="company.logoUrl" style="max-width: 200px; border-radius: 8px" fit="contain" />
        </div>
      </template>

      <el-empty v-else-if="!loading" description="暂无企业详情" />
    </div>
  </el-dialog>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { companyApi } from '@/api'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false,
  },
  companyId: {
    type: [Number, String],
    default: null,
  },
  companyName: {
    type: String,
    default: '',
  },
})

const emit = defineEmits(['update:modelValue'])

const loading = ref(false)
const company = ref(null)

const dialogTitle = computed(() => `${props.companyName || '企业'} - 详细信息`)

watch(
  () => [props.modelValue, props.companyId],
  async ([visible, companyId]) => {
    if (!visible || !companyId) {
      if (!visible) company.value = null
      return
    }

    loading.value = true
    try {
      const res = await companyApi.getById(companyId)
      if (res?.code === 200) {
        company.value = res.data
      } else {
        company.value = null
        ElMessage.error(res?.message || '获取企业详情失败')
      }
    } catch (error) {
      company.value = null
      ElMessage.error(error?.response?.data?.message || '获取企业详情失败')
    } finally {
      loading.value = false
    }
  },
  { immediate: true },
)

function normalizeUrl(url) {
  if (!url) return ''
  return /^https?:\/\//i.test(url) ? url : `https://${url}`
}
</script>

<style scoped>
.company-link {
  color: var(--tp-primary);
  text-decoration: none;
}

.company-link:hover {
  text-decoration: underline;
}

.section-block {
  margin-top: 16px;
}

.section-block h4 {
  margin: 0 0 8px;
  color: var(--tp-primary);
  font-size: 14px;
}

.section-block p {
  margin: 0;
  color: var(--tp-text-secondary);
  line-height: 1.7;
  white-space: pre-line;
}
</style>
