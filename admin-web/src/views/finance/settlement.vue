<template>
  <div class="page-card">
    <div class="page-header">
      <h2 class="page-title">结算单</h2>
      <div class="flex gap8">
        <el-button @click="download('/settlements/export', {}, '结算单.xlsx')">导出</el-button>
        <el-button type="primary" @click="dialog = true">发起结算</el-button>
      </div>
    </div>

    <el-table :data="rows" stripe v-loading="loading">
      <el-table-column prop="settleNo" label="结算单号" width="180" />
      <el-table-column prop="type" label="类型" width="100" />
      <el-table-column prop="partnerName" label="合作方" min-width="150" />
      <el-table-column label="金额" width="110">
        <template #default="{ row }">¥{{ Number(row.amount || 0).toFixed(2) }}</template>
      </el-table-column>
      <el-table-column label="已付" width="110">
        <template #default="{ row }">¥{{ Number(row.paidAmount || 0).toFixed(2) }}</template>
      </el-table-column>
      <el-table-column label="佣金" width="100">
        <template #default="{ row }">¥{{ Number(row.commission || 0).toFixed(2) }}</template>
      </el-table-column>
      <el-table-column label="真实到账" width="120">
        <template #default="{ row }">¥{{ Number(row.realAmount || 0).toFixed(2) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag size="small" :type="row.status === 3 ? 'success' : row.status === 2 ? 'primary' : 'warning'">
            {{ ['待确认', '已确认', '已结算', '已归档'][row.status] }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button v-if="row.status === 0" link type="primary" size="small" @click="confirm(row)">确认</el-button>
          <el-button v-if="row.approvalStatus === 0" link type="success" size="small" @click="approve(row, true)">审批通过</el-button>
          <el-button v-if="row.approvalStatus === 1 && row.status !== 3" link type="primary" size="small" @click="pay(row)">打款</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialog" title="发起结算" width="560px">
      <el-form :model="form" label-width="110px">
        <el-form-item label="结算类型">
          <el-select v-model="form.type" style="width: 100%">
            <el-option label="按单团" value="group" /><el-option label="按订单" value="order" /><el-option label="按合作方" value="partner" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.type === 'group'" label="排期ID"><el-input-number v-model="form.scheduleId" :min="1" style="width: 100%" /></el-form-item>
        <el-form-item v-if="form.type === 'order'" label="订单ID"><el-input-number v-model="form.orderId" :min="1" style="width: 100%" /></el-form-item>
        <template v-if="form.type === 'partner'">
          <el-form-item label="合作方类型">
            <el-select v-model="form.partnerType" style="width: 100%">
              <el-option label="供应商" value="supplier" /><el-option label="分销商" value="distributor" /><el-option label="导游" value="guide" />
            </el-select>
          </el-form-item>
          <el-form-item label="合作方ID"><el-input-number v-model="form.partnerId" :min="1" style="width: 100%" /></el-form-item>
          <el-form-item label="合作方名称"><el-input v-model="form.partnerName" /></el-form-item>
        </template>
        <el-form-item label="手动金额"><el-input-number v-model="form.amount" :min="0" :precision="2" style="width: 100%" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="create">生成结算单</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api, download } from '@/api/request'

const rows = ref<any[]>([])
const loading = ref(false)
const dialog = ref(false)
const form = reactive<any>({ type: 'group', scheduleId: null, orderId: null, partnerType: 'supplier', partnerId: null, partnerName: '', amount: null })

async function load() {
  loading.value = true
  try {
    const data: any = await api.get('/settlements', { page: 1, size: 100 })
    rows.value = data.records || []
  } finally {
    loading.value = false
  }
}
async function create() {
  await api.post('/settlements', form)
  ElMessage.success('结算单已生成')
  dialog.value = false
  load()
}
async function confirm(row: any) {
  await api.post(`/settlements/${row.id}/confirm`)
  ElMessage.success('已确认')
  load()
}
async function approve(row: any, ok: boolean) {
  await api.post(`/settlements/${row.id}/approve`, null, { approve: ok })
  ElMessage.success('审批完成')
  load()
}
async function pay(row: any) {
  await api.post(`/settlements/${row.id}/pay`, { amount: row.amount })
  ElMessage.success('打款完成')
  load()
}
onMounted(load)
</script>
