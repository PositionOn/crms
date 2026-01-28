import { createRouter, createWebHistory } from 'vue-router'
import { setActivePinia } from 'pinia'
import { pinia } from '../store'
import { useAuthStore } from '../store/auth'

const Login = () => import('../pages/Login.vue')
const Layout = () => import('../layout/Layout.vue')

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: Login },
    {
      path: '/',
      component: Layout,
      children: [
        { path: '', redirect: '/cashier' },
        { path: 'cashier', component: () => import('../pages/cashier/Cashier.vue') },
        { path: 'ledger', component: () => import('../pages/ledger/Ledger.vue') },
        { path: 'stats', component: () => import('../pages/stats/Revenue.vue') },
        { path: 'config/operators', component: () => import('../pages/config/Operators.vue') },
        { path: 'config/tables', component: () => import('../pages/config/Tables.vue') },
        { path: 'config/categories', component: () => import('../pages/config/Categories.vue') },
        { path: 'config/dishes', component: () => import('../pages/config/Dishes.vue') },
        { path: 'config/discount-rules', component: () => import('../pages/config/DiscountRules.vue') }
      ]
    }
  ]
})

router.beforeEach((to) => {
  // 确保 Pinia 已激活（在守卫 / 非组件环境中）
  setActivePinia(pinia)
  const auth = useAuthStore()
  if (to.path === '/login') return true
  if (!auth.token) return '/login'
  return true
})

