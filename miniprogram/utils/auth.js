const { request } = require('./request')

function saveLogin(result) {
  wx.setStorageSync('tp_token', result.token)
  wx.setStorageSync('tp_user', result)
  const app = getApp()
  app.globalData.token = result.token
  app.globalData.user = result
  app.globalData.role = result.userType === 'GUIDE' ? 'guide' : result.userType === 'SCENIC' ? 'scenic' : 'member'
}

function isLogin() {
  return !!wx.getStorageSync('tp_token')
}

function logout() {
  wx.removeStorageSync('tp_token')
  wx.removeStorageSync('tp_user')
  const app = getApp()
  app.globalData.token = ''
  app.globalData.user = null
  app.globalData.role = 'member'
}

function requireLogin() {
  if (isLogin()) return true
  wx.navigateTo({ url: '/pages/login/index' })
  return false
}

async function wechatLogin(nickname) {
  const code = await new Promise((resolve, reject) => {
    wx.login({ success: (r) => resolve(r.code), fail: reject })
  })
  const result = await request({
    url: '/auth/member-login',
    method: 'POST',
    auth: false,
    data: { tenantCode: getApp().globalData.tenantCode, code, nickname: nickname || '微信游客' }
  })
  saveLogin(result)
  return result
}

module.exports = { saveLogin, isLogin, logout, requireLogin, wechatLogin }
