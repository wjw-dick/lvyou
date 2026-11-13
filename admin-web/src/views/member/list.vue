<template>
  <div>
    <div class="stat-grid mb16">
      <div class="stat-card"><div class="stat-label">会员总数</div><div class="stat-value">{{ analysis.total || 0 }}</div></div>
      <div class="stat-card"><div class="stat-label">近 30 天活跃</div><div class="stat-value">{{ analysis.active || 0 }}</div></div>
      <div class="stat-card"><div class="stat-label">沉默会员</div><div class="stat-value">{{ analysis.silent || 0 }}</div></div>
      <div class="stat-card"><div class="stat-label">等级分布</div>
        <div class="mt8">
          <el-tag v-for="(v, k) in analysis.levels" :key="k" style="margin-right: 4px">L{{ k }}: {{ v }}</el-tag>
        </div>
      </div>
    </div>

    <div class="page-card">
      <div class="page-header">
        <h2 class="page-title">会员列表</h2>
        <el-input v-model="keyword" placeholder="昵称/手机" style="width: 200px" clearable @change="load" />
      </div>
      <el-table :data="rows" stripe v-loading="loading">
        <el-table-column prop="nickname" label="昵称" width="150" />
        <el-table-column prop="unionId" label="UnionID" min-width="190" show-overflow-tooltip />
        <el-table-column prop="phoneMask" label="手机号" width="130" />
        <el-table-column prop="level" label="等级" width="80" />
        <el-table-column prop="points" label="积分" width="90" />
        <el-table-column label="累计消费" width="120">
          <template #default="{ row }">¥{{ Number(row.totalConsume || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="lastActiveAt" label="最近活跃" width="170" />
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row)">详情</el-button>
            <el-button link type="primary" size="small" @click="adjust(row)">调整积分</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="display: flex; justify-content: flex-end; margin-top: 16px">
        <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total" layout="total, prev, pager, next" @change="load" />
      </div>
    </div>

    <el-drawer v-model="drawer" title="会员详情" size="560px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="昵称">{{ detail.member?.nickname }}</el-descriptions-item>
        <el-descriptions-item label="UnionID">{{ detail.member?.unionId }}</el-descriptions-item>
        <el-descriptions-item label="等级">L{{ detail.member?.level }}</el-descriptions-item>
        <el-descriptions-item label="积分">{{ detail.member?.points }}</el-descriptions-item>
        <el-descriptions-item label="累计消费">¥{{ Number(detail.member?.totalConsume || 0).toFixed(2) }}</el-descriptions-item>
        <el-descriptions-item label="优惠券">{{ detail.coupons?.length || 0 }} 张</el-descriptions-item>
      </el-descriptions>
      <h4>消费记录</h4>
      <el-table :data="detail.orders || []" size="small">
        <el-table-column prop="orderNo" label="订单号" width="170" />
        <el-table-column prop="productTitle" label="线路" />
        <el-table-column label="金额" width="100">
          <template #default="{ row }">¥{{ Number(row.paidAmount || 0).toFixed(2) }}</template>
        </el-table-column>
      </el-table>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '@/api/request'

const rows = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const keyword = ref('')
const loading = ref(false)
const analysis = ref<any>({})
const drawer = ref(false)
const detail = reactive<any>({})

async function load() {
  loading.value = true
  try {
    const data: any = await api.get('/members', { page: page.value, size: size.value, keyword: keyword.value })
    rows.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}
async function openDetail(row: any) {
  Object.assign(detail, await api.get(`/members/${row.id}`))
  drawer.value = true
}
async function adjust(row: any) {
  const { value } = await ElMessageBox.prompt('输入积分变动值（正数增加，负数扣减）', '调整积分', { inputValue: '100' })
  await api.put(`/members/${row.id}`, { points: Number(value) })
  ElMessage.success('积分已调整')
  load()
}
onMounted(async () => {
  analysis.value = (await api.get('/members/analysis')) || {}
  load()
})
</script>
