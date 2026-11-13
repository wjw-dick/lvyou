<template>
  <div>
    <div class="page-header">
      <h2 class="page-title">客流看板</h2>
      <el-select v-model="scenicId" clearable placeholder="全部景区" style="width: 220px" @change="load">
        <el-option v-for="s in scenics" :key="s.id" :label="s.name" :value="s.id" />
      </el-select>
    </div>
    <div class="stat-grid mb16">
      <div class="stat-card"><div class="stat-label">今日入园人数</div><div class="stat-value">{{ data.inCount || 0 }}</div></div>
      <div class="stat-card"><div class="stat-label">拥堵预警区域</div><div class="stat-value">{{ data.congestionAlerts || 0 }}</div></div>
      <div class="stat-card"><div class="stat-label">日期</div><div class="stat-value" style="font-size: 20px">{{ data.date }}</div></div>
    </div>
    <el-row :gutter="16">
      <el-col :span="14">
        <div class="page-card">
          <h3 class="page-title">分时段客流</h3>
          <div ref="chartRef" style="height: 340px" />
        </div>
      </el-col>
      <el-col :span="10">
        <div class="page-card">
          <h3 class="page-title">区域流量</h3>
          <el-table :data="data.areas || []" stripe>
            <el-table-column prop="name" label="区域" />
            <el-table-column prop="capacity" label="容量" width="90" />
            <el-table-column prop="inCount" label="当前人数" width="100" />
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.congested ? 'danger' : 'success'" size="small">{{ row.congested ? '拥堵' : '正常' }}</el-tag>
              </template>
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

const scenics = ref<any[]>([])
const scenicId = ref<number>()
const data = ref<any>({})
const chartRef = ref<HTMLElement>()
let chart: echarts.ECharts | null = null

async function load() {
  data.value = (await api.get('/scenic/traffic', { scenicId: scenicId.value })) || {}
  await nextTick()
  render()
}
function render() {
  if (!chartRef.value) return
  if (!chart) chart = echarts.init(chartRef.value)
  const byHour = data.value.byHour || {}
  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, top: 30, bottom: 30 },
    xAxis: { type: 'category', data: Object.keys(byHour).map((h) => `${h}:00`) },
    yAxis: { type: 'value' },
    series: [{ type: 'bar', data: Object.values(byHour), itemStyle: { color: '#1f6feb', borderRadius: [4, 4, 0, 0] } }]
  })
}
onMounted(async () => {
  scenics.value = (await api.get('/scenic/all')) || []
  load()
})
</script>
