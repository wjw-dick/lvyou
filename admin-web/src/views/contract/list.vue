<template>
  <div class="page-card">
    <div class="page-header">
      <h2 class="page-title">电子合同</h2>
      <el-button type="primary" @click="dialog = true">生成合同</el-button>
    </div>
    <el-table :data="rows" stripe v-loading="loading">
      <el-table-column prop="contractNo" label="合同编号" width="190" />
      <el-table-column prop="title" label="标题" min-width="240" show-overflow-tooltip />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag size="small" :type="row.signStatus === 1 ? 'success' : 'warning'">
            {{ row.signStatus === 1 ? '已签署' : '待签署' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="signTime" label="签署时间" width="170" />
      <el-table-column label="CA 证书" min-width="160">
        <template #default="{ row }">{{ row.caCert || '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="220">
        <template #default="{ row }">
          <el-button v-if="row.signStatus === 0" link type="primary" size="small" @click="sign(row)">在线签署</el-button>
          <el-button link type="primary" size="small" @click="download(`/contracts/${row.id}/pdf`, {}, `合同-${row.contractNo}.pdf`)">下载 PDF</el-button>
          <el-button v-if="row.signStatus === 1" link type="success" size="small" @click="sync(row)">同步文旅局</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialog" title="生成电子合同" width="480px">
      <el-form label-width="100px">
        <el-form-item label="关联订单ID"><el-input-number v-model="form.orderId" :min="1" style="width: 100%" /></el-form-item>
        <el-form-item label="合同模板">
          <el-select v-model="form.templateId" clearable style="width: 100%">
            <el-option v-for="t in templates" :key="t.id" :label="t.name" :value="t.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="create">生成</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api, download } from '@/api/request'

const rows = ref<any[]>([])
const templates = ref<any[]>([])
const loading = ref(false)
const dialog = ref(false)
const form = reactive<any>({ orderId: null, templateId: null })

async function load() {
  loading.value = true
  try {
    const data: any = await api.get('/contracts', { page: 1, size: 100 })
    rows.value = data.records || []
  } finally {
    loading.value = false
  }
}
async function sign(row: any) {
  await ElMessageBox.confirm('将进行人脸识别（模拟）并加盖 CA 数字证书，确认签署？', '在线签署', { type: 'warning' })
  const r: any = await api.post(`/contracts/${row.id}/sign`, { faceVerified: true })
  ElMessage.success(`签署成功，CA：${r.caCert}`)
  load()
}
async function sync(row: any) {
  await api.post(`/contracts/${row.id}/sync-gov`)
  ElMessage.success('已同步（模拟）国家旅游局电子合同系统')
  load()
}
async function create() {
  await api.post('/contracts', form)
  ElMessage.success('合同已生成')
  dialog.value = false
  load()
}
onMounted(async () => {
  templates.value = (await api.get('/contract/templates')) || []
  load()
})
</script>
