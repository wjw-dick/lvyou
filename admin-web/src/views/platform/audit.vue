<template>
  <CrudPage
    title="平台审计日志"
    endpoint="/platform/audit"
    :can-add="false"
    :can-edit="false"
    :can-delete="false"
    :columns="[
      { prop: 'tenantId', label: '租户ID', width: 90 },
      { prop: 'operatorName', label: '操作人', width: 120 },
      { prop: 'userType', label: '类型', width: 110 },
      { prop: 'module', label: '模块', width: 140 },
      { prop: 'action', label: '动作', width: 170 },
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
