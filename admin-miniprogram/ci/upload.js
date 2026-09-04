/**
 * miniprogram-ci 上传脚本（模板）
 * 用法：
 *   npm i -D miniprogram-ci
 *   node ci/upload.js --appid <AppID> --key ci/private.key --version 1.0.0 --desc "折叠思维管理台"
 *
 * 前置：微信公众平台生成「小程序代码上传密钥」放入 ci/private.key，AppID 取正式小程序 AppID。
 */
const ci = require('miniprogram-ci')
const path = require('path')
const fs = require('fs')

const args = Object.fromEntries(
  process.argv.slice(2).map((arg, i, arr) =>
    arg.startsWith('--') ? [arg.slice(2), arr[i + 1]] : null
  ).filter(Boolean)
)

const appid = args.appid
const keyPath = args.key || 'ci/private.key'
const version = args.version || '1.0.0'
const desc = args.desc || '折叠思维管理台'

if (!appid) {
  console.error('缺少 --appid 参数')
  process.exit(1)
}
if (!fs.existsSync(path.resolve(__dirname, '..', keyPath))) {
  console.error(`找不到上传密钥：${keyPath}（请在微信公众平台生成并放入）`)
  process.exit(1)
}

async function main() {
  const project = new ci.Project({
    appid,
    type: 'miniProgram',
    projectPath: path.resolve(__dirname, '..'),
    privateKeyPath: path.resolve(__dirname, '..', keyPath),
    ignores: ['node_modules/**/*', 'ci/**/*']
  })

  const uploadResult = await ci.upload({
    project,
    version,
    desc,
    setting: { es6: true, minify: true, urlCheck: false }
  })
  console.log('上传成功：', uploadResult)
}

main().catch(err => {
  console.error('上传失败：', err)
  process.exit(1)
})
