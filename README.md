# 餐厅收银管理系统（RCMS / cat-ording）

技术栈：JDK17 + Spring Boot + MyBatis-Plus + MySQL + Vue3 + Element Plus

## 目录结构

- `cat-system/`：后端 Spring Boot
- `cat-vue/`：前端 Vue3 管理台
- `scripts/`：初始化与启动脚本

## 本地依赖

- JDK：17+
- Maven：3.8+
- Node：18+（本机已是 Node 22 也可）
- MySQL：8+（或使用下方 Docker Compose）

## 数据库（推荐：Docker 一键启动）

```bash
docker compose up -d
```

初始化建表：

```bash
mysql -h 127.0.0.1 -P 3306 -u root -pmysql123 < cat-system/src/main/resources/db/schema.sql
mysql -h 127.0.0.1 -P 3306 -u root -pmysql123 cat_system < cat-system/src/main/resources/db/seed.sql
```

## 启动后端

```bash
cd cat-system
mvn spring-boot:run
```

后端默认：`http://localhost:8080`

## 启动前端

```bash
cd cat-vue
npm install
npm run dev
```

前端默认：`http://localhost:5173`（已配置代理到后端 `/api`）

## 默认账号

- 用户名：`admin`
- 密码：`admin123`

