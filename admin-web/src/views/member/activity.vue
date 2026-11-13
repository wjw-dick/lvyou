<template>
  <CrudPage
    title="营销活动"
    endpoint="/activities"
    :can-delete="false"
    :columns="[
      { prop: 'name', label: '活动名称', minWidth: 200 },
      { prop: 'type', label: '类型', width: 110 },
      { prop: 'startTime', label: '开始时间', width: 170 },
      { prop: 'endTime', label: '结束时间', width: 170 },
      { prop: 'status', label: '状态', width: 90, type: 'tag' }
    ]"
    :fields="[
      { prop: 'name', label: '活动名称', required: true },
      { prop: 'type', label: '类型', type: 'select', options: [{ label: '优惠券', value: 'coupon' }, { label: '满减', value: 'fullcut' }, { label: '限时促销', value: 'limit' }] },
      { prop: 'startTime', label: '开始时间', type: 'datetime' },
      { prop: 'endTime', label: '结束时间', type: 'datetime' },
      { prop: 'status', label: '启用', type: 'switch' },
      { prop: 'ruleJson', label: '规则 JSON', type: 'textarea', rows: 5, span: 24 }
    ]"
    :defaults="{ type: 'coupon', status: 1 }"
  />
</template>

<script setup lang="ts">
import CrudPage from '@/components/CrudPage.vue'
</script>
