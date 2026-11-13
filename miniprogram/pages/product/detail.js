const { request } = require('../../utils/request')

Page({
  data: {
    product: null,
    itineraries: [],
    schedules: [],
    scheduleId: null,
    selectedSchedule: null,
    loading: true
  },

  onLoad(options) {
    this.productId = options.id
    this.load()
  },

  async load() {
    try {
      const data = await request({ url: '/public/products/' + this.productId, auth: false })
      const schedules = await request({ url: '/public/products/' + this.productId + '/schedules', auth: false })
      this.setData({
        product: data.product,
        itineraries: data.itineraries || [],
        schedules: schedules || [],
        scheduleId: schedules && schedules.length ? schedules[0].id : null,
        selectedSchedule: schedules && schedules.length ? schedules[0] : null,
        loading: false
      })
      wx.setNavigationBarTitle({ title: data.product.title })
    } catch (e) {
      this.setData({ loading: false })
    }
  },

  selectSchedule(e) {
    const id = Number(e.currentTarget.dataset.id)
    const s = this.data.schedules.find((x) => x.id === id)
    this.setData({ scheduleId: id, selectedSchedule: s })
  },

  book() {
    if (!this.data.scheduleId) {
      wx.showToast({ title: '请选择团期', icon: 'none' })
      return
    }
    wx.navigateTo({ url: `/pages/product/booking?productId=${this.data.product.id}&scheduleId=${this.data.scheduleId}` })
  },

  previewImage(e) {
    const urls = (this.data.product.cover ? [this.data.product.cover] : [])
    if (urls.length) wx.previewImage({ urls, current: urls[0] })
  },

  onShareAppMessage() {
    return {
      title: this.data.product ? this.data.product.title : '旅游线路',
      path: '/pages/product/detail?id=' + this.productId
    }
  }
})
