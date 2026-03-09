import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  { path: '/', component: () => import('@/views/Home.vue') },
  { path: '/login', component: () => import('@/views/Login.vue') },
  { path: '/register', component: () => import('@/views/Register.vue') },
  { path: '/talents', component: () => import('@/views/TalentList.vue') },
  { path: '/talents/:id', component: () => import('@/views/TalentDetail.vue') },
  { path: '/jobs', component: () => import('@/views/JobList.vue') },
  { path: '/jobs/:id', component: () => import('@/views/JobDetail.vue') },
  { path: '/match', component: () => import('@/views/Match.vue') },
  { path: '/courses', component: () => import('@/views/CourseList.vue') },
  { path: '/policies', component: () => import('@/views/PolicyList.vue') },
  { path: '/policies/:id', component: () => import('@/views/PolicyDetail.vue') },
  {
    path: '/company',
    component: () => import('@/views/CompanyProfile.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/my-applications',
    component: () => import('@/views/MyApplications.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/admin',
    component: () => import('@/views/admin/Dashboard.vue'),
    meta: { requiresAdmin: true },
  },
  {
    path: '/admin/users',
    component: () => import('@/views/admin/UserManage.vue'),
    meta: { requiresAdmin: true },
  },
  {
    path: '/admin/news',
    component: () => import('@/views/admin/NewsManage.vue'),
    meta: { requiresAdmin: true },
  },
  {
    path: '/admin/courses',
    component: () => import('@/views/admin/CourseManage.vue'),
    meta: { requiresAdmin: true },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to) => {
  const store = useUserStore()
  if (to.meta.requiresAdmin && !store.isAdmin) {
    return '/login'
  }
  if (to.meta.requiresAuth && !store.isLoggedIn) {
    return '/login'
  }
  if ((to.path === '/login' || to.path === '/register') && store.isLoggedIn) {
    return '/'
  }
})

export default router
