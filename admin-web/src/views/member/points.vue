<template>
  <div class="page-card">
    <div class="page-header">
      <h2 class="page-title">积分商城</h2>
      <el-button type="primary" @click="openAdd">新增积分商品</el-button>
    </div>
    <el-tabs v-model="tab">
      <el-tab-pane label="积分商品" name="goods">
        <el-table :data="goods" stripe v-loading="loading">
          <el-table-column prop="name" label="商品" min-width="200" />
          <el-table-column prop="type" label="类型" width="100" />
          <el-table-column prop="points" label="所需积分" width="110" />
          <el-table-column prop="stock" label="库存" width="90" />
          <el-table-column label="状态" width="90">
            <template #default="{ row }"><el-tag size="small" :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '上架' : '下架' }}</el-tag></template>
          </el-table-column>
          <el-table-column label="操作" width="100">
            <template #default="{ row }"><el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button></template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="兑换记录" name="exchanges">
        <el-table :data="exchanges" stripe>
          <el-table-column prop="goodName" label="商品" min-width="180" />
          <el-table-column prop="memberName" label="会员" width="140" />
          <el-table-column prop="points" label="积分" width="100" />
          <el-table-column prop="createdAt" label="兑换时间" width="180" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }"><el-tag size="small" :type="row.status === 1 ? 'success' : 'warning'">{{ row.status === 1 ? '已核销' : '待核销' }}</el-tag></template>
          </el-table-column>
          <el-table-column label="操作" width="100">
            <template #default="{ row }">
              <el-button v-if="row.status === 0" link type="primary" size="small" @click="verify(row)">核销</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="dialog" :title="form.id ? '编辑积分商品' : '新增积分商品'" width="520px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="商品名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.type" style="width: 100%">
            <el-option label="门票" value="ticket" /><el-option label="特产" value="goods" /><el-option label="优惠券" value="coupon" />
          </el-select>
        </el-form-item>
        <el-form-item label="所需积分"><el-input-number v-model="form.points" :min="1" style="width: 100%" /></el-form-item>
        <el-form-item label="库存"><el-input-number v-model="form.stock" :min="0" style="width: 100%" /></el-form-item>
        <el-form-item label="图片"><ImageUpload v-model="form.image" :multiple="false" /></el-form-item>
        <el-form-item label="上架"><el-switch v-model="form.status" :active-value="1" :inactive-value="0" /></el-form-item>
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
import ImageUpload from '@/components/ImageUpload.vue'

const tab = ref('goods')
const goods = ref<any[]>([])
const exchanges = ref<any[]>([])
const loading = ref(false)
const dialog = ref(false)
const form = reactive<any>({ name: '', type: 'goods', points: 100, stock: 10, image: '', status: 1 })

async function load() {
  loading.value = true
  try {
    goods.value = (await api.get('/points/goods')) || []
    exchanges.value = (await api.get('/points/exchanges')) || []
  } finally {
    loading.value = false
  }
}
function openAdd() {
  Object.assign(form, { id: null, name: '', type: 'goods', points: 100, stock: 10, image: '', status: 1 })
  dialog.value = true
}
function openEdit(row: any) {
  Object.assign(form, row)
  dialog.value = true
}
async function save() {
  await api.post('/points/goods', form)
  ElMessage.success('已保存')
  dialog.value = false
  load()
}
async function verify(row: any) {
  await api.post(`/points/exchanges/${row.id}/verify`)
  ElMessage.success('已核销')
  load()
}
onMounted(load)
</script>
