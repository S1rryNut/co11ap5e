const { request, clearToken } = require('../../utils/request')

Page({
  data: {
    articles: [],
    loading: true,
    message: ''
  },
  onShow() {
    this.load()
  },
  async load() {
    this.setData({ loading: true, message: '' })
    try {
      const articles = await request({ url: '/admin/articles' })
      this.setData({ articles })
    } catch (error) {
      if (error && error.status === 403) {
        clearToken()
        wx.reLaunch({ url: '/pages/login/index' })
        return
      }
      this.setData({ message: '文章加载失败' })
    } finally {
      this.setData({ loading: false })
    }
  },
  open(event) {
    wx.navigateTo({ url: `/pages/editor/index?id=${event.currentTarget.dataset.id}` })
  },
  create() {
    wx.navigateTo({ url: '/pages/editor/index' })
  },
  settings() {
    wx.navigateTo({ url: '/pages/settings/index' })
  }
})
