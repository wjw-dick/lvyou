<template>
  <div class="container section">
    <div v-if="!token" class="card login">
      <h2>分销商中心</h2>
      <p class="muted">查询实时报价、余位，在线下单并查看佣金结算。</p>
      <div class="field"><label>租户编码</label><input v-model="form.tenantCode" class="input" /></div>
      <div class="field"><label>账号</label><input v-model="form.username" class="input" /></div>
      <div class="field"><label>密码</label><input v-model="form.password" type="password" class="input" /></div>
      <button class="btn primary block" @click="login">登录</button>
      <p class="muted">演示：md001 / admin123</p>
    </div>

    <template v-else>
      <div class="page-head">
        <h1 class="section-title">分销商工作台</h1>
        <button class="btn" @click="logout">退出</button>
      </div>
      <div class="stats">
        <div class="card"><span class="muted">累计佣金</span><b>¥{{ Number(commission.total || 0).toFixed(2) }}</b></div>
        <div class="card"><span class="muted">已结算</span><b>¥{{ Number(commission.settled || 0).toFixed(2) }}</b></div>
        <div class="card"><span class="muted">待入账</span><b>¥{{ Number(commission.pending || 0).toFixed(2) }}</b></div>
        <div class="card"><span class="muted">分销订单</span><b>{{ orders.length }}</b></div>
      </div>

      <div class="tabs">
        <button :class="{ active: tab === 'products' }" @click="tab = 'products'">实时报价</button>
        <button :class="{ active: tab === 'orders' }" @click="tab = 'orders'">分销订单</button>
        <button :class="{ active: tab === 'commission' }" @click="tab = 'commission'">佣金明细</button>
      </div>

      <div v-if="tab === 'products'" class="grid prod-grid">
        <div v-for="p in products" :key="p.id" class="card prod">
          <b>{{ p.title }}</b>
          <div class="muted">{{ p.days }}天 · {{ p.destination }} · 余位 {{ p.stock }}</div>
          <div class="row"><span>分销价</span><b class="price">¥{{ p.adultPrice }}</b></div>
          <div class="row"><span>儿童价</span><span>¥{{ p.childPrice }}</span></div>
          <router-link :to="`/products/${p.id}`" class="btn primary block">去下单</router-link>
        </div>
        <div v-if="!products.length" class="empty">暂无报价，请联系旅行社配置</div>
      </div>

      <div v-if="tab === 'orders'">
        <div v-for="o in orders" :key="o.id" class="card order">
          <div class="flex"><b>{{ o.productTitle }}</b><span class="tag">{{ o.orderNo }}</span></div>
          <div class="muted">{{ o.departDate }} · {{ o.adultCount }}大{{ o.childCount }}小 · ¥{{ o.totalAmount }}</div>
        </div>
        <div v-if="!orders.length" class="empty">暂无分销订单</div>
      </div>

      <div v-if="tab === 'commission'">
        <div v-for="c in commission.records || []" :key="c.id" class="card order">
          <div class="flex"><b>订单 #{{ c.orderId }}</b><b class="price">¥{{ Number(c.amount || 0).toFixed(2) }}</b></div>
          <div class="muted">比例 {{ (Number(c.rate) * 100).toFixed(1) }}% · {{ c.status === 1 ? '已入账' : '待入账' }}</div>
        </div>
        <div v-if="!(commission.records || []).length" class="empty">暂无佣金记录</div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { api } from '@/api'

const token = ref(localStorage.getItem('tp_portal_token'))
const tab = ref('products')
const form = reactive({ tenantCode: 'HLJ', username: 'md001', password: 'admin123' })
const products = ref([])
const orders = ref([])
const commission = ref({})

async function login() {
  const r = await api.post('/auth/distributor-login', form)
  localStorage.setItem('tp_portal_token', r.token)
  token.value = r.token
  load()
}
async function load() {
  if (!token.value) return
  try {
    products.value = (await api.get('/distributor/products')) || []
    orders.value = (await api.get('/distributor/orders')) || []
    commission.value = (await api.get('/distributor/commission')) || {}
  } catch (e) { /* ignore */ }
}
function logout() {
  localStorage.removeItem('tp_portal_token')
  token.value = ''
  location.reload()
}
onMounted(load)
</script>

<style scoped>
.login { padding: 28px; max-width: 420px; margin: 0 auto; }
.page-head { display: flex; justify-content: space-between; align-items: center; }
.stats { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin-bottom: 16px; }
.stats .card { padding: 16px; display: flex; flex-direction: column; gap: 6px; }
.stats b { font-size: 22px; }
.tabs { display: flex; gap: 8px; margin-bottom: 14px; }
.tabs button { padding: 8px 18px; border-radius: 8px; border: 1px solid var(--border); background: #fff; cursor: pointer; }
.tabs button.active { background: var(--primary); color: #fff; border-color: var(--primary); }
.prod-grid { grid-template-columns: repeat(auto-fill, minmax(260px, 1fr)); }
.prod { padding: 16px; display: flex; flex-direction: column; gap: 8px; }
.row { display: flex; justify-content: space-between; font-size: 13px; }
.order { padding: 14px; margin-bottom: 10px; }
.flex { display: flex; justify-content: space-between; align-items: center; }
@media (max-width: 720px) { .stats { grid-template-columns: 1fr 1fr; } }
</style>
