<template>
  <div class="page-card">
    <div class="page-header">
      <h2 class="page-title">票务库存 / 分时限流</h2>
      <el-button type="primary" @click="openAdd">配置库存</el-button>
    </div>
    <el-form inline>
      <el-form-item label="门票">
        <el-select v-model="ticketId" filterable style="width: 240px" @change="load">
          <el-option v-for="t in tickets" :key="t.ticket.id" :label="t.ticket.name" :value="t.ticket.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="日期">
        <el-date-picker v-model="range" type="daterange" value-format="YYYY-MM-DD" @change="load" />
      </el-form-item>
    </el-form>
    <el-table :data="rows" stripe v-loading="loading">
      <el-table-column prop="date" label="日期" width="130" />
      <el-table-column prop="timeSlot" label="时段" width="150" />
      <el-table-column prop="stock" label="库存" width="100" />
      <el-table-column prop="sold" label="已售" width="100" />
      <el-table-column prop="reserved" label="团队预留" width="110" />
      <el-table-column prop="left" label="剩余" width="100" />
    </el-table>

    <el-dialog v-model="dialog" title="配置分时库存" width="480px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="日期"><el-date-picker v-model="form.stockDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item>
        <el-form-item label="时段"><el-input v-model="form.timeSlot" placeholder="如 08:00-12:00，默认 all" /></el-form-item>
        <el-form-item label="库存"><el-input-number v-model="form.stock" :min="0" style="width: 100%" /></el-form-item>
        <el-form-item label="团队预留"><el-input-number v-model="form.reserved" :min="0" style="width: 100%" /></el-form-item>
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

const tickets = ref<any[]>([])
const ticketId = ref<number>()
const range = ref<string[]>([])
const rows = ref<any[]>([])
const loading = ref(false)
const dialog = ref(false)
const form = reactive<any>({ stockDate: '', timeSlot: 'all', stock: 1000, reserved: 0 })

async function load() {
  if (!ticketId.value) return
  loading.value = true
  try {
    rows.value = (await api.get('/scenic/stock', {
      ticketId: ticketId.value, startDate: range.value?.[0], endDate: range.value?.[1]
    })) || []
  } finally {
    loading.value = false
  }
}
function openAdd() { dialog.value = true }
async function save() {
  await api.post('/scenic/stock', { ...form, ticketId: ticketId.value })
  ElMessage.success('已保存')
  dialog.value = false
  load()
}
onMounted(async () => {
  tickets.value = (await api.get('/scenic/tickets')) || []
  ticketId.value = tickets.value[0]?.ticket?.id
  load()
})
</script>
