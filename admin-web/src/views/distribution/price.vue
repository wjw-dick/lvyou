<template>
  <div class="page-card">
    <div class="page-header">
      <h2 class="page-title">分销报价</h2>
      <div class="flex gap8">
        <el-select v-model="distributorId" filterable placeholder="选择分销商" style="width: 240px" @change="load">
          <el-option v-for="d in distributors" :key="d.id" :label="d.name" :value="d.id" />
        </el-select>
        <el-button type="primary" :disabled="!distributorId" @click="addPrice">新增报价</el-button>
      </div>
    </div>
    <el-table :data="rows" stripe v-loading="loading">
      <el-table-column label="线路" min-width="220">
        <template #default="{ row }">{{ row.productTitle }}</template>
      </el-table-column>
      <el-table-column label="分销商价" width="160">
        <template #default="{ row }">
          <el-input-number v-model="row.price.adultPrice" :precision="2" :min="0" size="small" />
        </template>
      </el-table-column>
      <el-table-column label="儿童价" width="160">
        <template #default="{ row }">
          <el-input-number v-model="row.price.childPrice" :precision="2" :min="0" size="small" />
        </template>
      </el-table-column>
      <el-table-column label="标准成人价" width="130">
        <template #default="{ row }">¥{{ Number(row.adultPrice || 0).toFixed(2) }}</template>
      </el-table-column>
    </el-table>
    <div class="mt16">
      <el-button type="primary" :disabled="!distributorId" @click="saveAll">保存报价</el-button>
    </div>

    <el-dialog v-model="dialog" title="新增报价" width="480px">
      <el-form label-width="90px">
        <el-form-item label="线路">
          <el-select v-model="form.productId" filterable style="width: 100%">
            <el-option v-for="p in products" :key="p.id" :label="`${p.title}（标准价¥${p.adultPrice}）`" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="成人价"><el-input-number v-model="form.adultPrice" :min="0" :precision="2" style="width: 100%" /></el-form-item>
        <el-form-item label="儿童价"><el-input-number v-model="form.childPrice" :min="0" :precision="2" style="width: 100%" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="saveNew">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '@/api/request'

const distributors = ref<any[]>([])
const products = ref<any[]>([])
const distributorId = ref<number>()
const rows = ref<any[]>([])
const loading = ref(false)
const dialog = ref(false)
const form = reactive<any>({ productId: null, adultPrice: 0, childPrice: 0 })

async function load() {
  if (!distributorId.value) return
  loading.value = true
  try {
    rows.value = (await api.get(`/distributors/${distributorId.value}/prices`)) || []
  } finally {
    loading.value = false
  }
}
function addPrice() { dialog.value = true }
async function saveNew() {
  await api.post(`/distributors/${distributorId.value}/prices`, [form])
  ElMessage.success('已保存')
  dialog.value = false
  load()
}
async function saveAll() {
  await api.post(`/distributors/${distributorId.value}/prices`, rows.value.map((r) => ({
    productId: r.price.productId, adultPrice: r.price.adultPrice, childPrice: r.price.childPrice
  })))
  ElMessage.success('报价已保存')
  load()
}
onMounted(async () => {
  const d: any = await api.get('/distributors', { page: 1, size: 200 })
  distributors.value = d.records || []
  const p: any = await api.get('/products', { page: 1, size: 200 })
  products.value = p.records || []
})
</script>
