<template>
  <div class="page-card">
    <div class="page-header">
      <h2 class="page-title">日历排期</h2>
      <div class="flex gap8">
        <el-button @click="$router.push('/schedule/list')">列表视图</el-button>
        <el-button type="primary" @click="generateDialog = true">批量生成出团计划</el-button>
      </div>
    </div>

    <el-row :gutter="16">
      <el-col :span="17">
        <el-calendar v-model="current">
          <template #date-cell="{ data }">
            <div class="cell" :class="{ selected: data.day === selectedDay }" @click="selectedDay = data.day; filterList()">
              <div class="day">{{ Number(data.day.split('-')[2]) }}</div>
              <div v-for="s in itemsOf(data.day).slice(0, 3)" :key="s.id" class="event" :class="'s' + s.status">
                {{ s.productTitle?.slice(0, 8) }} · {{ s.sold }}/{{ s.quota }}
              </div>
              <div v-if="itemsOf(data.day).length > 3" class="more">+{{ itemsOf(data.day).length - 3 }}</div>
            </div>
          </template>
        </el-calendar>
      </el-col>
      <el-col :span="7">
        <div class="side">
          <h3 class="page-title">{{ selectedDay }}</h3>
          <div v-for="s in dayList" :key="s.id" class="side-item">
            <div class="flex"><b>{{ s.scheduleNo }}</b><span class="spacer" /><el-tag size="small">{{ statusMap[s.status] }}</el-tag></div>
            <div>{{ s.productTitle }}</div>
            <div class="muted">集合：{{ s.meetingPoint }} {{ s.meetingTime }} · 导游：{{ s.guideName || '待指派' }}</div>
            <div class="muted">收客 {{ s.sold }}/{{ s.quota }} · 成团 {{ s.minGroup }} 人</div>
            <div class="mt8">
              <el-button size="small" @click="planPdf(s)">计划书 PDF</el-button>
            </div>
          </div>
          <el-empty v-if="!dayList.length" description="当日无排期" :image-size="60" />
        </div>
      </el-col>
    </el-row>

    <el-dialog v-model="generateDialog" title="批量生成出团计划" width="540px">
      <el-form label-width="100px">
        <el-form-item label="线路">
          <el-select v-model="gen.productId" filterable style="width: 100%">
            <el-option v-for="p in products" :key="p.id" :label="p.title" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="期间">
          <el-date-picker v-model="gen.range" type="daterange" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="每周">
          <el-checkbox-group v-model="gen.weekdays">
            <el-checkbox v-for="(w, i) in weekNames" :key="i" :value="i + 1">{{ w }}</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="generateDialog = false">取消</el-button>
        <el-button type="primary" @click="generate">生成</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { api, download } from '@/api/request'

const statusMap: Record<string, string> = { 1: '收客中', 2: '已成团', 3: '已满', 4: '已出团', 5: '已完结' }
const weekNames = ['一', '二', '三', '四', '五', '六', '日']
const current = ref(new Date())
const selectedDay = ref(new Date().toISOString().slice(0, 10))
const list = ref<any[]>([])
const products = ref<any[]>([])
const generateDialog = ref(false)
const gen = reactive<any>({ productId: null, range: [], weekdays: [1, 2, 3, 4, 5, 6, 7] })

const dayList = computed(() => list.value.filter((s) => s.departDate === selectedDay.value))

function itemsOf(day: string) {
  return list.value.filter((s) => s.departDate === day)
}
function filterList() { /* computed handles it */ }

async function load() {
  const y = current.value.getFullYear()
  const m = current.value.getMonth()
  const start = `${y}-${String(m + 1).padStart(2, '0')}-01`
  const endDate = new Date(y, m + 1, 0)
  const end = `${y}-${String(m + 1).padStart(2, '0')}-${String(endDate.getDate()).padStart(2, '0')}`
  list.value = (await api.get('/schedules/calendar', { startDate: start, endDate: end })) || []
}
async function generate() {
  if (!gen.productId || !gen.range?.length) return ElMessage.warning('请选择线路和期间')
  const r: any = await api.post('/schedules/generate', {
    productId: gen.productId, startDate: gen.range[0], endDate: gen.range[1], weekdays: gen.weekdays
  })
  ElMessage.success(`已生成 ${r.created} 个出团计划`)
  generateDialog.value = false
  load()
}
function planPdf(s: any) {
  download(`/schedules/${s.id}/plan`, {}, `出团计划书-${s.scheduleNo}.pdf`)
}
watch(current, load)
onMounted(async () => {
  const p: any = await api.get('/products', { page: 1, size: 200 })
  products.value = p.records || []
  load()
})
</script>

<style scoped>
.cell { height: 92px; padding: 2px; overflow: hidden; }
.cell.selected { background: #e8f1ff; border-radius: 4px; }
.day { font-size: 12px; color: #606266; }
.event { font-size: 11px; background: #e8f1ff; color: #1f6feb; border-radius: 3px; padding: 0 3px; margin-top: 2px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.event.s3 { background: #fef0f0; color: #f56c6c; }
.event.s4, .event.s5 { background: #f4f4f5; color: #909399; }
.more { font-size: 11px; color: #909399; }
.side { background: #fff; border-radius: 8px; padding: 12px; border: 1px solid #ebeef5; min-height: 600px; }
.side-item { border-bottom: 1px dashed #ebeef5; padding: 10px 0; font-size: 13px; }
.muted { color: #909399; font-size: 12px; }
</style>
