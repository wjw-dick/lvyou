<template>
  <CrudPage
    title="景区档案"
    endpoint="/scenic/list"
    :can-delete="false"
    :can-add="false"
    :can-edit="false"
    :columns="[
      { prop: 'name', label: '景区', minWidth: 180 },
      { prop: 'level', label: '等级', width: 80 },
      { prop: 'province', label: '省份', width: 110 },
      { prop: 'city', label: '城市', width: 110 },
      { prop: 'phone', label: '客服电话', width: 140 },
      { prop: 'openTime', label: '开放时间', width: 140 },
      { prop: 'status', label: '状态', width: 90, type: 'tag' }
    ]"
    :fields="[
      { prop: 'name', label: '景区名称', required: true },
      { prop: 'level', label: '等级' },
      { prop: 'province', label: '省份' },
      { prop: 'city', label: '城市' },
      { prop: 'address', label: '地址', span: 24 },
      { prop: 'phone', label: '客服电话' },
      { prop: 'openTime', label: '开放时间' },
      { prop: 'status', label: '启用', type: 'switch' },
      { prop: 'cover', label: '封面图', type: 'image', span: 24 },
      { prop: 'intro', label: '景区简介', type: 'rich', span: 24 },
      { prop: 'notice', label: '公告', type: 'textarea', span: 24 },
      { prop: 'strategy', label: '攻略', type: 'rich', span: 24 }
    ]"
    :extra-query="{ page: 1, size: 100 }"
  >
    <template #header-actions>
      <el-button type="primary" @click="openEdit()">新增景区</el-button>
    </template>
  </CrudPage>

  <el-dialog v-model="dialog" :title="form.id ? '编辑景区' : '新增景区'" width="720px">
    <el-form :model="form" label-width="100px">
      <el-row :gutter="12">
        <el-col :span="12"><el-form-item label="景区名称"><el-input v-model="form.name" /></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="等级"><el-input v-model="form.level" /></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="省份"><el-input v-model="form.province" /></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="城市"><el-input v-model="form.city" /></el-form-item></el-col>
        <el-col :span="24"><el-form-item label="地址"><el-input v-model="form.address" /></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="客服电话"><el-input v-model="form.phone" /></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="开放时间"><el-input v-model="form.openTime" /></el-form-item></el-col>
        <el-col :span="24"><el-form-item label="封面图"><ImageUpload v-model="form.cover" :multiple="false" /></el-form-item></el-col>
        <el-col :span="24"><el-form-item label="简介"><RichEditor v-model="form.intro" :height="200" /></el-form-item></el-col>
        <el-col :span="24"><el-form-item label="公告"><el-input v-model="form.notice" type="textarea" :rows="2" /></el-form-item></el-col>
      </el-row>
    </el-form>
    <template #footer>
      <el-button @click="dialog = false">取消</el-button>
      <el-button type="primary" @click="save">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import CrudPage from '@/components/CrudPage.vue'
import ImageUpload from '@/components/ImageUpload.vue'
import RichEditor from '@/components/RichEditor.vue'
import { api } from '@/api/request'

const dialog = ref(false)
const form = reactive<any>({})

function openEdit(row?: any) {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, row || { status: 1 })
  dialog.value = true
}
async function save() {
  await api.post('/scenic', form)
  ElMessage.success('已保存')
  dialog.value = false
  location.reload()
}
</script>
