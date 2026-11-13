<template>
  <div class="page-card">
    <div class="page-header"><h2 class="page-title">导游报账审核</h2></div>
    <el-table :data="rows" stripe v-loading="loading">
      <el-table-column label="导游" width="110">
        <template #default="{ row }">{{ row.guideName }}</template>
      </el-table-column>
      <el-table-column label="出团编号" width="180">
        <template #default="{ row }">{{ row.scheduleNo }}</template>
      </el-table-column>
      <el-table-column label="类型" width="110">
        <template #default="{ row }">{{ typeMap[row.expense?.type] || row.expense?.type }}</template>
      </el-table-column>
      <el-table-column label="金额" width="110">
        <template #default="{ row }">¥{{ Number(row.expense?.amount || 0).toFixed(2) }}</template>
      </el-table-column>
      <el-table-column label="单据" width="120">
        <template #default="{ row }">
          <el-image v-for="(u, i) in (row.expense?.receiptImages || '').split(',').filter(Boolean)" :key="i" :src="u"
                    :preview-src-list="(row.expense?.receiptImages || '').split(',').filter(Boolean)" fit="cover"
                    style="width: 32px; height: 32px; margin-right: 2px; border-radius: 3px" />
        </template>
      </el-table-column>
      <el-table-column label="备注" min-width="160">
        <template #default="{ row }">{{ row.expense?.remark }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag size="small" :type="row.expense?.status === 1 ? 'success' : row.expense?.status === 2 ? 'danger' : 'warning'">
            {{ row.expense?.status === 1 ? '已通过' : row.expense?.status === 2 ? '已驳回' : '待审核' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <template v-if="row.expense?.status === 0">
            <el-button link type="success" size="small" @click="audit(row, true)">通过</el-button>
            <el-button link type="danger" size="small" @click="audit(row, false)">驳回</el-button>
          </template>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '@/api/request'

const typeMap: Record<string, string> = { ticket: '门票', meal: '餐费', other: '其他消费' }
const rows = ref<any[]>([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const data: any = await api.get('/guide/expenses', { page: 1, size: 100 })
    rows.value = data.records || []
  } finally {
    loading.value = false
  }
}
async function audit(row: any, approve: boolean) {
  await api.post(`/guide/expenses/${row.expense.id}/audit`, null, { approve })
  ElMessage.success('已处理')
  load()
}
onMounted(load)
</script>
