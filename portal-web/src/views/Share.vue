<template>
  <div class="container section narrow">
    <div v-if="product" class="card share">
      <div class="cover" :style="{ backgroundImage: product.cover ? `url(${product.cover})` : '' }" />
      <h2>{{ product.title }}</h2>
      <p class="muted">{{ product.destination }} · {{ product.days }} 天 · 来自好友分享</p>
      <div class="price">¥{{ product.adultPrice }} <small>起/人</small></div>
      <div class="actions">
        <router-link :to="`/products/${product.id}`" class="btn primary block">查看详情并预订</router-link>
        <button class="btn block" @click="copyLink">复制分享链接</button>
      </div>
      <p class="muted">分享来源：{{ source }} · 通过分享页下单可享会员权益</p>
    </div>
    <div v-else class="empty">分享内容不存在或已下架</div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { api } from '@/api'

const route = useRoute()
const product = ref(null)
const source = ref('')

onMounted(async () => {
  try {
    if (route.params.type === 'promotion') {
      const p = await api.get(`/public/promotion/${route.params.id}`)
      source.value = `推广码 ${p.code}`
      if (p.productId) {
        const d = await api.get(`/public/products/${p.productId}`)
        product.value = d.product
      }
    } else {
      const d = await api.get(`/public/products/${route.params.id}`)
      product.value = d.product
      source.value = '线路分享'
    }
  } catch (e) { /* ignore */ }
})
async function copyLink() {
  await navigator.clipboard.writeText(location.href)
  alert('链接已复制')
}
</script>

<style scoped>
.narrow { max-width: 480px; }
.share { overflow: hidden; text-align: center; padding-bottom: 20px; }
.cover { height: 200px; background: linear-gradient(135deg, #dbeafe, #93c5fd); background-size: cover; background-position: center; }
.share h2 { margin: 16px 16px 4px; }
.share .price { font-size: 26px; margin: 10px 0; }
.actions { padding: 0 20px; display: flex; flex-direction: column; gap: 8px; }
</style>
