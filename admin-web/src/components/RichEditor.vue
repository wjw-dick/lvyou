<template>
  <div class="rich-editor">
    <Toolbar :editor="editorRef" :default-config="toolbarConfig" mode="default" class="toolbar" />
    <Editor
      v-model="valueHtml"
      :default-config="editorConfig"
      mode="default"
      :style="{ height: height + 'px', overflowY: 'hidden' }"
      @on-created="onCreated"
    />
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, ref, shallowRef, watch } from 'vue'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import '@wangeditor/editor/dist/css/style.css'
import { uploadFile } from '@/api/request'

const props = withDefaults(defineProps<{ modelValue?: string; height?: number; placeholder?: string }>(), {
  modelValue: '',
  height: 300,
  placeholder: '请输入内容，支持图片/视频/表格，粘贴图片自动上传'
})
const emit = defineEmits<{ (e: 'update:modelValue', v: string): void }>()

const editorRef = shallowRef()
const valueHtml = ref('')
const toolbarConfig = {}
const editorConfig = {
  placeholder: props.placeholder,
  MENU_CONF: {
    uploadImage: {
      async customUpload(file: File, insertFn: (url: string, alt: string, href: string) => void) {
        const res = await uploadFile(file, 'rich')
        insertFn(res.url, '', res.url)
      }
    },
    uploadVideo: {
      async customUpload(file: File, insertFn: (url: string, poster: string) => void) {
        const res = await uploadFile(file, 'video')
        insertFn(res.url, '')
      }
    }
  }
}

watch(
  () => props.modelValue,
  (v) => {
    if (v !== valueHtml.value) valueHtml.value = v || ''
  },
  { immediate: true }
)
watch(valueHtml, (v) => emit('update:modelValue', v))

function onCreated(editor: any) {
  editorRef.value = editor
}

onBeforeUnmount(() => editorRef.value?.destroy())
</script>

<style scoped>
.rich-editor { border: 1px solid #dcdfe6; border-radius: 6px; width: 100%; }
.toolbar { border-bottom: 1px solid #ebeef5; }
:deep(.w-e-text-container) { background: #fff; }
</style>
