
# Spring Boot 短链接、消息队列 & 用户认证系统

<br>

### **项目简介**

本项目是基于 Spring Boot 开发的短链接系统，同时集成了 RabbitMQ 消息队列，Redis 缓存优化，WebSocket 通知和 JWT 认证，并使用布隆过滤器 (Bloom Filter) 进行优化。

### **功能亮点**

  - 短链接生成与跳转
  
  - 访问统计与 Redis 缓存优化
  
  - WebSocket 实时消息推送
  
  - RabbitMQ + Redis 任务队列优化
  
  - Spring Security + JWT 用户认证
  
  - Bloom Filter 过滤器优化
  
  - 基于 RESTful API 的 CRUD 操作
  
  - Swagger API 文档支持
  
  - Docker 容器化部署与 Nginx 代理


### **技术栈**

  - 后端: Spring Boot, Spring Security, JWT, Redis, RabbitMQ, WebSocket
  
  - 数据库: MySQL, Redis (缓存与计数存储)
  
  - 缓存优化: Bloom Filter 过滤器
  
  - 前端: HTML, JavaScript (用于测试)
  
  - 容器化: Docker, Docker Compose, Nginx 反向代理
  
  - API 文档: Swagger (OpenAPI 3)


### **系统架构**
    
    graph DT;
        用户 -->|请求短链接| Nginx -->|转发 API| SpringBoot
        SpringBoot -->|查询 Redis| Redis
        SpringBoot -->|查询 MySQL| MySQL
        SpringBoot -->|写入 RabbitMQ| RabbitMQ
        RabbitMQ -->|消费消息| Redis
        Redis -->|定期同步访问数据| MySQL
        用户 -->|用户认证| JWT
        BloomFilter -->|过滤无效请求| SpringBoot


### **快速启动**

**1. 使用 Docker Compose 启动服务**

    docker-compose up -d

**2. 访问 Web 页面**

  - 短链接测试: http://localhost

  - Swagger API: http://localhost/swagger-ui/index.html

  - RabbitMQ 管理界面: http://localhost:15672 （默认账号: guest / guest）


## **API 说明**

### 1. 短链接 API
| 方法  | 路径                         | 说明                |
|------|------------------------------|---------------------|
| POST | `/api/short-url`             | 生成短链接         |
| GET  | `/api/short-url/{shortKey}`   | 访问短链接（302 跳转）|
| GET  | `/api/short-url/stats/{shortKey}` | 获取访问统计 |

### 2. 任务队列 (RabbitMQ)
| 方法  | 路径                          | 说明                |
|------|-------------------------------|---------------------|
| 生产者 | `/api/rabbitmq/send/{shortKey}` | 发送短链接访问消息 |
| 消费者 | `RabbitMQConsumer.java`      | 监听队列，处理访问计数 |

### 3. 用户认证 API (JWT)
| 方法  | 路径                          | 说明                     |
|------|-------------------------------|--------------------------|
| POST | `/api/auth/login`             | 用户登录，获取 JWT       |
| POST | `/api/auth/register`          | 用户注册                 |
| POST | `/api/auth/logout`            | 用户登出，清除 Token      |
| POST | `/api/auth/refresh`           | 刷新 JWT Token           |

### 4. CRUD 操作（产品管理）
| 方法  | 路径                         | 说明                |
|------|------------------------------|---------------------|
| GET  | `/api/products/{id}`         | 获取指定 ID 的产品信息 |
| GET  | `/api/products`              | 获取所有产品         |
| POST | `/api/products`              | 创建新产品          |
| PUT  | `/api/products/{id}`         | 更新产品信息        |
| DELETE | `/api/products/{id}`       | 删除产品            |


### 5. 布隆过滤器优化

防止缓存穿透: 先通过 Bloom Filter 检查数据是否存在，减少数据库查询压力。

提高系统性能: 适用于高并发短链接查询，提高 Redis 命中率。

<br>

## 部署与运行

1. 环境变量 (参考 .env 文件)
2. Nginx 配置 (反向代理)


## 未来优化

  - 增加 Prometheus 监控 Redis / MySQL / RabbitMQ
  - 研究 Kafka 处理高并发短链接访问
  - 尝试 CI/CD，自动化构建 Docker 镜像和部署


## English Version

### Project Overview
A Spring Boot based short URL service system integrating JWT authentication, message queue decoupling (RabbitMQ), Redis caching, WebSocket real-time push, and Bloom filter protection. The project demonstrates high-concurrency optimization and modular architecture.

### Tech Stack
- Backend: Spring Boot, Spring Security, JWT, Redis, RabbitMQ
- Database: MySQL
- Queue & Caching: Redis List, Bloom Filter
- Deployment: Docker, Nginx, Swagger for API docs

### Features
- Short URL generation & redirection
- Async access tracking via RabbitMQ
- Redis List + batch sync to MySQL
- Bloom Filter for invalid key filtering
- JWT-based user auth system with RefreshToken
- WebSocket for real-time product push updates
- RESTful API + Swagger UI

### APIs
| Request | Path                    |  explanation   |
|------|------------------------------|---------------------|
| POST | `/api/short-url`             | Generate Short URL   |
| GET  | `/api/short-url/{shortKey}`   | Short URL redirect（302）|
| GET  | `/api/short-url/stats/{shortKey}` | Get access records |

### 3. 用户认证 API (JWT)
| Request  | Path                          | explanation          |
|------|-------------------------------|--------------------------|
| POST | `/api/auth/login`             | User Login, Obtain JWT   |
| POST | `/api/auth/register`          | User register            |
| POST | `/api/auth/logout`            | User Logout, clear Token |
| POST | `/api/auth/refresh`           | Refresh JWT Token        |



### Getting Started
```bash
docker-compose up -d
