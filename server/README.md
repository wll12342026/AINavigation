# 新生智游后端

Spring Boot + Spring Data JPA + MySQL 后端，为管理端和学生端提供统一数据接口。

## 本地运行

需要 Java 21+、Maven 3.9+ 与 MySQL 8。不要把数据库密码写进 `application.yml`。

```powershell
$env:CAMPUS_DB_PASSWORD = "<你的本机 MySQL 密码>"
mvn spring-boot:run
```

默认配置：

- 服务端口：`8088`
- 数据库：`freshman_campus_ai`
- 数据库用户：`root`
- 建表策略：开发阶段自动增量建表
- 局域网访问：监听 `0.0.0.0`

如需覆盖默认值，可设置 `CAMPUS_DB_URL`、`CAMPUS_DB_USERNAME` 与 `CAMPUS_DB_PASSWORD`。

## 当前模块

- `place`：校园语义地点与坐标
- `road`：可通行路段、限制原因和无障碍属性
- `task`：新生任务及完成进度
- `knowledge`：AI 可追溯知识条目与有效期
- `dashboard`：管理首页实时聚合
- `ai`：运营影响分析接口；当前是可替换的规则基线，后续可接入大模型、RAG 与路径算法
- `auth`：演示登录接口，正式部署前需更换为 Spring Security + JWT

## 验证

```powershell
mvn test
```

测试环境使用 H2 内存数据库，不会读写本机 MySQL。
