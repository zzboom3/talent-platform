import request from './request'

export const authApi = {
  login: (data) => request.post('/auth/login', data),
  register: (data) => request.post('/auth/register', data),
}

export const talentApi = {
  list: () => request.get('/talents'),
  getById: (id) => request.get(`/talents/${id}`),
  getMy: () => request.get('/talents/my'),
  showcase: () => request.get('/talents/showcase'),
  create: (data) => request.post('/talents', data),
  update: (id, data) => request.put(`/talents/${id}`, data),
  delete: (id) => request.delete(`/talents/${id}`),
}

export const jobApi = {
  list: () => request.get('/jobs'),
  getById: (id) => request.get(`/jobs/${id}`),
  getMy: () => request.get('/jobs/my'),
  create: (data) => request.post('/jobs', data),
  update: (id, data) => request.put(`/jobs/${id}`, data),
  delete: (id) => request.delete(`/jobs/${id}`),
}

export const companyApi = {
  getById: (id) => request.get(`/company/detail/${id}`),
  getMy: () => request.get('/company/my'),
  create: (data) => request.post('/company', data),
  update: (id, data) => request.put(`/company/${id}`, data),
}

export const applicationApi = {
  apply: (jobId) => request.post('/applications', { jobId }),
  my: () => request.get('/applications/my'),
  companyApplications: () => request.get('/applications/company'),
  updateStatus: (id, status) => request.put(`/applications/${id}/status`, { status }),
}

export const newsApi = {
  list: (category) => request.get('/news', { params: category ? { category } : {} }),
  getById: (id) => request.get(`/news/${id}`),
  create: (data) => request.post('/news', data),
  update: (id, data) => request.put(`/news/${id}`, data),
  delete: (id) => request.delete(`/news/${id}`),
}

export const courseApi = {
  list: () => request.get('/courses'),
  getById: (id) => request.get(`/courses/${id}`),
  create: (data) => request.post('/courses', data),
  update: (id, data) => request.put(`/courses/${id}`, data),
  delete: (id) => request.delete(`/courses/${id}`),
  enroll: (courseId) => request.post('/enrollments', { courseId }),
  myEnrollments: () => request.get('/enrollments/my'),
  updateProgress: (id, data) => request.put(`/enrollments/${id}/progress`, data),
}

export const matchApi = {
  search: (skills) => request.get('/match', { params: { skills } }),
  talents: (keyword) => request.get('/match/talents', { params: { keyword: keyword || '' } }),
}

export const aiApi = {
  match: (data) => request.post('/ai/match', data),
  matchTalents: (data) => request.post('/ai/match-talents', data),
  recommendCourses: () => request.post('/ai/recommend-courses'),
  recommendShowcase: (limit = 10) => request.get('/ai/admin/recommend-showcase', {
    params: { limit },
    skipAuthRedirect: true,
  }),
  careerAssessment: () => request.post('/ai/career-assessment'),
  assessmentHistory: () => request.get('/ai/assessments'),
  getAssessment: (id) => request.get(`/ai/assessments/${id}`),
}

export const blockchainApi = {
  getChain: () => request.get('/blockchain/chain'),
  verify: () => request.get('/blockchain/verify'),
  getBlock: (hash) => request.get(`/blockchain/block/${hash}`),
  adminStats: () => request.get('/blockchain/admin/stats'),
  adminCertificates: () => request.get('/blockchain/admin/certificates'),
  certify: (enrollmentId) => request.post('/blockchain/certify', { enrollmentId }),
  myCertificates: () => request.get('/blockchain/certificates'),
  getCertificate: (id) => request.get(`/blockchain/certificates/${id}`),
}

export const recruitmentApi = {
  list: () => request.get('/recruitment'),
  listOpen: () => request.get('/recruitment/open'),
  getById: (id) => request.get(`/recruitment/${id}`),
  my: () => request.get('/recruitment/my'),
  create: (data) => request.post('/recruitment', data),
  update: (id, data) => request.put(`/recruitment/${id}`, data),
  delete: (id) => request.delete(`/recruitment/${id}`),
}

export const fileApi = {
  upload: (file, directory = 'files') => {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('directory', directory)
    return request.post('/files/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 300000,
      skipAuthRedirect: true,
    })
  },
}

export const adminApi = {
  users: () => request.get('/admin/users', { skipAuthRedirect: true }),
  deleteUser: (id) => request.delete(`/admin/users/${id}`, { skipAuthRedirect: true }),
  updateRole: (id, role) => request.put(`/admin/users/${id}/role`, { role }, { skipAuthRedirect: true }),
  stats: () => request.get('/admin/stats', { skipAuthRedirect: true }),
  talents: () => request.get('/admin/talents', { skipAuthRedirect: true }),
  companies: () => request.get('/admin/companies', { skipAuthRedirect: true }),
  auditCompany: (id, status) => request.put(`/admin/company/${id}/audit`, { status }, { skipAuthRedirect: true }),
  toggleCompanyVisibility: (id, visible) => request.put(`/admin/company/${id}/visibility`, { visible }, { skipAuthRedirect: true }),
  toggleFeatured: (id, data) => request.put(`/admin/talents/${id}/featured`, data, { skipAuthRedirect: true }),
  toggleTalentVisibility: (id, visible) => request.put(`/admin/talents/${id}/visibility`, { visible }, { skipAuthRedirect: true }),
  recommendShowcase: (limit = 10) => request.get('/ai/admin/recommend-showcase', {
    params: { limit },
    skipAuthRedirect: true,
  }),
  apiStats: () => request.get('/admin/monitor/api-stats', { skipAuthRedirect: true }),
  newsList: (params) => request.get('/admin/news', { params, skipAuthRedirect: true }),
  createAnnouncement: (data) => request.post('/admin/news/announcements', data, { skipAuthRedirect: true }),
  updateAnnouncement: (id, data) => request.put(`/admin/news/announcements/${id}`, data, { skipAuthRedirect: true }),
  reviewNews: (id, reviewStatus) => request.put(`/admin/news/${id}/review`, { reviewStatus }, { skipAuthRedirect: true }),
  deleteNews: (id) => request.delete(`/admin/news/${id}`, { skipAuthRedirect: true }),
  crawlerStatus: () => request.get('/admin/crawler/status', { skipAuthRedirect: true }),
  crawlerRun: () => request.post('/admin/crawler/run', null, { timeout: 180000, skipAuthRedirect: true }),
  blockchainStats: () => request.get('/admin/blockchain/stats', { skipAuthRedirect: true }),
  blockchainCertificates: () => request.get('/admin/blockchain/certificates', { skipAuthRedirect: true }),
}

export const statsApi = {
  public: () => request.get('/stats'),
  dashboard: () => request.get('/stats/dashboard'),
  monthlyTrend: () => request.get('/stats/monthly-trend'),
  applicationStatus: () => request.get('/stats/application-status'),
}

function notificationParams(extra = {}) {
  const token = typeof localStorage !== 'undefined' ? localStorage.getItem('token') : null
  return { ...extra, ...(token ? { token } : {}) }
}

export const notificationApi = {
  list: (params) => request.get('/notifications', { params: notificationParams(params || {}), skipAuthRedirect: true }),
  unreadCount: () => request.get('/notifications/unread-count', { params: notificationParams(), skipAuthRedirect: true }),
  markRead: (id) => request.put(`/notifications/${id}/read`, null, { params: notificationParams(), skipAuthRedirect: true }),
  markAllRead: () => request.put('/notifications/read-all', null, { params: notificationParams(), skipAuthRedirect: true }),
}
