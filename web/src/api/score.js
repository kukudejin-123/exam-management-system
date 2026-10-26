import request from './request'

export const scoreApi = {
  gradeBook: (examId, params) =>
    request.get(`/scores/exams/${examId}/grade-book`, { params }),
  finalScores: (classId) => request.get(`/scores/classes/${classId}/final`),
  update: (scoreId, finalScore) =>
    request.post(`/scores/${scoreId}`, { finalScore }),
  mine: () => request.get('/scores/mine')
}
