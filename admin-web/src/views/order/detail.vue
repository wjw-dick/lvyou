<template>
  <div v-loading="loading">
    <div class="page-header">
      <h2 class="page-title">订单详情</h2>
      <div class="flex gap8">
        <el-button @click="$router.back()">返回</el-button>
        <el-button v-if="order.payStatus !== 1" type="success" @click="pay">支付</el-button>
        <el-button v-if="order.status === 2" type="primary" @click="confirm">确认订单</el-button>
        <el-button @click="notify">发送出团通知</el-button>
        <el-button @click="createContract">生成合同</el-button>
      </div>
    </div>

    <el-row :gutter="16">
      <el-col :span="16">
        <div class="page-card mb16">
          <el-descriptions :column="3" border>
            <el-descriptions-item label="订单号">{{ order.orderNo }}</el-descriptions-item>
            <el-descriptions-item label="线路">{{ order.productTitle }}</el-descriptions-item>
            <el-descriptions-item label="出团日期">{{ order.departDate }}</el-descriptions-item>
            <el-descriptions-item label="联系人">{{ order.contactName }}</el-descriptions-item>
            <el-descriptions-item label="手机号">{{ order.contactPhone }}</el-descriptions-item>
            <el-descriptions-item label="人数">{{ order.adultCount }} 成人 / {{ order.childCount }} 儿童</el-descriptions-item>
            <el-descriptions-item label="订单金额">¥{{ money(order.totalAmount) }}</el-descriptions-item>
            <el-descriptions-item label="已付金额">¥{{ money(order.paidAmount) }}</el-descriptions-item>
            <el-descriptions-item label="退款金额">¥{{ money(order.refundAmount) }}</el-descriptions-item>
            <el-descriptions-item label="支付状态">{{ payMap[order.payStatus] }}</el-descriptions-item>
            <el-descriptions-item label="订单状态">{{ statusMap[order.status] }}</el-descriptions-item>
            <el-descriptions-item label="渠道">{{ order.channel }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <div class="page-card mb16">
          <h3 class="page-title">游客名单（证件/手机脱敏）</h3>
          <el-table :data="tourists" stripe size="small">
            <el-table-column prop="name" label="姓名" width="110" />
            <el-table-column prop="idType" label="证件类型" width="110" />
            <el-table-column prop="idNoMask" label="证件号" width="190" />
            <el-table-column prop="phoneMask" label="手机号" width="130" />
            <el-table-column label="类型" width="80">
              <template #default="{ row }">{{ row.isAdult === 1 ? '成人' : '儿童' }}</template>
            </el-table-column>
            <el-table-column label="核销" width="90">
              <template #default="{ row }">
                <el-tag :type="row.verifyStatus === 1 ? 'success' : 'info'" size="small">
                  {{ row.verifyStatus === 1 ? '已核销' : '未核销' }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div class="page-card mb16">
          <h3 class="page-title">状态轨迹</h3>
          <el-timeline>
            <el-timeline-item v-for="l in logs" :key="l.id" :timestamp="l.createdAt" placement="top">
              <b>{{ l.action }}</b> · {{ l.remark }} <span class="muted">（{{ l.operator }}）</span>
            </el-timeline-item>
          </el-timeline>
        </div>
      </el-col>

      <el-col :span="8">
        <div class="page-card mb16">
          <h3 class="page-title">支付流水</h3>
          <div v-for="p in payments" :key="p.id" class="list-row">
            <span>{{ p.type === 'refund' ? '退款' : '支付' }} · {{ p.channel }}</span>
            <span>¥{{ money(p.amount) }}</span>
          </div>
          <el-empty v-if="!payments.length" description="暂无流水" :image-size="60" />
        </div>
        <div class="page-card mb16">
          <h3 class="page-title">退改记录</h3>
          <div v-for="r in refunds" :key="r.id" class="refund-item">
            <div class="flex">
              <span>{{ r.refundNo }}</span>
              <span class="spacer" />
              <el-tag size="small" :type="r.status === 3 ? 'success' : r.status === 2 ? 'danger' : 'warning'">
                {{ refundMap[r.status] }}
              </el-tag>
            </div>
            <div class="muted">¥{{ money(r.amount) }} · {{ r.reason }}</div>
            <div v-if="r.status === 0" class="mt8">
              <el-button size="small" type="success" @click="approve(r, true)">通过并退款</el-button>
              <el-button size="small" type="danger" @click="approve(r, false)">驳回</el-button>
            </div>
          </div>
          <el-empty v-if="!refunds.length" description="暂无退改" :image-size="60" />
        </div>
        <div class="page-card">
          <h3 class="page-title">核销记录</h3>
          <div v-for="v in verifyRecords" :key="v.id" class="list-row">
            <span>{{ v.gate }} · {{ v.operatorName }}</span>
            <span>{{ v.verifyAt }}</span>
          </div>
          <el-empty v-if="!verifyRecords.length" description="暂无核销" :image-size="60" />
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '@/api/request'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const order = ref<any>({})
const tourists = ref<any[]>([])
const logs = ref<any[]>([])
const payments = ref<any[]>([])
const refunds = ref<any[]>([])
const verifyRecords = ref<any[]>([])
const statusMap: Record<string, string> = { 1: '待支付', 2: '待确认', 3: '已成团', 4: '待出团', 5: '已出团', 6: '已核销', 7: '已完结', 8: '已取消' }
const payMap: Record<string, string> = { 0: '未支付', 1: '已支付', 2: '部分退款', 3: '已退款' }
const refundMap: Record<string, string> = { 0: '待审批', 1: '已通过', 2: '已拒绝', 3: '已退款' }
const id = route.params.id as string

function money(v: any) { return Number(v || 0).toFixed(2) }

async function load() {
  loading.value = true
  try {
    const data: any = await api.get(`/orders/${id}`)
    order.value = data.order
    tourists.value = data.tourists || []
    logs.value = data.logs || []
    payments.value = data.payments || []
    refunds.value = data.refunds || []
    verifyRecords.value = data.verifyRecords || []
  } finally {
    loading.value = false
  }
}
async function pay() {
  await api.post(`/orders/${id}/pay`)
  ElMessage.success('支付成功（模拟微信支付）')
  load()
}
async function confirm() {
  await api.post(`/orders/${id}/confirm`)
  ElMessage.success('已确认')
  load()
}
async function notify() {
  await api.post(`/orders/${id}/notify`)
  ElMessage.success('出团通知已推送')
  load()
}
async function approve(r: any, ok: boolean) {
  await ElMessageBox.confirm(ok ? '通过并原路退款？' : '驳回该退改申请？', '审批确认', { type: 'warning' })
  await api.post(`/orders/refunds/${r.id}/approve`, null, { approve: ok })
  ElMessage.success('已处理')
  load()
}
async function createContract() {
  const c: any = await api.post('/contracts', { orderId: Number(id) })
  ElMessage.success(`合同已生成：${c.contractNo}`)
  router.push('/contract/list')
}
onMounted(load)
</script>

<style scoped>
.list-row { display: flex; justify-content: space-between; padding: 6px 0; border-bottom: 1px dashed #ebeef5; font-size: 13px; }
.refund-item { padding: 8px 0; border-bottom: 1px dashed #ebeef5; }
.refund-item .muted { font-size: 12px; }
</style>
