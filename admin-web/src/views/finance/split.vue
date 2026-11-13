<template>
  <div class="page-card">
    <div class="page-header">
      <h2 class="page-title">分账 / 资金托管</h2>
      <div class="flex gap8">
        <el-input-number v-model="orderId" :min="1" placeholder="订单ID" style="width: 180px" />
        <el-button type="primary" @click="execute">执行分账</el-button>
      </div>
    </div>
    <el-row :gutter="16">
      <el-col :span="10">
        <h4>分账接收方</h4>
        <el-table :data="accounts" stripe size="small">
          <el-table-column prop="name" label="名称" />
          <el-table-column prop="type" label="类型" width="100" />
          <el-table-column label="比例" width="90">
            <template #default="{ row }">{{ (Number(row.splitRate) * 100).toFixed(1) }}%</template>
          </el-table-column>
          <el-table-column label="操作" width="90">
            <template #default="{ row }"><el-button link type="primary" size="small" @click="editAccount(row)">编辑</el-button></template>
          </el-table-column>
        </el-table>
        <el-button class="mt16" @click="addAccount">新增接收方</el-button>
      </el-col>
      <el-col :span="14">
        <h4>分账明细</h4>
        <el-table :data="records" stripe size="small">
          <el-table-column prop="record.splitNo" label="分账单号" width="180" />
          <el-table-column prop="accountName" label="接收方" width="130" />
          <el-table-column prop="record.orderId" label="订单ID" width="90" />
          <el-table-column label="金额" width="110">
            <template #default="{ row }">¥{{ Number(row.record.amount || 0).toFixed(2) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="90">
            <template #default="{ row }"><el-tag size="small" :type="row.record.status === 1 ? 'success' : 'danger'">{{ row.record.status === 1 ? '成功' : '失败' }}</el-tag></template>
          </el-table-column>
          <el-table-column label="操作" width="80">
            <template #default="{ row }"><el-button v-if="row.record.status === 2" link type="primary" size="small" @click="retry(row)">重试</el-button></template>
          </el-table-column>
        </el-table>
      </el-col>
    </el-row>

    <el-dialog v-model="dialog" title="分账接收方" width="480px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.type" style="width: 100%">
            <el-option label="平台" value="platform" /><el-option label="旅行社" value="tenant" />
            <el-option label="景区" value="scenic" /><el-option label="供应商" value="supplier" />
          </el-select>
        </el-form-item>
        <el-form-item label="微信账户"><el-input v-model="form.wxAccount" /></el-form-item>
        <el-form-item label="分账比例"><el-input-number v-model="form.splitRate" :min="0" :max="1" :precision="4" style="width: 100%" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="saveAccount">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '@/api/request'

const accounts = ref<any[]>([])
const records = ref<any[]>([])
const orderId = ref<number>()
const dialog = ref(false)
const form = reactive<any>({ name: '', type: 'platform', wxAccount: '', splitRate: 0.05 })

async function load() {
  accounts.value = (await api.get('/split/accounts')) || []
  records.value = (await api.get('/split/records')) || []
}
function addAccount() {
  Object.assign(form, { id: null, name: '', type: 'platform', wxAccount: '', splitRate: 0.05 })
  dialog.value = true
}
function editAccount(row: any) {
  Object.assign(form, row)
  dialog.value = true
}
async function saveAccount() {
  await api.post('/split/accounts', form)
  ElMessage.success('已保存')
  dialog.value = false
  load()
}
async function execute() {
  if (!orderId.value) return ElMessage.warning('请输入订单ID')
  const r: any = await api.post('/split/execute', null, { orderId: orderId.value })
  ElMessage.success(`分账完成，共 ${r.created} 笔`)
  load()
}
async function retry(row: any) {
  await api.post(`/split/records/${row.record.id}/retry`)
  ElMessage.success('已重试')
  load()
}
onMounted(load)
</script>
