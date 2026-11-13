<template>
  <div class="page-card">
    <div class="page-header">
      <h2 class="page-title">数据字典</h2>
      <el-tag type="info">全局标准字典由平台维护，租户仅可扩展自定义项</el-tag>
    </div>
    <el-tabs v-model="tab">
      <el-tab-pane label="全局标准字典" name="global">
        <el-table :data="global" stripe size="small" max-height="600">
          <el-table-column prop="dictType" label="字典类型" width="180" />
          <el-table-column prop="dictLabel" label="名称" width="180" />
          <el-table-column prop="dictValue" label="值" width="150" />
          <el-table-column prop="sort" label="排序" width="80" />
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="租户自定义" name="custom">
        <div class="mb16">
          <el-button type="primary" @click="dialog = true">新增自定义字典</el-button>
        </div>
        <el-table :data="custom" stripe size="small">
          <el-table-column prop="dictType" label="字典类型" width="180" />
          <el-table-column prop="label" label="名称" width="180" />
          <el-table-column prop="value" label="值" width="150" />
          <el-table-column prop="sort" label="排序" width="80" />
          <el-table-column label="操作" width="120">
            <template #default="{ row }"><el-button link type="danger" size="small" @click="remove(row)">删除</el-button></template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="dialog" title="新增自定义字典" width="480px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="字典类型"><el-input v-model="form.dictType" placeholder="如 destination" /></el-form-item>
        <el-form-item label="名称"><el-input v-model="form.label" /></el-form-item>
        <el-form-item label="值"><el-input v-model="form.value" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" /></el-form-item>
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

const tab = ref('global')
const global = ref<any[]>([])
const custom = ref<any[]>([])
const dialog = ref(false)
const form = reactive<any>({ dictType: '', label: '', value: '', sort: 0 })

async function load() {
  const d: any = await api.get('/system/dict')
  global.value = d.global || []
  custom.value = d.custom || []
}
async function save() {
  await api.post('/system/dict', form)
  ElMessage.success('已保存')
  dialog.value = false
  load()
}
async function remove(row: any) {
  await api.del(`/system/dict/${row.id}`)
  ElMessage.success('已删除')
  load()
}
onMounted(load)
</script>
