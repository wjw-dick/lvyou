const { request } = require('../../utils/request')

Page({
  data: { messages: [], bizMap: { booking: '报名确认', group: '成团通知', depart: '出团通知', settle: '结算到账', refund: '退款通知', marketing: '优惠活动', contract: '合同签署' } },
  async onShow() {
    try {
      const messages = await request({ url: '/member/messages' })
      this.setData({ messages: messages || [] })
    } catch (e) { /* ignore */ }
  }
})
