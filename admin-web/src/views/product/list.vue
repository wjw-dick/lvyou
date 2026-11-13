<template>
  <div class="page-card">
    <div class="page-header">
      <h2 class="page-title">线路产品</h2>
      <div class="flex gap8">
        <el-upload :action="importUrl" :headers="headers" :show-file-list="false" accept=".xlsx,.xls" :on-success="onImport">
          <el-button>批量导入</el-button>
        </el-upload>
        <el-button @click="exportExcel">导出 Excel</el-button>
        <el-button type="primary" @click="$router.push('/product/edit/new')">新增产品</el-button>
      </div>
    </div>

    <el-form inline>
      <el-form-item label="关键词">
        <el-input v-model="query.keyword" clearable placeholder="标题/产品编号" style="width: 190px" />
      </el-form-item>
      <el-form-item label="品类">
        <el-select v-model="query.category" clearable style="width: 130px">
          <el-option v-for="o in categories" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" clearable style="width: 110px">
          <el-option label="上架" :value="1" />
          <el-option label="下架" :value="0" />
        </el-select>
      </el-form-item>
      <el-button type="primary" @click="load">查询</el-button>
      <el-button @click="reset">重置</el-button>
    </el-form>

    <el-table :data="rows" stripe v-loading="loading">
      <el-table-column label="封面" width="80">
        <template #default="{ row }">
          <el-image :src="row.cover" :preview-src-list="row.cover ? [row.cover] : []" fit="cover"
                    style="width: 48px; height: 48px; border-radius: 4px; background: #f5f7fa" />
        </template>
      </el-table-column>
      <el-table-column prop="productNo" label="产品编号" width="170" />
      <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
      <el-table-column prop="category" label="品类" width="90" />
      <el-table-column prop="days" label="天数" width="70" />
      <el-table-column prop="destination" label="目的地" width="110" />
      <el-table-column label="成人价" width="100">
        <template #default="{ row }">¥{{ Number(row.adultPrice || 0).toFixed(2) }}</template>
      </el-table-column>
      <el-table-column label="库存/已售" width="110">
        <template #default="{ row }">{{ row.stock }} / {{ row.sold }}</template>
      </el-table-column>
      <el-table-column label="渠道" min-width="150">
        <template #default="{ row }">
          <el-tag v-for="c in (row.channels || '').split(',').filter(Boolean)" :key="c" size="small" style="margin-right: 4px">{{ c }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? '上架' : '下架' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="$router.push(`/product/edit/${row.id}`)">编辑</el-button>
          <el-button link type="primary" size="small" @click="toggle(row)">{{ row.status === 1 ? '下架' : '上架' }}</el-button>
          <el-button link type="primary" size="small" @click="channels(row)">渠道</el-button>
          <el-button link type="danger" size="small" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div style="display: flex; justify-content: flex-end; margin-top: 16px">
      <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total"
                     :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" @change="load" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api, download } from '@/api/request'

const categories = [
  { label: '跟团游', value: 'line' }, { label: '签证', value: 'visa' }, { label: '机票', value: 'flight' },
  { label: '酒店', value: 'hotel' }, { label: '门票', value: 'ticket' }, { label: '租车', value: 'car' },
  { label: '邮轮', value: 'cruise' }, { label: '特产商品', value: 'goods' }
]
const rows = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const loading = ref(false)
const query = reactive<any>({ keyword: '', category: '', status: undefined })
const importUrl = '/api/products/import'
const headers = computed(() => ({ Authorization: `Bearer ${localStorage.getItem('tp_token')}` }))

async function load() {
  loading.value = true
  try {
    const data: any = await api.get('/products', { ...query, page: page.value, size: size.value })
    rows.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}
function reset() {
  query.keyword = ''
  query.category = ''
  query.status = undefined
  page.value = 1
  load()
}
async function toggle(row: any) {
  await api.post(`/products/${row.id}/status`, null, { status: row.status === 1 ? 0 : 1 })
  ElMessage.success('状态已更新')
  load()
}
async function channels(row: any) {
  const { value } = await ElMessageBox.prompt('请输入上架渠道，逗号分隔（miniprogram,mp,h5,app,ota）', '多渠道上架', {
    inputValue: row.channels || 'miniprogram,mp,h5,app'
  })
  await api.post(`/products/${row.id}/channels`, value.split(',').map((s) => s.trim()))
  ElMessage.success('渠道已更新')
  load()
}
async function remove(row: any) {
  await ElMessageBox.confirm(`确定删除「${row.title}」？`, '危险操作', { type: 'warning' })
  await api.del(`/products/${row.id}`)
  ElMessage.success('已删除')
  load()
}
function exportExcel() {
  download('/products/export', {}, '线路产品.xlsx')
}
function onImport(res: any) {
  if (res.code === 0) {
    ElMessage.success(`导入成功 ${res.data.success}/${res.data.total}`)
    load()
  }
}
onMounted(load)
</script>
