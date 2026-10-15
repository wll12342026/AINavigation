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

## 开发运行

```powershell
npm install
npm run dev
```

访问 `http://127.0.0.1:5173/`。

## 后端与数据库

后端监听 `8088`，首次运行会自动创建 `freshman_campus_ai` 数据库、数据表与演示数据。数据库密码只通过环境变量传入，不提交到代码仓库。

```powershell
$env:CAMPUS_DB_PASSWORD = "<你的本机 MySQL 密码>"
npm run dev:server
```

另开一个终端启动管理端：

```powershell
npm run dev
```

开发环境中的 `/api` 会自动代理到后端。常用接口包括：

- `GET /actuator/health`：数据库与服务健康状态
- `GET /api/dashboard/overview`：管理首页汇总数据
- `GET/POST/PUT/DELETE /api/places`：校园地点
- `GET/POST/PUT/DELETE /api/roads`：导航路网
- `GET/POST/PUT/DELETE /api/tasks`：新生任务
- `GET/POST/PUT/DELETE /api/knowledge`：AI 知识库
- `POST /api/ai/analyze`：空间运营影响分析
- `POST /api/auth/login`：演示登录接口

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

`npm run dev:mobile` 启动后会显示二维码。手机端包含首页、3D校园、AI助手、新生任务和个人中心五个导航页面；网络受限时使用 tunnel 模式。

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
