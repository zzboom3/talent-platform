import request from './request'

export const authApi = {
  login: (data) => request.post('/auth/login', data),
  register: (data) => request.post('/auth/register', data),
}

export const talentApi = {
  list: () => request.get('/talents'),
  getById: (id) => request.get(`/talents/${id}`),
  getMy: () => request.get('/talents/my'),
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
  getMy: () => request.get('/company/my'),
  create: (data) => request.post('/company', data),
  update: (id, data) => request.put(`/company/${id}`, data),
}

export const applicationApi = {
  apply: (jobId) => request.post('/applications', { jobId }),
  my: () => request.get('/applications/my'),
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
}

export const matchApi = {
  search: (skills) => request.get('/match', { params: { skills } }),
}

export const adminApi = {
  users: () => request.get('/admin/users'),
  deleteUser: (id) => request.delete(`/admin/users/${id}`),
  updateRole: (id, role) => request.put(`/admin/users/${id}/role`, { role }),
  stats: () => request.get('/admin/stats'),
}

// 公开统计，无需登录，供首页使用
export const statsApi = {
  public: () => request.get('/stats'),
}
