<template>
  <div class="page-card">
    <div class="page-header">
      <h2 class="page-title">社区内容审核</h2>
      <el-button @click="openReports">举报处理</el-button>
    </div>
    <el-table :data="rows" stripe v-loading="loading">
      <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
      <el-table-column prop="type" label="类型" width="100" />
      <el-table-column label="图片" width="150">
        <template #default="{ row }">
          <el-image v-for="(u, i) in (row.images || '').split(',').filter(Boolean).slice(0, 3)" :key="i" :src="u"
                    :preview-src-list="(row.images || '').split(',').filter(Boolean)" fit="cover"
                    style="width: 34px; height: 34px; margin-right: 2px; border-radius: 3px" />
        </template>
      </el-table-column>
      <el-table-column prop="likes" label="点赞" width="80" />
      <el-table-column prop="comments" label="评论" width="80" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag size="small" :type="row.status === 1 ? 'success' : row.status === 2 ? 'danger' : 'warning'">
            {{ row.status === 1 ? '已通过' : row.status === 2 ? '已拒绝' : '待审核' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button v-if="row.status === 0" link type="success" size="small" @click="audit(row, true)">通过</el-button>
          <el-button v-if="row.status === 0" link type="danger" size="small" @click="audit(row, false)">拒绝</el-button>
          <el-button link type="danger" size="small" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="reportDialog" title="举报处理" width="720px">
      <el-table :data="reports" size="small">
        <el-table-column label="内容" min-width="160">
          <template #default="{ row }">{{ row.postTitle }}</template>
        </el-table-column>
        <el-table-column label="原因" min-width="160">
          <template #default="{ row }">{{ row.report.reason }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">{{ ['待处理', '已忽略', '已下架'][row.report.status] }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button link type="danger" size="small" @click="handleReport(row, true)">下架内容</el-button>
            <el-button link size="small" @click="handleReport(row, false)">忽略</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '@/api/request'

const rows = ref<any[]>([])
const reports = ref<any[]>([])
const loading = ref(false)
const reportDialog = ref(false)

async function load() {
  loading.value = true
  try {
    const data: any = await api.get('/ugc/posts', { page: 1, size: 100 })
    rows.value = data.records || []
  } finally {
    loading.value = false
  }
}
async function audit(row: any, approve: boolean) {
  await api.post(`/ugc/posts/${row.id}/audit`, null, { approve })
  ElMessage.success('已处理')
  load()
}
async function remove(row: any) {
  await ElMessageBox.confirm('确定删除该内容？', '危险操作', { type: 'warning' })
  await api.del(`/ugc/posts/${row.id}`)
  ElMessage.success('已删除')
  load()
}
async function openReports() {
  reports.value = (await api.get('/ugc/reports')) || []
  reportDialog.value = true
}
async function handleReport(row: any, takeDown: boolean) {
  await api.post(`/ugc/reports/${row.report.id}/handle`, null, { takeDown })
  ElMessage.success('已处理')
  openReports()
  load()
}
onMounted(load)
</script>
