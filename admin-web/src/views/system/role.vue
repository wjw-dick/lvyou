<template>
  <div class="page-card">
    <div class="page-header">
      <h2 class="page-title">角色权限</h2>
      <el-button type="primary" @click="openAdd">新增角色</el-button>
    </div>
    <el-table :data="roles" stripe v-loading="loading">
      <el-table-column prop="name" label="角色" width="160" />
      <el-table-column prop="code" label="编码" width="150" />
      <el-table-column prop="remark" label="说明" min-width="200" />
      <el-table-column label="菜单权限" width="110">
        <template #default="{ row }">{{ row.menus?.length || 0 }} 项</template>
      </el-table-column>
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="edit(row)">编辑</el-button>
          <el-button v-if="row.code !== 'tenant_admin'" link type="danger" size="small" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialog" :title="form.id ? '编辑角色' : '新增角色'" width="720px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="角色名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="角色编码"><el-input v-model="form.code" :disabled="!!form.id" /></el-form-item>
        <el-form-item label="说明"><el-input v-model="form.remark" /></el-form-item>
        <el-form-item label="菜单权限">
          <el-tree
            ref="treeRef"
            :data="menuTree"
            show-checkbox
            node-key="id"
            :props="{ label: 'title', children: 'children' }"
            default-expand-all
            style="width: 100%; max-height: 380px; overflow: auto; border: 1px solid #ebeef5; padding: 8px; border-radius: 6px"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '@/api/request'

const roles = ref<any[]>([])
const menus = ref<any[]>([])
const loading = ref(false)
const dialog = ref(false)
const treeRef = ref()
const form = reactive<any>({ name: '', code: '', remark: '', menuIds: [] })

const menuTree = ref<any[]>([])
function buildTree() {
  const roots = menus.value.filter((m) => !m.parentId)
  menuTree.value = roots.map((r) => ({ ...r, children: menus.value.filter((m) => m.parentId === r.id) }))
}

async function load() {
  loading.value = true
  try {
    roles.value = (await api.get('/system/roles')) || []
  } finally {
    loading.value = false
  }
}
function openAdd() {
  Object.assign(form, { id: null, name: '', code: '', remark: '', menuIds: [] })
  dialog.value = true
  nextTick(() => treeRef.value?.setCheckedKeys([]))
}
function edit(row: any) {
  Object.assign(form, { id: row.id, name: row.name, code: row.code, remark: row.remark, menuIds: row.menus || [] })
  dialog.value = true
  nextTick(() => treeRef.value?.setCheckedKeys(row.menus || []))
}
async function save() {
  const menuIds = [...treeRef.value.getCheckedKeys(), ...treeRef.value.getHalfCheckedKeys()]
  await api.post('/system/roles', { ...form, menuIds })
  ElMessage.success('已保存')
  dialog.value = false
  load()
}
async function remove(row: any) {
  await ElMessageBox.confirm('确定删除该角色？', '危险操作', { type: 'warning' })
  await api.del(`/system/roles/${row.id}`)
  ElMessage.success('已删除')
  load()
}
onMounted(async () => {
  menus.value = (await api.get('/system/menus')) || []
  buildTree()
  load()
})
</script>
