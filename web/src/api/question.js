import request from './request'

export const questionApi = {
  page: (params) => request.get('/questions', { params }),
  get: (id) => request.get(`/questions/${id}`),
  create: (payload) => request.post('/questions', payload),
  batchCreate: (list) => request.post('/questions/batch', list),
  update: (id, payload) => request.put(`/questions/${id}`, payload),
  changeStatus: (id, status) =>
    request.put(`/questions/${id}/status`, null, { params: { status } }),
  remove: (id) => request.delete(`/questions/${id}`)
}
