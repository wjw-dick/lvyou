<template>
  <CrudPage
    title="供应商档案"
    endpoint="/suppliers"
    :columns="[
      { prop: 'name', label: '供应商', minWidth: 170 },
      { prop: 'type', label: '类型', width: 100, type: 'tag' },
      { prop: 'contact', label: '联系人', width: 100 },
      { prop: 'phone', label: '电话', width: 130 },
      { prop: 'address', label: '地址', minWidth: 180 },
      { prop: 'settlementPeriod', label: '账期(天)', width: 100 },
      { prop: 'status', label: '状态', width: 90, type: 'tag' }
    ]"
    :fields="[
      { prop: 'name', label: '供应商名称', required: true },
      { prop: 'type', label: '类型', type: 'select', options: [{ label: '酒店', value: 'hotel' }, { label: '用车/车队', value: 'bus' }, { label: '餐厅', value: 'restaurant' }, { label: '地接社', value: 'land' }, { label: '门票', value: 'ticket' }] },
      { prop: 'contact', label: '联系人' },
      { prop: 'phone', label: '电话' },
      { prop: 'email', label: '邮箱' },
      { prop: 'username', label: '开放端账号' },
      { prop: 'password', label: '开放端密码', type: 'password' },
      { prop: 'settlementPeriod', label: '结算账期(天)', type: 'number' },
      { prop: 'commissionRate', label: '佣金比例', type: 'number', precision: 4 },
      { prop: 'status', label: '状态', type: 'switch' },
      { prop: 'address', label: '地址', span: 24 },
      { prop: 'remark', label: '备注', type: 'textarea', span: 24 }
    ]"
    :search-fields="[{ prop: 'keyword', label: '供应商名称' }, { prop: 'type', label: '类型', type: 'select', options: [{ label: '酒店', value: 'hotel' }, { label: '用车', value: 'bus' }, { label: '餐厅', value: 'restaurant' }, { label: '地接社', value: 'land' }] }]"
    :defaults="{ type: 'hotel', status: 1, settlementPeriod: 30 }"
  />
</template>

<script setup lang="ts">
import CrudPage from '@/components/CrudPage.vue'
</script>
