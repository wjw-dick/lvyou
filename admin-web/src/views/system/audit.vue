<template>
  <CrudPage
    title="审计日志"
    endpoint="/system/audit"
    :can-add="false"
    :can-edit="false"
    :can-delete="false"
    :columns="[
      { prop: 'operatorName', label: '操作人', width: 120 },
      { prop: 'module', label: '模块', width: 130 },
      { prop: 'action', label: '动作', width: 160 },
      { prop: 'target', label: '对象', minWidth: 200 },
      { prop: 'ip', label: 'IP', width: 140 },
      { prop: 'result', label: '结果', width: 90, type: 'tag' },
      { prop: 'createdAt', label: '时间', width: 170 }
    ]"
    :search-fields="[{ prop: 'keyword', label: '操作人/动作' }, { prop: 'module', label: '模块' }]"
  />
</template>

<script setup lang="ts">
import CrudPage from '@/components/CrudPage.vue'
</script>
