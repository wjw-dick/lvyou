<template>
  <CrudPage
    title="平台公告"
    endpoint="/platform/notices"
    :columns="[
      { prop: 'title', label: '标题', minWidth: 220 },
      { prop: 'type', label: '类型', width: 100 },
      { prop: 'createdAt', label: '发布时间', width: 170 }
    ]"
    :fields="[
      { prop: 'title', label: '标题', required: true, span: 24 },
      { prop: 'type', label: '类型', type: 'select', options: [{ label: '公告', value: 'notice' }, { label: '活动', value: 'activity' }] },
      { prop: 'status', label: '发布', type: 'switch' },
      { prop: 'content', label: '内容', type: 'rich', span: 24 }
    ]"
    :defaults="{ type: 'notice', status: 1 }"
  />
</template>

<script setup lang="ts">
import CrudPage from '@/components/CrudPage.vue'
</script>
