const { request, upload } = require('../../utils/request')
const { isLogin } = require('../../utils/auth')

Page({
  data: {
    tab: 'schedules',
    schedules: [],
    selected: null,
    tourists: [],
    expenses: [],
    cash: [],
    report: {},
    suppliers: [],
    expense: { type: 'ticket', amount: '', remark: '', receiptImages: '' },
    cashForm: { amount: '', remark: '' },
    expTypes: [
      { value: 'ticket', label: '门票' },
      { value: 'meal', label: '餐费' },
      { value: 'other', label: '其他消费' }
    ]
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
      const [schedules, cash, suppliers] = await Promise.all([
        request({ url: '/guide/app/schedules' }),
        request({ url: '/guide/app/cash' }),
        request({ url: '/guide/app/suppliers' })
      ])
      this.setData({ schedules: schedules || [], cash: cash || [], suppliers: suppliers || [] })
    } catch (e) { /* ignore */ }
  },

  switchTab(e) {
    this.setData({ tab: e.currentTarget.dataset.tab })
  },

  onPickSchedule(e) {
    const s = this.data.schedules[Number(e.detail.value)]
    if (s) {
      this.setData({ tab: 'schedules' })
      this.selectSchedule({ currentTarget: { dataset: { id: s.scheduleId } } })
    }
  },

  async selectSchedule(e) {
    const id = e.currentTarget.dataset.id
    const selected = this.data.schedules.find((s) => s.scheduleId === id)
    this.setData({ selected })
    wx.showLoading({ title: '加载中' })
    try {
      const [tourists, expenses, report] = await Promise.all([
        request({ url: '/guide/app/schedules/' + id + '/tourists' }),
        request({ url: '/guide/app/expenses', data: { scheduleId: id } }),
        request({ url: '/guide/app/report', data: { scheduleId: id } })
      ])
      this.setData({ tourists: tourists || [], expenses: expenses || [], report: report || {} })
    } finally {
      wx.hideLoading()
    }
  },

  async rollcall() {
    if (!this.data.selected) return
    const r = await request({
      url: '/guide/app/schedules/' + this.data.selected.scheduleId + '/rollcall',
      method: 'POST',
      data: { present: this.data.tourists.length }
    })
    wx.showModal({ title: '点名完成', content: `应到 ${r.expected} 人，实到 ${r.present} 人`, showCancel: false })
  },

  call(e) {
    wx.makePhoneCall({ phoneNumber: e.currentTarget.dataset.phone })
  },

  onExpense(e) {
    this.setData({ ['expense.' + e.currentTarget.dataset.field]: e.detail.value })
  },

  selectType(e) {
    const t = this.data.expTypes[Number(e.detail.value)]
    this.setData({ 'expense.type': t.value })
  },

  async chooseReceipt() {
    const res = await wx.chooseMedia({ count: 3, mediaType: ['image'] })
    const urls = []
    for (const f of res.tempFiles) {
      const r = await upload(f.tempFilePath, 'guide-expense')
      urls.push(r.url)
    }
    this.setData({ 'expense.receiptImages': urls.join(',') })
  },

  async submitExpense() {
    if (!this.data.selected) {
      wx.showToast({ title: '请先选择出团计划', icon: 'none' })
      return
    }
    await request({
      url: '/guide/app/expenses',
      method: 'POST',
      data: { ...this.data.expense, scheduleId: this.data.selected.scheduleId, amount: Number(this.data.expense.amount) }
    })
    wx.showToast({ title: '已提交报账', icon: 'success' })
    this.setData({ expense: { type: 'ticket', amount: '', remark: '', receiptImages: '' } })
    this.selectSchedule({ currentTarget: { dataset: { id: this.data.selected.scheduleId } } })
  },

  onCash(e) {
    this.setData({ ['cashForm.' + e.currentTarget.dataset.field]: e.detail.value })
  },

  async requestCash() {
    if (!this.data.selected) {
      wx.showToast({ title: '请先选择出团计划', icon: 'none' })
      return
    }
    await request({
      url: '/guide/app/cash',
      method: 'POST',
      data: { ...this.data.cashForm, scheduleId: this.data.selected.scheduleId, amount: Number(this.data.cashForm.amount) }
    })
    wx.showToast({ title: '领款申请已提交', icon: 'success' })
    this.load()
  }
})
