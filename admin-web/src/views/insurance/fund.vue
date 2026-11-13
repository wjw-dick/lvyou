<template>
  <CrudPage
    title="质保金/保证金台账"
    endpoint="/funds"
    :columns="[
      { prop: 'name', label: '名称', minWidth: 200 },
      { prop: 'type', label: '类型', width: 110 },
      { prop: 'amount', label: '应缴金额', width: 130, type: 'money' },
      { prop: 'paidAmount', label: '实缴金额', width: 130, type: 'money' },
      { prop: 'expireDate', label: '到期日', width: 120 },
      { prop: 'status', label: '状态', width: 90, type: 'tag' }
    ]"
    :fields="[
      { prop: 'name', label: '名称', required: true },
      { prop: 'type', label: '类型', type: 'select', options: [{ label: '质量保证金', value: 'quality' }, { label: '履约保证金', value: 'margin' }] },
      { prop: 'amount', label: '应缴金额', type: 'number', precision: 2 },
      { prop: 'paidAmount', label: '实缴金额', type: 'number', precision: 2 },
      { prop: 'expireDate', label: '到期日', type: 'date' },
      { prop: 'status', label: '状态', type: 'switch' },
      { prop: 'remark', label: '备注', type: 'textarea', span: 24 }
    ]"
    :defaults="{ type: 'quality', status: 1 }"
  />
</template>

<script setup lang="ts">
import CrudPage from '@/components/CrudPage.vue'
</script>
