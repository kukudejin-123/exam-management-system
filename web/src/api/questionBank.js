import request from './request'

export const bankApi = {
  all: () => request.get('/question-banks')
}
