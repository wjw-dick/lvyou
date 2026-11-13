const { request } = require('./utils/request')

App({
  globalData: {
    baseUrl: 'http://127.0.0.1:8080/api',
    tenantCode: 'HLJ',
    token: '',
    user: null,
    role: 'member',
    nav: []
  },

  onLaunch() {
    const token = wx.getStorageSync('tp_token')
    const user = wx.getStorageSync('tp_user')
    if (token) {
      this.globalData.token = token
      this.globalData.user = user
      this.globalData.role = user?.userType === 'GUIDE' ? 'guide' : user?.userType === 'SCENIC' ? 'scenic' : 'member'
    }
    this.loadNav()
  },

  async loadNav() {
    try {
      const content = await request({ url: '/public/nav', data: { platform: 'miniprogram' }, auth: false })
      const nav = JSON.parse(content || '[]')
      this.globalData.nav = nav
      // tabBar 后台可视化：文案实时更新（页面路径随小程序包固定）
      const fixed = ['pages/index/index', 'pages/product/list', 'pages/order/list', 'pages/mine/index']
      nav.slice(0, 4).forEach((item, i) => {
        if (!item.title) return
        try {
          wx.setTabBarItem({ index: i, text: item.title, fail: () => {} })
        } catch (e) { /* 低版本基础库忽略 */ }
      })
    } catch (e) { /* ignore */ }
  }
})
