<template>
  <CrudPage
    title="合同模板"
    endpoint="/contract/templates"
    :columns="[
      { prop: 'name', label: '模板名称', minWidth: 220 },
      { prop: 'status', label: '状态', width: 90, type: 'tag' }
    ]"
    :fields="[
      { prop: 'name', label: '模板名称', required: true },
      { prop: 'status', label: '启用', type: 'switch' },
      { prop: 'content', label: '模板内容（支持 {{contact}} {{product}} {{date}} {{people}}）', type: 'rich', span: 24 }
    ]"
    :defaults="{ status: 1 }"
  />
</template>

<script setup lang="ts">
import CrudPage from '@/components/CrudPage.vue'
</script>
