<template>
  <div class="page-card">
    <div class="page-header">
      <h2 class="page-title">平台参数</h2>
      <el-button type="primary" @click="save">保存并生效</el-button>
    </div>
    <el-table :data="rows" stripe>
      <el-table-column prop="paramKey" label="参数键" width="220" />
      <el-table-column label="参数值" min-width="220">
        <template #default="{ row }">
          <el-switch v-if="row.valueType === 'bool'" v-model="row.paramValue" active-value="true" inactive-value="false" />
          <el-input v-else v-model="row.paramValue" />
        </template>
      </el-table-column>
      <el-table-column prop="valueType" label="类型" width="100" />
      <el-table-column prop="remark" label="说明" min-width="220" />
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '@/api/request'

const rows = ref<any[]>([])
async function load() { rows.value = (await api.get('/platform/params')) || [] }
async function save() {
  await api.put('/platform/params', rows.value)
  ElMessage.success('平台参数已保存并生效')
  load()
}
onMounted(load)
</script>
