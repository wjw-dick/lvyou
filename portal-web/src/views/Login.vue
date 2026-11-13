<template>
  <div class="container section narrow">
    <h1 class="section-title">登录</h1>
    <p class="section-sub">游客 UnionID 登录（模拟微信网页授权） / 分销商账号登录</p>
    <div class="tabs">
      <button :class="{ active: tab === 'member' }" @click="tab = 'member'">游客登录</button>
      <button :class="{ active: tab === 'distributor' }" @click="tab = 'distributor'">分销商登录</button>
    </div>

    <div v-if="tab === 'member'" class="card form">
      <div class="field"><label>租户编码</label><input v-model="form.tenantCode" class="input" /></div>
      <div class="field"><label>微信授权 code（模拟可随意填写）</label><input v-model="form.code" class="input" placeholder="mock-code-001" /></div>
      <div class="field"><label>昵称</label><input v-model="form.nickname" class="input" /></div>
      <button class="btn primary block" @click="memberLogin">微信一键登录</button>
      <p class="muted">生产环境将跳转微信 snsapi_userinfo 授权，UnionID 与小程序/App 互通。</p>
    </div>

    <div v-else class="card form">
      <div class="field"><label>租户编码</label><input v-model="form.tenantCode" class="input" /></div>
      <div class="field"><label>账号</label><input v-model="dist.username" class="input" /></div>
      <div class="field"><label>密码</label><input v-model="dist.password" type="password" class="input" /></div>
      <button class="btn primary block" @click="distributorLogin">登录分销商中心</button>
      <p class="muted">演示账号：md001 / admin123</p>
    </div>
    <p v-if="error" class="error">{{ error }}</p>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '@/api'

const router = useRouter()
const tab = ref('member')
const error = ref('')
const form = reactive({ tenantCode: 'HLJ', code: 'mock-code-001', nickname: 'H5 游客' })
const dist = reactive({ username: 'md001', password: 'admin123' })

async function memberLogin() {
  error.value = ''
  try {
    const r = await api.post('/auth/member-login', form)
    localStorage.setItem('tp_portal_token', r.token)
    localStorage.setItem('tp_portal_user', JSON.stringify(r))
    router.push('/member')
  } catch (e) { error.value = e.message }
}
async function distributorLogin() {
  error.value = ''
  try {
    const r = await api.post('/auth/distributor-login', { tenantCode: form.tenantCode, ...dist })
    localStorage.setItem('tp_portal_token', r.token)
    localStorage.setItem('tp_portal_user', JSON.stringify(r))
    router.push('/distributor')
  } catch (e) { error.value = e.message }
}
</script>

<style scoped>
.narrow { max-width: 460px; }
.tabs { display: flex; gap: 8px; margin-bottom: 14px; }
.tabs button { flex: 1; height: 38px; border: 1px solid var(--border); background: #fff; border-radius: 8px; cursor: pointer; }
.tabs button.active { background: var(--primary); color: #fff; border-color: var(--primary); }
.form { padding: 20px; }
.error { color: #f56c6c; font-size: 13px; }
</style>
