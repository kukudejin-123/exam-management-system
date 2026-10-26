import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

// 统一响应约定：code === 0 表示成功，其余为业务错误码
const SUCCESS = 0

const service = axios.create({
  baseURL: '/api',
  timeout: 15000
})

// 请求拦截：带上 JWT
service.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('accessToken')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截：把后端的 Result 外壳拆开，只把 data 交给页面
service.interceptors.response.use(
  (response) => {
    const body = response.data
    // 非 JSON（如文件下载）直接透传
    if (body === null || typeof body !== 'object' || !('code' in body)) {
      return body
    }
    if (body.code === SUCCESS) {
      return body.data
    }
    // 业务失败：统一提示，页面不需要每个 catch 都弹一次
    ElMessage.error(body.message || '操作失败')
    if (body.code >= 2001 && body.code <= 2005) {
      // 登录态失效：清干净并回登录页
      localStorage.removeItem('accessToken')
      localStorage.removeItem('refreshToken')
      router.push('/login')
    }
    return Promise.reject(new Error(body.message || '操作失败'))
  },
  (error) => {
    const status = error.response?.status
    if (status === 401) {
      localStorage.removeItem('accessToken')
      localStorage.removeItem('refreshToken')
      router.push('/login')
      ElMessage.error('未登录或登录已过期')
    } else if (status === 403) {
      ElMessage.error('没有权限执行该操作')
    } else if (status === 404) {
      ElMessage.error(error.response?.data?.message || '资源不存在')
    } else {
      ElMessage.error('网络异常，请稍后重试')
    }
    return Promise.reject(error)
  }
)

export default service
