<template>
  <div class="page-card">
    <div class="page-header">
      <h2 class="page-title">对象存储配置</h2>
      <el-tag v-if="activeDriver" type="success">当前驱动：{{ activeDriver }}</el-tag>
    </div>

    <el-row :gutter="16">
      <el-col v-for="d in drivers" :key="d.value" :span="4" style="min-width: 150px">
        <div class="driver-card" :class="{ selected: driver === d.value }" @click="select(d.value)">
          <el-icon size="28"><component :is="d.icon" /></el-icon>
          <div class="driver-name">{{ d.label }}</div>
          <div class="muted">{{ d.desc }}</div>
          <el-tag v-if="activeDriver === d.value" type="success" size="small" class="mt8">使用中</el-tag>
        </div>
      </el-col>
    </el-row>

    <el-card shadow="never" class="mt16" v-if="driver">
      <template #header><b>{{ driverLabel }} 参数配置</b></template>
      <el-form :model="conf" label-width="120px" style="max-width: 620px">
        <template v-if="driver === 'local'">
          <el-form-item label="存储目录"><el-input v-model="conf.baseDir" placeholder="./data/upload" /></el-form-item>
        </template>
        <template v-else>
          <el-form-item label="Endpoint"><el-input v-model="conf.endpoint" placeholder="minio.example.com:9000" /></el-form-item>
          <el-form-item label="Bucket"><el-input v-model="conf.bucket" placeholder="travel" /></el-form-item>
          <el-form-item label="Access Key"><el-input v-model="conf.accessKey" /></el-form-item>
          <el-form-item label="Secret Key">
            <el-input v-model="conf.secretKey" type="password" show-password placeholder="留空或回掩码表示不修改" />
          </el-form-item>
          <el-form-item v-if="driver === 'oss' || driver === 'cos' || driver === 's3'" label="Region">
            <el-input v-model="conf.region" />
          </el-form-item>
        </template>
        <el-form-item>
          <el-button :loading="testing" @click="test">连接测试</el-button>
          <el-tag v-if="testResult !== null" :type="testResult ? 'success' : 'danger'" class="ml8">
            {{ testResult ? '连接成功' : '连接失败' }}
          </el-tag>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :disabled="!testPassed" @click="save">保存并应用</el-button>
          <span class="muted" style="margin-left: 12px">连接测试通过后才可保存</span>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '@/api/request'

const drivers = [
  { value: 'local', label: '本地磁盘', icon: 'FolderOpened', desc: '服务器本地目录' },
  { value: 's3', label: 'AWS S3', icon: 'Cloudy', desc: 'Amazon S3' },
  { value: 'oss', label: '阿里云 OSS', icon: 'Sunny', desc: '阿里云对象存储' },
  { value: 'cos', label: '腾讯云 COS', icon: 'MostlyCloudy', desc: '腾讯云对象存储' },
  { value: 'minio', label: 'MinIO', icon: 'Box', desc: '私有化部署' }
]
const driver = ref('local')
const activeDriver = ref('')
const conf = reactive<Record<string, string>>({})
const testResult = ref<boolean | null>(null)
const testing = ref(false)
const testPassed = computed(() => testResult.value === true)
const driverLabel = computed(() => drivers.find((d) => d.value === driver.value)?.label)

async function load() {
  const list: any[] = (await api.get('/system/storage/configs')) || []
  const active = list.find((x) => x.active === 1)
  activeDriver.value = active?.driver || 'local'
  driver.value = activeDriver.value
  const found = list.find((x) => x.driver === driver.value)
  Object.keys(conf).forEach((k) => delete conf[k])
  Object.assign(conf, found?.config || {})
  testResult.value = null
}
function select(v: string) {
  if (activeDriver.value && v !== activeDriver.value) {
    ElMessageBox.confirm('切换存储驱动后新文件写入新驱动，历史文件仍按原 URL 可读。确认切换？', '二次确认', { type: 'warning' })
      .then(() => { driver.value = v; testResult.value = null })
      .catch(() => {})
  } else {
    driver.value = v
    testResult.value = null
  }
}
async function test() {
  testing.value = true
  try {
    const ok = await api.post('/system/storage/test', { driver: driver.value, config: conf })
    testResult.value = !!ok
    ElMessage[ok ? 'success' : 'warning'](ok ? '连接成功' : '连接失败')
  } catch {
    testResult.value = false
  } finally {
    testing.value = false
  }
}
async function save() {
  await ElMessageBox.confirm(`确定保存并应用「${driverLabel.value}」？`, '保存并应用', { type: 'warning' })
  await api.post('/system/storage/save', { driver: driver.value, config: conf, activate: true })
  ElMessage.success('已保存并应用')
  load()
}
onMounted(load)
</script>

<style scoped>
.driver-card { border: 1px solid #dcdfe6; border-radius: 8px; padding: 16px; text-align: center; cursor: pointer; background: #fff; height: 150px; transition: all 0.15s; }
.driver-card:hover { border-color: #1f6feb; }
.driver-card.selected { border: 2px solid #1f6feb; background: #f5f9ff; }
.driver-name { font-weight: 600; margin: 8px 0 4px; }
.ml8 { margin-left: 8px; }
</style>
