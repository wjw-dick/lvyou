<template>
  <div class="page-card">
    <div class="page-header">
      <h2 class="page-title">采购单</h2>
      <el-button type="primary" @click="dialog = true">新增采购单</el-button>
    </div>
    <el-table :data="rows" stripe v-loading="loading">
      <el-table-column label="采购单号" width="180">
        <template #default="{ row }">{{ row.purchase.purchaseNo }}</template>
      </el-table-column>
      <el-table-column label="供应商" min-width="160">
        <template #default="{ row }">{{ row.supplierName }}</template>
      </el-table-column>
      <el-table-column label="资源" min-width="140">
        <template #default="{ row }">{{ row.purchase.resourceName }}</template>
      </el-table-column>
      <el-table-column label="数量" width="80">
        <template #default="{ row }">{{ row.purchase.quantity }}</template>
      </el-table-column>
      <el-table-column label="金额" width="110">
        <template #default="{ row }">¥{{ Number(row.purchase.amount || 0).toFixed(2) }}</template>
      </el-table-column>
      <el-table-column label="出团编号" width="170">
        <template #default="{ row }">{{ row.scheduleNo }}</template>
      </el-table-column>
      <el-table-column label="状态" width="105">
        <template #default="{ row }">
          <el-tag size="small" :type="row.purchase.status === 1 ? 'success' : row.purchase.status === 2 ? 'danger' : 'warning'">
            {{ ['待确认', '已确认', '已拒绝', '已完结'][row.purchase.status] }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="140">
        <template #default="{ row }">
          <el-button v-if="row.purchase.status === 0" link type="success" size="small" @click="confirm(row, true)">确认</el-button>
          <el-button v-if="row.purchase.status === 0" link type="danger" size="small" @click="confirm(row, false)">拒绝</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialog" title="新增采购单" width="540px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="供应商">
          <el-select v-model="form.supplierId" filterable style="width: 100%" @change="loadResources">
            <el-option v-for="s in suppliers" :key="s.id" :label="`${s.name}（${s.type}）`" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="资源">
          <el-select v-model="form.resourceId" filterable style="width: 100%" @change="onResource">
            <el-option v-for="r in resources" :key="r.id" :label="`${r.name} · 剩${r.stock - r.sold} · ¥${r.costPrice}`" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="数量"><el-input-number v-model="form.quantity" :min="1" style="width: 100%" /></el-form-item>
        <el-form-item label="单价"><el-input-number v-model="form.unitPrice" :min="0" :precision="2" style="width: 100%" /></el-form-item>
        <el-form-item label="关联排期ID"><el-input-number v-model="form.scheduleId" :min="1" style="width: 100%" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="create">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '@/api/request'

const rows = ref<any[]>([])
const suppliers = ref<any[]>([])
const resources = ref<any[]>([])
const loading = ref(false)
const dialog = ref(false)
const form = reactive<any>({ supplierId: null, resourceId: null, quantity: 1, unitPrice: 0, scheduleId: null })

async function load() {
  loading.value = true
  try {
    const data: any = await api.get('/purchases', { page: 1, size: 100 })
    rows.value = data.records || []
  } finally {
    loading.value = false
  }
}
async function loadResources() {
  form.resourceId = null
  resources.value = (await api.get(`/suppliers/${form.supplierId}/resources`)) || []
}
function onResource(id: number) {
  const r = resources.value.find((x) => x.id === id)
  if (r) form.unitPrice = Number(r.costPrice)
}
async function create() {
  if (!form.supplierId || !form.resourceId) return ElMessage.warning('请选择供应商和资源')
  await api.post('/purchases', form)
  ElMessage.success('采购单已生成')
  dialog.value = false
  load()
}
async function confirm(row: any, accept: boolean) {
  await api.post(`/purchases/${row.purchase.id}/confirm`, null, { accept })
  ElMessage.success('已处理')
  load()
}
onMounted(async () => {
  suppliers.value = (await api.get('/suppliers/all')) || []
  load()
})
</script>
