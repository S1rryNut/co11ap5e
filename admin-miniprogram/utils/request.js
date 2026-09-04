const DEFAULT_BASE_URL = 'https://co11ap5e.site/api'

const getBaseUrl = () => wx.getStorageSync('apiBase') || DEFAULT_BASE_URL
const getToken = () => wx.getStorageSync('adminToken') || ''

const request = ({ url, method = 'GET', data = {}, auth = true }) => new Promise((resolve, reject) => {
  wx.request({
    url: `${getBaseUrl()}${url}`,
    method,
    data,
    header: {
      'content-type': 'application/json',
      'X-Mini-Program-Client': 'co11ap5e-admin',
      ...(auth && getToken() ? { Authorization: `Bearer ${getToken()}` } : {})
    },
    success(response) {
      if (response.statusCode >= 200 && response.statusCode < 300) {
        resolve(response.data)
        return
      }
      reject({ status: response.statusCode, data: response.data })
    },
    fail(error) {
      reject(error)
    }
  })
})

const login = async (username, password) => {
  const result = await request({
    url: '/admin/auth/mobile-login',
    method: 'POST',
    auth: false,
    data: { username, password }
  })
  wx.setStorageSync('adminToken', result.token)
  return result
}

module.exports = {
  getBaseUrl,
  setBaseUrl: value => wx.setStorageSync('apiBase', value),
  getToken,
  setToken: value => wx.setStorageSync('adminToken', value),
  clearToken: () => wx.removeStorageSync('adminToken'),
  request,
  login
}
