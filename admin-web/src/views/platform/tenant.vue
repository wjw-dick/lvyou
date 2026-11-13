<template>
  <div class="page-card">
    <div class="page-header">
      <h2 class="page-title">租户管理</h2>
      <el-button type="primary" @click="openAdd">开通租户</el-button>
    </div>
    <el-form inline>
      <el-form-item label="关键词">
        <el-input v-model="keyword" clearable placeholder="名称/编码" style="width: 200px" @keyup.enter="load" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="status" clearable style="width: 130px">
          <el-option label="试用" :value="1" /><el-option label="正常" :value="2" />
          <el-option label="停用" :value="3" /><el-option label="到期" :value="4" />
        </el-select>
      </el-form-item>
      <el-button type="primary" @click="load">查询</el-button>
    </el-form>

    <el-table :data="rows" stripe v-loading="loading">
      <el-table-column prop="code" label="编码" width="90" />
      <el-table-column prop="name" label="旅行社" min-width="190" />
      <el-table-column prop="province" label="省份" width="110" />
      <el-table-column prop="city" label="城市" width="110" />
      <el-table-column prop="contact" label="联系人" width="100" />
      <el-table-column prop="phone" label="电话" width="130" />
      <el-table-column prop="expireTime" label="到期时间" width="170" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag size="small" :type="row.status === 2 ? 'success' : row.status === 4 ? 'danger' : row.status === 3 ? 'info' : 'warning'">
            {{ ['', '试用', '正常', '停用', '到期'][row.status] }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="配额（用户/产品/订单）" width="190">
        <template #default="{ row }">{{ row.quotaUser }} / {{ row.quotaProduct }} / {{ row.quotaOrder }}</template>
      </el-table-column>
      <el-table-column label="操作" width="250" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="view(row)">下钻只读</el-button>
          <el-button link type="primary" size="small" @click="edit(row)">编辑</el-button>
          <el-button link type="primary" size="small" @click="renew(row)">续期</el-button>
          <el-button link :type="row.status === 3 ? 'success' : 'danger'" size="small" @click="toggle(row)">
            {{ row.status === 3 ? '启用' : '停用' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialog" :title="form.id ? '编辑租户' : '开通租户'" width="620px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="租户编码"><el-input v-model="form.code" :disabled="!!form.id" placeholder="如 HLJ" /></el-form-item>
        <el-form-item label="旅行社名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="省份/城市">
          <el-input v-model="form.province" style="width: 48%" />
          <el-input v-model="form.city" style="width: 48%; margin-left: 4%" />
        </el-form-item>
        <el-form-item label="联系人"><el-input v-model="form.contact" /></el-form-item>
        <el-form-item label="电话"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="开通月数"><el-input-number v-model="form.months" :min="1" :max="120" /></el-form-item>
        <el-row :gutter="8">
          <el-col :span="8"><el-form-item label="用户配额"><el-input-number v-model="form.quotaUser" :min="1" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="产品配额"><el-input-number v-model="form.quotaProduct" :min="1" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="订单配额"><el-input-number v-model="form.quotaOrder" :min="1" style="width: 100%" /></el-form-item></el-col>
        </el-row>
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '@/api/request'
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'

const rows = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const keyword = ref('')
const status = ref<number>()
const loading = ref(false)
const dialog = ref(false)
const form = reactive<any>({})
const store = useAuthStore()
const router = useRouter()

async function load() {
  loading.value = true
  try {
    const data: any = await api.get('/platform/tenants', { page: page.value, size: size.value, keyword: keyword.value, status: status.value })
    rows.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}
function openAdd() {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, { months: 12, quotaUser: 20, quotaProduct: 200, quotaOrder: 100000, quotaStorage: 10737418240 })
  dialog.value = true
}
function edit(row: any) {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, row)
  dialog.value = true
}
async function save() {
  if (form.id) await api.put(`/platform/tenants/${form.id}`, form)
  else await api.post('/platform/tenants', form)
  ElMessage.success('已保存')
  dialog.value = false
  load()
}
async function toggle(row: any) {
  await ElMessageBox.confirm(row.status === 3 ? '确定启用该租户？' : '停用后该租户全部用户不可登录，数据保留。确定？', '二次确认', { type: 'warning' })
  await api.post(`/platform/tenants/${row.id}/status`, null, { status: row.status === 3 ? 2 : 3 })
  ElMessage.success('已处理')
  load()
}
async function renew(row: any) {
  const { value } = await ElMessageBox.prompt('续期月数', '租户续期', { inputValue: '12' })
  await api.post(`/platform/tenants/${row.id}/renew`, null, { months: Number(value) })
  ElMessage.success('已续期')
  load()
}
function view(row: any) {
  store.setViewTenant(String(row.id), row.name)
  ElMessage.success(`已切换为「${row.name}」只读视图`)
  router.push('/dashboard')
}
onMounted(load)
</script>
