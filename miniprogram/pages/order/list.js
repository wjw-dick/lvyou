const { request } = require('../../utils/request')
const { isLogin } = require('../../utils/auth')

Page({
  data: {
    status: '',
    tabs: [
      { value: '', label: '全部' },
      { value: 1, label: '待支付' },
      { value: 3, label: '已成团' },
      { value: 5, label: '已出团' },
      { value: 6, label: '已核销' }
    ],
    orders: [],
    statusMap: { 1: '待支付', 2: '待确认', 3: '已成团', 4: '待出团', 5: '已出团', 6: '已核销', 7: '已完结', 8: '已取消' },
    loading: false
  },

  onShow() {
    if (!isLogin()) {
      wx.navigateTo({ url: '/pages/login/index' })
      return
    }
    this.load()
  },

  async load() {
    this.setData({ loading: true })
    try {
      const orders = await request({ url: '/member/orders', data: { status: this.data.status || undefined } })
      this.setData({ orders: orders || [], loading: false })
    } catch (e) {
      this.setData({ loading: false })
    }
  },

  changeTab(e) {
    this.setData({ status: e.currentTarget.dataset.value }, () => this.load())
  },

  goDetail(e) {
    wx.navigateTo({ url: '/pages/order/detail?id=' + e.currentTarget.dataset.id })
  },

  async pay(e) {
    const id = e.currentTarget.dataset.id
    await request({ url: '/member/orders/' + id + '/pay', method: 'POST' })
    wx.showToast({ title: '支付成功', icon: 'success' })
    this.load()
  }
})
