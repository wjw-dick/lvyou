<template>
  <div class="page-card">
    <div class="page-header">
      <h2 class="page-title">门票产品</h2>
      <el-button type="primary" @click="openAdd">新增门票</el-button>
    </div>
    <el-table :data="rows" stripe v-loading="loading">
      <el-table-column label="景区" width="150">
        <template #default="{ row }">{{ row.scenicName }}</template>
      </el-table-column>
      <el-table-column label="门票名称" min-width="180">
        <template #default="{ row }">{{ row.ticket.name }}</template>
      </el-table-column>
      <el-table-column label="类型" width="100">
        <template #default="{ row }">{{ typeMap[row.ticket.type] }}</template>
      </el-table-column>
      <el-table-column label="成人价" width="100">
        <template #default="{ row }">¥{{ Number(row.ticket.price || 0).toFixed(2) }}</template>
      </el-table-column>
      <el-table-column label="儿童价" width="100">
        <template #default="{ row }">¥{{ Number(row.ticket.childPrice || 0).toFixed(2) }}</template>
      </el-table-column>
      <el-table-column label="团队价" width="100">
        <template #default="{ row }">¥{{ Number(row.ticket.teamPrice || 0).toFixed(2) }}</template>
      </el-table-column>
      <el-table-column label="有效天数" width="95">
        <template #default="{ row }">{{ row.ticket.validDays }}</template>
      </el-table-column>
      <el-table-column label="日限流" width="95">
        <template #default="{ row }">{{ row.ticket.dailyLimit }}</template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }"><el-tag size="small" :type="row.ticket.status === 1 ? 'success' : 'info'">{{ row.ticket.status === 1 ? '在售' : '停售' }}</el-tag></template>
      </el-table-column>
      <el-table-column label="操作" width="110">
        <template #default="{ row }"><el-button link type="primary" size="small" @click="openEdit(row.ticket)">编辑</el-button></template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialog" :title="form.id ? '编辑门票' : '新增门票'" width="620px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="所属景区">
          <el-select v-model="form.scenicId" style="width: 100%">
            <el-option v-for="s in scenics" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="门票名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="票种">
          <el-select v-model="form.type" style="width: 100%">
            <el-option label="单票" value="single" /><el-option label="联票" value="combo" />
            <el-option label="年卡" value="annual" /><el-option label="团队票" value="team" /><el-option label="优待票" value="preferential" />
          </el-select>
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="8"><el-form-item label="成人价"><el-input-number v-model="form.price" :precision="2" :min="0" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="儿童价"><el-input-number v-model="form.childPrice" :precision="2" :min="0" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="团队价"><el-input-number v-model="form.teamPrice" :precision="2" :min="0" style="width: 100%" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="有效天数"><el-input-number v-model="form.validDays" :min="1" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="日限流"><el-input-number v-model="form.dailyLimit" :min="0" style="width: 100%" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="退改规则"><el-input v-model="form.refundRule" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="核销规则"><el-input v-model="form.verifyRule" type="textarea" :rows="2" placeholder="一次性核销/时间窗/闸口绑定" /></el-form-item>
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

const typeMap: Record<string, string> = { single: '单票', combo: '联票', annual: '年卡', team: '团队票', preferential: '优待票' }
const rows = ref<any[]>([])
const scenics = ref<any[]>([])
const loading = ref(false)
const dialog = ref(false)
const form = reactive<any>({ scenicId: null, name: '', type: 'single', price: 0, childPrice: 0, teamPrice: 0, validDays: 1, dailyLimit: 0, status: 1 })

async function load() {
  loading.value = true
  try {
    rows.value = (await api.get('/scenic/tickets')) || []
  } finally {
    loading.value = false
  }
}
function openAdd() {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, { scenicId: scenics.value[0]?.id, name: '', type: 'single', price: 0, childPrice: 0, teamPrice: 0, validDays: 1, dailyLimit: 0, status: 1 })
  dialog.value = true
}
function openEdit(t: any) {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, t)
  dialog.value = true
}
async function save() {
  await api.post('/scenic/tickets', form)
  ElMessage.success('已保存')
  dialog.value = false
  load()
}
onMounted(async () => {
  scenics.value = (await api.get('/scenic/all')) || []
  load()
})
</script>
