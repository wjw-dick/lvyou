<template>
  <CrudPage
    title="全局标准字典"
    endpoint="/platform/dict"
    :columns="[
      { prop: 'dictType', label: '类型', width: 170 },
      { prop: 'dictLabel', label: '名称', width: 190 },
      { prop: 'dictValue', label: '值', width: 170 },
      { prop: 'sort', label: '排序', width: 80 },
      { prop: 'status', label: '状态', width: 90, type: 'tag' }
    ]"
    :fields="[
      { prop: 'dictType', label: '字典类型', required: true, placeholder: 'destination/order_status...' },
      { prop: 'dictLabel', label: '名称', required: true },
      { prop: 'dictValue', label: '值', required: true },
      { prop: 'sort', label: '排序', type: 'number' },
      { prop: 'status', label: '启用', type: 'switch' },
      { prop: 'remark', label: '备注', type: 'textarea', span: 24 }
    ]"
    :search-fields="[{ prop: 'type', label: '字典类型' }]"
    :defaults="{ sort: 0, status: 1 }"
  />
</template>

<script setup lang="ts">
import CrudPage from '@/components/CrudPage.vue'
</script>
