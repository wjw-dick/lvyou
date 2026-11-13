<template>
  <div>
    <div class="page-header">
      <h2 class="page-title">工作台</h2>
      <span class="muted">{{ data.tenantName }}</span>
    </div>

    <div class="stat-grid">
      <div class="stat-card">
        <div class="stat-label">今日订单</div>
        <div class="stat-value">{{ data.todayOrders || 0 }}</div>
        <div class="stat-extra">累计 {{ data.orderCount || 0 }} 单</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">累计销售额</div>
        <div class="stat-value">¥{{ money(data.salesAmount) }}</div>
        <div class="stat-extra">已支付订单口径</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">在售产品</div>
        <div class="stat-value">{{ data.productCount || 0 }}</div>
        <div class="stat-extra">配额 {{ data.quota?.product || '-' }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">会员数</div>
        <div class="stat-value">{{ data.memberCount || 0 }}</div>
        <div class="stat-extra">UnionID 多端互通</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">7 日内出团</div>
        <div class="stat-value">{{ data.upcomingSchedules || 0 }}</div>
        <div class="stat-extra">关注成团预警</div>
      </div>
    </div>

    <el-row :gutter="16" class="mt16">
      <el-col :span="16">
        <div class="page-card">
          <div class="page-header">
            <h3 class="page-title">出团预警</h3>
            <el-button link type="primary" @click="$router.push('/schedule/list')">全部排期</el-button>
          </div>
          <el-table :data="warnings" stripe size="small">
            <el-table-column prop="scheduleNo" label="出团编号" width="160" />
            <el-table-column prop="productTitle" label="线路" min-width="180" show-overflow-tooltip />
            <el-table-column prop="departDate" label="出团日期" width="110" />
            <el-table-column label="收客" width="100">
              <template #default="{ row }">{{ row.sold }}/{{ row.quota }}</template>
            </el-table-column>
            <el-table-column label="预警" width="90">
              <template #default="{ row }">
                <el-tag :type="row.level === 'red' ? 'danger' : row.level === 'yellow' ? 'warning' : 'success'" size="small">
                  {{ row.level === 'red' ? '红灯' : row.level === 'yellow' ? '黄灯' : '正常' }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!warnings.length" description="暂无预警，排期健康" />
        </div>
      </el-col>
      <el-col :span="8">
        <div class="page-card">
          <h3 class="page-title">快捷入口</h3>
          <div class="quick-list">
            <el-button v-for="q in quicks" :key="q.path" plain @click="$router.push(q.path)">{{ q.label }}</el-button>
          </div>
          <el-divider />
          <div class="muted">租户配额使用</div>
          <div v-for="(v, k) in data.usage" :key="k" class="quota-row">
            <span>{{ quotaLabel(String(k)) }}</span>
            <span>{{ v }} / {{ (data.quota || {})[String(k)] || '-' }}</span>
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { api } from '@/api/request'

const data = ref<any>({})
const warnings = ref<any[]>([])
const quicks = [
  { label: '新增线路', path: '/product/list' },
  { label: '排期管理', path: '/schedule/calendar' },
  { label: '订单核销', path: '/order/list' },
  { label: '结算单', path: '/finance/settlement' },
  { label: '景区验票', path: '/scenic/verify' },
  { label: '系统配置', path: '/system/config' }
]

function money(v: any) {
  return Number(v || 0).toFixed(2)
}
function quotaLabel(k: string) {
  return { product: '产品数', order: '订单数', user: '用户数', storage: '存储' }[k] || k
}

onMounted(async () => {
  data.value = await api.get('/system/dashboard')
  warnings.value = (await api.get('/schedules/warnings')) || []
})
</script>

<style scoped>
.quick-list { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; margin-top: 12px; }
.quick-list .el-button { margin: 0; }
.quota-row { display: flex; justify-content: space-between; padding: 6px 0; border-bottom: 1px dashed #ebeef5; font-size: 13px; }
</style>
