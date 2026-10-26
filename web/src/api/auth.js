import request from './request'

export const authApi = {
  login: (userNo, password) =>
    request.post('/auth/login', { userNo, password }),

  register: (payload) =>
    request.post('/auth/register', payload),

  logout: () =>
    request.post('/auth/logout'),

  refresh: (refreshToken) =>
    request.post('/auth/refresh', { refreshToken })
}
