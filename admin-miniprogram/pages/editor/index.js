const { request } = require('../../utils/request')

Page({
  data: {
    id: '',
    form: {
      title: '',
      slug: '',
      excerpt: '',
      category: '工程实践',
      tags: [],
      content: '',
      published: true,
      readingMinutes: 5
    },
    loading: false,
    saving: false,
    message: ''
  },
  onLoad(options) {
    if (options.id) this.loadArticle(options.id)
  },
  async loadArticle(id) {
    this.setData({ id, loading: true })
    try {
      const article = await request({ url: `/admin/articles/${id}` })
      this.setData({ form: article })
    } catch {
      this.setData({ message: '文章加载失败' })
    } finally {
      this.setData({ loading: false })
    }
  },
  input(event) {
    const field = event.currentTarget.dataset.field
    this.setData({ [`form.${field}`]: event.detail.value })
  },
  category(event) {
    this.setData({ 'form.category': event.detail.value })
  },
  togglePublished(event) {
    this.setData({ 'form.published': event.detail.value })
  },
  async save() {
    const form = this.data.form
    if (!form.title || !form.slug || !form.excerpt || !form.content) {
      this.setData({ message: '标题、固定链接、摘要和正文不能为空' })
      return
    }
    this.setData({ saving: true, message: '' })
    try {
      await request({
        url: this.data.id ? `/admin/articles/${this.data.id}` : '/admin/articles',
        method: this.data.id ? 'PUT' : 'POST',
        data: form
      })
      wx.navigateBack()
    } catch (error) {
      this.setData({ message: error && error.status === 409 ? '固定链接已存在' : '保存失败' })
    } finally {
      this.setData({ saving: false })
    }
  }
})
