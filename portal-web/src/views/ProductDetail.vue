<template>
  <div v-if="product" class="container section">
    <div class="gallery">
      <div class="main-img" :style="{ backgroundImage: cover ? `url(${cover})` : '' }">
        <span v-if="!cover">{{ product.title.slice(0, 2) }}</span>
      </div>
      <div class="thumbs">
        <div v-for="(img, i) in images" :key="i" class="thumb" :class="{ active: img === cover }"
             :style="{ backgroundImage: `url(${img})` }" @click="cover = img" />
      </div>
    </div>

    <div class="detail-grid">
      <div>
        <div class="card head-card">
          <div class="flex">
            <h1>{{ product.title }}</h1>
            <span class="tag">{{ product.days }} 天</span>
          </div>
          <p class="muted">{{ product.subtitle }}</p>
          <div class="price-line">
            <span class="price big">¥{{ product.adultPrice }}</span>
            <span class="muted">起/人 · 儿童 ¥{{ product.childPrice }} · 单房差 ¥{{ product.singleRoomDiff }}</span>
          </div>
          <div class="chips">
            <span>{{ product.departure }} 出发</span>
            <span>{{ product.destination }}</span>
            <span>集合：{{ product.meetingPoint }}</span>
            <span>余位 {{ Math.max(0, product.stock - product.sold) }}</span>
          </div>
        </div>

        <div class="card block">
          <h2 class="block-title">行程安排</h2>
          <div v-for="it in itineraries" :key="it.id" class="day">
            <div class="day-badge">D{{ it.dayNo }}</div>
            <div class="day-body">
              <b>{{ it.title }}</b>
              <div class="muted">{{ it.spots }} · {{ it.hotel || '住宿另定' }} · 用餐：{{ it.meals || '-' }}</div>
              <p v-if="it.content">{{ it.content }}</p>
            </div>
          </div>
          <div v-if="!itineraries.length" class="empty">行程待完善</div>
        </div>

        <div class="card block two-col">
          <div>
            <h2 class="block-title">费用包含</h2>
            <p class="pre">{{ product.includeText || '-' }}</p>
          </div>
          <div>
            <h2 class="block-title">费用不含</h2>
            <p class="pre">{{ product.excludeText || '-' }}</p>
          </div>
        </div>

        <div class="card block">
          <h2 class="block-title">退改规则</h2>
          <p class="pre">{{ product.refundRule || '按合同约定执行' }}</p>
        </div>

        <div v-if="product.guideContent" class="card block">
          <h2 class="block-title">图文详情</h2>
          <div class="rich" v-html="product.guideContent" />
        </div>
      </div>

      <aside>
        <div class="card booking">
          <h3>在线预订</h3>
          <div class="field">
            <label>出团日期</label>
            <select v-model="form.scheduleId" class="input">
              <option :value="null">请选择团期</option>
              <option v-for="s in schedules" :key="s.id" :value="s.id" :disabled="s.left <= 0">
                {{ s.departDate }} · 余位 {{ s.left }}
              </option>
            </select>
          </div>
          <div class="two">
            <div class="field">
              <label>成人</label>
              <input v-model.number="form.adultCount" type="number" min="1" class="input" />
            </div>
            <div class="field">
              <label>儿童</label>
              <input v-model.number="form.childCount" type="number" min="0" class="input" />
            </div>
          </div>
          <div class="field">
            <label>联系人</label>
            <input v-model="form.contactName" class="input" placeholder="请输入姓名" />
          </div>
          <div class="field">
            <label>手机号</label>
            <input v-model="form.contactPhone" class="input" placeholder="用于接收藏出团通知" />
          </div>
          <div class="field">
            <label>游客实名信息</label>
            <div v-for="(t, i) in form.tourists" :key="i" class="tourist">
              <input v-model="t.name" class="input" placeholder="姓名" />
              <select v-model="t.idType" class="input">
                <option value="id_card">身份证</option>
                <option value="passport">护照</option>
              </select>
              <input v-model="t.idNo" class="input" placeholder="证件号（加密存储）" />
            </div>
            <button class="btn small-btn" @click="addTourist">+ 添加游客</button>
          </div>
          <div class="sum">
            <span>合计</span>
            <b class="price">¥{{ total }}</b>
          </div>
          <button class="btn primary block large" :disabled="submitting" @click="submit">
            {{ submitting ? '提交中...' : '提交预订' }}
          </button>
          <p class="muted small">提交即表示同意《旅游合同》与实名制出行要求</p>
        </div>

        <div class="card tips">
          <b>出行保障</b>
          <p>· 实名制下单，证件 AES 加密存储</p>
          <p>· 电子合同在线签署，支持验真</p>
          <p>· 余位原子扣减，杜绝超售</p>
        </div>
      </aside>
    </div>

    <div v-if="success" class="mask" @click.self="success = false">
      <div class="success card">
        <h2>预订成功</h2>
        <p>订单号：<b>{{ success.orderNo }}</b></p>
        <p class="muted">请保存订单号，可在「订单查询」查看进度</p>
        <button class="btn primary" @click="payMock">模拟微信支付</button>
        <button class="btn" @click="goQuery">去查询订单</button>
      </div>
    </div>
  </div>
  <div v-else class="container section empty">加载中...</div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '@/api'

const route = useRoute()
const router = useRouter()
const product = ref(null)
const itineraries = ref([])
const schedules = ref([])
const cover = ref('')
const success = ref(null)
const submitting = ref(false)
const form = reactive({
  scheduleId: null, adultCount: 1, childCount: 0, contactName: '', contactPhone: '', tourists: []
})

const images = computed(() => (product.value?.images || '').split(',').filter(Boolean))
const total = computed(() => {
  if (!product.value) return '0.00'
  return (Number(product.value.adultPrice) * (form.adultCount || 0) + Number(product.value.childPrice) * (form.childCount || 0)).toFixed(2)
})

function addTourist(adult = true) {
  form.tourists.push({ name: '', idType: 'id_card', idNo: '', phone: form.contactPhone, isAdult: adult ? 1 : 0 })
}
function syncTourists() {
  const need = (form.adultCount || 0) + (form.childCount || 0)
  while (form.tourists.length < need) addTourist(form.tourists.length < (form.adultCount || 0))
  while (form.tourists.length > need) form.tourists.pop()
}
watch(() => [form.adultCount, form.childCount], syncTourists)

async function submit() {
  if (!form.scheduleId) return alert('请选择团期')
  if (!form.contactName || !form.contactPhone) return alert('请填写联系人姓名和手机号')
  submitting.value = true
  try {
    const order = await api.post('/public/orders', {
      productId: product.value.id,
      scheduleId: form.scheduleId,
      adultCount: form.adultCount,
      childCount: form.childCount,
      contactName: form.contactName,
      contactPhone: form.contactPhone,
      tourists: form.tourists.filter((t) => t.name),
      channel: 'h5'
    })
    success.value = order
  } catch (e) {
    alert(e.message)
  } finally {
    submitting.value = false
  }
}
async function payMock() {
  await api.get(`/pay/mock/${success.value.orderNo}`)
  alert('支付成功（模拟微信支付）')
  success.value = false
  goQuery()
}
function goQuery() {
  router.push({ path: '/order-query', query: { orderNo: success.value?.orderNo } })
}

onMounted(async () => {
  const data = await api.get(`/public/products/${route.params.id}`)
  product.value = data.product
  itineraries.value = data.itineraries || []
  cover.value = product.value.cover || ''
  schedules.value = (await api.get(`/public/products/${route.params.id}/schedules`)) || []
  if (schedules.value.length) form.scheduleId = schedules.value[0].id
  syncTourists()
})
</script>

<style scoped>
.gallery { margin-bottom: 18px; }
.main-img { height: 320px; border-radius: 14px; background: linear-gradient(135deg, #dbeafe, #93c5fd); background-size: cover; background-position: center; display: grid; place-items: center; font-size: 56px; color: #1f6feb; }
.thumbs { display: flex; gap: 8px; margin-top: 8px; }
.thumb { width: 74px; height: 54px; border-radius: 6px; background-size: cover; background-position: center; cursor: pointer; border: 2px solid transparent; background-color: #e5edff; }
.thumb.active { border-color: var(--primary); }
.detail-grid { display: grid; grid-template-columns: 1fr 340px; gap: 20px; align-items: start; }
.head-card { padding: 20px; }
.head-card h1 { font-size: 26px; margin: 0; }
.flex { display: flex; align-items: center; gap: 10px; }
.price-line { margin: 12px 0; display: flex; align-items: baseline; gap: 10px; flex-wrap: wrap; }
.price.big { font-size: 30px; }
.chips { display: flex; gap: 8px; flex-wrap: wrap; }
.chips span { background: #f5f7fa; color: #606266; border-radius: 999px; padding: 4px 12px; font-size: 12px; }
.block { padding: 20px; margin-top: 16px; }
.block-title { font-size: 18px; margin: 0 0 14px; }
.day { display: flex; gap: 12px; padding: 12px 0; border-top: 1px dashed var(--border); }
.day-badge { width: 46px; height: 30px; background: var(--primary-light); color: var(--primary); border-radius: 6px; display: grid; place-items: center; font-weight: 700; flex-shrink: 0; }
.day-body p { margin: 6px 0 0; color: #606266; font-size: 13px; }
.two-col { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }
.pre { white-space: pre-wrap; color: #606266; font-size: 14px; margin: 0; }
.rich :deep(img) { max-width: 100%; border-radius: 8px; }
.rich :deep(p) { color: #606266; }
.booking { padding: 18px; position: sticky; top: 80px; }
.booking h3 { margin: 0 0 14px; }
.two { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
.tourist { display: grid; grid-template-columns: 0.8fr 0.9fr 1.3fr; gap: 6px; margin-bottom: 6px; }
.small-btn { height: 30px; padding: 0 12px; font-size: 12px; margin-top: 4px; }
.sum { display: flex; justify-content: space-between; align-items: center; border-top: 1px dashed var(--border); padding: 12px 0; margin-top: 8px; }
.sum b { font-size: 22px; }
.small { font-size: 11px; text-align: center; }
.tips { padding: 18px; margin-top: 16px; }
.tips p { color: var(--muted); font-size: 12px; margin: 4px 0; }
.mask { position: fixed; inset: 0; background: #0f172a66; display: grid; place-items: center; z-index: 50; }
.success { padding: 30px; text-align: center; width: 340px; display: flex; flex-direction: column; gap: 10px; }
.success h2 { margin: 0; }
@media (max-width: 900px) {
  .detail-grid { grid-template-columns: 1fr; }
  .booking { position: static; }
  .main-img { height: 220px; }
  .two-col { grid-template-columns: 1fr; }
}
</style>
