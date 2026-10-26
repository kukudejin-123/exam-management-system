import request from './request'

export const classApi = {
  page: (params) => request.get('/classes', { params }),
  all: () => request.get('/classes/all'),
  get: (id) => request.get(`/classes/${id}`),
  create: (payload) => request.post('/classes', payload),
  update: (id, payload) => request.put(`/classes/${id}`, payload),
  archive: (id, archived) => request.put(`/classes/${id}/archive`, null, { params: { archived } }),
  remove: (id) => request.delete(`/classes/${id}`)
}
