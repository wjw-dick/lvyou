<template>
  <div class="page-card">
    <div class="page-header">
      <h2 class="page-title">租户配置</h2>
      <el-button type="primary" @click="save">保存并生效</el-button>
    </div>
    <el-alert type="info" :closable="false" class="mb16"
              title="保存后实时生效；密钥类字段 AES 加密存储，页面回掩码显示。" />
    <el-form :model="form" label-width="150px" style="max-width: 760px">
      <el-form-item label="租户名称"><el-input v-model="form['tenant.name']" /></el-form-item>
      <el-form-item label="Logo"><ImageUpload v-model="form['tenant.logo']" :multiple="false" /></el-form-item>
      <el-form-item label="客服电话"><el-input v-model="form['tenant.service_phone']" /></el-form-item>
      <el-form-item label="小程序 AppID"><el-input v-model="form['tenant.mp_app_id']" /></el-form-item>
      <el-form-item label="出团通知模板">
        <el-input v-model="form['tenant.depart_template']" type="textarea" :rows="3" />
      </el-form-item>
      <el-form-item label="通知开关">
        <el-switch v-model="form['tenant.notify.enabled']" active-value="true" inactive-value="false" />
      </el-form-item>
      <el-form-item label="微信支付商户号"><el-input v-model="form['tenant.wx_mch_id']" /></el-form-item>
      <el-form-item label="微信支付密钥"><el-input v-model="form['tenant.wx_pay_secret']" type="password" show-password /></el-form-item>
      <el-form-item label="OTA 开关">
        <el-switch v-model="form['tenant.ota_enabled']" active-value="true" inactive-value="false" />
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '@/api/request'
import ImageUpload from '@/components/ImageUpload.vue'

const form = reactive<Record<string, any>>({})

async function load() {
  const d: any = await api.get('/system/config')
  Object.assign(form, d)
}
async function save() {
  await api.put('/system/config', form)
  ElMessage.success('配置已保存并生效')
  load()
}
onMounted(load)
</script>
