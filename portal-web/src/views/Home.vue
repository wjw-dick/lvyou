<template>
  <div>
    <section class="hero">
      <div class="container hero-inner">
        <div class="hero-copy">
          <span class="tag">冰雪季 · 雪乡 / 哈尔滨</span>
          <h1>去雪乡，赴一场童话之约</h1>
          <p>纯玩无购物 · 实名制出行 · 电子合同保障 · 余位实时同步</p>
          <div class="search">
            <input v-model="keyword" class="input" placeholder="搜索线路 / 目的地，如 雪乡" @keyup.enter="search" />
            <button class="btn primary" @click="search">搜索</button>
          </div>
          <div class="hero-stats">
            <div><b>{{ stats.productCount }}</b><span>在售线路</span></div>
            <div><b>{{ stats.orderCount }}</b><span>累计服务</span></div>
            <div><b>{{ stats.memberCount }}</b><span>会员同行</span></div>
          </div>
        </div>
        <div class="hero-art">
          <div class="snow1" /><div class="snow2" /><div class="snow3" />
          <div class="art-card">
            <div class="art-title">雪乡童话世界 2 日游</div>
            <div class="art-price">¥899 <small>/人起</small></div>
            <div class="art-tags"><span>纯玩</span><span>含门票</span><span>哈尔滨出发</span></div>
          </div>
        </div>
      </div>
    </section>

    <section class="section container">
      <h2 class="section-title">热门目的地</h2>
      <p class="section-sub">从冰雪世界到百年老街，选一个心动的地方</p>
      <div class="dest-grid">
        <router-link v-for="d in destinations.slice(0, 6)" :key="d.id" :to="`/products?destination=${d.name}`" class="dest-card">
          <div class="dest-cover" :style="{ backgroundImage: d.cover ? `url(${d.cover})` : '' }">
            <span v-if="!d.cover">{{ d.name.slice(0, 1) }}</span>
          </div>
          <div class="dest-info">
            <b>{{ d.name }}</b>
            <span class="muted">{{ d.summary || d.city }}</span>
          </div>
        </router-link>
      </div>
    </section>

    <section class="section container">
      <div class="flex-between">
        <div>
          <h2 class="section-title">热门线路</h2>
          <p class="section-sub">实时余位 · 在线预订 · 电子合同</p>
        </div>
        <router-link to="/products" class="btn">查看全部</router-link>
      </div>
      <div class="product-grid">
        <router-link v-for="p in products" :key="p.id" :to="`/products/${p.id}`" class="product-card card">
          <div class="product-cover" :style="{ backgroundImage: p.cover ? `url(${p.cover})` : '' }">
            <span v-if="!p.cover">{{ p.title.slice(0, 2) }}</span>
            <span class="days">{{ p.days }}天</span>
          </div>
          <div class="product-body">
            <h3>{{ p.title }}</h3>
            <p class="muted">{{ p.subtitle || p.destination }}</p>
            <div class="product-foot">
              <span class="price">¥{{ p.adultPrice }} <small>起/人</small></span>
              <span class="tag">余位 {{ Math.max(0, p.stock - p.sold) }}</span>
            </div>
          </div>
        </router-link>
      </div>
      <div v-if="!products.length" class="empty">暂无在售线路</div>
    </section>

    <section class="section container">
      <div class="notice card">
        <div class="notice-head">
          <h2 class="section-title">公告与资讯</h2>
          <router-link to="/destinations" class="muted">更多攻略 →</router-link>
        </div>
        <div v-for="n in notices" :key="n.id" class="notice-row">
          <span class="tag">公告</span>
          <span class="notice-title">{{ n.title }}</span>
          <span class="muted">{{ (n.content || '').replace(/<[^>]+>/g, '').slice(0, 60) }}</span>
        </div>
        <div v-if="!notices.length" class="empty">暂无公告</div>
      </div>
    </section>

    <section class="features container">
      <div class="feature"><b>实名制出行</b><span>证件加密存储、展示脱敏</span></div>
      <div class="feature"><b>电子合同</b><span>在线签署、CA 存证、可验真</span></div>
      <div class="feature"><b>余位实时</b><span>原子扣减防超售</span></div>
      <div class="feature"><b>多端互通</b><span>小程序 / 公众号 / H5 / App</span></div>
    </section>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '@/api'

const router = useRouter()
const keyword = ref('')
const products = ref([])
const destinations = ref([])
const notices = ref([])
const stats = reactive({ productCount: 0, orderCount: 0, memberCount: 0 })

function search() {
  router.push({ path: '/products', query: { keyword: keyword.value } })
}

onMounted(async () => {
  try {
    const [p, d, n] = await Promise.all([
      api.get('/public/products', { page: 1, size: 6 }),
      api.get('/public/destinations'),
      api.get('/public/notices')
    ])
    products.value = p.records || []
    destinations.value = d || []
    notices.value = n || []
    stats.productCount = p.total || products.value.length
    stats.orderCount = 1286
    stats.memberCount = 3280
  } catch (e) { /* ignore */ }
})
</script>

<style scoped>
.hero { background: radial-gradient(circle at 80% 20%, #1f6feb33, transparent 45%), linear-gradient(135deg, #0f172a, #1e3a8a); color: #fff; padding: 60px 0 70px; }
.hero-inner { display: grid; grid-template-columns: 1.2fr 0.8fr; gap: 30px; align-items: center; }
.hero-copy h1 { font-size: 40px; line-height: 1.2; margin: 14px 0 10px; }
.hero-copy p { color: #c7d2fe; margin: 0 0 20px; }
.search { display: flex; gap: 10px; max-width: 460px; }
.search .input { height: 48px; border: none; }
.hero-stats { display: flex; gap: 36px; margin-top: 28px; }
.hero-stats div { display: flex; flex-direction: column; }
.hero-stats b { font-size: 24px; }
.hero-stats span { color: #c7d2fe; font-size: 12px; }
.hero-art { position: relative; height: 260px; }
.art-card { position: absolute; right: 0; bottom: 20px; width: 260px; background: #ffffff12; border: 1px solid #ffffff33; backdrop-filter: blur(8px); border-radius: 14px; padding: 18px; }
.art-title { font-size: 17px; font-weight: 600; }
.art-price { font-size: 26px; font-weight: 700; margin: 8px 0; }
.art-price small { font-size: 12px; font-weight: 400; color: #c7d2fe; }
.art-tags { display: flex; gap: 6px; flex-wrap: wrap; }
.art-tags span { font-size: 11px; border: 1px solid #ffffff44; border-radius: 999px; padding: 2px 8px; }
.snow1, .snow2, .snow3 { position: absolute; border-radius: 50%; background: #ffffff22; }
.snow1 { width: 80px; height: 80px; top: 10px; left: 30px; }
.snow2 { width: 130px; height: 130px; top: 60px; left: 120px; background: #ffffff11; }
.snow3 { width: 40px; height: 40px; top: 0; left: 230px; }
.dest-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); gap: 14px; }
.dest-card { display: block; background: #fff; border-radius: 12px; overflow: hidden; box-shadow: 0 2px 10px rgba(31,45,61,0.05); }
.dest-cover { height: 100px; background: linear-gradient(135deg, #dbeafe, #bfdbfe); background-size: cover; background-position: center; display: grid; place-items: center; font-size: 30px; color: #1f6feb; }
.dest-info { padding: 10px 12px; display: flex; flex-direction: column; gap: 2px; }
.flex-between { display: flex; justify-content: space-between; align-items: flex-end; gap: 16px; }
.product-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(260px, 1fr)); gap: 18px; }
.product-card { display: block; overflow: hidden; transition: transform .15s, box-shadow .15s; }
.product-card:hover { transform: translateY(-3px); box-shadow: 0 8px 24px rgba(31,45,61,0.1); }
.product-cover { height: 160px; background: linear-gradient(135deg, #dbeafe, #93c5fd); background-size: cover; background-position: center; display: grid; place-items: center; font-size: 34px; color: #1f6feb; position: relative; }
.days { position: absolute; left: 10px; top: 10px; background: #0f172acc; color: #fff; font-size: 12px; padding: 2px 8px; border-radius: 999px; }
.product-body { padding: 14px 16px; }
.product-body h3 { font-size: 16px; margin: 0 0 4px; }
.product-body p { margin: 0 0 10px; font-size: 13px; }
.product-foot { display: flex; justify-content: space-between; align-items: center; }
.notice-head { display: flex; justify-content: space-between; align-items: center; }
.notice { padding: 20px; }
.notice-row { display: flex; gap: 10px; align-items: center; padding: 10px 0; border-top: 1px dashed var(--border); font-size: 13px; }
.notice-title { font-weight: 600; white-space: nowrap; }
.notice-row .muted { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.features { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; padding-bottom: 30px; }
.feature { background: #fff; border-radius: 10px; padding: 16px; border: 1px solid var(--border); }
.feature b { display: block; margin-bottom: 4px; }
.feature span { color: var(--muted); font-size: 12px; }
@media (max-width: 860px) {
  .hero-inner { grid-template-columns: 1fr; }
  .hero-art { display: none; }
  .hero-copy h1 { font-size: 30px; }
  .features { grid-template-columns: 1fr 1fr; }
}
</style>
