<template>
  <div class="page-card">
    <div class="page-header">
      <h2 class="page-title">旅游保险</h2>
      <el-button type="primary" @click="openAdd">新增保险产品</el-button>
    </div>
    <el-tabs v-model="tab">
      <el-tab-pane label="保险产品" name="products">
        <el-table :data="products" stripe>
          <el-table-column prop="name" label="产品" min-width="200" />
          <el-table-column prop="company" label="保险公司" width="160" />
          <el-table-column prop="scope" label="范围" width="100" />
          <el-table-column label="保费" width="100">
            <template #default="{ row }">¥{{ Number(row.premium || 0).toFixed(2) }}</template>
          </el-table-column>
          <el-table-column prop="coverageJson" label="保障项" min-width="200" show-overflow-tooltip />
          <el-table-column label="状态" width="90">
            <template #default="{ row }"><el-tag size="small" :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '在售' : '下架' }}</el-tag></template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="投保单/保单" name="orders">
        <el-table :data="orders" stripe>
          <el-table-column label="产品" min-width="180">
            <template #default="{ row }">{{ row.productName }}</template>
          </el-table-column>
          <el-table-column label="被保险人" width="130">
            <template #default="{ row }">{{ row.order.insuredName }}</template>
          </el-table-column>
          <el-table-column label="保费" width="100">
            <template #default="{ row }">¥{{ Number(row.order.premium || 0).toFixed(2) }}</template>
          </el-table-column>
          <el-table-column label="保额" width="120">
            <template #default="{ row }">¥{{ Number(row.order.coverage || 0).toFixed(2) }}</template>
          </el-table-column>
          <el-table-column prop="order.policyNo" label="保单号" width="180" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }"><el-tag size="small" :type="row.order.status === 1 ? 'success' : 'warning'">{{ row.order.status === 1 ? '已投保' : '待投保' }}</el-tag></template>
          </el-table-column>
          <el-table-column label="操作" width="100">
            <template #default="{ row }">
              <el-button v-if="row.order.status === 0" link type="primary" size="small" @click="issue(row)">出单</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="dialog" title="新增保险产品" width="560px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="产品名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="保险公司"><el-input v-model="form.company" /></el-form-item>
        <el-form-item label="范围">
          <el-select v-model="form.scope" style="width: 100%"><el-option label="境内" value="domestic" /><el-option label="境外" value="overseas" /></el-select>
        </el-form-item>
        <el-form-item label="保费"><el-input-number v-model="form.premium" :min="0" :precision="2" style="width: 100%" /></el-form-item>
        <el-form-item label="保障项 JSON"><el-input v-model="form.coverageJson" type="textarea" :rows="3" placeholder='{"意外身故":"30万","医疗":"5万"}' /></el-form-item>
        <el-form-item label="在售"><el-switch v-model="form.status" :active-value="1" :inactive-value="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '@/api/request'

const tab = ref('products')
const products = ref<any[]>([])
const orders = ref<any[]>([])
const dialog = ref(false)
const form = reactive<any>({ name: '', company: '', scope: 'domestic', premium: 0, coverageJson: '', status: 1 })

async function load() {
  products.value = (await api.get('/insurance/products')) || []
  orders.value = (await api.get('/insurance/orders')) || []
}
function openAdd() {
  Object.assign(form, { name: '', company: '', scope: 'domestic', premium: 0, coverageJson: '', status: 1 })
  dialog.value = true
}
async function save() {
  await api.post('/insurance/products', form)
  ElMessage.success('已保存')
  dialog.value = false
  load()
}
async function issue(row: any) {
  await api.post(`/insurance/orders/${row.order.id}/issue`)
  ElMessage.success('已出单')
  load()
}
onMounted(load)
</script>
