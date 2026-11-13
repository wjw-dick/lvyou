<template>
  <CrudPage
    title="平台用户"
    endpoint="/platform/users"
    :can-delete="false"
    :columns="[
      { prop: 'username', label: '账号', width: 140 },
      { prop: 'realName', label: '姓名', width: 130 },
      { prop: 'phone', label: '手机号', width: 140 },
      { prop: 'roleCode', label: '角色', width: 160 },
      { prop: 'status', label: '状态', width: 90, type: 'tag' }
    ]"
    :fields="[
      { prop: 'username', label: '账号', required: true },
      { prop: 'password', label: '密码', type: 'password' },
      { prop: 'realName', label: '姓名' },
      { prop: 'phone', label: '手机号' },
      { prop: 'roleCode', label: '角色', type: 'select', options: [{ label: '平台超管', value: 'platform_admin' }, { label: '平台运营', value: 'platform_ops' }] }
    ]"
    :defaults="{ roleCode: 'platform_admin' }"
  />
</template>

<script setup lang="ts">
import CrudPage from '@/components/CrudPage.vue'
</script>
