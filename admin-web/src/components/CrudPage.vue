<template>
  <div class="page-card">
    <div class="page-header">
      <h2 class="page-title">{{ title }}</h2>
      <div class="flex gap8">
        <slot name="header-actions" />
        <el-button v-if="exportUrl" @click="doExport">导出 Excel</el-button>
        <el-button v-if="canAdd" type="primary" @click="openAdd">新增</el-button>
      </div>
    </div>

    <el-form v-if="searchFields.length" inline class="search-bar" @submit.prevent>
      <el-form-item v-for="f in searchFields" :key="f.prop" :label="f.label">
        <el-select v-if="f.type === 'select'" v-model="query[f.prop]" clearable style="width: 150px" placeholder="全部">
          <el-option v-for="o in f.options || []" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
        <el-date-picker
          v-else-if="f.type === 'date'"
          v-model="query[f.prop]"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="选择日期"
          style="width: 150px"
        />
        <el-input v-else v-model="query[f.prop]" clearable placeholder="请输入" style="width: 170px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="load">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="rows" stripe v-loading="loading" size="default">
      <el-table-column
        v-for="c in columns"
        :key="c.prop"
        :prop="c.prop"
        :label="c.label"
        :width="c.width"
        :min-width="c.minWidth || 110"
        show-overflow-tooltip
      >
        <template #default="{ row }">
          <el-tag v-if="c.type === 'tag'" :type="tagType(row[c.prop])" size="small">{{ row[c.prop] }}</el-tag>
          <el-image
            v-else-if="c.type === 'image'"
            :src="row[c.prop]"
            :preview-src-list="[row[c.prop]]"
            fit="cover"
            style="width: 46px; height: 46px; border-radius: 4px"
          />
          <span v-else-if="c.type === 'bool'">{{ row[c.prop] ? '是' : '否' }}</span>
          <span v-else-if="c.type === 'money'">{{ formatMoney(row[c.prop]) }}</span>
          <span v-else-if="c.type === 'date'">{{ row[c.prop] }}</span>
          <span v-else>{{ c.formatter ? c.formatter(row) : row[c.prop] }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button v-for="a in visibleActions(row)" :key="a.label" link :type="a.type || 'primary'" size="small" @click="a.handler(row)">
            {{ a.label }}
          </el-button>
          <el-button v-if="canEdit" link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
          <el-button v-if="canDelete" link type="danger" size="small" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="load"
        @current-change="load"
      />
    </div>

    <el-dialog v-model="dialog" :title="form.id ? '编辑' + title : '新增' + title" width="720px" destroy-on-close>
      <el-form :model="form" label-width="110px">
        <el-row :gutter="16">
          <el-col v-for="f in fields" :key="f.prop" :span="f.span || 12">
            <el-form-item :label="f.label" :required="f.required">
              <el-input v-if="!f.type || f.type === 'input'" v-model="form[f.prop]" :placeholder="f.placeholder || '请输入'" />
              <el-input v-else-if="f.type === 'password'" v-model="form[f.prop]" type="password" show-password placeholder="不修改请留空" />
              <el-input-number v-else-if="f.type === 'number'" v-model="form[f.prop]" :precision="f.precision" :min="0" style="width: 100%" />
              <el-input v-else-if="f.type === 'textarea'" v-model="form[f.prop]" type="textarea" :rows="f.rows || 4" />
              <el-select v-else-if="f.type === 'select'" v-model="form[f.prop]" style="width: 100%" clearable filterable>
                <el-option v-for="o in (f.options || optionsMap[f.prop] || [])" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
              <el-select v-else-if="f.type === 'multiselect'" v-model="form[f.prop]" multiple style="width: 100%" clearable>
                <el-option v-for="o in (f.options || optionsMap[f.prop] || [])" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
              <el-switch v-else-if="f.type === 'switch'" v-model="form[f.prop]" :active-value="1" :inactive-value="0" />
              <el-date-picker
                v-else-if="f.type === 'date'"
                v-model="form[f.prop]"
                type="date"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
              <el-date-picker
                v-else-if="f.type === 'datetime'"
                v-model="form[f.prop]"
                type="datetime"
                value-format="YYYY-MM-DD HH:mm:ss"
                style="width: 100%"
              />
              <ImageUpload v-else-if="f.type === 'images'" v-model="form[f.prop]" :multiple="true" />
              <ImageUpload v-else-if="f.type === 'image'" v-model="form[f.prop]" :multiple="false" />
              <RichEditor v-else-if="f.type === 'rich'" v-model="form[f.prop]" :height="260" />
              <span v-else class="muted">不支持的类型：{{ f.type }}</span>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api, download } from '@/api/request'
import ImageUpload from '@/components/ImageUpload.vue'
import RichEditor from '@/components/RichEditor.vue'

interface Field {
  prop: string
  label: string
  type?: string
  options?: { label: string; value: any }[]
  optionsEndpoint?: string
  optionLabel?: string
  optionValue?: string
  span?: number
  required?: boolean
  placeholder?: string
  precision?: number
  rows?: number
}
interface Column {
  prop: string
  label: string
  width?: number | string
  minWidth?: number
  type?: string
  formatter?: (row: any) => any
}
interface RowAction {
  label: string
  type?: string
  show?: (row: any) => boolean
  handler: (row: any) => void
}

const props = withDefaults(
  defineProps<{
    title: string
    endpoint: string
    columns: Column[]
    fields?: Field[]
    searchFields?: Field[]
    defaults?: Record<string, any>
    canAdd?: boolean
    canEdit?: boolean
    canDelete?: boolean
    rowActions?: RowAction[]
    exportUrl?: string
    extraQuery?: Record<string, any>
    transformRow?: (row: any) => any
    beforeSubmit?: (form: any, isEdit: boolean) => any
  }>(),
  { fields: () => [], searchFields: () => [], defaults: () => ({}), canAdd: true, canEdit: true, canDelete: true, rowActions: () => [] }
)

const rows = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const loading = ref(false)
const saving = ref(false)
const dialog = ref(false)
const form = reactive<any>({})
const query = reactive<any>({})
const optionsMap = reactive<Record<string, any[]>>({})

const searchFields = computed(() => props.searchFields || [])
const fields = computed(() => props.fields || [])

async function load() {
  loading.value = true
  try {
    const data: any = await api.get(props.endpoint, { ...props.extraQuery, ...query, page: page.value, size: size.value })
    const list = Array.isArray(data) ? data : data?.records || []
    rows.value = props.transformRow ? list.map(props.transformRow) : list
    total.value = Array.isArray(data) ? data.length : data?.total || 0
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  Object.keys(query).forEach((k) => delete query[k])
  page.value = 1
  load()
}

function openAdd() {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, JSON.parse(JSON.stringify(props.defaults || {})))
  dialog.value = true
}

function openEdit(row: any) {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, JSON.parse(JSON.stringify(row)))
  dialog.value = true
}

async function save() {
  saving.value = true
  try {
    let payload = { ...form }
    if (props.beforeSubmit) payload = props.beforeSubmit(payload, !!form.id)
    await api.post(props.endpoint, payload)
    ElMessage.success('保存成功')
    dialog.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(row: any) {
  await ElMessageBox.confirm('确定删除该记录？删除后不可恢复。', '危险操作', { type: 'warning' })
  await api.del(`${props.endpoint}/${row.id}`)
  ElMessage.success('删除成功')
  load()
}

function visibleActions(row: any) {
  return (props.rowActions || []).filter((a) => !a.show || a.show(row))
}

function doExport() {
  if (!props.exportUrl) return
  download(props.exportUrl, { ...query }, `${props.title}.xlsx`)
}

function formatMoney(v: any) {
  if (v === null || v === undefined || v === '') return '0.00'
  return Number(v).toFixed(2)
}

function tagType(v: any) {
  const n = Number(v)
  if (n === 1 || n === 2) return 'success'
  if (n === 0) return 'info'
  if (n === 3 || n === 4) return 'warning'
  if (n === 8) return 'danger'
  return 'primary'
}

onMounted(async () => {
  for (const f of fields.value) {
    if (f.optionsEndpoint && !optionsMap[f.prop]) {
      const data: any = await api.get(f.optionsEndpoint, { page: 1, size: 500 })
      const list = Array.isArray(data) ? data : data?.records || []
      optionsMap[f.prop] = list.map((x: any) => ({
        label: x[f.optionLabel || 'name'] ?? x.title,
        value: x[f.optionValue || 'id']
      }))
    }
  }
  load()
})
</script>

<style scoped>
.search-bar { margin-bottom: 8px; }
.pagination { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
