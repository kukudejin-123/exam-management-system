# 考试管理系统（exam-management-system）

> 软件工程实训 · 任务一。对应实训内容 4「完成技术预研，搭建开发框架」。
> 技术选型与依据见 Wiki `11-技术预研报告.md`，测试体系与界面规范见 `12-开发框架与单元测试.md`。

## 技术栈

| 层 | 选型 | 版本 |
|---|---|---|
| 后端框架 | Spring Boot | 3.2.5 |
| 安全 | Spring Security + JWT | 6.2 / jjwt 0.12.5 |
| 数据访问 | MyBatis-Plus | 3.5.5 |
| 数据库 | MySQL | 8.0 |
| 缓存 | Redis | 7.x |
| 前端 | Vue 3 + Vite + Element Plus | 3.4 / 5.x / 2.x |
| 单元测试 | JUnit 5 + Mockito + AssertJ + Testcontainers + Vitest | — |
| 覆盖率 | JaCoCo（行 ≥70%、分支 ≥60% 门禁） | 0.8.12 |
| 构建 | Maven | 3.9 |
| CI | GitHub Actions / Azure Pipelines | — |

## 模块划分

```
exam-common       通用能力：统一响应、异常、错误码、分页、脱敏工具
exam-repository   数据访问：实体、Mapper、SQL
exam-security     安全认证：JWT、Spring Security 配置、RBAC
exam-service      业务逻辑：Service 接口与实现、事务边界
exam-api          对外 REST 接口：Controller、DTO、参数校验
exam-server       启动模块：配置、打包、容器化
```

## 快速开始

前置：JDK 17+、Maven 3.9+、Docker（Testcontainers 需要）。

```bash
# 1. 起本地依赖（MySQL 8 + Redis 7）
docker compose -f docker/docker-compose.yml up -d

# 2. 建表
mysql -h127.0.0.1 -uroot -proot123 < doc/sql/schema.sql

# 3. 编译并跑全部测试（含 JaCoCo 门禁）
mvn clean verify

# 4. 启动
mvn -pl exam-server spring-boot:run
```

启动后：接口 `http://localhost:8080/api`，接口文档 `http://localhost:8080/swagger-ui.html`，健康检查 `http://localhost:8080/actuator/health`。

## 测试

```bash
mvn clean verify                              # 全量（单测 + 集成 + 覆盖率门禁）
mvn test -Dtest='*Test'                       # 仅单元测试
mvn test -Dtest='*IT'                         # 仅集成测试
cd web && npm run test:unit                   # 前端
```

覆盖率报告：`<module>/target/site/jacoco/index.html`。

## 进度

| 阶段 | 内容 | 状态 |
|---|---|---|
| Phase 0 | 工程骨架（pom、目录、CI、README） | 已完成 |
| Phase 1 | exam-common（统一响应/异常/工具） | 已完成，23 条用例，行覆盖 81.3% / 分支 65.4% |
| Phase 2~10 | 数据库、repository、security、service、api、server、前端 | 进行中 |
