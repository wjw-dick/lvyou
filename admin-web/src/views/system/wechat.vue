<template>
  <div class="page-card">
    <div class="page-header">
      <h2 class="page-title">微信配置</h2>
      <el-button type="primary" @click="saveConfig">保存配置</el-button>
    </div>
    <el-tabs v-model="tab">
      <el-tab-pane label="基础配置" name="base">
        <el-alert type="warning" :closable="false" class="mb16"
                  title="AppSecret / EncodingAESKey 加密存储、页面回掩码；修改后实时生效并写入审计日志。" />
        <el-form label-width="190px" style="max-width: 720px">
          <el-divider content-position="left">小程序（游客端/导游端/景区端）</el-divider>
          <el-form-item label="小程序 AppID"><el-input v-model="cfg['miniprogram.appid']" /></el-form-item>
          <el-form-item label="小程序 AppSecret">
            <el-input v-model="cfg['miniprogram.secret']" type="password" show-password placeholder="回掩码表示不修改" />
          </el-form-item>
          <el-divider content-position="left">公众号（服务号）</el-divider>
          <el-form-item label="公众号 AppID"><el-input v-model="cfg['mp.appid']" /></el-form-item>
          <el-form-item label="公众号 AppSecret">
            <el-input v-model="cfg['mp.secret']" type="password" show-password />
          </el-form-item>
          <el-form-item label="服务器 URL"><el-input v-model="cfg['mp.url']" placeholder="https://域名/api/wechat/callback" /></el-form-item>
          <el-form-item label="Token"><el-input v-model="cfg['mp.token']" /></el-form-item>
          <el-form-item label="EncodingAESKey">
            <el-input v-model="cfg['mp.aes_key']" type="password" show-password />
          </el-form-item>
          <el-form-item label="网页授权域名"><el-input v-model="cfg['mp.oauth_domain']" /></el-form-item>
          <el-divider content-position="left">开放平台 UnionID</el-divider>
          <el-form-item label="开放平台 AppID"><el-input v-model="cfg['open.platform_appid']" /></el-form-item>
        </el-form>
        <el-button @click="syncMenu">保存并同步自定义菜单</el-button>
      </el-tab-pane>

      <el-tab-pane label="模板消息" name="templates">
        <el-table :data="templates" stripe>
          <el-table-column prop="bizType" label="业务节点" width="150" />
          <el-table-column prop="title" label="标题" width="160" />
          <el-table-column label="模板 ID">
            <template #default="{ row }"><el-input v-model="row.templateId" placeholder="微信公众平台申请" /></template>
          </el-table-column>
          <el-table-column label="启用" width="100">
            <template #default="{ row }"><el-switch v-model="row.enabled" :active-value="1" :inactive-value="0" /></template>
          </el-table-column>
          <el-table-column label="操作" width="100">
            <template #default="{ row }"><el-button link type="primary" size="small" @click="saveTemplate(row)">保存</el-button></template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="内容运营" name="content">
        <el-row :gutter="16">
          <el-col :span="12">
            <h4>图文素材</h4>
            <el-table :data="materials" size="small">
              <el-table-column prop="title" label="标题" />
              <el-table-column prop="mediaId" label="MediaID" width="140" />
            </el-table>
          </el-col>
          <el-col :span="12">
            <h4>粉丝标签</h4>
            <el-table :data="tags" size="small">
              <el-table-column prop="name" label="标签" />
              <el-table-column prop="count" label="人数" width="90" />
            </el-table>
            <el-button class="mt16" type="primary" @click="massSend">定向群发（模拟）</el-button>
          </el-col>
        </el-row>
        <h4 class="mt16">粉丝列表</h4>
        <el-table :data="fans" size="small">
          <el-table-column prop="nickname" label="昵称" />
          <el-table-column prop="openId" label="OpenID" min-width="200" />
          <el-table-column prop="lastActiveAt" label="最近活跃" width="170" />
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api, http } from '@/api/request'

const tab = ref('base')
const cfg = reactive<Record<string, string>>({})
const templates = ref<any[]>([])
const materials = ref<any[]>([])
const tags = ref<any[]>([])
const fans = ref<any[]>([])

async function load() {
  const list: any[] = (await api.get('/system/wechat/configs')) || []
  list.forEach((c) => (cfg[c.key] = c.value))
  templates.value = (await api.get('/system/message/templates')) || []
  materials.value = (await api.get('/system/wechat/materials')) || []
  tags.value = (await api.get('/system/wechat/tags')) || []
  const f: any = await api.get('/system/wechat/fans', { page: 1, size: 50 })
  fans.value = f.records || []
}
async function saveConfig() {
  await api.put('/system/wechat/configs', cfg)
  ElMessage.success('微信配置已保存并生效')
  load()
}
async function saveTemplate(row: any) {
  await api.post('/system/message/templates', row)
  ElMessage.success('模板已保存')
}
async function syncMenu() {
  const menu = (await api.get('/system/nav', { platform: 'mp' })) || '[]'
  const r: any = await http.post('/system/wechat/menu/sync', menu, {
    headers: { 'Content-Type': 'text/plain' }
  })
  ElMessage.success(r.message || '菜单已同步')
}
async function massSend() {
  await api.post('/system/wechat/mass', { scope: 'all' })
  ElMessage.success('群发任务已提交（模拟，真实发送受微信频率限制）')
}
onMounted(load)
</script>
