import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const routes = [
  { path: '/', component: () => import('@/views/Home.vue') },
  { path: '/login', component: () => import('@/views/Login.vue'), meta: { hideHeader: true } },
  { path: '/register', component: () => import('@/views/Register.vue'), meta: { hideHeader: true } },
  { path: '/talents', component: () => import('@/views/TalentList.vue'), meta: { requiresTalentBrowse: true } },
  { path: '/talents/:id', component: () => import('@/views/TalentDetail.vue'), meta: { requiresTalentBrowse: true } },
  { path: '/talent-showcase', component: () => import('@/views/TalentShowcase.vue'), meta: { requiresTalentBrowse: true } },
  { path: '/jobs', component: () => import('@/views/JobList.vue') },
  { path: '/jobs/:id', component: () => import('@/views/JobDetail.vue') },
  { path: '/match', component: () => import('@/views/Match.vue') },
  { path: '/courses', component: () => import('@/views/CourseList.vue') },
  { path: '/courses/:id', component: () => import('@/views/CourseDetail.vue') },
  { path: '/policies', component: () => import('@/views/PolicyList.vue') },
  { path: '/policies/:id', component: () => import('@/views/PolicyDetail.vue') },
  { path: '/career-assessment', component: () => import('@/views/CareerAssessment.vue'), meta: { requiresAuth: true } },
  { path: '/certificates', component: () => import('@/views/CertificateList.vue'), meta: { requiresAuth: true } },
  { path: '/certificates/:id', component: () => import('@/views/CertificateDetail.vue'), meta: { requiresAuth: true } },
  { path: '/data-screen', component: () => import('@/views/DataScreen.vue'), meta: { hideHeader: true } },
  { path: '/company', component: () => import('@/views/CompanyProfile.vue'), meta: { requiresAuth: true } },
  { path: '/company/jobs', component: () => import('@/views/CompanyJobs.vue'), meta: { requiresAuth: true } },
  { path: '/my-profile', component: () => import('@/views/MyProfile.vue'), meta: { requiresAuth: true } },
  { path: '/my-applications', component: () => import('@/views/MyApplications.vue'), meta: { requiresAuth: true } },
  { path: '/notifications', component: () => import('@/views/NotificationCenter.vue'), meta: { requiresAuth: true } },
  { path: '/admin', component: () => import('@/views/admin/Dashboard.vue'), meta: { requiresAdmin: true } },
  { path: '/admin/users', component: () => import('@/views/admin/UserManage.vue'), meta: { requiresAdmin: true } },
  { path: '/admin/news', component: () => import('@/views/admin/NewsManage.vue'), meta: { requiresAdmin: true } },
  { path: '/admin/courses', component: () => import('@/views/admin/CourseManage.vue'), meta: { requiresAdmin: true } },
  { path: '/admin/companies', component: () => import('@/views/admin/CompanyManage.vue'), meta: { requiresAdmin: true } },
  { path: '/admin/showcase', component: () => import('@/views/admin/TalentShowcaseManage.vue'), meta: { requiresAdmin: true } },
  { path: '/admin/monitor', component: () => import('@/views/admin/SystemMonitor.vue'), meta: { requiresAdmin: true } },
  { path: '/admin/blockchain', component: () => import('@/views/admin/BlockchainManage.vue'), meta: { requiresAdmin: true } },
  { path: '/:pathMatch(.*)*', component: () => import('@/views/NotFound.vue') },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to) => {
  const store = useUserStore()
  if (to.meta.requiresTalentBrowse && !store.canBrowseTalents) {
    if (!store.isLoggedIn) {
      return { path: '/login', query: { redirect: to.fullPath } }
    }
    ElMessage.warning('仅企业端和管理员可查看人才信息')
    return store.isTalent ? '/my-profile' : '/'
  }
  if (to.meta.requiresAdmin && !store.isAdmin) {
    if (store.isLoggedIn) {
      ElMessage.warning('无管理权限')
      return '/'
    }
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.meta.requiresAuth && !store.isLoggedIn) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if ((to.path === '/login' || to.path === '/register') && store.isLoggedIn) {
    return '/'
  }
})

export default router
