<template>
  <div class="container section">
    <h1 class="section-title">关于我们</h1>
    <p class="section-sub">旅行社机构官网 · 资质公示 · 联系我们</p>
    <div class="about-grid">
      <div class="card intro">
        <h2>{{ tenant.name }}</h2>
        <p class="muted">{{ tenant.province }} {{ tenant.city }}</p>
        <p>深耕冰雪旅游多年，专注雪乡、哈尔滨及东北全域线路，提供跟团游、定制游、门票与包车服务。所有线路实名制出行，电子合同在线签署。</p>
        <div class="info">
          <div><span class="muted">客服电话</span><b>{{ tenant.phone || tenant['tenant.service_phone'] || '400-800-1234' }}</b></div>
          <div><span class="muted">服务时间</span><b>09:00 - 21:00</b></div>
          <div><span class="muted">经营资质</span><b>旅行社业务经营许可证（以门店公示为准）</b></div>
        </div>
      </div>
      <div class="card side">
        <h3>为什么选择我们</h3>
        <ul>
          <li>纯玩无购物，行程透明</li>
          <li>实名制 + 保险 + 应急预案</li>
          <li>电子合同 CA 存证，可在线验真</li>
          <li>小程序 / 公众号 / H5 / App 多端同源</li>
        </ul>
        <router-link to="/products" class="btn primary block">查看线路</router-link>
      </div>
    </div>
    <div class="card legal">
      <h3>合规与隐私</h3>
      <p>本平台依据《旅游法》《个人信息保护法》处理游客信息。身份证号/护照号/手机号均加密存储、展示脱敏；未经授权不向第三方提供。电子合同同步国家旅游局电子合同系统（按需）。</p>
      <p class="muted">隐私协议 / 用户协议 / 投诉渠道：客服电话或门店现场。</p>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { api } from '@/api'

const tenant = ref({})
onMounted(async () => { tenant.value = (await api.get('/public/tenant')) || {} })
</script>

<style scoped>
.about-grid { display: grid; grid-template-columns: 1.4fr 0.6fr; gap: 18px; }
.intro, .side, .legal { padding: 22px; }
.intro h2 { margin: 0 0 4px; }
.info { display: grid; gap: 12px; margin-top: 18px; }
.info div { display: flex; flex-direction: column; }
.side ul { padding-left: 18px; color: #606266; }
.side li { margin-bottom: 6px; }
.legal { margin-top: 18px; }
@media (max-width: 800px) { .about-grid { grid-template-columns: 1fr; } }
</style>
