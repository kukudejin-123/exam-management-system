import request from './request'

export const examApi = {
  create: (payload) => request.post('/exams', payload),
  get: (id) => request.get(`/exams/${id}`),
  byClass: (classId) => request.get(`/exams/by-class/${classId}`),
  roster: (id) => request.get(`/exams/${id}/roster`),
  students: (classId) => request.get(`/exams/students/${classId}`)
}
