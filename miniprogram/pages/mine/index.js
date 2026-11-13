const { isLogin, logout } = require('../../utils/auth')

Page({
  data: {
    user: {},
    role: 'member',
    lang: '中文',
    roleMap: { member: '游客会员', guide: '导游', scenic: '景区员工' }
  },

  onShow() {
    const user = wx.getStorageSync('tp_user') || {}
    this.setData({
      user,
      role: user.userType === 'GUIDE' ? 'guide' : user.userType === 'SCENIC' ? 'scenic' : 'member'
    })
  },

  goLogin() {
    wx.navigateTo({ url: '/pages/login/index' })
  },

  goOrders() {
    wx.switchTab({ url: '/pages/order/list' })
  },

  goMember() {
    wx.navigateTo({ url: '/pages/member/index' })
  },

  goMessages() {
    navigateGuard('/pages/message/index')
  },

  goGuide() {
    wx.navigateTo({ url: '/pages/guide/index' })
  },

  goScenic() {
    wx.navigateTo({ url: '/pages/scenic/index' })
  },

  goAgreement(e) {
    wx.navigateTo({ url: '/pages/agreement/index?type=' + e.currentTarget.dataset.type })
  },

  switchLang() {
    wx.showActionSheet({
      itemList: ['简体中文', 'English'],
      success: (r) => this.setData({ lang: r.tapIndex === 0 ? '中文' : 'English' })
    })
  },

  doLogout() {
    wx.showModal({
      title: '提示',
      content: '确定退出登录？',
      success: (r) => {
        if (r.confirm) {
          logout()
          this.setData({ user: {} })
        }
      }
    })
  }
})

function navigateGuard(url) {
  if (!isLogin()) {
    wx.navigateTo({ url: '/pages/login/index' })
    return
  }
  wx.navigateTo({ url })
}
