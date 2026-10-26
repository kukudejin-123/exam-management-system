import { defineStore } from 'pinia'
import { authApi } from '@/api/auth'

// 角色常量与后端 Role 枚举保持一致
export const ROLE = {
  ADMIN: 'ADMIN',
  TEACHER: 'TEACHER',
  MARKER: 'MARKER',
  STUDENT: 'STUDENT'
}

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('accessToken') || '',
    refreshToken: localStorage.getItem('refreshToken') || '',
    userId: Number(localStorage.getItem('userId')) || null,
    role: localStorage.getItem('role') || '',
    name: localStorage.getItem('name') || ''
  }),

  getters: {
    isLoggedIn: (state) => !!state.token,
    isAdmin: (state) => state.role === ROLE.ADMIN,
    isTeacher: (state) => state.role === ROLE.TEACHER,
    isStudent: (state) => state.role === ROLE.STUDENT,
    isMarker: (state) => state.role === ROLE.MARKER,
    // 教师与管理员可写；学生与阅卷人只读
    canWrite: (state) => state.role === ROLE.ADMIN || state.role === ROLE.TEACHER,
    roleLabel: (state) => ({
      [ROLE.ADMIN]: '管理员',
      [ROLE.TEACHER]: '教师',
      [ROLE.MARKER]: '阅卷人',
      [ROLE.STUDENT]: '学生'
    }[state.role] || '')
  },

  actions: {
    async login(userNo, password) {
      const data = await authApi.login(userNo, password)
      this.token = data.accessToken
      this.refreshToken = data.refreshToken
      this.userId = data.userId
      this.role = data.role
      localStorage.setItem('accessToken', data.accessToken)
      localStorage.setItem('refreshToken', data.refreshToken)
      localStorage.setItem('userId', String(data.userId))
      localStorage.setItem('role', data.role)
      return data
    },

    async logout() {
      try {
        await authApi.logout()
      } catch {
        // 登出失败也要清本地状态，否则用户被卡在登录态
      } finally {
        this.resetState()
      }
    },

    resetState() {
      this.token = ''
      this.refreshToken = ''
      this.userId = null
      this.role = ''
      this.name = ''
      localStorage.removeItem('accessToken')
      localStorage.removeItem('refreshToken')
      localStorage.removeItem('userId')
      localStorage.removeItem('role')
      localStorage.removeItem('name')
    }
  }
})
