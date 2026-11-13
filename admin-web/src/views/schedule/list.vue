<template>
  <div class="page-card">
    <div class="page-header">
      <h2 class="page-title">出团计划</h2>
      <div class="flex gap8">
        <el-button @click="$router.push('/schedule/calendar')">日历视图</el-button>
        <el-button type="primary" @click="openCreate">新增排期</el-button>
      </div>
    </div>

    <el-form inline>
      <el-form-item label="状态">
        <el-select v-model="query.status" clearable style="width: 130px">
          <el-option v-for="(v, k) in statusMap" :key="k" :label="v" :value="Number(k)" />
        </el-select>
      </el-form-item>
      <el-form-item label="日期">
        <el-date-picker v-model="range" type="daterange" value-format="YYYY-MM-DD" start-placeholder="开始" end-placeholder="结束" />
      </el-form-item>
      <el-button type="primary" @click="load">查询</el-button>
    </el-form>

    <el-table :data="rows" stripe v-loading="loading">
      <el-table-column prop="scheduleNo" label="出团编号" width="180" />
      <el-table-column prop="productTitle" label="线路" min-width="190" show-overflow-tooltip />
      <el-table-column prop="departDate" label="出团日期" width="115" />
      <el-table-column prop="meetingPoint" label="集合地点" min-width="160" show-overflow-tooltip />
      <el-table-column prop="meetingTime" label="集合时间" width="100" />
      <el-table-column prop="guideName" label="导游" width="100" />
      <el-table-column label="收客" width="100">
        <template #default="{ row }">{{ row.sold }}/{{ row.quota }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status >= 4 ? 'info' : row.status === 3 ? 'danger' : row.status === 2 ? 'success' : 'primary'" size="small">
            {{ statusMap[row.status] }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="250" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="assign(row)">指派导游</el-button>
          <el-button link type="primary" size="small" @click="planPdf(row)">计划书</el-button>
          <el-button link type="primary" size="small" @click="changeStatus(row)">状态</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div style="display: flex; justify-content: flex-end; margin-top: 16px">
      <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total"
                     layout="total, prev, pager, next" @change="load" />
    </div>

    <el-dialog v-model="dialog" title="新增排期" width="560px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="线路">
          <el-select v-model="form.productId" filterable style="width: 100%" @change="onProduct">
            <el-option v-for="p in products" :key="p.id" :label="p.title" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="出团日期"><el-date-picker v-model="form.departDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item>
        <el-form-item label="返程日期"><el-date-picker v-model="form.returnDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item>
        <el-form-item label="集合地点"><el-input v-model="form.meetingPoint" /></el-form-item>
        <el-form-item label="集合时间"><el-input v-model="form.meetingTime" placeholder="07:00" /></el-form-item>
        <el-form-item label="名额"><el-input-number v-model="form.quota" :min="1" style="width: 100%" /></el-form-item>
        <el-form-item label="成团人数"><el-input-number v-model="form.minGroup" :min="1" style="width: 100%" /></el-form-item>
        <el-form-item label="预警天数"><el-input-number v-model="form.warnDays" :min="1" style="width: 100%" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="create">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="assignDialog" title="指派导游" width="420px">
      <el-select v-model="assignGuideId" filterable style="width: 100%" placeholder="选择导游">
        <el-option v-for="g in guides" :key="g.id" :label="`${g.name}（${g.level || ''} ${g.phone || ''}）`" :value="g.id" />
      </el-select>
      <template #footer>
        <el-button @click="assignDialog = false">取消</el-button>
        <el-button type="primary" @click="doAssign">确认并推送</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api, download } from '@/api/request'

const statusMap: Record<string, string> = { 1: '收客中', 2: '已成团', 3: '已满', 4: '已出团', 5: '已完结' }
const rows = ref<any[]>([])
const products = ref<any[]>([])
const guides = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const loading = ref(false)
const dialog = ref(false)
const assignDialog = ref(false)
const assignGuideId = ref<number>()
const assignTarget = ref<any>(null)
const range = ref<string[]>([])
const query = reactive<any>({ status: undefined })
const form = reactive<any>({ productId: null, departDate: '', returnDate: '', meetingPoint: '', meetingTime: '07:00', quota: 30, minGroup: 10, warnDays: 3 })

async function load() {
  loading.value = true
  try {
    const data: any = await api.get('/schedules', {
      ...query, page: page.value, size: size.value,
      startDate: range.value?.[0], endDate: range.value?.[1]
    })
    rows.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}
function openCreate() { dialog.value = true }
function onProduct(id: number) {
  const p = products.value.find((x) => x.id === id)
  if (p) { form.quota = p.stock; form.minGroup = p.minGroup; form.meetingPoint = p.meetingPoint }
}
async function create() {
  if (!form.productId || !form.departDate) return ElMessage.warning('请选择线路和出团日期')
  await api.post('/schedules', form)
  ElMessage.success('排期已创建')
  dialog.value = false
  load()
}
async function assign(row: any) {
  assignTarget.value = row
  assignGuideId.value = row.guideId
  assignDialog.value = true
}
async function doAssign() {
  if (!assignGuideId.value) return ElMessage.warning('请选择导游')
  await api.post(`/schedules/${assignTarget.value.id}/guide`, null, { guideId: assignGuideId.value })
  ElMessage.success('已指派并推送导游')
  assignDialog.value = false
  load()
}
async function changeStatus(row: any) {
  const { value } = await ElMessageBox.prompt('输入状态：1收客中 2已成团 3已满 4已出团 5已完结', '变更状态', { inputValue: String(row.status) })
  await api.post(`/schedules/${row.id}/status`, null, { status: Number(value) })
  ElMessage.success('状态已更新')
  load()
}
function planPdf(row: any) {
  download(`/schedules/${row.id}/plan`, {}, `出团计划书-${row.scheduleNo}.pdf`)
}
onMounted(async () => {
  const p: any = await api.get('/products', { page: 1, size: 200 })
  products.value = p.records || []
  guides.value = (await api.get('/guides/all')) || []
  load()
})
</script>
