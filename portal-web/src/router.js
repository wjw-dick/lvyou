import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/', component: () => import('./views/Home.vue'), meta: { title: '雪乡国旅 · 首页' } },
  { path: '/products', component: () => import('./views/Products.vue'), meta: { title: '全部线路' } },
  { path: '/products/:id', component: () => import('./views/ProductDetail.vue'), meta: { title: '线路详情' } },
  { path: '/destinations', component: () => import('./views/Destinations.vue'), meta: { title: '目的地攻略' } },
  { path: '/about', component: () => import('./views/About.vue'), meta: { title: '关于我们' } },
  { path: '/order-query', component: () => import('./views/OrderQuery.vue'), meta: { title: '订单查询' } },
  { path: '/login', component: () => import('./views/Login.vue'), meta: { title: '登录' } },
  { path: '/member', component: () => import('./views/Member.vue'), meta: { title: '会员中心' } },
  { path: '/distributor', component: () => import('./views/Distributor.vue'), meta: { title: '分销商中心' } },
  { path: '/share/:type/:id', component: () => import('./views/Share.vue'), meta: { title: '分享查询' } }
]

const router = createRouter({ history: createWebHistory(), routes })

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} · 雪乡国旅` : '雪乡国旅'
})

export default router
