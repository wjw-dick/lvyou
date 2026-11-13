const { request } = require('../../utils/request')

Page({
  data: {
    order: null,
    tourists: [],
    logs: [],
    statusMap: { 1: '待支付', 2: '待确认', 3: '已成团', 4: '待出团', 5: '已出团', 6: '已核销', 7: '已完结', 8: '已取消' },
    payMap: { 0: '未支付', 1: '已支付', 2: '部分退款', 3: '已退款' }
  },

  onLoad(options) {
    this.orderId = options.id
    this.load()
  },

  async load() {
    const data = await request({ url: '/member/orders/' + this.orderId })
    this.setData({
      order: data.order,
      tourists: data.tourists || [],
      logs: data.logs || []
    })
  },

  async pay() {
    await request({ url: '/member/orders/' + this.orderId + '/pay', method: 'POST' })
    wx.showToast({ title: '支付成功', icon: 'success' })
    this.load()
  },

  async refund() {
    wx.showModal({
      title: '申请退改',
      editable: true,
      placeholderText: '请输入退改原因',
      success: async (res) => {
        if (!res.confirm) return
        await request({
          url: '/member/orders/' + this.orderId + '/refund',
          method: 'POST',
          data: { reason: res.content || '游客申请退改' }
        })
        wx.showToast({ title: '已提交，等待审批', icon: 'none' })
        this.load()
      }
    })
  },

  async contract() {
    const c = await request({ url: '/contracts', method: 'POST', data: { orderId: Number(this.orderId) } })
    wx.showModal({
      title: '电子合同',
      content: '合同编号：' + c.contractNo + '，是否前往签署？',
      success: async (res) => {
        if (!res.confirm) return
        await request({ url: '/contracts/' + c.id + '/sign', method: 'POST', data: { faceVerified: true } })
        wx.showToast({ title: '签署成功', icon: 'success' })
      }
    })
  },

  copyCode() {
    wx.setClipboardData({ data: this.data.order.orderNo })
  }
})
