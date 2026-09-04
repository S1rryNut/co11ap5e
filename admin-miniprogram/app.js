App({
  onLaunch() {
    if (!wx.getStorageSync('apiBase')) {
      wx.setStorageSync('apiBase', 'https://co11ap5e.site/api')
    }
  }
})
