<template>
  <div class="container section">
    <h1 class="section-title">全部线路</h1>
    <p class="section-sub">支持关键词 / 目的地 / 天数筛选，实时余位</p>
    <div class="filters card">
      <input v-model="query.keyword" class="input" placeholder="搜索线路" @keyup.enter="load" />
      <select v-model="query.destination" class="input" @change="load">
        <option value="">全部目的地</option>
        <option v-for="d in destinations" :key="d.id" :value="d.name">{{ d.name }}</option>
      </select>
      <select v-model="query.days" class="input" @change="load">
        <option value="">全部天数</option>
        <option v-for="d in [1, 2, 3, 5, 7]" :key="d" :value="d">{{ d }} 天</option>
      </select>
      <button class="btn primary" @click="load">筛选</button>
    </div>

    <div class="list">
      <router-link v-for="p in products" :key="p.id" :to="`/products/${p.id}`" class="row card">
        <div class="cover" :style="{ backgroundImage: p.cover ? `url(${p.cover})` : '' }">
          <span v-if="!p.cover">{{ p.title.slice(0, 2) }}</span>
        </div>
        <div class="info">
          <div class="flex">
            <h3>{{ p.title }}</h3>
            <span class="tag">{{ p.category === 'line' ? '跟团游' : p.category }}</span>
          </div>
          <p class="muted">{{ p.subtitle || '' }}</p>
          <div class="meta">
            <span>{{ p.days }} 天</span>
            <span>{{ p.departure }} 出发</span>
            <span>余位 {{ Math.max(0, p.stock - p.sold) }}</span>
            <span v-if="p.destination">目的地 {{ p.destination }}</span>
          </div>
        </div>
        <div class="buy">
          <div class="price">¥{{ p.adultPrice }} <small>起/人</small></div>
          <button class="btn primary">查看详情</button>
        </div>
      </router-link>
    </div>
    <div v-if="!products.length" class="empty">没有符合条件的线路</div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { api } from '@/api'

const route = useRoute()
const products = ref([])
const destinations = ref([])
const query = reactive({ keyword: route.query.keyword || '', destination: route.query.destination || '', days: '' })

async function load() {
  const data = await api.get('/public/products', { ...query, page: 1, size: 50 })
  products.value = data.records || []
}
onMounted(async () => {
  destinations.value = (await api.get('/public/destinations')) || []
  load()
})
</script>

<style scoped>
.filters { display: grid; grid-template-columns: 1.6fr 1fr 0.8fr auto; gap: 10px; padding: 14px; margin-bottom: 18px; }
.list { display: flex; flex-direction: column; gap: 14px; }
.row { display: grid; grid-template-columns: 210px 1fr 150px; gap: 16px; padding: 14px; }
.cover { height: 140px; border-radius: 8px; background: linear-gradient(135deg, #dbeafe, #93c5fd); background-size: cover; background-position: center; display: grid; place-items: center; font-size: 30px; color: #1f6feb; }
.info h3 { font-size: 18px; margin: 0 0 6px; }
.flex { display: flex; align-items: center; gap: 10px; }
.meta { display: flex; gap: 14px; flex-wrap: wrap; color: var(--muted); font-size: 13px; }
.buy { display: flex; flex-direction: column; justify-content: center; align-items: flex-end; gap: 10px; }
.buy .price { font-size: 22px; }
@media (max-width: 760px) {
  .filters { grid-template-columns: 1fr; }
  .row { grid-template-columns: 110px 1fr; }
  .cover { height: 100px; }
  .buy { grid-column: 1 / -1; flex-direction: row; justify-content: space-between; align-items: center; }
}
</style>
