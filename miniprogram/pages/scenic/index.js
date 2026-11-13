const { request } = require('../../utils/request')
const { isLogin } = require('../../utils/auth')

Page({
  data: {
    tab: 'verify',
    code: '',
    gate: '1号闸口',
    gates: ['1号闸口', '2号闸口', '团队通道', '备用闸口'],
    result: null,
    traffic: {},
    records: [],
    notices: [],
    me: {},
    verifying: false
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
      const [traffic, records, notices, me] = await Promise.all([
        request({ url: '/scenic/app/traffic' }),
        request({ url: '/scenic/app/records' }),
        request({ url: '/scenic/app/notices' }),
        request({ url: '/scenic/app/me' })
      ])
      this.setData({ traffic: traffic || {}, records: records || [], notices: notices || [], me: me || {} })
    } catch (e) { /* ignore */ }
  },

  switchTab(e) {
    this.setData({ tab: e.currentTarget.dataset.tab })
  },

  onCode(e) {
    this.setData({ code: e.detail.value })
  },

  onGate(e) {
    this.setData({ gate: this.data.gates[Number(e.detail.value)] })
  },

  scan() {
    wx.scanCode({
      success: (res) => {
        this.setData({ code: res.result })
        this.verify()
      }
    })
  },

  async verify() {
    if (!this.data.code) {
      wx.showToast({ title: '请输入/扫描门票二维码', icon: 'none' })
      return
    }
    this.setData({ verifying: true, result: null })
    try {
      const r = await request({
        url: '/scenic/verify',
        method: 'POST',
        data: { code: this.data.code, gate: this.data.gate, verifyType: 'qrcode' }
      })
      this.setData({
        result: {
          ok: true,
          title: '验票成功',
          sub: `${r.visitor || '游客'} · ${r.gate} · 第 ${r.verifyCount} 次核销${r.multiDay ? '（多日票）' : ''}`
        },
        code: ''
      })
      wx.vibrateShort({ type: 'light' })
      this.load()
    } catch (e) {
      this.setData({ result: { ok: false, title: '验票失败', sub: e.message || '门票无效或已核销' } })
    } finally {
      this.setData({ verifying: false })
    }
  }
})
