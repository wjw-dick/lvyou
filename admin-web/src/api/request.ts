import axios from 'axios'
import { ElMessage } from 'element-plus'

export const http = axios.create({ baseURL: '/api', timeout: 30000 })

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('tp_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  const viewTenant = localStorage.getItem('tp_view_tenant')
  if (viewTenant) config.headers['X-Tenant-Id'] = viewTenant
  const tenantCode = localStorage.getItem('tp_tenant_code')
  if (tenantCode && config.url?.startsWith('/public')) config.headers['X-Tenant-Code'] = tenantCode
  return config
})

http.interceptors.response.use(
  (res) => {
    const body = res.data
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code === 0) return body.data
      ElMessage.error(body.message || '请求失败')
      return Promise.reject(new Error(body.message))
    }
    return body
  },
  (err) => {
    const status = err.response?.status
    const msg = err.response?.data?.message || err.message || '网络异常'
    if (status === 401) {
      localStorage.removeItem('tp_token')
      if (!location.pathname.includes('/login')) location.href = '/login'
    } else if (status === 403) {
      ElMessage.error(msg || '无权访问该资源')
    } else {
      ElMessage.error(msg)
    }
    return Promise.reject(err)
  }
)

export const api = {
  get: (url: string, params?: any): Promise<any> => http.get(url, { params }) as any,
  post: (url: string, data?: any, params?: any): Promise<any> => http.post(url, data, { params }) as any,
  put: (url: string, data?: any, config?: any): Promise<any> => http.put(url, data, config) as any,
  del: (url: string, params?: any): Promise<any> => http.delete(url, { params }) as any
}

/** 带登录态下载文件（Excel/PDF） */
export async function download(url: string, params?: any, filename = 'download.xlsx') {
  const res = await http.get(url, { params, responseType: 'blob' } as any)
  const blob = new Blob([res as any])
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = filename
  link.click()
  URL.revokeObjectURL(link.href)
}

/** 上传文件到对象存储 */
export async function uploadFile(file: File, bizType = 'common') {
  const form = new FormData()
  form.append('file', file)
  form.append('bizType', bizType)
  return http.post('/upload', form, { headers: { 'Content-Type': 'multipart/form-data' } }) as Promise<any>
}
