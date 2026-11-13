<template>
  <CrudPage
    title="电子发票"
    endpoint="/invoices"
    :can-edit="false"
    :can-delete="false"
    :columns="[
      { prop: 'title', label: '抬头', minWidth: 200 },
      { prop: 'taxNo', label: '税号', width: 180 },
      { prop: 'amount', label: '金额', width: 120, type: 'money' },
      { prop: 'invoiceNo', label: '发票号', width: 180 },
      { prop: 'status', label: '状态', width: 100, type: 'tag' }
    ]"
    :fields="[
      { prop: 'title', label: '发票抬头', required: true },
      { prop: 'taxNo', label: '税号' },
      { prop: 'amount', label: '开票金额', type: 'number', precision: 2 },
      { prop: 'orderId', label: '关联订单ID', type: 'number' },
      { prop: 'settlementId', label: '关联结算单ID', type: 'number' }
    ]"
    :search-fields="[{ prop: 'status', label: '状态', type: 'select', options: [{ label: '待开票', value: 0 }, { label: '已开票', value: 1 }] }]"
    :row-actions="[{ label: '开票', show: (r) => r.status === 0, handler: issue }]"
    :defaults="{ amount: 0 }"
  />
</template>

<script setup lang="ts">
import CrudPage from '@/components/CrudPage.vue'
import { api } from '@/api/request'
import { ElMessage } from 'element-plus'

async function issue(row: any) {
  await api.post(`/invoices/${row.id}/issue`)
  ElMessage.success('已开具发票')
  location.reload()
}
</script>
