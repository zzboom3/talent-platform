<template>
  <div class="page-wrap">
    <div class="page-header">
      <h2>人才库</h2>
      <el-button v-if="isTalent" type="primary" @click="openDialog">
        {{ myProfile ? '编辑我的档案' : '创建我的档案' }}
      </el-button>
    </div>

    <el-row :gutter="20">
      <el-col :span="6" v-for="t in list" :key="t.id">
        <el-card shadow="hover" class="talent-card" @click="$router.push(`/talents/${t.id}`)">
          <div style="text-align:center">
            <el-avatar :size="64" :src="t.avatarUrl" icon="UserFilled" />
            <div class="talent-name">{{ t.realName || t.user?.username }}</div>
            <div class="talent-city">{{ t.city || '未填写城市' }}</div>
            <div class="talent-edu">{{ t.education || '未填写学历' }}</div>
          </div>
          <el-divider />
          <div class="skill-tags">
            <el-tag v-for="sk in splitSkills(t.skills)" :key="sk" size="small" style="margin:2px">
              {{ sk }}
            </el-tag>
            <span v-if="!t.skills" style="color:#ccc;font-size:12px">暂未填写技能</span>
          </div>
        </el-card>
      </el-col>
    </el-row>
    <el-empty v-if="!list.length" description="暂无人才信息" />

    <!-- 创建/编辑档案弹窗 -->
    <el-dialog v-model="dialogVisible" :title="myProfile ? '编辑档案' : '创建档案'" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="真实姓名"><el-input v-model="form.realName" /></el-form-item>
        <el-form-item label="技能标签">
          <el-input v-model="form.skills" placeholder="用逗号分隔，如：Java,Python,Vue" />
        </el-form-item>
        <el-form-item label="学历">
          <el-select v-model="form.education" style="width:100%">
            <el-option v-for="e in educations" :key="e" :label="e" :value="e" />
          </el-select>
        </el-form-item>
        <el-form-item label="工作经历">
          <el-input v-model="form.experience" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="城市"><el-input v-model="form.city" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveProfile">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { talentApi } from '@/api'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const store = useUserStore()
const list = ref([])
const myProfile = ref(null)
const dialogVisible = ref(false)
const isTalent = computed(() => store.isTalent)
const form = reactive({ realName: '', skills: '', education: '', experience: '', city: '' })
const educations = ['高中/中专', '大专', '本科', '硕士', '博士']

onMounted(async () => {
  const res = await talentApi.list()
  if (res.code === 200) list.value = res.data
  if (store.isTalent) {
    const r = await talentApi.getMy().catch(() => null)
    if (r?.code === 200) myProfile.value = r.data
  }
})

function openDialog() {
  if (myProfile.value) {
    Object.assign(form, myProfile.value)
  }
  dialogVisible.value = true
}

async function saveProfile() {
  let res
  if (myProfile.value) {
    res = await talentApi.update(myProfile.value.id, form)
  } else {
    res = await talentApi.create(form)
  }
  if (res.code === 200) {
    ElMessage.success('保存成功')
    myProfile.value = res.data
    dialogVisible.value = false
    const r = await talentApi.list()
    if (r.code === 200) list.value = r.data
  } else {
    ElMessage.error(res.message)
  }
}

const splitSkills = (s) => s ? s.split(/[,，]/).map(t => t.trim()).filter(Boolean) : []
</script>

<style scoped>
.page-wrap { padding: 24px 40px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.page-header h2 { font-size: 22px; }
.talent-card { cursor: pointer; margin-bottom: 20px; }
.talent-card:hover { transform: translateY(-2px); transition: .2s; }
.talent-name { font-size: 16px; font-weight: bold; margin-top: 12px; }
.talent-city, .talent-edu { color: #888; font-size: 13px; margin-top: 4px; }
.skill-tags { min-height: 28px; }
</style>
