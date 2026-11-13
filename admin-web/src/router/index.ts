import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes: RouteRecordRaw[] = [
  { path: '/login', name: 'Login', component: () => import('@/views/login/index.vue'), meta: { public: true } },
  {
    path: '/',
    component: () => import('@/layout/index.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', component: () => import('@/views/dashboard/index.vue') },
      { path: 'product/list', component: () => import('@/views/product/list.vue') },
      { path: 'product/edit/:id', component: () => import('@/views/product/edit.vue') },
      { path: 'product/destination', component: () => import('@/views/product/destination.vue') },
      { path: 'schedule/calendar', component: () => import('@/views/schedule/calendar.vue') },
      { path: 'schedule/list', component: () => import('@/views/schedule/list.vue') },
      { path: 'order/list', component: () => import('@/views/order/list.vue') },
      { path: 'order/detail/:id', component: () => import('@/views/order/detail.vue') },
      { path: 'guide/list', component: () => import('@/views/guide/list.vue') },
      { path: 'guide/assignment', component: () => import('@/views/guide/assignment.vue') },
      { path: 'guide/expense', component: () => import('@/views/guide/expense.vue') },
      { path: 'resource/supplier', component: () => import('@/views/resource/supplier.vue') },
      { path: 'resource/purchase', component: () => import('@/views/resource/purchase.vue') },
      { path: 'finance/settlement', component: () => import('@/views/finance/settlement.vue') },
      { path: 'finance/invoice', component: () => import('@/views/finance/invoice.vue') },
      { path: 'finance/split', component: () => import('@/views/finance/split.vue') },
      { path: 'distribution/list', component: () => import('@/views/distribution/list.vue') },
      { path: 'distribution/price', component: () => import('@/views/distribution/price.vue') },
      { path: 'distribution/commission', component: () => import('@/views/distribution/commission.vue') },
      { path: 'contract/list', component: () => import('@/views/contract/list.vue') },
      { path: 'contract/template', component: () => import('@/views/contract/template.vue') },
      { path: 'member/list', component: () => import('@/views/member/list.vue') },
      { path: 'member/coupon', component: () => import('@/views/member/coupon.vue') },
      { path: 'member/activity', component: () => import('@/views/member/activity.vue') },
      { path: 'member/points', component: () => import('@/views/member/points.vue') },
      { path: 'scenic/list', component: () => import('@/views/scenic/list.vue') },
      { path: 'scenic/ticket', component: () => import('@/views/scenic/ticket.vue') },
      { path: 'scenic/stock', component: () => import('@/views/scenic/stock.vue') },
      { path: 'scenic/traffic', component: () => import('@/views/scenic/traffic.vue') },
      { path: 'scenic/verify', component: () => import('@/views/scenic/verify.vue') },
      { path: 'content/ugc', component: () => import('@/views/content/ugc.vue') },
      { path: 'content/custom', component: () => import('@/views/content/custom.vue') },
      { path: 'content/live', component: () => import('@/views/content/live.vue') },
      { path: 'report/business', component: () => import('@/views/report/business.vue') },
      { path: 'report/cockpit', component: () => import('@/views/report/cockpit.vue') },
      { path: 'system/user', component: () => import('@/views/system/user.vue') },
      { path: 'system/role', component: () => import('@/views/system/role.vue') },
      { path: 'system/dict', component: () => import('@/views/system/dict.vue') },
      { path: 'system/config', component: () => import('@/views/system/config.vue') },
      { path: 'system/storage', component: () => import('@/views/system/storage.vue') },
      { path: 'system/wechat', component: () => import('@/views/system/wechat.vue') },
      { path: 'system/message', component: () => import('@/views/system/message.vue') },
      { path: 'system/nav', component: () => import('@/views/system/nav.vue') },
      { path: 'system/audit', component: () => import('@/views/system/audit.vue') },
      { path: 'insurance/list', component: () => import('@/views/insurance/list.vue') },
      { path: 'insurance/fund', component: () => import('@/views/insurance/fund.vue') },
      { path: 'platform/dashboard', component: () => import('@/views/platform/dashboard.vue') },
      { path: 'platform/tenant', component: () => import('@/views/platform/tenant.vue') },
      { path: 'platform/dict', component: () => import('@/views/platform/dict.vue') },
      { path: 'platform/param', component: () => import('@/views/platform/param.vue') },
      { path: 'platform/notice', component: () => import('@/views/platform/notice.vue') },
      { path: 'platform/audit', component: () => import('@/views/platform/audit.vue') },
      { path: 'platform/user', component: () => import('@/views/platform/user.vue') }
    ]
  },
  { path: '/:pathMatch(.*)*', component: () => import('@/views/error/404.vue') }
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach((to) => {
  const store = useAuthStore()
  if (!to.meta.public && !store.token) return '/login'
  if (to.path === '/login' && store.token) return '/'
  return true
})

export default router
