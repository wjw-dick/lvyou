<template>
  <div class="page-card">
    <div class="page-header"><h2 class="page-title">带团安排</h2></div>
    <el-table :data="rows" stripe v-loading="loading">
      <el-table-column prop="guideName" label="导游" width="110" />
      <el-table-column prop="scheduleNo" label="出团编号" width="180" />
      <el-table-column prop="productTitle" label="线路" min-width="200" show-overflow-tooltip />
      <el-table-column prop="departDate" label="出团日期" width="115" />
      <el-table-column prop="pushTime" label="推送时间" width="170" />
      <el-table-column label="确认状态" width="110">
        <template #default="{ row }">
          <el-tag size="small" :type="row.status === 1 ? 'success' : row.status === 2 ? 'danger' : 'warning'">
            {{ row.status === 1 ? '已确认' : row.status === 2 ? '已拒绝' : '待确认' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button v-if="row.status === 0" link type="success" size="small" @click="confirm(row, true)">确认</el-button>
          <el-button v-if="row.status === 0" link type="danger" size="small" @click="confirm(row, false)">拒绝</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div style="display: flex; justify-content: flex-end; margin-top: 16px">
      <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total" layout="total, prev, pager, next" @change="load" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '@/api/request'

const rows = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const data: any = await api.get('/guide/assignments', { page: page.value, size: size.value })
    rows.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}
async function confirm(row: any, accept: boolean) {
  await api.post(`/guide/assignments/${row.id}/confirm`, null, { accept })
  ElMessage.success('已处理')
  load()
}
onMounted(load)
</script>
