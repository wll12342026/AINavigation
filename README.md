# 新生智游——校园数字孪生与AI新生服务系统

面向新生校园导航与任务服务的管理端与学生手机端。管理首页已接入 Spring Boot + MySQL 后端；数字孪生场景在真实模型导入前继续使用可替换的演示底图。

## 技术架构

- Vue 3：组件化页面
- Vite：开发与生产构建
- TypeScript：类型约束
- Vue Router：九个管理页面路由
- Pinia：全局校区状态和操作提示
- Spring Boot：业务接口、数据校验与健康检查
- Spring Data JPA + MySQL：地点、路网、任务和知识库数据
- 自建设计系统：不依赖重型 UI 框架，保留参考图的视觉风格

## 项目结构

```text
src/
├─ components/
│  ├─ common/          # 指标卡、场景、消息等通用组件
│  └─ layout/          # 侧边导航与顶部栏
├─ data/                # 导航与静态配置
├─ router/              # 路由配置
├─ stores/              # Pinia 状态
├─ types/               # TypeScript 类型
└─ views/               # 九个独立页面
server/                  # Spring Boot 后端
student-mobile/          # Expo 学生手机端
```

`prototype-static/` 保留迁移前的静态视觉原型，仅作对照，不是正式运行入口。

## 手动启动（Windows）

### 1. 首次运行准备

请先安装以下软件：

- Node.js 20 或更高版本
- MySQL 8
- JDK 21 或更高版本
- Maven 3.9 或更高版本
- 手机端预览需要在手机安装 Expo Go

第一次下载项目后，在项目根目录安装依赖：

```powershell
cd "C:\Users\31162\OneDrive\Desktop\wll\图书馆管理系统"
npm install
```

启动完整项目需要打开三个 PowerShell 窗口。三个窗口都不要关闭；需要停止服务时，在对应窗口按 `Ctrl + C`。

### 2. 终端一：启动后端和数据库连接

先确认本机 MySQL 服务已经启动，然后执行：

```powershell
cd "C:\Users\31162\OneDrive\Desktop\wll\图书馆管理系统"
$env:CAMPUS_DB_PASSWORD = "<你的本机 MySQL 密码>"
npm run dev:server
```

密码只在当前 PowerShell 窗口中临时生效，不要把真实密码写进代码或提交到 GitHub。

正常模式不会自动生成演示地点、道路、任务或知识。只有明确设置下面的变量时才会生成演示数据：

```powershell
$env:CAMPUS_SEED_DEMO_DATA = "true"
```

旧版本已经生成的数据不会被直接删除。新版后端会识别并标记这些历史演示记录，正常业务接口和首页统计默认排除它们；管理首页会显示被排除的记录数量。新建和导入的数据默认作为正式数据参与统计。

后端监听 `8088` 端口。首次运行会自动创建 `freshman_campus_ai` 数据库、数据表和演示数据。出现 Spring Boot 启动完成信息后，可访问：

```text
http://127.0.0.1:8088/actuator/health
```

页面返回 `UP` 表示后端和数据库连接正常。

### 3. 终端二：启动网页管理端

新打开一个 PowerShell 窗口并执行：

```powershell
cd "C:\Users\31162\OneDrive\Desktop\wll\图书馆管理系统"
npm run dev
```

本机浏览器访问：

```text
http://127.0.0.1:5173/
```

开发环境中的 `/api` 会自动转发到 `http://127.0.0.1:8088`，因此必须同时保持后端运行。

### 4. 终端三：启动 Expo 手机端

再打开一个 PowerShell 窗口并执行：

```powershell
cd "C:\Users\31162\OneDrive\Desktop\wll\图书馆管理系统"
npm run dev:mobile
```

终端显示二维码后：

- Android：使用 Expo Go 扫描二维码。
- iPhone：电脑端 Expo CLI 与手机端 Expo Go 建议登录同一个 Expo 账号，再用系统相机或 Expo Go 扫码。
- 手机和电脑应连接同一个 Wi-Fi，并允许 Windows 防火墙放行 Node.js、Java、5173、8081 和 8088 端口。

如果局域网模式无法连接，可以改用隧道模式：

```powershell
npm run dev:mobile:tunnel
```

### 5. 同一 Wi-Fi 下用其他设备访问网页

在电脑 PowerShell 中查看当前无线网卡的 IPv4 地址：

```powershell
ipconfig
```

例如电脑地址为 `192.168.1.20`，同一 Wi-Fi 下的手机或其他电脑访问：

```text
http://192.168.1.20:5173/
```

`127.0.0.1` 只代表当前设备，手机不能使用 `http://127.0.0.1:5173/` 访问电脑上的项目。更换 Wi-Fi 后，局域网 IPv4 地址可能变化，需要重新执行 `ipconfig` 查看。

### 6. 推荐启动顺序

1. 启动 MySQL。
2. 启动后端，确认健康检查为 `UP`。
3. 启动 Vue 网页管理端。
4. 需要手机预览时，再启动 Expo。

### 7. 常见问题

- 网页能打开但没有数据：检查后端窗口是否仍在运行，并访问 `http://127.0.0.1:8088/actuator/health`。
- 后端提示数据库连接失败：检查 MySQL 是否启动，以及 `CAMPUS_DB_PASSWORD` 是否为本机正确密码。
- 手机打不开网页：确认手机和电脑处于同一 Wi-Fi，并检查 Windows 防火墙是否放行。
- Expo 二维码无法连接：先关闭原 Expo 进程，再执行 `npm run dev:mobile:tunnel`。
- 端口被占用：关闭之前启动的同类服务，或在其终端按 `Ctrl + C` 后重新启动。

## 后端接口

开发环境中的 `/api` 会自动代理到后端。常用接口包括：

- `GET /actuator/health`：数据库与服务健康状态
- `GET /api/dashboard/overview`：管理首页汇总数据
- `GET /api/analytics/overview`：AI咨询、任务和热门地点分析
- `GET/POST/PUT/DELETE /api/places`：校园地点
- `GET/POST/PUT/DELETE /api/roads`：导航路网
- `GET/POST/PUT/DELETE /api/tasks`：新生任务
- `GET/POST/PUT/DELETE /api/knowledge`：AI 知识库
- `GET/POST/PUT/DELETE /api/media`：实景素材元数据
- `GET /api/media/summary`：素材质检汇总
- `GET/PUT /api/settings`：系统配置
- `POST /api/ai/analyze`：空间运营影响分析
- `POST /api/auth/login`：演示登录接口

## 数据真实性规则

- 数量、比例、图表、最近更新、任务进度和AI咨询趋势均由后端汇总，前端不再保存业务兜底数字。
- 接口失败时页面显示错误或空状态，不会切换到本地演示数据。
- AI分析只返回当前数据库中能够匹配的地点、道路、任务和知识；找不到证据时会明确拒绝编造。
- 无人机数字孪生模型尚未导入时只显示占位底图，模型进度固定为未接入，不伪造场景坐标或导航路线。
- 实景图片文件上传功能尚未接入对象存储；当前素材页管理的是数据库中的素材地址、地点绑定和质检元数据。

后端测试：

```powershell
npm run test:server
```

## 生产构建

```powershell
npm run build
```

构建产物位于 `dist/`。当前版本已通过 TypeScript 检查与 Vite 生产构建。

## 学生手机端

手机端位于 `student-mobile/`，使用 Expo SDK 57、React Native 和 TypeScript，可通过 Expo Go 扫码进行真机预览。

```powershell
npm run dev:mobile
npm run dev:mobile:tunnel
npm run check:mobile
npm run build:mobile
```

`npm run dev:mobile` 启动后会显示二维码。手机端包含首页、3D校园、AI助手、新生任务和个人中心五个导航页面；网络受限时使用 tunnel 模式。完整启动方法见上方“手动启动（Windows）”。

## 已实现路由

- `/` 运行总览
- `/twin` 数字孪生
- `/places` 校园地点
- `/network` 导航路网
- `/tasks` 新生任务
- `/knowledge` AI 知识库
- `/media` 实景素材
- `/analytics` 运营分析
- `/settings` 系统设置
