<template>
  <div class="container section">
    <div v-if="!token" class="card login-tip">
      <h2>请先登录</h2>
      <p class="muted">UnionID 登录后可查看本人订单、合同、核销凭证与会员权益。</p>
      <router-link to="/login" class="btn primary">去登录</router-link>
    </div>

    <template v-else>
      <div class="member-head card">
        <div class="avatar">{{ (profile.member?.nickname || '游').slice(0, 1) }}</div>
        <div class="info">
          <h2>{{ profile.member?.nickname }}</h2>
          <p class="muted">UnionID：{{ profile.member?.unionId }}</p>
          <div class="chips">
            <span>L{{ profile.member?.level }} 会员</span>
            <span>积分 {{ profile.member?.points || 0 }}</span>
            <span>累计消费 ¥{{ Number(profile.member?.totalConsume || 0).toFixed(2) }}</span>
          </div>
        </div>
        <button class="btn" @click="logout">退出登录</button>
      </div>

      <div class="tabs">
        <button v-for="t in tabs" :key="t.key" :class="{ active: tab === t.key }" @click="tab = t.key">{{ t.label }}</button>
      </div>

      <div v-if="tab === 'orders'">
        <div v-for="o in orders" :key="o.id" class="card order">
          <div class="flex">
            <b>{{ o.productTitle }}</b>
            <span class="tag">{{ statusMap[o.status] }}</span>
          </div>
          <div class="muted">{{ o.orderNo }} · {{ o.departDate }} · {{ o.adultCount }}大{{ o.childCount }}小</div>
          <div class="flex end">
            <span class="price">¥{{ Number(o.paidAmount || o.totalAmount || 0).toFixed(2) }}</span>
            <button v-if="o.payStatus === 0" class="btn primary small" @click="pay(o)">去支付</button>
            <button v-if="o.payStatus === 1" class="btn small" @click="applyRefund(o)">申请退改</button>
          </div>
        </div>
        <div v-if="!orders.length" class="empty">暂无订单</div>
      </div>

      <div v-if="tab === 'coupons'" class="coupon-grid">
        <div v-for="c in coupons" :key="c.id" class="card coupon" :class="{ used: c.status !== 0 }">
          <b>{{ c.name }}</b>
          <span class="amount">¥{{ c.amount }}</span>
          <span class="muted">满 ¥{{ c.minAmount }} 可用 · {{ c.status === 0 ? '未使用' : '已使用' }}</span>
        </div>
        <div v-if="!coupons.length" class="empty">暂无优惠券</div>
      </div>

      <div v-if="tab === 'messages'">
        <div v-for="m in messages" :key="m.id" class="card msg">
          <div class="flex"><b>{{ bizMap[m.bizType] || m.bizType }}</b><span class="muted">{{ m.createdAt }}</span></div>
          <p class="muted">{{ m.content }}</p>
        </div>
        <div v-if="!messages.length" class="empty">暂无消息</div>
      </div>

      <div v-if="tab === 'profile'">
        <div class="card profile">
          <h3>会员权益</h3>
          <p class="muted">积分可在积分商城兑换门票/特产/优惠券；等级越高权益越多。</p>
          <div class="rights">
            <div><b>L1</b><span>注册会员</span></div>
            <div><b>L2</b><span>消费满 2000</span></div>
            <div><b>L3</b><span>消费满 10000</span></div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { api } from '@/api'

const token = ref(localStorage.getItem('tp_portal_token'))
const tab = ref('orders')
const tabs = [
  { key: 'orders', label: '我的订单' },
  { key: 'coupons', label: '优惠券' },
  { key: 'messages', label: '消息中心' },
  { key: 'profile', label: '会员权益' }
]
const statusMap = { 1: '待支付', 2: '待确认', 3: '已成团', 4: '待出团', 5: '已出团', 6: '已核销', 7: '已完结', 8: '已取消' }
const bizMap = { booking: '报名确认', group: '成团通知', depart: '出团通知', settle: '结算到账', refund: '退款通知', marketing: '优惠活动', contract: '合同签署' }
const profile = ref({})
const orders = ref([])
const coupons = ref([])
const messages = ref([])

async function load() {
  if (!token.value) return
  try {
    profile.value = (await api.get('/member/profile')) || {}
    orders.value = (await api.get('/member/orders')) || []
    coupons.value = (await api.get('/member/coupons')) || []
    messages.value = (await api.get('/member/messages')) || []
  } catch (e) { /* token expired */ }
}
async function pay(order) {
  await api.post(`/member/orders/${order.id}/pay`)
  alert('支付成功（模拟微信支付）')
  load()
}
async function applyRefund(order) {
  const reason = prompt('请输入退改原因')
  if (reason === null) return
  await api.post(`/member/orders/${order.id}/refund`, { reason })
  alert('退改申请已提交，等待审批')
}
function logout() {
  localStorage.removeItem('tp_portal_token')
  localStorage.removeItem('tp_portal_user')
  token.value = ''
  location.reload()
}
onMounted(load)
</script>

<style scoped>
.login-tip { padding: 40px; text-align: center; }
.member-head { display: flex; align-items: center; gap: 16px; padding: 20px; }
.avatar { width: 62px; height: 62px; border-radius: 50%; background: linear-gradient(135deg, #1f6feb, #38bdf8); color: #fff; display: grid; place-items: center; font-size: 24px; }
.info { flex: 1; }
.info h2 { margin: 0 0 2px; }
.info p { margin: 0 0 6px; }
.chips { display: flex; gap: 6px; flex-wrap: wrap; }
.chips span { background: #f5f7fa; border-radius: 999px; padding: 2px 10px; font-size: 12px; color: #606266; }
.tabs { display: flex; gap: 8px; margin: 16px 0; }
.tabs button { padding: 8px 18px; border-radius: 8px; border: 1px solid var(--border); background: #fff; cursor: pointer; }
.tabs button.active { background: var(--primary); color: #fff; border-color: var(--primary); }
.order, .msg, .profile { padding: 16px; margin-bottom: 12px; }
.flex { display: flex; justify-content: space-between; align-items: center; gap: 10px; }
.flex.end { margin-top: 10px; justify-content: flex-end; }
.btn.small { height: 32px; padding: 0 12px; font-size: 12px; }
.coupon-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 12px; }
.coupon { padding: 16px; display: flex; flex-direction: column; gap: 4px; border-left: 4px solid var(--primary); }
.coupon.used { opacity: 0.5; border-left-color: #c0c4cc; }
.coupon .amount { font-size: 24px; color: #f56c6c; font-weight: 700; }
.rights { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; margin-top: 14px; }
.rights div { background: #fafcff; border: 1px solid var(--border); border-radius: 8px; padding: 14px; display: flex; flex-direction: column; }
.rights b { font-size: 20px; color: var(--primary); }
.rights span { font-size: 12px; color: var(--muted); }
@media (max-width: 640px) { .member-head { flex-wrap: wrap; } .rights { grid-template-columns: 1fr; } }
</style>
