# 新生智游——Expo 学生手机端

基于 Expo SDK 57、React Native 与 TypeScript 的原生手机预览应用，重点展示“校园任务智能体”而不是普通地图导航。可直接使用 Expo Go 扫码运行。

## 主要页面

- 首页：AI办事意图输入、智能服务、推荐路线
- 3D校园：三维场景导航、任务节点、入口实景
- 问AI：任务理解、流程生成、材料提醒、证据来源
- 任务：报到流程、材料检查、AI动态重排
- 我的：校园足迹、历史路线、AI对话和隐私设置

## 命令

```powershell
npm install
npm run start
```

启动后：

- Android：在 Expo Go 中点击“Scan QR code”扫描终端二维码。
- iPhone：使用系统相机扫描二维码；如提示账号限制，在电脑和 Expo Go 中登录同一个 Expo 账号。
- 手机和电脑通常需要连接同一 Wi-Fi；局域网不通时运行 `npm run start:tunnel`。

“问 AI”页面会从 Expo 开发地址自动识别电脑 IP，并访问电脑的 `8088` 后端；无需把当前 Wi-Fi 地址写进代码。请先在项目根目录启动后端，并允许 Java 使用 Windows 专用网络。

## 工程结构

```text
App.tsx                 # 五个页面的入口和底部导航
src/components/         # 数字孪生占位场景与通用组件
src/screens/            # 首页、3D校园、AI、任务、我的
src/theme.ts            # 颜色和阴影设计令牌
app.json                # Expo 应用配置
```
