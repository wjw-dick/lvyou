const app = getApp()

function request({ url, method = 'GET', data = {}, auth = true, loading = false }) {
  const baseUrl = app.globalData.baseUrl
  const token = wx.getStorageSync('tp_token')
  if (loading) wx.showLoading({ title: '加载中', mask: true })
  return new Promise((resolve, reject) => {
    wx.request({
      url: baseUrl + url,
      method,
      data,
      header: {
        'Content-Type': 'application/json',
        'X-Tenant-Code': app.globalData.tenantCode,
        ...(auth && token ? { Authorization: 'Bearer ' + token } : {})
      },
      success(res) {
        if (loading) wx.hideLoading()
        const body = res.data
        if (body && typeof body === 'object' && 'code' in body) {
          if (body.code === 0) return resolve(body.data)
          wx.showToast({ title: body.message || '请求失败', icon: 'none' })
          return reject(new Error(body.message))
        }
        resolve(body)
      },
      fail(err) {
        if (loading) wx.hideLoading()
        wx.showToast({ title: '网络异常', icon: 'none' })
        reject(err)
      }
    })
  })
}

function upload(filePath, bizType = 'common') {
  return new Promise((resolve, reject) => {
    wx.uploadFile({
      url: app.globalData.baseUrl + '/upload',
      filePath,
      name: 'file',
      formData: { bizType },
      header: {
        'X-Tenant-Code': app.globalData.tenantCode,
        Authorization: 'Bearer ' + wx.getStorageSync('tp_token')
      },
      success(res) {
        const body = JSON.parse(res.data)
        if (body.code === 0) resolve(body.data)
        else reject(new Error(body.message))
      },
      fail: reject
    })
  })
}

module.exports = { request, upload }
