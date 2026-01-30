# 餐厅收银管理系统（RCMS / cat-ording）

> 一个功能完整的餐厅收银管理系统，支持订单管理、菜品管理、支付处理、账目统计等全套餐饮业务流程

## 🎯 系统初衷

一些个体工商户只做小型记账，不想依赖第三方平台等记账系统。该新项目初衷是本地部署，可用户一些(烧烤店，快餐店)这种做台账
无任何花里胡哨配置。jdk+mysql+vue 即可，由于纯后台管理，不需要一些中间件植入。
---

## 📋 核心功能

### 1. **订单管理**
- 创建消费订单，支持多种商品组合
- 订单详情查看与修改
- 订单状态追踪（待支付、已支付、已完成）
- 批量订单查询与统计

### 2. **菜品与分类管理**
- 菜品库维护（名称、价格、描述等）
- 菜品分类组织
- 菜品上下架管理
- 菜品搜索与筛选

### 3. **收银与支付**
- 订单快速收银功能
- 多种支付方式支持（现金、微信、支付宝等）
- 实时支付状态更新
- 支付记录查询


### 5. **财务与账目**
- 营业额统计
- 每日/周期账目查看
- 收入对账
- 财务报表生成

### 6. **用户与权限**
- 管理员账户管理
- 基于 JWT 的身份认证
- 操作员身份追踪

### 7. **桌台管理**
-提供每一桌开台，取消开台
-提供每一桌的菜单记录，开单结账

---

## 🏗️ 系统架构

### 目录结构

```
cat-ording/
├── cat-system/           # 后端 Spring Boot 应用
│   ├── src/main/java/
│   │   └── com/catording/rcms/
│   │       ├── modules/
│   │       │   ├── order/       # 订单模块
│   │       │   ├── dish/        # 菜品模块
│   │       │   ├── payment/     # 支付模块
│   │       │   ├── discount/    # 折扣模块
│   │       │   ├── ledger/      # 账本模块
│   │       │   └── cashier/     # 收银台模块
│   │       └── common/
│   │           ├── security/    # JWT 认证
│   │           ├── exception/   # 异常处理
│   │           └── config/      # 配置类
│   └── src/main/resources/
│       └── db/                  # 数据库脚本
│
├── cat-vue/              # 前端 Vue3 应用
│   └── src/
│       ├── pages/        # 页面组件
│       ├── layout/       # 布局组件
│       ├── components/   # 公共组件
│       ├── api/          # API 调用
│       ├── store/        # Pinia 状态管理
│       └── router/       # 路由配置
│
└── scripts/              # 启动脚本
```

### 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| **后端框架** | Spring Boot | 3.x |
| **ORM** | MyBatis-Plus | 3.x |
| **数据库** | MySQL | 8.0+ |
| **认证** | JWT | - |
| **前端框架** | Vue | 3.5+ |
| **UI 框架** | Element Plus | 2.8+ |
| **前端工具** | Vite | 6.0+ |
| **包管理** | Maven (后端) / NPM (前端) | - |

---

## 🚀 快速开始

### 环境要求

- **JDK**：17+
- **Maven**：3.8+
- **Node**：18+（推荐 22+）
- **MySQL**：8.0+
- **Docker**：可选（用于数据库容器化）

### 1️⃣ 数据库初始化

**选项 A：使用 Docker Compose（推荐）**

```bash
docker compose up -d
```

**选项 B：手动初始化**

```bash
# 创建数据库和表
mysql -h 127.0.0.1 -P 3306 -u root -p < cat-system/src/main/resources/db/schema.sql

# 初始化数据
mysql -h 127.0.0.1 -P 3306 -u root -p cat_system < cat-system/src/main/resources/db/seed.sql
```

### 2️⃣ 启动后端

```bash
cd cat-system
mvn spring-boot:run
```

后端 API 地址：`http://localhost:8089`

### 3️⃣ 启动前端

```bash
cd cat-vue
npm install
npm run dev
```

前端应用地址：`http://localhost:5173`

> 前端已配置代理，API 请求自动转发至 `/api/*`

### 4️⃣ 登录系统

| 字段 | 值 |
|------|-----|
| 用户名 | `admin` |
| 密码 | `admin123` |

---

## 📊 核心数据模型

### 订单（ConsumeOrder）
- 订单号、下单时间、总金额
- 支付状态、支付方式
- 订单项关联

### 订单项（ConsumeOrderItem）
- 菜品 ID、数量、单价
- 所属订单关联

### 菜品（Dish）
- 菜品名称、价格、描述
- 所属分类
- 上下架状态

### 支付（Payment）
- 订单关联、支付金额
- 支付方式、时间戳
- 支付状态

### 折扣（DiscountRule）
- 规则名称、折扣比例
- 适用条件、优先级

### 账本（Ledger）
- 日期、交易额
- 交易类型、备注

---

## 🔐 安全特性

- ✅ **JWT 身份认证**：基于令牌的无状态认证
- ✅ **请求过滤**：JWT 认证拦截器
- ✅ **操作追踪**：记录操作员身份
- ✅ **异常处理**：全局异常捕获与友好错误响应

---

## 📝 API 设计

系统采用 **RESTful API** 设计，所有响应遵循统一格式：

```json
{
  "success": true,
  "data": { /* 响应数据 */ },
  "error": null,
  "meta": { /* 分页信息 */ }
}
```

### 核心端点示例

| 模块 | 端点 | 说明 |
|------|------|------|
| 订单 | `POST /api/orders` | 创建订单 |
| | `GET /api/orders/{id}` | 获取订单详情 |
| 菜品 | `GET /api/dishes` | 菜品列表 |
| | `POST /api/dishes` | 创建菜品 |
| 支付 | `POST /api/payments` | 提交支付 |
| 账本 | `GET /api/ledger` | 查询账目 |
| 收银 | `POST /api/cashier/checkout` | 收银结算 |

---

## 🛠️ 开发规范

### 代码结构
- **Controller**：HTTP 请求处理
- **Service**：业务逻辑实现
- **Mapper**：数据库访问
- **Entity**：数据模型

### 命名规范
- 包名：`com.catording.rcms.modules.{moduleName}`
- 类名：`{FunctionName}Entity` / `{FunctionName}Service` / `{FunctionName}Controller`
- 数据表：`table_{snake_case}`

---

## 📚 常见操作

### 查看后端日志
```bash
# 跟踪后端应用日志
tail -f logs/application.log
```

### 查看前端构建
```bash
# 生产环境构建
cd cat-vue
npm run build
```

### 数据库连接信息
| 参数 | 值 |
|------|-----|
| Host | `127.0.0.1` |
| Port | `3306` |
| User | `root` |
| Password | `mysql123` |
| Database | `cat_system` |

---

## 🔄 工作流示例

**典型的餐厅收银流程：**

1. **菜品下单** → 收银员通过前端选择菜品，创建订单
2. **订单生成** → 系统生成订单号，记录订单项
3. **应用折扣** → 系统自动计算折扣优惠
4. **支付结算** → 记录支付方式和金额
5. **账目统计** → 财务人员通过账本模块查看营业额

---

## 🤝 贡献指南

> 项目采用标准 Git 工作流，所有改动需通过 Pull Request 审核

---

## 📄 许可证

本项目采用开源许可证发布，具体见 `LICENSE` 文件

---

## 📞 技术支持
项目有bug或者交流可邮件发送
215063628@qq.com

---