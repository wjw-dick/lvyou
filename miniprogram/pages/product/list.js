const { request } = require('../../utils/request')

Page({
  data: {
    keyword: '',
    destination: '',
    days: '',
    products: [],
    loading: true
  },

  onLoad(options) {
    this.setData({ keyword: options.keyword || '', destination: options.destination || '' })
    this.load()
  },

  onKeyword(e) {
    this.setData({ keyword: e.detail.value })
  },

  onDays(e) {
    this.setData({ days: e.currentTarget.dataset.days })
    this.load()
  },

  async load() {
    this.setData({ loading: true })
    try {
      const data = await request({
        url: '/public/products',
        auth: false,
        data: { keyword: this.data.keyword, destination: this.data.destination, days: this.data.days, page: 1, size: 50 }
      })
      this.setData({ products: data.records || [], loading: false })
    } catch (e) {
      this.setData({ loading: false })
    }
  },

  goDetail(e) {
    wx.navigateTo({ url: '/pages/product/detail?id=' + e.currentTarget.dataset.id })
  },

  onShareAppMessage() {
    return { title: '精选旅游线路', path: '/pages/product/list' }
  }
})
