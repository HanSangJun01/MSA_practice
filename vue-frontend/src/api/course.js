import api from './index.js'

export const courseApi = {
  getCourses(params) {
    return api.get('/api/courses', { params })
  },

  getAll(params) {
    return api.get('/api/courses', { params })
  },

  getById(id) {
    return api.get(`/api/courses/${id}`)
  },

  getMyCourses() {
    return api.get('/api/courses/my')
  },

  getPendingApprovals() {
    return api.get('/api/courses/approval/pending')
  },

  // Gateway CORS allowedMethods 에 PATCH 가 없어(GET,POST,PUT,DELETE,OPTIONS 만 허용)
  // 쓰기 API를 PUT으로 받는다 (course-service CourseController 참고)
  decideApproval(id, payload) {
    return api.put(`/api/courses/${id}/approval`, payload)
  },

  create(data) {
    return api.post('/api/courses', data)
  },

  update(id, data) {
    return api.put(`/api/courses/${id}`, data)
  }
}