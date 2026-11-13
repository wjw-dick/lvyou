<template>
  <div class="page-card">
    <div class="page-header"><h2 class="page-title">定制游需求</h2></div>
    <el-table :data="rows" stripe v-loading="loading">
      <el-table-column label="目的地" width="130">
        <template #default="{ row }">{{ row.request.destination }}</template>
      </el-table-column>
      <el-table-column label="天数/人数" width="110">
        <template #default="{ row }">{{ row.request.days }}天 / {{ row.request.people }}人</template>
      </el-table-column>
      <el-table-column label="预算" width="110">
        <template #default="{ row }">¥{{ Number(row.request.budget || 0).toFixed(0) }}</template>
      </el-table-column>
      <el-table-column label="联系人" width="140">
        <template #default="{ row }">{{ row.request.contactName }} {{ row.request.contactPhone }}</template>
      </el-table-column>
      <el-table-column label="需求" min-width="200" show-overflow-tooltip>
        <template #default="{ row }">{{ row.request.requirement }}</template>
      </el-table-column>
      <el-table-column label="状态" width="105">
        <template #default="{ row }">
          <el-tag size="small" :type="row.request.status === 2 ? 'success' : row.request.status === 1 ? 'primary' : 'warning'">
            {{ ['待处理', '已报价', '已成单', '已关闭'][row.request.status] }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180">
        <template #default="{ row }">
          <el-button v-if="row.request.status === 0" link type="primary" size="small" @click="openQuote(row)">报价</el-button>
          <template v-for="q in row.quotes" :key="q.id">
            <el-button v-if="q.status === 0" link type="success" size="small" @click="accept(q)">接受报价 ¥{{ q.amount }}</el-button>
          </template>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialog" title="定制报价" width="500px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="报价金额"><el-input-number v-model="form.amount" :min="0" :precision="2" style="width: 100%" /></el-form-item>
        <el-form-item label="方案说明"><el-input v-model="form.content" type="textarea" :rows="5" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="submitQuote">提交报价</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '@/api/request'

const rows = ref<any[]>([])
const loading = ref(false)
const dialog = ref(false)
const target = ref<any>(null)
const form = reactive<any>({ amount: 0, content: '' })

async function load() {
  loading.value = true
  try {
    rows.value = (await api.get('/custom/requests')) || []
  } finally {
    loading.value = false
  }
}
function openQuote(row: any) {
  target.value = row
  dialog.value = true
}
async function submitQuote() {
  await api.post(`/custom/requests/${target.value.request.id}/quote`, form)
  ElMessage.success('报价已提交')
  dialog.value = false
  load()
}
async function accept(q: any) {
  await api.post(`/custom/quotes/${q.id}/accept`)
  ElMessage.success('已接受报价，可转为定制订单')
  load()
}
onMounted(load)
</script>
