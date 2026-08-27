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

  decideApproval(id, payload) {
    return api.patch(`/api/courses/${id}/approval`, payload)
  },

  create(data) {
    return api.post('/api/courses', data)
  },

  update(id, data) {
    return api.patch(`/api/courses/${id}`, data)
  }
}