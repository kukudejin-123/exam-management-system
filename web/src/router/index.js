import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

// 路由按角色分流：教师/管理员走管理后台，学生走答题与查分
const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录', public: true }
  },
  {
    path: '/',
    component: () => import('@/layouts/DefaultLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '工作台' }
      },
      {
        path: 'classes',
        name: 'Classes',
        component: () => import('@/views/Classes.vue'),
        meta: { title: '班级管理' }
      },
      {
        path: 'questions',
        name: 'Questions',
        component: () => import('@/views/Questions.vue'),
        meta: { title: '题库管理' }
      },
      {
        path: 'papers',
        name: 'Papers',
        component: () => import('@/views/Papers.vue'),
        meta: { title: '试卷管理' }
      },
      {
        path: 'exams',
        name: 'Exams',
        component: () => import('@/views/Exams.vue'),
        meta: { title: '考试发布' }
      },
      {
        path: 'grade-book',
        name: 'GradeBook',
        component: () => import('@/views/GradeBook.vue'),
        meta: { title: '成绩册' }
      },
      {
        path: 'my-scores',
        name: 'MyScores',
        component: () => import('@/views/MyScores.vue'),
        meta: { title: '我的成绩' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFound.vue'),
    meta: { title: '页面不存在', public: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫：未登录一律去登录页
router.beforeEach((to) => {
  const user = useUserStore()
  if (!user.isLoggedIn && !to.meta.public) {
    return { path: '/login', query: to.fullPath === '/' ? {} : { redirect: to.fullPath } }
  }
  if (user.isLoggedIn && to.path === '/login') {
    return { path: '/' }
  }
  document.title = to.meta.title
    ? `${to.meta.title} · 考试管理系统`
    : '考试管理系统'
  return true
})

export default router
