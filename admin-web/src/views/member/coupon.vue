<template>
  <CrudPage
    title="优惠券"
    endpoint="/coupons"
    :can-delete="false"
    :columns="[
      { prop: 'name', label: '券名称', minWidth: 180 },
      { prop: 'type', label: '类型', width: 100 },
      { prop: 'amount', label: '面额', width: 100, type: 'money' },
      { prop: 'minAmount', label: '门槛', width: 100, type: 'money' },
      { prop: 'total', label: '总量', width: 90 },
      { prop: 'received', label: '已领', width: 90 },
      { prop: 'endTime', label: '结束时间', width: 170 },
      { prop: 'status', label: '状态', width: 90, type: 'tag' }
    ]"
    :fields="[
      { prop: 'name', label: '券名称', required: true },
      { prop: 'type', label: '类型', type: 'select', options: [{ label: '满减券', value: 'cash' }, { label: '折扣券', value: 'discount' }] },
      { prop: 'amount', label: '面额（元）', type: 'number', precision: 2 },
      { prop: 'discount', label: '折扣（如 0.9）', type: 'number', precision: 2 },
      { prop: 'minAmount', label: '使用门槛', type: 'number', precision: 2 },
      { prop: 'total', label: '发放总量', type: 'number' },
      { prop: 'startTime', label: '开始时间', type: 'datetime' },
      { prop: 'endTime', label: '结束时间', type: 'datetime' },
      { prop: 'status', label: '启用', type: 'switch' }
    ]"
    :row-actions="[{ label: '全员发放', handler: issueAll }]"
    :defaults="{ type: 'cash', amount: 0, minAmount: 0, total: 100, status: 1 }"
  />
</template>

<script setup lang="ts">
import CrudPage from '@/components/CrudPage.vue'
import { api } from '@/api/request'
import { ElMessage, ElMessageBox } from 'element-plus'

async function issueAll(row: any) {
  await ElMessageBox.confirm(`确定向全部会员发放「${row.name}」？`, '发放确认')
  const r: any = await api.post(`/coupons/${row.id}/issue`, null, { all: true })
  ElMessage.success(`已发放 ${r.issued} 张`)
}
</script>
