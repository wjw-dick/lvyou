<template>
  <div class="container section narrow">
    <h1 class="section-title">订单查询</h1>
    <p class="section-sub">凭订单号 + 手机尾号查询，仅展示摘要（隐私保护）</p>
    <div class="card form">
      <div class="field"><label>订单号</label><input v-model="form.orderNo" class="input" placeholder="如 HLJ-OD-2026-0001" /></div>
      <div class="field"><label>下单手机尾号</label><input v-model="form.phoneTail" class="input" placeholder="手机号后 4 位" maxlength="4" /></div>
      <button class="btn primary block" @click="query">查询</button>
      <p v-if="error" class="error">{{ error }}</p>
    </div>
    <div v-if="order" class="card result">
      <h3>{{ order.productTitle }}</h3>
      <div class="row"><span>订单号</span><b>{{ order.orderNo }}</b></div>
      <div class="row"><span>出团日期</span><span>{{ order.departDate }}</span></div>
      <div class="row"><span>联系人</span><span>{{ order.contactMask }}</span></div>
      <div class="row"><span>支付状态</span><span>{{ ['未支付', '已支付', '部分退款', '已退款'][order.payStatus] }}</span></div>
      <div class="row"><span>订单状态</span><span>{{ statusMap[order.status] }}</span></div>
      <div class="row"><span>核销状态</span><span>{{ order.verifyStatus === 1 ? '已核销' : '未核销' }}</span></div>
      <p class="muted">如需查看完整信息，请登录会员中心。</p>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { api } from '@/api'

const route = useRoute()
const statusMap = { 1: '待支付', 2: '待确认', 3: '已成团', 4: '待出团', 5: '已出团', 6: '已核销', 7: '已完结', 8: '已取消' }
const form = reactive({ orderNo: route.query.orderNo || '', phoneTail: '' })
const order = ref(null)
const error = ref('')

async function query() {
  error.value = ''
  order.value = null
  try {
    order.value = await api.post('/public/order-query', form)
  } catch (e) {
    error.value = e.message
  }
}
onMounted(() => { if (form.orderNo) query() })
</script>

<style scoped>
.narrow { max-width: 560px; }
.form { padding: 20px; }
.result { padding: 20px; margin-top: 16px; }
.result h3 { margin: 0 0 12px; }
.row { display: flex; justify-content: space-between; padding: 8px 0; border-top: 1px dashed var(--border); font-size: 14px; }
.error { color: #f56c6c; font-size: 13px; }
</style>
