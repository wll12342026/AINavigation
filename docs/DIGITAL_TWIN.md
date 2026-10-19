# 校园高斯模型接入

## 本轮完成范围

数字孪生入口为 `/#/twin`，使用 Vue 3 组件 `SplatViewer.vue` 嵌入固定版本 `@playcanvas/supersplat-viewer@1.28.0`。本轮复用 PlantGrid 的技术路径，不迁移它的账号、数据库、树木业务、模型文件或页面文案。

- 支持已有 SOG 的 HTTP(S) 地址，以及完整 LOD 目录中的 `lod-meta.json` 地址。
- 场景名称和地址通过现有 `GET/PUT /api/settings` 保存，键为 `twinModelName`、`twinModelUrl`，不增加模型数据库表。
- 地址未保存时可以临时预览；支持失败重试、重新加载复位和浏览器支持情况下的全屏。
- 状态分为空模型、加载中、模型首帧已显示、加载失败。不会把 iframe 的 load 事件当成模型已渲染。
- 原路线仿真原型保存在 `src/views/RouteSimulationView.vue`，当前未挂载新路由；未经标定的路线不叠加到真实模型上。
- 设置页只读展示场景地址；通用设置保存不会覆盖数字孪生页维护的模型配置。

## 放置模型

启动或构建前会自动创建当前项目的 `public/models/` 目录。例如把自己的真实模型放到：

```text
C:\Users\31162\DeskBox\文件夹\wll\图书馆管理系统\public\models\campus.sog
```

运行网页后，先在浏览器确认模型 URL 可访问：

```text
http://127.0.0.1:5173/models/campus.sog
```

在数字孪生页填写场景名称和 `/models/campus.sog`，点击“临时预览”，模型首帧显示后可保存。不要把磁盘路径填入地址栏。同网段设备访问时，同源路径会自动使用当前网页地址；远程地址需要正确配置 CORS。

LOD 必须提供整个关联资源目录，不可只复制 JSON。生产构建会把 public 中的模型复制到 dist；模型过大时建议改用独立文件服务。`public/models/` 已忽略 Git，运行数据不会随源码上传。

当前不提供模型上传、照片训练或 PLY 转换。普通点云 PLY、GLB、OBJ、FBX 不是此入口的模型输入。受保护资源可使用受控 Cookie 或短期签名地址；iframe 不会自动附带业务接口的自定义授权请求头。

## 构建与状态协议

`scripts/build-twin-viewer.mjs` 在 `predev`、`prebuild` 执行，生成查看器 HTML、JS、CSS、配置、状态桥接和许可证。生成目录 `public/twin-viewer/` 不提交 Git。

桥接基于已核对的 1.28.0 模板与源码：`viewer.global.events` 的 firstFrame / progress:changed 事件，以及异常消息。升级依赖后需要重新核对内部事件。父页面验证消息来源窗口、同源 origin 和每次加载独立的 channel。

渲染 budget 参数单位为**百万点**，低内存/移动设备传1，普通桌面传3，高内存桌面传6；这不是模型总点数，也不是模型内存占用的保证。

## 验证结果

- 网页 TypeScript 与 Vite 生产构建通过，开发与正式构建产物均包含查看器。
- 3项地址与加载预算测试通过。
- 5项 Edge 无头浏览器测试通过：无模型状态、错误格式/磁盘路径、真实查看器404与重试、首帧消息验证与复位、临时预览/配置保存协议。
- 保存协议测试拦截设置接口，不改动实际数据库中的模型配置；就绪消息测试使用模拟桥接，不代表真实模型渲染通过。
- 已检查无模型与失败状态的页面截图，位于 output/twin-model-empty.png、output/twin-model-error.png。

测试命令（先运行网页开发服务；Windows使用已安装的Edge）：

```powershell
npm run test:twin
```

**尚未具备真实校园 SOG 文件**，因此真实模型加载、鼠标旋转/缩放/平移、真实模型全屏、移动设备性能及完整LOD加载均待实测。当前交付为可接入的查看器与管理入口，不宣称数字孪生真实模型验收完成。

## 下一步

模型到位后先验证资源和视角操作，再采集至少一组校园控制点，建立模型局部坐标与道路/地点坐标的映射。验证误差后再开发建筑标注、路线叠加、镜头跟随和导航仿真；仅把模型放进页面不能自动获得这些能力。
