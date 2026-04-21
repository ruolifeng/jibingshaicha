# AGENTS.md — 项目开发指南（AI Agent 专用）

> 本文件面向 AI Coding Agent 编写。阅读者应当被假定对此项目一无所知。
> 项目主要使用 **中文** 进行业务命名和注释，因此本文档使用中文撰写。

---

## 一、项目概述

本项目是一个基于 **Vue 3 + Vite + TypeScript** 的前后端分离式管理后台，业务领域为 **结核病（TB）筛查与追踪管理**。系统覆盖三类人群的筛查、追踪、诊断、治疗全生命周期：

- **学校人群**（学校筛查导入 → 追踪 → 胸片 → 转诊 → 诊断 → 潜伏感染者管理 / 患者管理）
- **重点人群**
- **密接人群**（支持三轮筛查：首次、半年后、一年后）

前端仓库位于根目录，后端（Spring Boot）位于 `admin/` 子目录，两者独立构建、独立部署。

### 业务主线（共用逻辑）

三条人群主线的追踪后逻辑高度共用：
1. 筛查导入 → 感染筛查阴性 → **结束归档**
2. 感染筛查阳性 → **追踪** → 系统选择（其他 / 未到位 / 到位）
3. 到位 → 上传胸片 → 转诊 → 诊断五分类：
   - 其它 → 备注原因 → 归档
   - 排除 → 归档
   - 疑似肺结核 → 后续按疑似肺结核处理
   - **潜伏感染者** → 通知单 → 预防性治疗 → 督导表 → 归集
   - **确诊患者** → 患者管理 → 通知单 → 随访 → 治疗记录卡 → 归集

> 详细流程图见 `docs/流程图识别-学校-重点-密接.md`（Mermaid 语法）。

---

## 二、技术栈

### 前端（根目录）

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue | 3.5.17 | 框架，使用 Composition API + `<script setup>` |
| TypeScript | 5.8.3 | 类型系统，`strict` 模式开启 |
| Vite | 7.0.4 | 构建工具 |
| Vue Router | 4.5.1 | 路由（默认 hash 模式，可切换 html5） |
| Pinia | 3.0.3 | 状态管理 |
| Element Plus | 2.10.4 | UI 组件库，中文语言包 |
| VXE-Table | 4.6.25 | 高级数据表格 |
| Axios | 1.10.0 | HTTP 客户端 |
| UnoCSS | 66.3.3 | 原子化 CSS（Wind3 preset） |
| SCSS | 1.78.0 (sass-embedded) | 样式预处理器 |
| Day.js | 1.11.13 | 日期处理 |
| js-cookie | 3.0.5 | Cookie 操作 |
| lodash-es | 4.17.21 | 工具函数 |
| screenfull | 6.0.2 | 全屏 API |

### 后端（`admin/` 目录）

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.3.2 | Java 后端框架（JDK 17） |
| MyBatis-Plus | 3.5.7 | ORM |
| MySQL | 8.x (mysql-connector-j 8.3.0) | 数据库 |
| HikariCP | 5.1.0 | 连接池 |
| Hutool | 5.8.31 | JWT、工具类 |
| Knife4j | 4.4.0 | API 文档（OpenAPI 3） |
| EasyExcel | 4.0.3 | Excel 导入导出 |
| Lombok | 1.18.34 | 简化实体类代码 |
| Spring Scheduling | — | 定时任务（提醒、备份、超时处理） |

---

## 三、项目结构

```
v3-admin-vite-main/
├── src/                          # 前端源码
│   ├── main.ts                   # 应用入口：注册 Pinia、Router、插件、全局样式
│   ├── App.vue                   # 根组件：el-config-provider、主题初始化
│   ├── router/                   # 路由配置
│   │   ├── index.ts              # 常量路由 + 动态路由定义
│   │   ├── config.ts             # 路由模式配置（hash / html5）、动态路由开关
│   │   ├── guard.ts              # 导航守卫：登录校验、权限校验、NProgress
│   │   ├── helper.ts             # 路由扁平化（用于 keep-alive 缓存）
│   │   └── whitelist.ts          # 免登录白名单（如 /login）
│   ├── pinia/                    # Pinia 状态管理
│   │   ├── index.ts              # createPinia() 导出
│   │   └── stores/
│   │       ├── user.ts           # 用户信息、Token、角色、权限、登出
│   │       ├── permission.ts     # 根据角色/权限过滤动态路由
│   │       ├── app.ts            # 侧边栏折叠、设备类型
│   │       ├── settings.ts       # 主题、布局模式、TagsView 等
│   │       └── tags-view.ts      # 已访问视图、缓存视图
│   ├── http/
│   │   └── axios.ts              # Axios 实例：拦截器（401 自动登出、错误处理）
│   ├── plugins/                  # 插件注册
│   │   ├── index.ts              # 统一安装入口
│   │   ├── element-plus-icons.ts # Element Plus 图标注册
│   │   ├── permission-directive.ts # 权限指令 v-permission
│   │   ├── svg-icon.ts           # SVG 图标组件（unplugin-svg-component）
│   │   └── vxe-table.ts          # VXE-Table 注册与中文配置
│   ├── layouts/                  # 布局系统
│   │   ├── index.vue             # 布局选择器（Left / Top / LeftTop 模式）
│   │   ├── modes/                # 三种布局模式组件
│   │   ├── components/           # Sidebar、Navbar、TagsView、Breadcrumb 等
│   │   └── composables/useResize.ts
│   ├── pages/                    # 页面组件（与路由一一对应）
│   │   ├── dashboard/
│   │   ├── login/
│   │   ├── school/               # 学校人群（筛查、潜伏、患者、历史）
│   │   ├── key-population/       # 重点人群
│   │   ├── close-contact/        # 密接人群
│   │   ├── statistics/
│   │   ├── message/
│   │   ├── system/               # 系统管理（用户、权限、备份）—— 仅管理员可见
│   │   └── error/
│   └── common/                   # 公共代码（路径别名 @@/）
│       ├── apis/                 # API 模块（按业务域拆分）
│       ├── components/           # 可复用业务组件
│       ├── composables/          # Vue Composables（主题、设备、分页等）
│       ├── constants/            # 常量（app-key、cache-key、疾病相关枚举）
│       ├── utils/                # 工具函数（缓存、校验、日期、权限、CSS）
│       └── assets/               # 样式、图片、SVG 图标
├── tests/                        # 测试文件
│   ├── demo.test.ts
│   ├── utils/validate.test.ts
│   └── components/Notify.test.ts
├── admin/                        # Java 后端（独立 Maven 项目）
│   ├── src/main/java/cn/luyou/   # 源码根包
│   │   ├── Application.java
│   │   ├── config/               # MyBatis-Plus、拦截器注册
│   │   ├── security/             # JWT 校验拦截器
│   │   ├── controller/           # REST 控制器（15+ 模块）
│   │   ├── service/ & impl/      # 业务层
│   │   ├── mapper/               # MyBatis-Plus Mapper
│   │   ├── model/                # Entity + VO
│   │   ├── task/                 # 定时任务
│   │   ├── utils/                # JwtUtil、BaseContext（ThreadLocal）
│   │   └── common/               # 统一响应包装、异常、枚举
│   └── src/main/resources/
│       └── init.sql              # 数据库初始化脚本
├── docs/
│   └── 流程图识别-学校-重点-密接.md   # 业务流程 Mermaid 图
├── .github/workflows/            # CI/CD
│   ├── deploy.yml                # 构建并部署到 gh-pages
│   └── release.yml               # 版本发布 + changelog
├── vite.config.ts                # Vite 配置（含 Vitest 配置）
├── tsconfig.json                 # TS 配置：strict、路径别名
├── uno.config.ts                 # UnoCSS 配置
├── eslint.config.js              # ESLint 配置
├── package.json
├── pnpm-lock.yaml
├── .env / .env.development / .env.production / .env.staging
└── index.html
```

### 关键后端 Controller 清单

- `DashboardController` — 首页统计（待追踪、待随访、通知等）
- `UserController` — 登录认证、用户管理
- `ScreeningSchoolController` / `ScreeningKeyPopulationController` / `ScreeningCloseContactController` — 三类人群筛查
- `LatentInfectionController` — 潜伏感染者管理
- `PatientController` — 确诊患者管理
- `NoticeController` — 通知管理
- `SupervisionFormController` — 督导表管理
- `ExportController` — 数据导出
- `BackupController` — 数据备份
- `StatisticsController` — 统计报表

---

## 四、构建与运行命令

### 前端（pnpm）

```bash
# 安装依赖
pnpm install

# 开发服务（端口 3333，代理 /api/v1 到 localhost:8888）
pnpm dev

# 生产构建（含类型检查）
pnpm build

# 预发布环境构建
pnpm build:staging

# 预览生产构建
pnpm preview

# 代码检查与自动修复
pnpm lint

# 运行测试
pnpm test
```

### 后端（Maven）

```bash
cd admin/
mvn clean package
# 或直接运行
mvn spring-boot:run
```

### 环境变量说明

| 文件 | `VITE_BASE_URL` | `VITE_PUBLIC_PATH` | 说明 |
|------|-----------------|--------------------|------|
| `.env` | — | — | 通用：`VITE_APP_TITLE`、`VITE_ROUTER_HISTORY=hash` |
| `.env.development` | `/api/v1` | `/` | 开发环境，代理到后端 `localhost:8888` |
| `.env.production` | `/api/v1` | `/` | 生产环境 |
| `.env.staging` | `https://apifoxmock.com/...` | `/` | 预发布，使用 Mock API |

> 后端 `server.servlet.context-path=/api/v1`，与前端 `VITE_BASE_URL` 对齐。

---

## 五、代码规范

### ESLint 配置

- 使用 `@antfu/eslint-config` + formatters
- 缩进：**2 空格**
- 引号：**双引号**
- 分号：**禁用**
- Vue 单文件组件块顺序：**`script` → `template` → `style`**
- EditorConfig：UTF-8、LF、自动去除行尾空格

### Git 钩子（Husky + lint-staged）

- `pre-commit`：
  1. `npx vue-tsc` — 全量 TypeScript 类型检查
  2. `npx lint-staged` — 对暂存文件运行 `eslint --fix`

### 路径别名

| 别名 | 目标 |
|------|------|
| `@/` | `src/` |
| `@@/` | `src/common/` |

### 自动导入

项目中配置了以下 unplugin 自动导入，编写代码时 **无需手动 import**：

- `unplugin-auto-import`：Vue、Vue Router、Pinia、Element Plus 等 API
- `unplugin-vue-components`：Element Plus、自定义组件
- `unplugin-svg-component`：SVG 文件自动注册为组件

---

## 六、测试策略

- **测试框架**：Vitest 3.2.4
- **DOM 环境**：happy-dom
- **Vue 测试工具**：@vue/test-utils
- **测试文件位置**：`tests/**/*.test.{ts,js}`
- **配置位置**：`vite.config.ts` 中的 `test` 字段

### 当前测试覆盖（非常有限）

| 文件 | 内容 |
|------|------|
| `tests/demo.test.ts` | Vitest 基础示例 |
| `tests/utils/validate.test.ts` | `isArray()` 测试 |
| `tests/components/Notify.test.ts` | 组件浅挂载测试 |

> ⚠️ **警告**：业务逻辑测试覆盖极低，新增核心功能时应补充单元测试或组件测试。

### 运行测试

```bash
pnpm test
```

---

## 七、认证与权限

### 认证机制

- JWT Token 存储在 Cookie 中（`js-cookie` 管理）
- HTTP 请求头携带：`Authorization: Bearer <token>`
- Axios 响应拦截器捕获 `401` → 自动清除登录态 → 跳转登录页

### 权限控制

- **角色（Role）**：`userRole === 1` 为超级管理员，绕过所有权限校验
- **权限码（Permission Code）**：前端路由 `meta.permission` 与用户 `permissions` 数组匹配
- **动态路由**：登录后根据用户角色/权限过滤并注册路由
- **权限指令**：`v-permission` 用于按钮级显隐控制
- **路由守卫**：`router/guard.ts` 处理登录状态、Token 有效性、权限校验、NProgress

### API 响应规范

```typescript
interface ApiResponse<T> {
  code: number   // 200 = 成功，401 = Token 过期/无效，其他为业务错误
  data: T
  msg: string    // 提示信息
}
```

---

## 八、部署与 CI/CD

### GitHub Actions

1. **`.github/workflows/deploy.yml`**
   - 触发条件：`push` 到 `main` 分支
   - 环境：Node 22.16.0 + pnpm 10.12.1
   - 步骤：`pnpm install` → `pnpm build` → 部署 `dist/` 到 `gh-pages` 分支
   - 需要 Secret：`ACCESS_TOKEN: ${{ secrets.V3_ADMIN_VITE }}`

2. **`.github/workflows/release.yml`**
   - 触发条件：推送 `v*` 标签
   - 使用 `npx changelogithub` 自动生成 changelog
   - 需要 `GITHUB_TOKEN`

### 前端部署

- 构建产物输出到 `dist/` 目录
- 生产构建为静态资源，可部署到任意静态托管服务（Nginx、CDN、GitHub Pages 等）
- 后端独立部署（JAR 包或容器化），与前端解耦

### 数据库初始化

- 后端提供 `admin/src/main/resources/init.sql` 用于初始化数据库结构
- 定时任务包含自动备份逻辑

---

## 九、安全注意事项

1. **Token 安全**：JWT 存储在 Cookie 中，需确保生产环境启用 `Secure` + `HttpOnly`（当前实现需检查）
2. **权限绕过**：超级管理员角色（`userRole === 1`）拥有全部权限，修改相关判断逻辑时需格外谨慎
3. **SQL 注入**：后端使用 MyBatis-Plus，原则上避免手写拼接 SQL；如需自定义 XML，必须使用 `#{}` 参数绑定
4. **XSS**：前端使用 Vue 模板语法自动转义，但 v-html 插入用户输入内容时需手动过滤
5. **文件上传**：检查上传接口的文件类型、大小限制及存储路径安全
6. **敏感数据**：患者姓名、身份证号等涉敏字段在传输和日志中应注意脱敏

---

## 十、开发建议

### 修改前端时的注意事项

- **布局与导航**：新增页面需在 `src/router/index.ts` 注册，并在 `src/pages/` 下创建对应目录
- **API 调用**：在 `src/common/apis/` 下按业务域新增接口模块，统一使用 `@@/` 别名引入
- **表格**：大量使用 VXE-Table + Element Plus 表格，注意区分两者 API；复杂表格优先用 VXE-Table
- **状态管理**：用户级状态放 `pinia/stores/`，组件级状态用 `composables/`
- **样式**：优先使用 UnoCSS 原子类（如 `flex-center`），复杂样式用 SCSS；主题变量通过 Element Plus 的 CSS 变量控制

### 修改后端时的注意事项

- **包名**：根包为 `cn.luyou`
- **统一响应**：Controller 返回 `Result<T>` 包装类，禁止直接返回裸对象
- **JWT 工具**：使用 `cn.luyou.utils.JwtUtil`，ThreadLocal 用户上下文使用 `BaseContext`
- **定时任务**：在 `cn.luyou.task` 包下编写，类上标注 `@Component`，方法标注 `@Scheduled`
- **Excel 导出**：使用 EasyExcel，大数据量注意内存和响应超时

### 前后端联调

- 前端开发服务器默认代理 `/api/v1` 到 `http://localhost:8888`
- 后端启动后确保上下文路径为 `/api/v1`
- 若需使用 Mock 数据，切换为 `.env.staging` 或修改 `vite.config.ts` 代理目标

---

## 十一、相关文档

- `docs/流程图识别-学校-重点-密接.md` — 结核病筛查完整业务流程（Mermaid 流程图）
- `admin/src/main/resources/init.sql` — 数据库初始化脚本
- `package.json` — 前端完整依赖与脚本
- `admin/pom.xml` — 后端完整依赖
