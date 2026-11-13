<template>
  <div class="page-card">
    <div class="page-header"><h2 class="page-title">扫码验票</h2></div>
    <el-row :gutter="16">
      <el-col :span="10">
        <div class="verify-box">
          <el-form label-position="top">
            <el-form-item label="门票二维码 / 票号">
              <el-input v-model="form.code" size="large" placeholder="扫码或输入二维码内容" @keyup.enter="verify" />
            </el-form-item>
            <el-form-item label="闸口">
              <el-select v-model="form.gate" style="width: 100%">
                <el-option label="1号闸口" value="1号闸口" /><el-option label="2号闸口" value="2号闸口" />
                <el-option label="团队通道" value="团队通道" /><el-option label="备用闸口" value="备用闸口" />
              </el-select>
            </el-form-item>
            <el-form-item label="核销方式">
              <el-select v-model="form.verifyType" style="width: 100%">
                <el-option label="二维码" value="qrcode" /><el-option label="身份证" value="idcard" />
                <el-option label="人脸识别" value="face" /><el-option label="NFC" value="nfc" />
              </el-select>
            </el-form-item>
            <el-button type="primary" size="large" style="width: 100%" :loading="verifying" @click="verify">确认验票</el-button>
          </el-form>
          <el-result v-if="result" :icon="result.ok ? 'success' : 'error'" :title="result.title" :sub-title="result.sub" class="mt16" />
        </div>
      </el-col>
      <el-col :span="14">
        <div class="traffic-line">
          <div class="stat-card"><div class="stat-label">今日入园</div><div class="stat-value">{{ traffic.inCount || 0 }}</div></div>
          <div class="stat-card"><div class="stat-label">拥堵预警</div><div class="stat-value">{{ traffic.congestionAlerts || 0 }}</div></div>
        </div>
        <h4>最近验票记录</h4>
        <el-table :data="records" stripe size="small" height="420">
          <el-table-column prop="code" label="核销码" min-width="180" show-overflow-tooltip />
          <el-table-column prop="gate" label="闸口" width="110" />
          <el-table-column prop="operatorName" label="操作人" width="100" />
          <el-table-column prop="verifyAt" label="时间" width="170" />
        </el-table>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { api } from '@/api/request'

const form = reactive({ code: '', gate: '1号闸口', verifyType: 'qrcode' })
const verifying = ref(false)
const result = ref<any>(null)
const records = ref<any[]>([])
const traffic = ref<any>({})

async function verify() {
  if (!form.code) return
  verifying.value = true
  result.value = null
  try {
    const r: any = await api.post('/scenic/verify', form)
    result.value = { ok: true, title: '验票成功', sub: `${r.visitor || ''} · ${r.gate} · 第 ${r.verifyCount} 次核销` }
    form.code = ''
    loadRecords()
  } catch (e: any) {
    result.value = { ok: false, title: '验票失败', sub: e?.message || '门票无效或已核销' }
  } finally {
    verifying.value = false
  }
}
async function loadRecords() {
  const d: any = await api.get('/scenic/verify-records', { page: 1, size: 50 })
  records.value = d.records || []
  traffic.value = (await api.get('/scenic/traffic')) || {}
}
onMounted(loadRecords)
</script>

<style scoped>
.verify-box { background: #fafcff; border: 1px solid #ebeef5; border-radius: 8px; padding: 20px; }
.traffic-line { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 16px; }
</style>
