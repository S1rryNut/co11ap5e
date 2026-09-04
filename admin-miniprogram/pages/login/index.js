const { login, request } = require('../../utils/request')

Page({
  data: {
    username: 'admin',
    password: '',
    loading: false,
    message: ''
  },
  onInput(event) {
    this.setData({ [event.currentTarget.dataset.field]: event.detail.value })
  },
  async submit() {
    if (!this.data.username || !this.data.password) {
      this.setData({ message: '请输入用户名和密码' })
      return
    }
    this.setData({ loading: true, message: '' })
    try {
      await login(this.data.username, this.data.password)
      await request({ url: '/admin/auth/me' })
      wx.redirectTo({ url: '/pages/articles/index' })
    } catch (error) {
      this.setData({ message: error && error.status === 401 ? '用户名或密码错误' : '登录失败，请检查服务器地址' })
    } finally {
      this.setData({ loading: false })
    }
  },
  goSettings() {
    wx.navigateTo({ url: '/pages/settings/index' })
  }
})
