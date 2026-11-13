<template>
  <div class="page-card" v-loading="loading">
    <div class="page-header">
      <h2 class="page-title">{{ isNew ? '新增线路产品' : '编辑线路产品' }}</h2>
      <div class="flex gap8">
        <el-button @click="$router.back()">返回</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </div>
    </div>

    <el-row :gutter="16">
      <el-col :span="16">
        <el-form :model="form" label-width="110px">
          <el-card shadow="never" class="mb16">
            <template #header><b>基本信息</b></template>
            <el-tabs v-model="lang">
              <el-tab-pane label="中文" name="zh-CN" />
              <el-tab-pane name="en-US">
                <template #label>EN <span v-if="!i18n.enTitle" class="red-dot" /></template>
              </el-tab-pane>
            </el-tabs>
            <template v-if="lang === 'zh-CN'">
              <el-form-item label="产品标题" required>
                <el-input v-model="form.title" placeholder="如 雪乡童话世界2日游" />
              </el-form-item>
              <el-form-item label="副标题">
                <el-input v-model="form.subtitle" placeholder="卖点一句话" />
              </el-form-item>
            </template>
            <template v-else>
              <el-form-item label="English Title">
                <el-input v-model="i18n.enTitle" placeholder="Snow Town 2-Day Tour" />
              </el-form-item>
              <el-form-item label="English Content">
                <RichEditor v-model="i18n.enGuideContent" :height="220" />
              </el-form-item>
            </template>
            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="品类">
                  <el-select v-model="form.category" style="width: 100%">
                    <el-option v-for="o in categories" :key="o.value" :label="o.label" :value="o.value" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="天数"><el-input-number v-model="form.days" :min="1" style="width: 100%" /></el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="出发地"><el-input v-model="form.departure" /></el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="目的地"><el-input v-model="form.destination" /></el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="关联目的地">
                  <el-select v-model="form.destinationId" clearable filterable style="width: 100%">
                    <el-option v-for="d in destinations" :key="d.id" :label="d.name" :value="d.id" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="集合地点"><el-input v-model="form.meetingPoint" /></el-form-item>
              </el-col>
            </el-row>
          </el-card>

          <el-card shadow="never" class="mb16">
            <template #header><b>价格与库存</b></template>
            <el-row :gutter="16">
              <el-col :span="8"><el-form-item label="成人价"><el-input-number v-model="form.adultPrice" :min="0" :precision="2" style="width: 100%" /></el-form-item></el-col>
              <el-col :span="8"><el-form-item label="儿童价"><el-input-number v-model="form.childPrice" :min="0" :precision="2" style="width: 100%" /></el-form-item></el-col>
              <el-col :span="8"><el-form-item label="单房差"><el-input-number v-model="form.singleRoomDiff" :min="0" :precision="2" style="width: 100%" /></el-form-item></el-col>
              <el-col :span="8"><el-form-item label="成本价"><el-input-number v-model="form.costPrice" :min="0" :precision="2" style="width: 100%" /></el-form-item></el-col>
              <el-col :span="8"><el-form-item label="库存/余位"><el-input-number v-model="form.stock" :min="0" style="width: 100%" /></el-form-item></el-col>
              <el-col :span="8"><el-form-item label="成团人数"><el-input-number v-model="form.minGroup" :min="1" style="width: 100%" /></el-form-item></el-col>
            </el-row>
          </el-card>

          <el-card shadow="never" class="mb16">
            <template #header>
              <div class="flex">
                <b>行程 D1~Dn</b>
                <div class="spacer" />
                <el-button size="small" @click="addDay">添加一天</el-button>
                <el-button size="small" @click="fillDays">按天数生成</el-button>
              </div>
            </template>
            <div v-for="(it, idx) in itineraries" :key="idx" class="day-block">
              <div class="flex gap8">
                <el-tag type="primary">D{{ it.dayNo }}</el-tag>
                <el-input v-model="it.title" placeholder="当天主题" />
                <el-button link type="danger" @click="itineraries.splice(idx, 1)">删除</el-button>
              </div>
              <el-row :gutter="8" class="mt8">
                <el-col :span="8"><el-input v-model="it.spots" placeholder="景点" /></el-col>
                <el-col :span="8"><el-input v-model="it.hotel" placeholder="酒店" /></el-col>
                <el-col :span="8"><el-input v-model="it.meals" placeholder="用餐" /></el-col>
                <el-col :span="8"><el-input v-model="it.transport" placeholder="交通" /></el-col>
                <el-col :span="8"><el-input v-model="it.guide" placeholder="导游" /></el-col>
                <el-col :span="8"><el-input v-model="it.accommodation" placeholder="住宿标准" /></el-col>
              </el-row>
              <el-input v-model="it.content" type="textarea" :rows="2" placeholder="行程详情" class="mt8" />
            </div>
            <el-empty v-if="!itineraries.length" description="尚未添加行程" />
          </el-card>

          <el-card shadow="never" class="mb16">
            <template #header><b>费用说明 / 退改规则</b></template>
            <el-form-item label="费用包含"><el-input v-model="form.includeText" type="textarea" :rows="2" /></el-form-item>
            <el-form-item label="费用不含"><el-input v-model="form.excludeText" type="textarea" :rows="2" /></el-form-item>
            <el-form-item label="退改规则"><el-input v-model="form.refundRule" type="textarea" :rows="3" /></el-form-item>
          </el-card>

          <el-card shadow="never">
            <template #header><b>图文详情（富文本）</b></template>
            <RichEditor v-model="form.guideContent" :height="320" />
          </el-card>
        </el-form>
      </el-col>

      <el-col :span="8">
        <el-card shadow="never" class="mb16">
          <template #header><b>主图 / 海报</b></template>
          <ImageUpload v-model="form.cover" :multiple="false" biz-type="product" />
          <div class="muted mt8">封面图独立成块，支持拖拽上传、预览、删除</div>
        </el-card>
        <el-card shadow="never" class="mb16">
          <template #header><b>景点实拍 / 相册</b></template>
          <ImageUpload v-model="form.images" :multiple="true" biz-type="product" />
        </el-card>
        <el-card shadow="never" class="mb16">
          <template #header><b>上架渠道</b></template>
          <el-checkbox-group v-model="channelList">
            <el-checkbox v-for="c in channelOptions" :key="c.value" :value="c.value">{{ c.label }}</el-checkbox>
          </el-checkbox-group>
          <el-form-item label="状态" class="mt16">
            <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="上架" inactive-text="下架" />
          </el-form-item>
        </el-card>
        <el-card shadow="never">
          <template #header><b>多渠道报价</b></template>
          <div v-for="(p, idx) in prices" :key="idx" class="price-row">
            <el-select v-model="p.channel" size="small" style="width: 110px">
              <el-option v-for="c in channelOptions" :key="c.value" :label="c.label" :value="c.value" />
            </el-select>
            <el-input-number v-model="p.adultPrice" :precision="2" :min="0" size="small" controls-position="right" style="width: 110px" />
            <el-button link type="danger" @click="prices.splice(idx, 1)">删除</el-button>
          </div>
          <el-button size="small" class="mt8" @click="prices.push({ channel: 'distributor', adultPrice: 0, childPrice: 0, singleRoomDiff: 0 })">添加渠道价</el-button>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { api } from '@/api/request'
import RichEditor from '@/components/RichEditor.vue'
import ImageUpload from '@/components/ImageUpload.vue'

const route = useRoute()
const router = useRouter()
const id = route.params.id as string
const isNew = id === 'new'
const loading = ref(false)
const saving = ref(false)
const lang = ref('zh-CN')
const destinations = ref<any[]>([])
const i18n = reactive({ enTitle: '', enGuideContent: '' })
const itineraries = ref<any[]>([])
const prices = ref<any[]>([])
const channelList = ref<string[]>(['miniprogram', 'mp', 'h5', 'app'])

const categories = [
  { label: '跟团游', value: 'line' }, { label: '签证', value: 'visa' }, { label: '机票', value: 'flight' },
  { label: '酒店', value: 'hotel' }, { label: '门票', value: 'ticket' }, { label: '租车', value: 'car' },
  { label: '邮轮', value: 'cruise' }, { label: '特产商品', value: 'goods' }
]
const channelOptions = [
  { label: '小程序', value: 'miniprogram' }, { label: '公众号', value: 'mp' },
  { label: 'H5 门户', value: 'h5' }, { label: '独立 App', value: 'app' },
  { label: 'OTA', value: 'ota' }, { label: '分销', value: 'distributor' }
]

const form = reactive<any>({
  title: '', subtitle: '', category: 'line', days: 1, departure: '哈尔滨', destination: '',
  destinationId: null, adultPrice: 0, childPrice: 0, singleRoomDiff: 0, costPrice: 0,
  includeText: '', excludeText: '', refundRule: '', meetingPoint: '', cover: '', images: '',
  guideContent: '', stock: 100, minGroup: 10, status: 1, channels: 'miniprogram,mp,h5,app'
})

function addDay() {
  itineraries.value.push({ dayNo: itineraries.value.length + 1, title: '', spots: '', hotel: '', meals: '', transport: '', guide: '', accommodation: '', content: '' })
}
function fillDays() {
  itineraries.value = []
  for (let i = 1; i <= (form.days || 1); i++) addDay()
}

async function load() {
  destinations.value = (await api.get('/destinations/all')) || []
  if (isNew) {
    fillDays()
    return
  }
  loading.value = true
  try {
    const data: any = await api.get(`/products/${id}`)
    Object.assign(form, data.product)
    itineraries.value = data.itineraries || []
    prices.value = data.prices || []
    channelList.value = (form.channels || '').split(',').filter(Boolean)
    for (const item of data.i18n || []) {
      if (item.lang === 'en-US' && item.field === 'title') i18n.enTitle = item.content
      if (item.lang === 'en-US' && item.field === 'guideContent') i18n.enGuideContent = item.content
    }
  } finally {
    loading.value = false
  }
}

async function save() {
  if (!form.title) return ElMessage.warning('请填写产品标题')
  saving.value = true
  try {
    const payload = { ...form, channels: channelList.value, itineraries: itineraries.value, prices: prices.value }
    let productId = id
    if (isNew) {
      const created: any = await api.post('/products', payload)
      productId = created.id
    } else {
      await api.put(`/products/${productId}`, payload)
    }
    await api.post(`/products/${productId}/i18n`, [
      { lang: 'en-US', field: 'title', content: i18n.enTitle },
      { lang: 'en-US', field: 'guideContent', content: i18n.enGuideContent }
    ])
    ElMessage.success('保存成功')
    router.push('/product/list')
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.red-dot { display: inline-block; width: 6px; height: 6px; background: #f56c6c; border-radius: 50%; margin-left: 2px; vertical-align: top; }
.day-block { border: 1px solid #ebeef5; border-radius: 6px; padding: 10px; margin-bottom: 10px; background: #fafcff; }
.price-row { display: flex; align-items: center; gap: 6px; margin-bottom: 6px; }
</style>
