<template>
  <CrudPage
    title="导游档案"
    endpoint="/guides"
    export-url="/report/export"
    :columns="[
      { prop: 'name', label: '姓名', width: 110 },
      { prop: 'guideNo', label: '导游证号', minWidth: 150 },
      { prop: 'level', label: '等级', width: 90 },
      { prop: 'phone', label: '联系方式', width: 130 },
      { prop: 'languages', label: '语种', width: 130 },
      { prop: 'availableDates', label: '可带团时间', minWidth: 180 },
      { prop: 'idCardMask', label: '证件号', width: 170 },
      { prop: 'status', label: '状态', width: 90, type: 'tag' }
    ]"
    :fields="[
      { prop: 'name', label: '姓名', required: true },
      { prop: 'guideNo', label: '导游证号', required: true },
      { prop: 'level', label: '等级', type: 'select', options: [{ label: '初级', value: '初级' }, { label: '中级', value: '中级' }, { label: '高级', value: '高级' }, { label: '领队', value: '领队' }] },
      { prop: 'phone', label: '手机号' },
      { prop: 'gender', label: '性别', type: 'select', options: [{ label: '男', value: '男' }, { label: '女', value: '女' }] },
      { prop: 'languages', label: '语种' },
      { prop: 'idCard', label: '身份证号（加密存储）' },
      { prop: 'deposit', label: '押金', type: 'number', precision: 2 },
      { prop: 'availableDates', label: '可带团时间', span: 24 },
      { prop: 'unionId', label: '微信 UnionID（绑定导游端）', span: 12 },
      { prop: 'status', label: '状态', type: 'switch' },
      { prop: 'remark', label: '备注', type: 'textarea', span: 24 }
    ]"
    :search-fields="[{ prop: 'keyword', label: '姓名/证号/手机' }, { prop: 'status', label: '状态', type: 'select', options: [{ label: '正常', value: 1 }, { label: '停用', value: 0 }] }]"
    :defaults="{ status: 1, deposit: 0 }"
  />
</template>

<script setup lang="ts">
import CrudPage from '@/components/CrudPage.vue'
</script>
