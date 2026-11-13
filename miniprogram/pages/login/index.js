const { request } = require('../../utils/request')
const { saveLogin, wechatLogin } = require('../../utils/auth')

Page({
  data: {
    tab: 'member',
    tenantCode: 'HLJ',
    nickname: '微信游客',
    unionId: '',
    phone: '',
    username: 'scenic01',
    password: 'admin123',
    loading: false
  },

  switchTab(e) {
    this.setData({ tab: e.currentTarget.dataset.tab })
  },

  onInput(e) {
    this.setData({ [e.currentTarget.dataset.field]: e.detail.value })
  },

  async memberLogin() {
    this.setData({ loading: true })
    try {
      const result = await wechatLogin(this.data.nickname)
      wx.showToast({ title: '登录成功', icon: 'success' })
      setTimeout(() => wx.switchTab({ url: '/pages/index/index' }), 500)
    } catch (e) { /* ignore */ } finally {
      this.setData({ loading: false })
    }
  },

  async guideLogin() {
    if (!this.data.unionId && !this.data.phone) {
      wx.showToast({ title: '请填写 UnionID 或手机号', icon: 'none' })
      return
    }
    this.setData({ loading: true })
    try {
      const result = await request({
        url: '/auth/guide-login',
        method: 'POST',
        auth: false,
        data: { tenantCode: this.data.tenantCode, unionId: this.data.unionId, phone: this.data.phone }
      })
      saveLogin(result)
      wx.showToast({ title: '登录成功', icon: 'success' })
      setTimeout(() => wx.redirectTo({ url: '/pages/guide/index' }), 500)
    } catch (e) { /* ignore */ } finally {
      this.setData({ loading: false })
    }
  },

  async scenicLogin() {
    this.setData({ loading: true })
    try {
      const result = await request({
        url: '/auth/scenic-login',
        method: 'POST',
        auth: false,
        data: { tenantCode: this.data.tenantCode, username: this.data.username, password: this.data.password }
      })
      saveLogin(result)
      wx.showToast({ title: '登录成功', icon: 'success' })
      setTimeout(() => wx.redirectTo({ url: '/pages/scenic/index' }), 500)
    } catch (e) { /* ignore */ } finally {
      this.setData({ loading: false })
    }
  }
})
