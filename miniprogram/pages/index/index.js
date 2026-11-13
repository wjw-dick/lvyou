const { request } = require('../../utils/request')

Page({
  data: {
    tenant: {},
    products: [],
    destinations: [],
    notices: [],
    keyword: '',
    loading: true
  },

  onLoad() {
    this.load()
  },

  async load() {
    try {
      const [tenant, products, destinations, notices] = await Promise.all([
        request({ url: '/public/tenant', auth: false }),
        request({ url: '/public/products', data: { page: 1, size: 6 }, auth: false }),
        request({ url: '/public/destinations', auth: false }),
        request({ url: '/public/notices', auth: false })
      ])
      this.setData({
        tenant: tenant || {},
        products: products.records || [],
        destinations: (destinations || []).slice(0, 6),
        notices: notices || [],
        loading: false
      })
    } catch (e) {
      this.setData({ loading: false })
    }
  },

  onKeyword(e) {
    this.setData({ keyword: e.detail.value })
  },

  onSearch() {
    wx.navigateTo({ url: '/pages/product/list?keyword=' + encodeURIComponent(this.data.keyword) })
  },

  goDestination(e) {
    wx.navigateTo({ url: '/pages/product/list?destination=' + encodeURIComponent(e.currentTarget.dataset.name) })
  },

  goDetail(e) {
    wx.navigateTo({ url: '/pages/product/detail?id=' + e.currentTarget.dataset.id })
  },

  goProducts() {
    wx.switchTab({ url: '/pages/product/list' })
  },

  goOrderQuery() {
    wx.switchTab({ url: '/pages/mine/index' })
  },

  onShareAppMessage() {
    return { title: this.data.tenant.name || '雪乡国旅', path: '/pages/index/index' }
  }
})
