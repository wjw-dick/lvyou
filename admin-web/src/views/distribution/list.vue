<template>
  <CrudPage
    title="分销商"
    endpoint="/distributors"
    :columns="[
      { prop: 'name', label: '名称', minWidth: 180 },
      { prop: 'type', label: '类型', width: 100 },
      { prop: 'contact', label: '联系人', width: 100 },
      { prop: 'phone', label: '手机号', width: 130 },
      { prop: 'shopName', label: '独立商城', minWidth: 160 },
      { prop: 'commissionRate', label: '佣金比例', width: 110 },
      { prop: 'status', label: '状态', width: 90, type: 'tag' }
    ]"
    :fields="[
      { prop: 'name', label: '分销商名称', required: true },
      { prop: 'type', label: '类型', type: 'select', options: [{ label: '门市', value: 'shop' }, { label: '代理商', value: 'agency' }, { label: '个人', value: 'individual' }] },
      { prop: 'contact', label: '联系人' },
      { prop: 'phone', label: '手机号' },
      { prop: 'username', label: '登录账号' },
      { prop: 'password', label: '登录密码', type: 'password' },
      { prop: 'shopName', label: '独立商城名称' },
      { prop: 'commissionRate', label: '佣金比例（0.05=5%）', type: 'number', precision: 4 },
      { prop: 'status', label: '启用', type: 'switch' }
    ]"
    :search-fields="[{ prop: 'keyword', label: '名称' }]"
    :defaults="{ type: 'shop', status: 1, commissionRate: 0.05 }"
  />
</template>

<script setup lang="ts">
import CrudPage from '@/components/CrudPage.vue'
</script>
