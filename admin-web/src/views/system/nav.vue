<template>
  <div class="page-card">
    <div class="page-header">
      <h2 class="page-title">菜单可视化配置</h2>
      <div class="flex gap8">
        <el-select v-model="platform" style="width: 180px" @change="load">
          <el-option v-for="p in platforms" :key="p.value" :label="p.label" :value="p.value" />
        </el-select>
        <el-button type="primary" @click="save">保存并生效</el-button>
      </div>
    </div>
    <el-alert type="info" :closable="false" class="mb16" title="保存后小程序 tabBar / H5 底部导航 / App 底部导航实时生效，无需发版。" />
    <el-row :gutter="16">
      <el-col :span="14">
        <el-input v-model="json" type="textarea" :rows="22" placeholder='[{"title":"首页","path":"/pages/index/index","icon":"home"}]' />
      </el-col>
      <el-col :span="10">
        <div class="preview">
          <div class="phone">
            <div class="phone-head">{{ platform }} 预览</div>
            <div v-for="(item, i) in parsed" :key="i" class="preview-item">
              <el-icon><component :is="item.icon || 'Menu'" /></el-icon>
              <span>{{ item.title }}</span>
              <span class="muted">{{ item.path || item.link }}</span>
            </div>
            <el-empty v-if="!parsed.length" description="暂无导航项" :image-size="60" />
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '@/api/request'

const platforms = [
  { value: 'miniprogram', label: '小程序 tabBar' },
  { value: 'h5', label: 'H5 门户导航' },
  { value: 'app', label: 'App 底部导航' },
  { value: 'mp', label: '公众号菜单' },
  { value: 'admin', label: '后台导航' }
]
const platform = ref('miniprogram')
const json = ref('[]')
const parsed = computed(() => {
  try { return JSON.parse(json.value || '[]') } catch { return [] }
})

async function load() {
  json.value = (await api.get('/system/nav', { platform: platform.value })) || '[]'
  try {
    json.value = JSON.stringify(JSON.parse(json.value), null, 2)
  } catch {}
}
async function save() {
  try { JSON.parse(json.value) } catch { return ElMessage.error('JSON 格式不正确') }
  await api.put(`/system/nav?platform=${platform.value}`, json.value, {
    headers: { 'Content-Type': 'application/json' }
  } as any)
  ElMessage.success('导航配置已保存并实时生效')
}
onMounted(load)
</script>

<style scoped>
.preview { display: flex; justify-content: center; }
.phone { width: 280px; min-height: 480px; border: 8px solid #1f2d3d; border-radius: 24px; padding: 12px; background: #fff; }
.phone-head { text-align: center; font-weight: 600; padding: 8px; border-bottom: 1px solid #ebeef5; }
.preview-item { display: flex; align-items: center; gap: 8px; padding: 12px 8px; border-bottom: 1px dashed #ebeef5; }
.preview-item .muted { margin-left: auto; font-size: 11px; }
</style>
