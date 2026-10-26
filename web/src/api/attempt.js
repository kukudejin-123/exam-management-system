import request from './request'

export const attemptApi = {
  enter: (examId) => request.post(`/attempts/exams/${examId}/enter`),
  paper: (attemptId) => request.get(`/attempts/${attemptId}/paper`),
  saveAnswer: (attemptId, questionId, answer) =>
    request.post(`/attempts/${attemptId}/answers`, { questionId, answer }),
  submit: (attemptId) => request.post(`/attempts/${attemptId}/submit`),
  remaining: (attemptId) => request.get(`/attempts/${attemptId}/remaining`)
}
