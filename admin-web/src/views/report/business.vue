<template>
  <div>
    <div class="page-header">
      <h2 class="page-title">经营报表</h2>
      <div class="flex gap8">
        <el-date-picker v-model="range" type="daterange" value-format="YYYY-MM-DD" @change="load" />
        <el-button @click="download('/report/export', { startDate: range?.[0], endDate: range?.[1] }, '经营报表.xlsx')">导出 Excel</el-button>
      </div>
    </div>
    <div class="stat-grid mb16">
      <div class="stat-card"><div class="stat-label">订单数</div><div class="stat-value">{{ data.orderCount || 0 }}</div></div>
      <div class="stat-card"><div class="stat-label">销售额</div><div class="stat-value">¥{{ money(data.salesAmount) }}</div></div>
      <div class="stat-card"><div class="stat-label">退款金额</div><div class="stat-value">¥{{ money(data.refundAmount) }}</div></div>
      <div class="stat-card"><div class="stat-label">成团率</div><div class="stat-value">{{ data.groupRate || 0 }}%</div></div>
      <div class="stat-card"><div class="stat-label">毛利</div><div class="stat-value">¥{{ money(data.grossProfit) }}</div></div>
      <div class="stat-card"><div class="stat-label">导游报账</div><div class="stat-value">¥{{ money(data.guideExpense) }}</div></div>
      <div class="stat-card"><div class="stat-label">结算金额</div><div class="stat-value">¥{{ money(data.settlementAmount) }}</div></div>
      <div class="stat-card"><div class="stat-label">分销佣金</div><div class="stat-value">¥{{ money(data.commission) }}</div></div>
    </div>
    <el-row :gutter="16">
      <el-col :span="14">
        <div class="page-card">
          <h3 class="page-title">线路销售额 TOP</h3>
          <div ref="productChart" style="height: 320px" />
        </div>
      </el-col>
      <el-col :span="10">
        <div class="page-card">
          <h3 class="page-title">渠道销售占比</h3>
          <div ref="channelChart" style="height: 320px" />
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import { api, download } from '@/api/request'

const data = ref<any>({})
const range = ref<string[]>([])
const productChart = ref<HTMLElement>()
const channelChart = ref<HTMLElement>()

function money(v: any) { return Number(v || 0).toFixed(2) }

async function load() {
  data.value = (await api.get('/report/business', { startDate: range.value?.[0], endDate: range.value?.[1] })) || {}
  await nextTick()
  render()
}
function render() {
  if (productChart.value) {
    const byProduct = data.value.byProduct || {}
    const sorted = Object.entries(byProduct).sort((a: any, b: any) => b[1] - a[1]).slice(0, 8)
    const c = echarts.init(productChart.value)
    c.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: 50, right: 20, top: 20, bottom: 80 },
      xAxis: { type: 'category', data: sorted.map((x) => x[0]), axisLabel: { rotate: 25, width: 90, overflow: 'truncate' } },
      yAxis: { type: 'value' },
      series: [{ type: 'bar', data: sorted.map((x) => x[1]), itemStyle: { color: '#1f6feb', borderRadius: [4, 4, 0, 0] } }]
    })
  }
  if (channelChart.value) {
    const byChannel = data.value.byChannel || {}
    const c = echarts.init(channelChart.value)
    c.setOption({
      tooltip: { trigger: 'item' },
      series: [{
        type: 'pie', radius: ['45%', '70%'],
        data: Object.entries(byChannel).map(([name, value]) => ({ name, value })),
        label: { formatter: '{b}\n{d}%' }
      }]
    })
  }
}
onMounted(load)
</script>
