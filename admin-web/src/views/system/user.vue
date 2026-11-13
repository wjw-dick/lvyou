<template>
  <CrudPage
    title="用户管理"
    endpoint="/system/users"
    :columns="[
      { prop: 'username', label: '账号', width: 130 },
      { prop: 'realName', label: '姓名', width: 120 },
      { prop: 'phone', label: '手机号', width: 140 },
      { prop: 'email', label: '邮箱', minWidth: 160 },
      { prop: 'isAdmin', label: '管理员', width: 90, type: 'bool' },
      { prop: 'status', label: '状态', width: 90, type: 'tag' }
    ]"
    :fields="[
      { prop: 'username', label: '账号', required: true },
      { prop: 'password', label: '密码', type: 'password' },
      { prop: 'realName', label: '姓名' },
      { prop: 'phone', label: '手机号' },
      { prop: 'email', label: '邮箱' },
      { prop: 'isAdmin', label: '租户管理员', type: 'switch' },
      { prop: 'roleIds', label: '角色', type: 'multiselect', optionsEndpoint: '/system/roles', span: 24 }
    ]"
    :search-fields="[{ prop: 'keyword', label: '账号/姓名' }]"
    :defaults="{ status: 1, isAdmin: 0, roleIds: [] }"
  />
</template>

<script setup lang="ts">
import CrudPage from '@/components/CrudPage.vue'
</script>
