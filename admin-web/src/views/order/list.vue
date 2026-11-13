<template>
  <div class="page-card">
    <div class="page-header">
      <h2 class="page-title">订单管理</h2>
      <div class="flex gap8">
        <el-button @click="download('/orders/tourists/export', {}, '游客名单.xlsx')">导出游客名单</el-button>
        <el-button type="primary" @click="verifyDialog = true">扫码核销</el-button>
      </div>
    </div>

    <el-form inline>
      <el-form-item label="关键词">
        <el-input v-model="query.keyword" clearable placeholder="订单号/联系人/手机" style="width: 200px" />
      </el-form-item>
      <el-form-item label="订单状态">
        <el-select v-model="query.status" clearable style="width: 130px">
          <el-option v-for="(v, k) in statusMap" :key="k" :label="v" :value="Number(k)" />
        </el-select>
      </el-form-item>
      <el-form-item label="支付状态">
        <el-select v-model="query.payStatus" clearable style="width: 130px">
          <el-option label="未支付" :value="0" /><el-option label="已支付" :value="1" />
          <el-option label="部分退款" :value="2" /><el-option label="已退款" :value="3" />
        </el-select>
      </el-form-item>
      <el-button type="primary" @click="load">查询</el-button>
    </el-form>

    <el-table :data="rows" stripe v-loading="loading">
      <el-table-column prop="orderNo" label="订单号" width="190" />
      <el-table-column prop="productTitle" label="线路" min-width="190" show-overflow-tooltip />
      <el-table-column prop="departDate" label="出团日期" width="110" />
      <el-table-column prop="contactName" label="联系人" width="100" />
      <el-table-column prop="contactPhone" label="手机号" width="125" />
      <el-table-column label="人数" width="90">
        <template #default="{ row }">{{ row.adultCount }}大{{ row.childCount }}小</template>
      </el-table-column>
      <el-table-column label="金额" width="110">
        <template #default="{ row }">¥{{ Number(row.paidAmount || row.totalAmount || 0).toFixed(2) }}</template>
      </el-table-column>
      <el-table-column label="支付" width="95">
        <template #default="{ row }">
          <el-tag :type="row.payStatus === 1 ? 'success' : row.payStatus === 3 ? 'danger' : 'info'" size="small">
            {{ payMap[row.payStatus] }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="95">
        <template #default="{ row }">
          <el-tag :type="row.status === 8 ? 'danger' : row.status >= 6 ? 'success' : 'primary'" size="small">
            {{ statusMap[row.status] }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="channel" label="渠道" width="105" />
      <el-table-column label="操作" width="240" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="$router.push(`/order/detail/${row.id}`)">详情</el-button>
          <el-button v-if="row.status === 2" link type="primary" size="small" @click="confirm(row)">确认</el-button>
          <el-button v-if="row.payStatus === 1 && row.status < 6" link type="primary" size="small" @click="notify(row)">出团通知</el-button>
          <el-button v-if="row.payStatus === 1 && row.verifyStatus === 0" link type="success" size="small" @click="verify(row)">核销</el-button>
          <el-button v-if="row.payStatus === 1" link type="warning" size="small" @click="refund(row)">退改</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div style="display: flex; justify-content: flex-end; margin-top: 16px">
      <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total"
                     :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" @change="load" />
    </div>

    <el-dialog v-model="verifyDialog" title="扫码核销" width="420px">
      <el-form label-width="80px">
        <el-form-item label="核销码"><el-input v-model="verifyForm.code" placeholder="订单号/二维码内容" /></el-form-item>
        <el-form-item label="核销入口">
          <el-select v-model="verifyForm.gate" style="width: 100%">
            <el-option label="门店" value="门店" /><el-option label="导游" value="导游" />
            <el-option label="1号闸口" value="1号闸口" /><el-option label="2号闸口" value="2号闸口" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="verifyDialog = false">取消</el-button>
        <el-button type="primary" :loading="verifying" @click="doVerify">确认核销</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="refundDialog" title="退改申请" width="460px">
      <el-form label-width="90px">
        <el-form-item label="订单"><span>{{ refundTarget?.orderNo }}</span></el-form-item>
        <el-form-item label="退款金额">
          <el-input-number v-model="refundForm.amount" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="退款原因"><el-input v-model="refundForm.reason" type="textarea" :rows="3" /></el-form-item>
        <el-alert type="info" :closable="false" title="按退改规则：提前7天100%、3-7天70%、3天内50%、出团后不可退" />
      </el-form>
      <template #footer>
        <el-button @click="refundDialog = false">取消</el-button>
        <el-button type="primary" @click="submitRefund">提交申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api, download } from '@/api/request'

const statusMap: Record<string, string> = {
  1: '待支付', 2: '待确认', 3: '已成团', 4: '待出团', 5: '已出团', 6: '已核销', 7: '已完结', 8: '已取消'
}
const payMap: Record<string, string> = { 0: '未支付', 1: '已支付', 2: '部分退款', 3: '已退款' }
const rows = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const loading = ref(false)
const verifyDialog = ref(false)
const verifying = ref(false)
const refundDialog = ref(false)
const refundTarget = ref<any>(null)
const query = reactive<any>({ keyword: '', status: undefined, payStatus: undefined })
const verifyForm = reactive({ code: '', gate: '门店' })
const refundForm = reactive({ amount: 0, reason: '' })

async function load() {
  loading.value = true
  try {
    const data: any = await api.get('/orders', { ...query, page: page.value, size: size.value })
    rows.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}
async function confirm(row: any) {
  await api.post(`/orders/${row.id}/confirm`)
  ElMessage.success('已确认')
  load()
}
async function notify(row: any) {
  await api.post(`/orders/${row.id}/notify`)
  ElMessage.success('出团通知已推送')
  load()
}
async function verify(row: any) {
  verifyForm.code = row.orderNo
  verifyDialog.value = true
}
async function doVerify() {
  if (!verifyForm.code) return ElMessage.warning('请输入核销码')
  verifying.value = true
  try {
    await api.post('/orders/verify', verifyForm)
    ElMessage.success('核销成功')
    verifyDialog.value = false
    load()
  } finally {
    verifying.value = false
  }
}
async function refund(row: any) {
  refundTarget.value = row
  refundForm.amount = Number(row.paidAmount || 0)
  refundForm.reason = ''
  refundDialog.value = true
}
async function submitRefund() {
  await api.post(`/orders/${refundTarget.value.id}/refund`, refundForm)
  ElMessage.success('退改申请已提交，等待审批')
  refundDialog.value = false
  load()
}
onMounted(load)
</script>
