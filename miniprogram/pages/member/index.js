const { request } = require('../../utils/request')
const { isLogin } = require('../../utils/auth')

Page({
  data: {
    profile: {},
    coupons: [],
    points: [],
    tab: 'coupons',
    bizMap: { order: '订单积分', exchange: '积分兑换', adjust: '后台调整', recharge: '充值' }
  },

  onShow() {
    if (!isLogin()) {
      wx.navigateTo({ url: '/pages/login/index' })
      return
    }
    this.load()
  },

  async load() {
    try {
      const [profile, coupons, points] = await Promise.all([
        request({ url: '/member/profile' }),
        request({ url: '/member/coupons' }),
        request({ url: '/member/points/logs' })
      ])
      this.setData({ profile: profile || {}, coupons: coupons || [], points: points || [] })
    } catch (e) { /* ignore */ }
  },

  switchTab(e) {
    this.setData({ tab: e.currentTarget.dataset.tab })
  },

  goOrders() {
    wx.switchTab({ url: '/pages/order/list' })
  },

  goMessages() {
    wx.navigateTo({ url: '/pages/message/index' })
  }
})
