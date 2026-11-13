<template>
  <CrudPage
    title="直播带货"
    endpoint="/content/live"
    :columns="[
      { prop: 'title', label: '标题', minWidth: 200 },
      { prop: 'cover', label: '封面', width: 90, type: 'image' },
      { prop: 'productId', label: '关联线路ID', width: 120 },
      { prop: 'status', label: '状态', width: 90, type: 'tag' },
      { prop: 'viewCount', label: '观看量', width: 100 }
    ]"
    :fields="[
      { prop: 'title', label: '直播标题', required: true },
      { prop: 'productId', label: '关联线路ID', type: 'number' },
      { prop: 'url', label: '直播/短视频链接', span: 24 },
      { prop: 'status', label: '状态', type: 'switch' },
      { prop: 'cover', label: '封面', type: 'image', span: 24 }
    ]"
    :defaults="{ status: 0, viewCount: 0 }"
  />
</template>

<script setup lang="ts">
import CrudPage from '@/components/CrudPage.vue'
</script>
