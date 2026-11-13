<template>
  <div>
    <div class="page-header">
      <h2 class="page-title">全国数据看板</h2>
      <el-tag type="danger">system 超管 · 全国视图</el-tag>
    </div>
    <div class="stat-grid mb16">
      <div class="stat-card"><div class="stat-label">旅行社（租户）</div><div class="stat-value">{{ data.tenantCount || 0 }}</div></div>
      <div class="stat-card"><div class="stat-label">产品数</div><div class="stat-value">{{ data.productCount || 0 }}</div></div>
      <div class="stat-card"><div class="stat-label">订单数</div><div class="stat-value">{{ data.orderCount || 0 }}</div></div>
      <div class="stat-card"><div class="stat-label">销售额</div><div class="stat-value">¥{{ Number(data.salesAmount || 0).toFixed(2) }}</div></div>
      <div class="stat-card"><div class="stat-label">会员数</div><div class="stat-value">{{ data.memberCount || 0 }}</div></div>
    </div>
    <el-row :gutter="16">
      <el-col :span="14">
        <div class="page-card">
          <h3 class="page-title">租户销售额对比</h3>
          <div ref="chartRef" style="height: 340px" />
        </div>
      </el-col>
      <el-col :span="10">
        <div class="page-card">
          <h3 class="page-title">租户明细</h3>
          <el-table :data="data.tenants || []" size="small" max-height="340">
            <el-table-column prop="tenantName" label="租户" min-width="150" />
            <el-table-column prop="productCount" label="产品" width="70" />
            <el-table-column prop="orderCount" label="订单" width="70" />
            <el-table-column label="销售额" width="100">
              <template #default="{ row }">¥{{ Number(row.salesAmount || 0).toFixed(0) }}</template>
            </el-table-column>
          </el-table>
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
const chartRef = ref<HTMLElement>()

onMounted(async () => {
  data.value = (await api.get('/platform/dashboard')) || {}
  await nextTick()
  if (!chartRef.value) return
  const chart = echarts.init(chartRef.value)
  const tenants = data.value.tenants || []
  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 60, right: 20, top: 30, bottom: 40 },
    xAxis: { type: 'category', data: tenants.map((t: any) => t.tenantName) },
    yAxis: { type: 'value' },
    series: [{ type: 'bar', data: tenants.map((t: any) => t.salesAmount), itemStyle: { color: '#1f6feb', borderRadius: [4, 4, 0, 0] } }]
  })
})
</script>
