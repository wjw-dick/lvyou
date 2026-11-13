import axios from 'axios'

export const TENANT_CODE = localStorage.getItem('tp_portal_tenant') || 'HLJ'

export const http = axios.create({ baseURL: '/api', timeout: 20000 })

http.interceptors.request.use((config) => {
  config.headers['X-Tenant-Code'] = TENANT_CODE
  const token = localStorage.getItem('tp_portal_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

http.interceptors.response.use(
  (res) => {
    const body = res.data
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code === 0) return body.data
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    return body
  },
  (err) => Promise.reject(new Error(err.response?.data?.message || err.message || '网络异常'))
)

export const api = {
  get: (url, params) => http.get(url, { params }),
  post: (url, data) => http.post(url, data)
}
