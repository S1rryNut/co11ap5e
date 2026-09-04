# 折叠思维 · 管理台小程序

个人使用的微信小程序：手机端管理网站文章（登录、列表、编辑、服务器地址设置）。

后端已配套支持：`POST /admin/auth/mobile-login`（返回 `{ username, token }`）与
`X-Mini-Program-Client: co11ap5e-admin` 请求头（走 CSRF 豁免）。代码已就绪，可直接导入开发者工具。

## 一、本地预览（开发阶段，最快上手）

1. 安装「微信开发者工具」（stable 即可）。
2. 导入本目录 `admin-miniprogram`。
3. AppID 用测试号即可（`project.config.json` 里 `appid` 当前为 `touristappid`）。
4. 详情 → 本地设置 → 勾选「不校验合法域名、web-view（业务域名）、TLS 版本以及 HTTPS 证书」。
5. 在登录页点「服务器设置」，把地址改成当前后端，例如 `http://<你的服务器地址>/api`，保存后返回登录。

> 说明：开发阶段服务器可为 IP + HTTP，仅适合开发者工具模拟器；
> 真机预览/体验版必须用 HTTPS + 已备案域名，见下文「正式部署」。

## 二、正式部署（体验版 / 发布）

前置条件（缺一不可）：

- **AppID**：在 [微信公众平台](https://mp.weixin.qq.com) 注册小程序（个人主体即可），拿到正式 AppID。
- **HTTPS 域名**：`co11ap5e.site` 备案通过，证书生效，`https://co11ap5e.site/api` 可访问。
- **合法域名**：公众平台 → 开发管理 → 开发设置 → 服务器域名，把 `https://co11ap5e.site` 加入 request 合法域名。

部署路径二选一：

### 方式 A：开发者工具上传（简单）
1. 把 `project.config.json` 的 `appid` 换成你的正式 AppID。
2. 登录开发者工具，确认 API 地址为 `https://co11ap5e.site/api`（登录页「服务器设置」）。
3. 点「上传」，填版本号与备注 → 在公众平台提交审核（个人小程序可选择不提交审核，仅发布为体验版自用）。

### 方式 B：miniprogram-ci 命令行上传（可自动化）
1. 在公众平台「开发设置 → 小程序代码上传密钥」生成并下载 `private.key`，并记下 AppID。
2. 把密钥放到本目录 `ci/private.key`（**不要提交到 Git**，已在 `.gitignore`）。
3. 安装依赖并上传：

```bash
npm i -D miniprogram-ci
node ci/upload.js --appid 你的AppID --key ci/private.key --version 1.0.0 --desc "折叠思维管理台"
```

上传后到公众平台 → 版本管理，将体验版设为可体验（成员扫码）或提交审核。

## 三、目录结构

```
app.js                  # 启动时默认 API = https://co11ap5e.site/api（可在设置页改）
app.json                # 页面与窗口配置
utils/request.js        # wx.request 封装：X-Mini-Program-Client 头、token、错误处理
pages/login             # 登录（mobile-login）
pages/articles          # 文章列表（/admin/articles）
pages/editor            # 新建/编辑文章（/admin/articles[/{id}]）
pages/settings          # 服务器地址 / 退出登录
ci/upload.js            # miniprogram-ci 上传脚本（模板）
```

## 四、安全说明

- 小程序用 `mobile-login` 换取 Bearer token，仅存于本地 Storage。
- 请勿把管理员密码、上传密钥 `ci/private.key` 提交到任何公开仓库。
