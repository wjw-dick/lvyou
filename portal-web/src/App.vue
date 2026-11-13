<template>
  <div class="site">
    <header class="header">
      <div class="container header-inner">
        <router-link to="/" class="brand">
          <span class="brand-logo">旅</span>
          <span class="brand-name">{{ tenantName }}</span>
        </router-link>
        <nav class="nav">
          <router-link to="/">首页</router-link>
          <router-link to="/products">全部线路</router-link>
          <router-link to="/destinations">目的地攻略</router-link>
          <router-link to="/about">关于我们</router-link>
        </nav>
        <div class="actions">
          <router-link to="/order-query" class="action-link">订单查询</router-link>
          <router-link to="/distributor" class="action-link">分销商</router-link>
          <router-link to="/member" class="btn primary small">会员中心</router-link>
        </div>
      </div>
    </header>
    <main>
      <router-view />
    </main>
    <footer class="footer">
      <div class="container footer-inner">
        <div>
          <b>{{ tenantName }}</b>
          <p class="muted">让每一次旅行都值得回忆 · 旅游数字化平台 SaaS</p>
        </div>
        <div class="footer-links">
          <router-link to="/about">旅行社介绍</router-link>
          <router-link to="/order-query">订单验真</router-link>
          <a href="#privacy">隐私协议</a>
          <a href="#terms">用户协议</a>
        </div>
      </div>
      <div class="copyright">© 2026 {{ tenantName }} · 旅行社业务经营许可证号以门店公示为准</div>
    </footer>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { api } from './api'

const tenantName = ref('雪乡国旅')
onMounted(async () => {
  try {
    const t = await api.get('/public/tenant')
    if (t?.name) tenantName.value = t.name
  } catch (e) { /* ignore */ }
})
</script>

<style scoped>
.site { min-height: 100vh; display: flex; flex-direction: column; }
.header { background: #fff; border-bottom: 1px solid var(--border); position: sticky; top: 0; z-index: 20; }
.header-inner { display: flex; align-items: center; height: 64px; gap: 24px; }
.brand { display: flex; align-items: center; gap: 8px; font-weight: 700; white-space: nowrap; }
.brand-logo { width: 32px; height: 32px; border-radius: 8px; background: linear-gradient(135deg, #1f6feb, #38bdf8); color: #fff; display: grid; place-items: center; }
.brand-name { font-size: 16px; }
.nav { display: flex; gap: 22px; margin-left: 12px; }
.nav a { color: #606266; font-size: 14px; }
.nav a.router-link-active { color: var(--primary); font-weight: 600; }
.actions { margin-left: auto; display: flex; align-items: center; gap: 14px; }
.action-link { font-size: 13px; color: #606266; }
.btn.small { height: 34px; padding: 0 14px; font-size: 13px; }
main { flex: 1; }
.footer { background: #0f172a; color: #cbd5e1; padding: 32px 0 18px; margin-top: 40px; }
.footer-inner { display: flex; justify-content: space-between; gap: 20px; }
.footer .muted { color: #94a3b8; margin: 6px 0 0; }
.footer-links { display: flex; gap: 18px; font-size: 13px; flex-wrap: wrap; }
.copyright { text-align: center; color: #64748b; font-size: 12px; margin-top: 20px; padding-top: 14px; border-top: 1px solid #1e293b; }
@media (max-width: 760px) {
  .header-inner { height: auto; padding: 10px 12px; flex-wrap: wrap; }
  .nav { order: 3; width: 100%; gap: 14px; overflow-x: auto; }
  .actions { gap: 8px; }
  .action-link { display: none; }
}
</style>
