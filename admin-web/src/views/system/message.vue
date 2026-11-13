<template>
  <div class="page-card">
    <div class="page-header"><h2 class="page-title">消息配置</h2></div>
    <el-tabs v-model="tab">
      <el-tab-pane label="模板消息" name="templates">
        <el-table :data="templates" stripe>
          <el-table-column prop="bizType" label="业务节点" width="150" />
          <el-table-column prop="title" label="标题" width="160" />
          <el-table-column label="模板 ID">
            <template #default="{ row }"><el-input v-model="row.templateId" placeholder="微信公众平台申请" /></template>
          </el-table-column>
          <el-table-column label="启用" width="90">
            <template #default="{ row }"><el-switch v-model="row.enabled" :active-value="1" :inactive-value="0" /></template>
          </el-table-column>
          <el-table-column label="操作" width="100">
            <template #default="{ row }"><el-button link type="primary" size="small" @click="save(row)">保存</el-button></template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="发送日志" name="logs">
        <el-table :data="logs" stripe size="small">
          <el-table-column prop="bizType" label="节点" width="120" />
          <el-table-column prop="memberId" label="会员ID" width="100" />
          <el-table-column prop="openId" label="OpenID" min-width="180" />
          <el-table-column prop="content" label="内容" min-width="220" show-overflow-tooltip />
          <el-table-column label="状态" width="90">
            <template #default="{ row }"><el-tag size="small" :type="row.status === 1 ? 'success' : row.status === 2 ? 'danger' : 'info'">{{ ['待发', '成功', '失败'][row.status] }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="createdAt" label="时间" width="170" />
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '@/api/request'

const tab = ref('templates')
const templates = ref<any[]>([])
const logs = ref<any[]>([])

async function load() {
  templates.value = (await api.get('/system/message/templates')) || []
  const d: any = await api.get('/system/message/logs', { page: 1, size: 100 })
  logs.value = d.records || []
}
async function save(row: any) {
  await api.post('/system/message/templates', row)
  ElMessage.success('模板已保存')
}
onMounted(load)
</script>
