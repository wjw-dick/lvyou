Page({
  data: { type: 'user' },
  onLoad(options) {
    this.setData({ type: options.type || 'user' })
    wx.setNavigationBarTitle({ title: options.type === 'privacy' ? '隐私协议' : '用户协议' })
  }
})
