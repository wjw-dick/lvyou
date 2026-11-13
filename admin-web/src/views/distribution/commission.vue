<template>
  <div class="page-card">
    <div class="page-header"><h2 class="page-title">分销佣金</h2></div>
    <el-table :data="rows" stripe v-loading="loading">
      <el-table-column label="订单号" width="190">
        <template #default="{ row }">{{ row.orderNo }}</template>
      </el-table-column>
      <el-table-column label="线路" min-width="200">
        <template #default="{ row }">{{ row.productTitle }}</template>
      </el-table-column>
      <el-table-column label="比例" width="100">
        <template #default="{ row }">{{ (Number(row.commission.rate) * 100).toFixed(1) }}%</template>
      </el-table-column>
      <el-table-column label="佣金" width="120">
        <template #default="{ row }">¥{{ Number(row.commission.amount || 0).toFixed(2) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag size="small" :type="row.commission.status === 1 ? 'success' : 'warning'">
            {{ row.commission.status === 1 ? '已入账' : '待入账' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button v-if="row.commission.status === 0" link type="primary" size="small" @click="settle(row)">入账</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '@/api/request'

const rows = ref<any[]>([])
const loading = ref(false)
async function load() {
  loading.value = true
  try {
    rows.value = (await api.get('/commissions')) || []
  } finally {
    loading.value = false
  }
}
async function settle(row: any) {
  await api.post(`/commissions/${row.commission.id}/settle`)
  ElMessage.success('佣金已入账')
  load()
}
onMounted(load)
</script>
