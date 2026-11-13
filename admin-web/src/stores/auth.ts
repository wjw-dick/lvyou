import { defineStore } from 'pinia'
import { api } from '@/api/request'

interface MenuNode {
  id: number
  parentId: number
  title: string
  path: string
  icon?: string
  component?: string
  type?: string
  sort?: number
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('tp_token') || '',
    user: JSON.parse(localStorage.getItem('tp_user') || 'null') as any,
    permissions: (JSON.parse(localStorage.getItem('tp_perms') || '[]') as string[]),
    menus: (JSON.parse(localStorage.getItem('tp_menus') || '[]') as MenuNode[]),
    viewTenantId: localStorage.getItem('tp_view_tenant') || '',
    viewTenantName: localStorage.getItem('tp_view_tenant_name') || ''
  }),
  getters: {
    isPlatform: (s) => s.user?.userType === 'PLATFORM',
    tenantCode: (s) => s.user?.tenantCode || '',
    can: (s) => (perm: string) => s.permissions.includes('*') || s.permissions.includes(perm)
  },
  actions: {
    async login(payload: { type: string; tenantCode?: string; username: string; password: string }) {
      const data = await api.post('/auth/login', payload)
      this.token = data.token
      this.user = {
        userId: data.userId,
        username: data.username,
        realName: data.realName,
        userType: data.userType,
        tenantId: data.tenantId,
        tenantCode: data.tenantCode,
        tenantName: data.tenantName
      }
      this.permissions = data.permissions || []
      this.menus = data.menus || []
      localStorage.setItem('tp_token', this.token)
      localStorage.setItem('tp_user', JSON.stringify(this.user))
      localStorage.setItem('tp_perms', JSON.stringify(this.permissions))
      localStorage.setItem('tp_menus', JSON.stringify(this.menus))
      if (data.tenantCode) localStorage.setItem('tp_tenant_code', data.tenantCode)
      return data
    },
    setViewTenant(id: string, name: string) {
      this.viewTenantId = id
      this.viewTenantName = name
      if (id) {
        localStorage.setItem('tp_view_tenant', id)
        localStorage.setItem('tp_view_tenant_name', name)
      } else {
        localStorage.removeItem('tp_view_tenant')
        localStorage.removeItem('tp_view_tenant_name')
      }
    },
    async fetchMe() {
      const data = await api.get('/auth/me')
      this.user = { ...this.user, ...data }
      this.permissions = data.permissions || []
      this.menus = data.menus || []
      localStorage.setItem('tp_perms', JSON.stringify(this.permissions))
      localStorage.setItem('tp_menus', JSON.stringify(this.menus))
    },
    logout() {
      localStorage.removeItem('tp_token')
      localStorage.removeItem('tp_user')
      localStorage.removeItem('tp_perms')
      localStorage.removeItem('tp_menus')
      localStorage.removeItem('tp_view_tenant')
      localStorage.removeItem('tp_view_tenant_name')
      this.token = ''
      this.user = null
      this.menus = []
    }
  }
})
