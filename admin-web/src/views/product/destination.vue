<template>
  <CrudPage
    title="目的地/景点/攻略"
    endpoint="/destinations"
    :columns="[
      { prop: 'name', label: '名称', minWidth: 160 },
      { prop: 'type', label: '类型', width: 110, type: 'tag' },
      { prop: 'province', label: '省份', width: 120 },
      { prop: 'city', label: '城市', width: 120 },
      { prop: 'summary', label: '简介', minWidth: 220 },
      { prop: 'status', label: '状态', width: 90, type: 'tag' }
    ]"
    :fields="[
      { prop: 'name', label: '名称', required: true },
      { prop: 'nameEn', label: '英文名' },
      { prop: 'type', label: '类型', type: 'select', options: [{ label: '地区', value: 'region' }, { label: '景点', value: 'scenic' }, { label: '攻略', value: 'strategy' }] },
      { prop: 'province', label: '省份' },
      { prop: 'city', label: '城市' },
      { prop: 'sort', label: '排序', type: 'number' },
      { prop: 'status', label: '启用', type: 'switch' },
      { prop: 'cover', label: '封面图', type: 'image', span: 24 },
      { prop: 'summary', label: '简介', type: 'textarea', span: 24 },
      { prop: 'content', label: '详情/攻略', type: 'rich', span: 24 },
      { prop: 'longitude', label: '经度', type: 'number', precision: 6 },
      { prop: 'latitude', label: '纬度', type: 'number', precision: 6 }
    ]"
    :search-fields="[
      { prop: 'keyword', label: '名称' },
      { prop: 'type', label: '类型', type: 'select', options: [{ label: '地区', value: 'region' }, { label: '景点', value: 'scenic' }, { label: '攻略', value: 'strategy' }] }
    ]"
    :defaults="{ type: 'scenic', status: 1, sort: 0 }"
  />
</template>

<script setup lang="ts">
import CrudPage from '@/components/CrudPage.vue'
</script>
