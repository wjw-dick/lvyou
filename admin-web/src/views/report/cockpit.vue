<template>
  <div>
    <div class="page-header">
      <h2 class="page-title">经营驾驶舱</h2>
      <span class="muted">实时看板 · 移动端自适应</span>
    </div>
    <div class="stat-grid mb16">
      <div class="stat-card"><div class="stat-label">今日订单</div><div class="stat-value">{{ data.todayOrders || 0 }}</div></div>
      <div class="stat-card"><div class="stat-label">今日销售额</div><div class="stat-value">¥{{ money(data.todaySales) }}</div></div>
      <div class="stat-card"><div class="stat-label">今日入园</div><div class="stat-value">{{ data.todayTraffic || 0 }}</div></div>
      <div class="stat-card"><div class="stat-label">7 日排期</div><div class="stat-value">{{ data.upcomingSchedules || 0 }}</div></div>
      <div class="stat-card"><div class="stat-label">成团预警</div><div class="stat-value" style="color: #f56c6c">{{ data.warningSchedules || 0 }}</div></div>
    </div>
    <el-row :gutter="16">
      <el-col :span="12">
        <div class="page-card">
          <h3 class="page-title">近 30 天客源结构</h3>
          <div ref="sourceChart" style="height: 320px" />
        </div>
      </el-col>
      <el-col :span="12">
        <div class="page-card">
          <h3 class="page-title">会员结构</h3>
          <div class="member-box">
            <div><span class="muted">新增会员（30天）</span><b>{{ data.customerStructure?.newMembers || 0 }}</b></div>
            <div><span class="muted">会员总数</span><b>{{ data.customerStructure?.totalMembers || 0 }}</b></div>
          </div>
          <el-divider />
          <h4>快捷入口</h4>
          <div class="flex gap8">
            <el-button @click="$router.push('/report/business')">经营报表</el-button>
            <el-button @click="$router.push('/schedule/list')">排期预警</el-button>
            <el-button @click="$router.push('/order/list')">订单</el-button>
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import { api } from '@/api/request'

const data = ref<any>({})
const sourceChart = ref<HTMLElement>()
function money(v: any) { return Number(v || 0).toFixed(2) }

onMounted(async () => {
  data.value = (await api.get('/report/cockpit')) || {}
  await nextTick()
  if (!sourceChart.value) return
  const source = data.value.sourceAmount || {}
  const c = echarts.init(sourceChart.value)
  c.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie', radius: ['40%', '65%'],
      data: Object.entries(source).map(([name, value]) => ({ name, value })),
      label: { formatter: '{b}: {d}%' }
    }]
  })
})
</script>

<style scoped>
.member-box { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-top: 16px; }
.member-box div { background: #fafcff; border: 1px solid #ebeef5; border-radius: 8px; padding: 16px; }
.member-box b { display: block; font-size: 28px; margin-top: 6px; }
</style>
