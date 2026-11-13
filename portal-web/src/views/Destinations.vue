<template>
  <div class="container section">
    <h1 class="section-title">目的地与攻略</h1>
    <p class="section-sub">发现目的地故事、景点攻略与出行建议</p>
    <div class="grid dest-grid">
      <div v-for="d in list" :key="d.id" class="card dest">
        <div class="cover" :style="{ backgroundImage: d.cover ? `url(${d.cover})` : '' }">
          <span v-if="!d.cover">{{ d.name.slice(0, 1) }}</span>
        </div>
        <div class="body">
          <div class="flex"><b>{{ d.name }}</b><span class="tag">{{ typeMap[d.type] || d.type }}</span></div>
          <p class="muted">{{ d.summary }}</p>
          <div v-if="d.content" class="content" v-html="d.content" />
        </div>
      </div>
    </div>
    <div v-if="!list.length" class="empty">暂无目的地内容</div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { api } from '@/api'

const typeMap = { region: '地区', scenic: '景点', strategy: '攻略' }
const list = ref([])
onMounted(async () => { list.value = (await api.get('/public/destinations')) || [] })
</script>

<style scoped>
.dest-grid { grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); }
.dest { overflow: hidden; }
.cover { height: 150px; background: linear-gradient(135deg, #dbeafe, #93c5fd); background-size: cover; background-position: center; display: grid; place-items: center; font-size: 38px; color: #1f6feb; }
.body { padding: 16px; }
.flex { display: flex; justify-content: space-between; align-items: center; }
.body p { margin: 6px 0; }
.content { font-size: 13px; color: #606266; max-height: 120px; overflow: hidden; }
</style>
