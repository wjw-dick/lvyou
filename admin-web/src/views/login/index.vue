<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-brand">
        <div class="logo">旅</div>
        <div>
          <h1>旅游数字化平台</h1>
          <p class="muted">SaaS 多租户 · 平台端 / 租户端统一入口</p>
        </div>
      </div>
      <el-tabs v-model="form.type" stretch>
        <el-tab-pane label="租户端" name="tenant" />
        <el-tab-pane label="平台端" name="platform" />
      </el-tabs>
      <el-form :model="form" label-position="top" @submit.prevent="submit">
        <el-form-item v-if="form.type === 'tenant'" label="租户编码">
          <el-input v-model="form.tenantCode" placeholder="如 HLJ" />
        </el-form-item>
        <el-form-item label="账号">
          <el-input v-model="form.username" placeholder="请输入账号" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" @keyup.enter="submit" />
        </el-form-item>
        <el-button type="primary" :loading="loading" style="width: 100%" @click="submit">登 录</el-button>
      </el-form>
      <div class="demo">
        <p class="muted">演示账号（密码均为 admin123）</p>
        <p class="muted">平台端：system</p>
        <p class="muted">租户端：HLJ / admin　·　XA / admin</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const store = useAuthStore()
const loading = ref(false)
const form = reactive({
  type: 'tenant',
  tenantCode: 'HLJ',
  username: 'admin',
  password: 'admin123'
})

async function submit() {
  if (!form.username || !form.password) return ElMessage.warning('请输入账号和密码')
  if (form.type === 'tenant' && !form.tenantCode) return ElMessage.warning('请输入租户编码')
  loading.value = true
  try {
    await store.login(form)
    ElMessage.success('登录成功')
    router.push(store.isPlatform ? '/platform/dashboard' : '/dashboard')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  background: radial-gradient(circle at 20% 20%, #1f6feb22, transparent 40%), #0f172a;
}
.login-card { width: 400px; background: #fff; border-radius: 12px; padding: 32px; box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3); }
.login-brand { display: flex; gap: 12px; align-items: center; margin-bottom: 16px; }
.logo { width: 44px; height: 44px; border-radius: 10px; background: linear-gradient(135deg, #1f6feb, #38bdf8); color: #fff; display: grid; place-items: center; font-size: 22px; font-weight: 700; }
h1 { font-size: 20px; margin: 0; }
.demo { margin-top: 16px; border-top: 1px dashed #ebeef5; padding-top: 12px; }
.demo p { margin: 2px 0; }
</style>
