<template>
  <el-container class="layout">
    <el-aside :width="collapsed ? '64px' : '220px'" class="aside">
      <div class="brand">
        <div class="logo">旅</div>
        <span v-if="!collapsed" class="brand-name">旅游数字化平台</span>
      </div>
      <el-scrollbar class="menu-scroll">
        <el-menu
          :default-active="route.path"
          :collapse="collapsed"
          :collapse-transition="false"
          background-color="#0f172a"
          text-color="#cbd5e1"
          active-text-color="#ffffff"
          router
        >
          <template v-for="item in menuTree" :key="item.id">
            <el-sub-menu v-if="item.children && item.children.length" :index="'m-' + item.id">
              <template #title>
                <el-icon><component :is="iconOf(item.icon)" /></el-icon>
                <span>{{ item.title }}</span>
              </template>
              <el-menu-item v-for="child in item.children" :key="child.id" :index="child.path">
                <el-icon><component :is="iconOf(child.icon)" /></el-icon>
                <span>{{ child.title }}</span>
              </el-menu-item>
            </el-sub-menu>
            <el-menu-item v-else :index="item.path">
              <el-icon><component :is="iconOf(item.icon)" /></el-icon>
              <template #title>{{ item.title }}</template>
            </el-menu-item>
          </template>
        </el-menu>
      </el-scrollbar>
    </el-aside>

    <el-container>
      <el-header class="header">
        <el-icon class="collapse-btn" @click="collapsed = !collapsed">
          <Expand v-if="collapsed" /><Fold v-else />
        </el-icon>
        <el-breadcrumb separator="/">
          <el-breadcrumb-item>{{ store.isPlatform ? '平台端' : '租户端' }}</el-breadcrumb-item>
          <el-breadcrumb-item v-for="c in breadcrumbs" :key="c">{{ c }}</el-breadcrumb-item>
        </el-breadcrumb>
        <div class="spacer" />
        <template v-if="store.isPlatform">
          <el-select
            v-model="tenantView"
            placeholder="全国视图"
            clearable
            filterable
            style="width: 220px"
            @change="switchTenant"
          >
            <el-option label="全国视图（跨租户统计）" value="" />
            <el-option v-for="t in tenants" :key="t.id" :label="`${t.name}（${t.code}）`" :value="String(t.id)" />
          </el-select>
          <el-tag v-if="tenantView" type="warning" size="small">下钻只读</el-tag>
        </template>
        <el-tag v-else type="success" size="small">{{ store.user?.tenantName || store.user?.tenantCode }}</el-tag>
        <el-dropdown trigger="click" @command="onLang">
          <span class="header-action"><el-icon><Switch /></el-icon> 中文</span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="zh-CN">简体中文</el-dropdown-item>
              <el-dropdown-item command="en-US">English</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <el-dropdown trigger="click" @command="onCommand">
          <span class="header-action">
            <el-avatar :size="28" style="background: #1f6feb">{{ avatarText }}</el-avatar>
            {{ store.user?.realName || store.user?.username }}
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">当前：{{ store.user?.tenantName }}</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>

      <div class="tabs">
        <el-tag
          v-for="tab in tabs"
          :key="tab.path"
          :closable="tabs.length > 1"
          :type="tab.path === route.path ? 'primary' : 'info'"
          :effect="tab.path === route.path ? 'dark' : 'plain'"
          class="tab"
          @close="closeTab(tab.path)"
          @click="router.push(tab.path)"
        >
          {{ tab.title }}
        </el-tag>
      </div>

      <el-main class="main">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { api } from '@/api/request'

const route = useRoute()
const router = useRouter()
const store = useAuthStore()
const collapsed = ref(false)
const tenants = ref<any[]>([])
const tenantView = ref(store.viewTenantId || '')
const tabs = ref<{ path: string; title: string }[]>([])

const avatarText = computed(() => (store.user?.realName || store.user?.username || 'U').slice(0, 1))

const menuTree = computed(() => {
  const menus = (store.menus || []).filter((m) => m.type !== 'F')
  const roots = menus.filter((m) => !m.parentId)
  return roots
    .map((r) => ({ ...r, children: menus.filter((m) => m.parentId === r.id).sort((a, b) => (a.sort || 0) - (b.sort || 0)) }))
    .sort((a, b) => (a.sort || 0) - (b.sort || 0))
})

const breadcrumbs = computed(() => route.path.split('/').filter(Boolean).map((p) => {
  const map: Record<string, string> = {
    product: '产品中心', schedule: '排期管理', order: '订单管理', guide: '导游管理', resource: '计调资源',
    finance: '财务结算', distribution: '分销管理', contract: '电子合同', member: '会员运营', scenic: '景区管理',
    content: '内容增长', report: '报表中心', system: '系统设置', platform: '平台端', insurance: '保险合规'
  }
  return map[p] || p
}))

function iconOf(name?: string) {
  return name || 'Menu'
}

async function loadTenants() {
  if (!store.isPlatform) return
  try {
    const data: any = await api.get('/platform/tenants', { page: 1, size: 200 })
    tenants.value = data.records || []
  } catch { /* ignore */ }
}

function switchTenant(val: string) {
  const t = tenants.value.find((x) => String(x.id) === val)
  store.setViewTenant(val || '', t?.name || '')
  window.location.reload()
}

function onLang(cmd: string) {
  localStorage.setItem('tp_lang', cmd)
  ElMessageBox.alert(cmd === 'en-US' ? 'Language preference saved. Content i18n is managed in 系统设置.' : '已切换为简体中文', '提示')
}

async function onCommand(cmd: string) {
  if (cmd === 'logout') {
    await ElMessageBox.confirm('确定退出登录？', '提示', { type: 'warning' })
    store.logout()
    router.push('/login')
  }
}

function closeTab(path: string) {
  const idx = tabs.value.findIndex((t) => t.path === path)
  if (idx < 0) return
  tabs.value.splice(idx, 1)
  if (path === route.path) router.push(tabs.value[tabs.value.length - 1]?.path || '/dashboard')
}

function resolveTitle(path: string) {
  const m = (store.menus || []).find((x) => x.path === path)
  return m?.title || breadcrumbs.value[breadcrumbs.value.length - 1] || '页面'
}

watch(
  () => route.path,
  (path) => {
    if (!tabs.value.find((t) => t.path === path)) tabs.value.push({ path, title: resolveTitle(path) })
  },
  { immediate: true }
)

onMounted(() => {
  loadTenants()
  if (!store.menus?.length) store.fetchMe().catch(() => {})
})
</script>

<style scoped>
.layout { height: 100vh; }
.aside { background: #0f172a; transition: width 0.2s; overflow: hidden; }
.brand { height: 56px; display: flex; align-items: center; gap: 10px; padding: 0 16px; color: #fff; border-bottom: 1px solid #1e293b; }
.logo { width: 30px; height: 30px; border-radius: 8px; background: linear-gradient(135deg, #1f6feb, #38bdf8); display: grid; place-items: center; font-weight: 700; }
.brand-name { font-weight: 600; font-size: 15px; white-space: nowrap; }
.menu-scroll { height: calc(100vh - 56px); }
.aside :deep(.el-menu) { border-right: none; }
.header { height: 56px; background: #fff; border-bottom: 1px solid #ebeef5; display: flex; align-items: center; gap: 12px; padding: 0 16px; }
.collapse-btn { cursor: pointer; font-size: 18px; color: #606266; }
.header-action { display: flex; align-items: center; gap: 6px; cursor: pointer; color: #606266; font-size: 13px; }
.tabs { background: #fff; padding: 6px 12px; border-bottom: 1px solid #ebeef5; display: flex; gap: 6px; overflow-x: auto; }
.tab { cursor: pointer; }
.main { padding: 16px; overflow-y: auto; background: #f5f7fa; }
.fade-enter-active, .fade-leave-active { transition: opacity 0.15s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
