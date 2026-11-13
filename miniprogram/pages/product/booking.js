const { request } = require('../../utils/request')
const { isLogin } = require('../../utils/auth')

Page({
  data: {
    product: null,
    productId: null,
    scheduleId: null,
    adultCount: 1,
    childCount: 0,
    contactName: '',
    contactPhone: '',
    tourists: [],
    total: '0.00',
    submitting: false
  },

  onLoad(options) {
    if (!isLogin()) {
      wx.redirectTo({ url: '/pages/login/index?redirect=booking' })
      return
    }
    this.setData({ productId: Number(options.productId), scheduleId: Number(options.scheduleId) })
    this.load()
  },

  async load() {
    const data = await request({ url: '/public/products/' + this.data.productId, auth: false })
    this.setData({ product: data.product })
    this.syncTourists()
    this.calc()
  },

  onAdult(e) {
    this.setData({ adultCount: Math.max(1, Number(e.detail.value) || 1) }, () => {
      this.syncTourists()
      this.calc()
    })
  },

  onChild(e) {
    this.setData({ childCount: Math.max(0, Number(e.detail.value) || 0) }, () => {
      this.syncTourists()
      this.calc()
    })
  },

  onContact(e) {
    this.setData({ [e.currentTarget.dataset.field]: e.detail.value })
  },

  onTourist(e) {
    const idx = Number(e.currentTarget.dataset.idx)
    const field = e.currentTarget.dataset.field
    const tourists = this.data.tourists
    tourists[idx][field] = e.detail.value
    this.setData({ tourists })
  },

  syncTourists() {
    const need = this.data.adultCount + this.data.childCount
    const list = this.data.tourists.slice(0, need)
    while (list.length < need) {
      list.push({ name: '', idType: 'id_card', idNo: '', phone: '', isAdult: list.length < this.data.adultCount ? 1 : 0 })
    }
    this.setData({ tourists: list })
  },

  calc() {
    if (!this.data.product) return
    const total = Number(this.data.product.adultPrice) * this.data.adultCount +
      Number(this.data.product.childPrice) * this.data.childCount
    this.setData({ total: total.toFixed(2) })
  },

  async submit() {
    if (!this.data.contactName || !this.data.contactPhone) {
      wx.showToast({ title: '请填写联系人和手机号', icon: 'none' })
      return
    }
    if (this.data.tourists.some((t) => !t.name || !t.idNo)) {
      wx.showToast({ title: '请完善游客实名信息', icon: 'none' })
      return
    }
    this.setData({ submitting: true })
    try {
      const order = await request({
        url: '/public/orders',
        method: 'POST',
        data: {
          productId: this.data.productId,
          scheduleId: this.data.scheduleId,
          adultCount: this.data.adultCount,
          childCount: this.data.childCount,
          contactName: this.data.contactName,
          contactPhone: this.data.contactPhone,
          tourists: this.data.tourists,
          channel: 'miniprogram'
        }
      })
      wx.showModal({
        title: '预订成功',
        content: '订单号：' + order.orderNo + '，是否立即支付？',
        confirmText: '去支付',
        success: (r) => {
          if (r.confirm) {
            wx.redirectTo({ url: '/pages/order/detail?id=' + order.id })
          } else {
            wx.redirectTo({ url: '/pages/order/list' })
          }
        }
      })
    } catch (e) {
      /* toast handled */
    } finally {
      this.setData({ submitting: false })
    }
  }
})
