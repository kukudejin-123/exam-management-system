import request from './request'

export const paperApi = {
  page: (params) => request.get('/papers', { params }),
  get: (id) => request.get(`/papers/${id}`),
  questions: (id) => request.get(`/papers/${id}/questions`),
  compose: (ruleId, shuffle = true) =>
    request.post('/papers/compose', null, { params: { ruleId, shuffle } }),
  preview: (ruleId, shuffle = false) =>
    request.get('/papers/preview', { params: { ruleId, shuffle } }),
  validate: (id) => request.get(`/papers/${id}/validate`),
  publish: (id) => request.post(`/papers/${id}/publish`)
}
