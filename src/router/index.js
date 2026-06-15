import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '@/api/authService'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { requiresGuest: true },
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue'),
    meta: { requiresGuest: true },
  },
  {
    path: '/home',
    name: 'Home',
    component: () => import('@/views/Home.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/chat',
    name: 'Chat',
    component: () => import('@/components/ChatWindow.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/statistics',
    name: 'Statistics',
    component: () => import('@/views/Statistics.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/teams',
    name: 'TeamList',
    component: () => import('@/views/TeamList.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/teams/create',
    name: 'TeamCreate',
    component: () => import('@/views/TeamCreate.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/user-profile',
    name: 'UserProfile',
    component: () => import('@/views/UserProfileDashboard.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/teams/:id',
    name: 'TeamDetail',
    component: () => import('@/views/TeamDetail.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/partners',
    name: 'PartnerMatching',
    component: () => import('@/views/PartnerMatching.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/chat-history',
    name: 'ChatHistory',
    component: () => import('@/views/ChatHistory.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/',
    redirect: (to) => {
      const token = getToken()
      return token ? '/home' : '/login'
    },
  },
  // 添加一个通配符路由用于处理未匹配的路径
  {
    path: '/:pathMatch(.*)*',
    redirect: '/home',
    name: 'NotFound'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to, from, next) => {
  const token = getToken()

  if (to.meta.requiresAuth && !token) {
    // 需要认证但未登录，重定向到登录页
    next('/login')
  } else if (to.meta.requiresGuest && token) {
    // 已登录用户访问登录/注册页，重定向到首页
    next('/home')
  } else {
    // 正常导航
    next()
  }
})

export default router