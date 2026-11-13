<template>
  <div class="upload-card">
    <el-upload
      v-model:file-list="fileList"
      :action="action"
      :headers="headers"
      :data="{ bizType }"
      :multiple="multiple"
      :limit="multiple ? 9 : 1"
      list-type="picture-card"
      :on-success="onSuccess"
      :on-remove="onRemove"
      :on-preview="onPreview"
      drag
      accept="image/*"
    >
      <el-icon><Plus /></el-icon>
      <template #tip>
        <div class="muted">支持拖拽/多图；单张不超过 10MB；自动上传至对象存储</div>
      </template>
    </el-upload>
    <el-image-viewer v-if="previewVisible" :url-list="[previewUrl]" @close="previewVisible = false" />
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'

const props = withDefaults(defineProps<{ modelValue?: string | string[]; multiple?: boolean; bizType?: string }>(), {
  multiple: true,
  bizType: 'common'
})
const emit = defineEmits<{ (e: 'update:modelValue', v: string): void }>()

const action = '/api/upload'
const headers = computed(() => ({ Authorization: `Bearer ${localStorage.getItem('tp_token')}` }))
const previewVisible = ref(false)
const previewUrl = ref('')
const fileList = ref<any[]>([])

function urls(): string[] {
  if (!props.modelValue) return []
  return Array.isArray(props.modelValue) ? props.modelValue : String(props.modelValue).split(',').filter(Boolean)
}

watch(
  () => props.modelValue,
  () => {
    const list = urls().map((u, i) => ({ name: `图片${i + 1}`, url: u, uid: u }))
    if (fileList.value.map((f) => f.url).join(',') !== list.map((f) => f.url).join(',')) fileList.value = list
  },
  { immediate: true }
)

function onSuccess(res: any) {
  if (res?.code === 0) {
    const next = [...urls(), res.data.url]
    emit('update:modelValue', props.multiple ? next.join(',') : res.data.url)
  }
}
function onRemove(file: any) {
  const remain = urls().filter((u) => u !== file.url)
  emit('update:modelValue', remain.join(','))
}
function onPreview(file: any) {
  previewUrl.value = file.url
  previewVisible.value = true
}
</script>
