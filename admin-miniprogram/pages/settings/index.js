const { getBaseUrl, setBaseUrl, clearToken } = require('../../utils/request')

Page({
  data: {
    apiBase: '',
    message: ''
  },
  onLoad() {
    this.setData({ apiBase: getBaseUrl() })
  },
  input(event) {
    this.setData({ apiBase: event.detail.value })
  },
  save() {
    const value = this.data.apiBase.trim().replace(/\/+$/, '')
    if (!/^https?:\/\//.test(value)) {
      this.setData({ message: '服务器地址必须以 http(s):// 开头' })
      return
    }
    setBaseUrl(value)
    this.setData({ message: '服务器地址已保存（真机需 https + 已备案域名）' })
  },
  logout() {
    clearToken()
    wx.reLaunch({ url: '/pages/login/index' })
  }
})
