# 智能伙伴匹配平台

一个基于 Spring Boot、Vue 3 与 LangChain4j 的智能伙伴匹配系统。项目围绕“用户画像 -> 智能推荐 -> 匹配请求 -> 队伍协作 -> AI 对话记忆”构建，支持用户注册登录、画像管理、伙伴推荐、队伍推荐、实时群聊、多角色 AI 助手、聊天历史和运营统计。

## 项目概览

系统采用前后端分离架构：

- 后端：Spring Boot 3.2.6 + Java 17，提供 REST API、WebSocket、JWT 鉴权、MyBatis-Plus 持久化、LangChain4j AI 服务。
- 前端：Vue 3 + Vite + Element Plus，提供登录注册、智能聊天、伙伴匹配、队伍管理、画像编辑、统计图表等页面。
- 存储：MySQL 保存业务关系数据，MongoDB 保存聊天记忆，Redis 缓存推荐结果并配合 Redisson 做并发控制，Pinecone 保存用户/队伍向量。
- AI：接入阿里 DashScope/Qwen 流式模型与 Embedding 模型，部分配置也保留了 Ollama 本地模型接入能力。

## 核心功能

### 1. 用户认证

- 用户注册、登录、验证码校验。
- 登录成功后生成 JWT，前端保存在 `localStorage`。
- `/api/**` 请求默认需要 `Authorization: Bearer <token>`。
- WebSocket 连接通过查询参数携带 token。

相关代码：

- `src/main/java/com/ithuangma/java/ai/langchain4j/Controller/UserController.java`
- `src/main/java/com/ithuangma/java/ai/langchain4j/interceptor/JwtInterceptor.java`
- `frontend/src/api/authService.js`

### 2. 用户画像

用户画像是推荐系统的基础，包含：

- MBTI 类型
- 标签
- 兴趣爱好
- 偏好设置
- 性格特征
- 沟通风格
- 头像地址

画像保存后会被转成文本并写入向量库，用于相似用户、伙伴和队伍推荐。

相关接口：

- `POST /api/v1/user-profile`
- `GET /api/v1/user-profile/{userId}`
- `POST /api/v1/user-profile/batch`
- `PUT /api/v1/user-profile/{userId}/tags`
- `PUT /api/v1/user-profile/{userId}/mbti`
- `GET /api/v1/user-profile/{userId}/similar-users`
- `GET /api/v1/user-profile/{userId}/suitable-teams`
- `PUT /api/v1/user-profile/{userId}/avatar`

### 3. 多维智能匹配

系统不是只做关键词匹配，而是采用“向量召回 + 多维评分重排”的策略。

伙伴匹配维度：

- MBTI 匹配
- 兴趣标签语义相似度
- 爱好契合度
- 沟通风格

队伍匹配维度：

- MBTI 兼容性
- 兴趣标签语义相似度
- 性格特征
- 沟通风格

匹配类型支持：

- `friend`：交友
- `study`：学习
- `hobby`：兴趣
- `sports`：运动
- `game`：游戏
- `date`：恋爱

相关代码：

- `src/main/java/com/ithuangma/java/ai/langchain4j/matching/MultiDimensionMatcher.java`
- `src/main/java/com/ithuangma/java/ai/langchain4j/matching/MbtiCompatibility.java`
- `src/main/java/com/ithuangma/java/ai/langchain4j/Service/Impl/PartnerMatchingServiceImpl.java`

### 4. 伙伴关系管理

用户可以获取推荐伙伴、发送匹配请求、接受/拒绝请求、查看已匹配伙伴、取消请求和解除匹配。

相关接口：

- `GET /api/v1/partners/recommend/{userId}`
- `POST /api/v1/partners/request`
- `POST /api/v1/partners/accept/{requestId}`
- `POST /api/v1/partners/reject/{requestId}`
- `GET /api/v1/partners/matches/{userId}`
- `GET /api/v1/partners/pending/{userId}`
- `GET /api/v1/partners/sent/{userId}`
- `POST /api/v1/partners/cancel/{requestId}`
- `POST /api/v1/partners/unmatch/{relationId}`

前端页面：`frontend/src/views/PartnerMatching.vue`

### 5. AI 伙伴匹配助手

伙伴匹配模块额外提供一个可对话的 AI 助手：

- 用户可以通过自然语言询问推荐对象。
- AI 可调用 LangChain4j `@Tool` 工具执行推荐、发起匹配、查看请求、接受/拒绝请求、推荐队伍等操作。
- 对话接口为流式输出。

相关代码：

- `src/main/java/com/ithuangma/java/ai/langchain4j/assistant/PartnerMatchingAgent.java`
- `src/main/java/com/ithuangma/java/ai/langchain4j/Tools/PartnerMatchingTools.java`
- `POST /api/v1/partners/chat`

### 6. 队伍管理与推荐

系统支持创建队伍、加入队伍、查看队伍、获取成员、基于画像推荐队伍。

队伍访问级别：

- `0`：公开队伍，可直接加入。
- `1`：审核队伍，需要通过匹配评分，当前阈值为 `0.6`。
- `2`：私有队伍，需要邀请。

加入队伍时使用 Redisson 分布式锁防止重复加入。

相关接口：

- `POST /api/v1/teams`
- `POST /api/v1/teams/{teamId}/join`
- `GET /api/v1/teams/user/{userId}`
- `GET /api/v1/teams/all`
- `GET /api/v1/teams/{teamId}`
- `GET /api/v1/teams/match/{userId}`
- `GET /api/v1/teams/{teamId}/members`

前端页面：

- `frontend/src/views/TeamList.vue`
- `frontend/src/views/TeamCreate.vue`
- `frontend/src/views/TeamDetail.vue`

### 7. 实时队伍聊天

队伍聊天使用 WebSocket：

- 连接地址：`/websocket/team/{teamId}/{userId}?token=<JWT>`
- 普通用户消息写入 MongoDB `chat_sessions`。
- 消息广播给当前队伍在线成员。
- 当消息包含“AI、助手、建议、怎么看、为什么、？”等关键词时，会异步触发 AI 回复。
- AI 回复同样会保存到 MongoDB 并广播。

相关代码：

- `src/main/java/com/ithuangma/java/ai/langchain4j/websocket/TeamChatWebSocket.java`
- `src/main/java/com/ithuangma/java/ai/langchain4j/Controller/TeamChatController.java`
- `frontend/src/api/websocket.js`

### 8. 多角色 AI 聊天

前端提供多智能体聊天入口：

| 智能体 | 前端 ID | 角色 | 接口 |
| --- | --- | --- | --- |
| 章志康 | `zhangzhikang` | 医疗助手 | `POST /api/xiaozhi/chat` |
| 杨楠 | `yangnan` | 购物助手 | `POST /api/shopping/chat` |
| 涂志兴 | `tuzhixing` | 恋爱助手 | `POST /api/love/chat` |
| 陈俊辉 | `learning` | 学习助手 | `POST /api/learning/chat` |

这些接口均为流式响应，使用 LangChain4j 的记忆、RAG 和工具能力。购物助手支持创建、查询、取消订单；医疗助手支持预约挂号相关工具。

相关代码：

- `frontend/src/components/ChatWindow.vue`
- `frontend/src/config/agents.js`
- `src/main/java/com/ithuangma/java/ai/langchain4j/assistant`
- `src/main/java/com/ithuangma/java/ai/langchain4j/Tools`

### 9. 聊天历史与统计

聊天历史保存在 MongoDB，支持：

- 查询队伍聊天记录。
- 查询用户与某个 AI 的私聊记录。
- 查询用户聊天统计。
- 清理过期聊天记录。

统计模块支持：

- 用户注册统计。
- AI 接口请求统计。
- 多智能体请求对比。
- 近 3 天、7 天、30 天概览。

相关接口：

- `GET /api/v1/chat-history/team/{teamId}`
- `GET /api/v1/chat-history/user/{userId}/ai/{aiId}`
- `GET /api/v1/chat-history/user/{userId}/stats`
- `DELETE /api/v1/chat-history/cleanup`
- `GET /api/statistics/recent-three-days`
- `GET /api/statistics/recent-seven-days`
- `GET /api/statistics/recent-thirty-days`

### 10. 文件上传

通用上传接口将图片上传到阿里云 OSS。

- 接口：`POST /admin/common/upload`
- 支持类型：`jpg`、`jpeg`、`png`、`gif`、`webp`
- 大小限制：5 MB

## 系统架构

```text
┌──────────────────────────┐
│ Vue 3 + Vite 前端         │
│ 登录 / 聊天 / 匹配 / 队伍 │
└─────────────┬────────────┘
              │ HTTP + WebSocket
┌─────────────▼────────────┐
│ Spring Boot 后端          │
│ Controller / Service / AI │
└──────┬────────┬──────────┘
       │        │
       │        ├── LangChain4j + DashScope/Qwen
       │        ├── Pinecone / InMemory 向量库
       │        └── Ollama 本地模型配置
       │
┌──────▼────────┬───────────────┬──────────────┐
│ MySQL          │ MongoDB        │ Redis         │
│ 用户/画像/队伍 │ 聊天会话/记忆   │ 推荐缓存/锁    │
└───────────────┴───────────────┴──────────────┘
```

## 技术栈

后端：

- Java 17
- Spring Boot 3.2.6
- Spring MVC / WebFlux / WebSocket
- LangChain4j 1.0.0-beta3
- MyBatis-Plus 3.5.11
- MySQL
- MongoDB
- Redis + Redisson
- Pinecone
- Knife4j / OpenAPI
- JWT
- 阿里云 DashScope / OSS

前端：

- Vue 3.5
- Vite 5
- Vue Router 4
- Element Plus
- Axios
- ECharts
- Font Awesome

## 目录结构

```text
.
├── frontend/                         # Vue 3 前端工程
│   ├── src/api/                      # 前端 API 封装
│   ├── src/components/               # 通用组件与聊天组件
│   ├── src/views/                    # 页面视图
│   ├── src/router/                   # 前端路由
│   └── vite.config.js                # Vite 代理配置
├── sql/                              # 部分 MySQL 建表脚本
├── src/main/java/.../Controller/     # REST 控制器
├── src/main/java/.../Service/        # 业务服务接口
├── src/main/java/.../Service/Impl/   # 业务服务实现
├── src/main/java/.../assistant/      # LangChain4j AI Service
├── src/main/java/.../Tools/          # LangChain4j 工具调用
├── src/main/java/.../Config/         # 模型、记忆、Redis、WebSocket 等配置
├── src/main/java/.../matching/       # 多维匹配算法
├── src/main/java/.../websocket/      # 队伍聊天 WebSocket
├── src/main/resources/mapper/        # MyBatis XML Mapper
├── src/main/resources/*prompt*.txt   # 各智能体提示词模板
├── src/test/java/...                 # AI、RAG、Mongo、向量等测试
├── pom.xml                           # Maven 后端依赖
└── 智能伙伴匹配系统 - 最终技术规格说明书 (v3.0).md
```

## 环境要求

基础环境：

- JDK 17+
- Maven 3.8+
- Node.js 18+
- npm
- MySQL 8.0+
- MongoDB 6.0+
- Redis

外部服务：

- 阿里云 DashScope API Key
- Pinecone API Key，可选；未配置时后端会回退到内存向量库
- 阿里云 OSS AccessKey，可选；仅上传功能需要
- Ollama，可选；团队聊天配置中保留了本地模型能力

## 配置说明

主要配置文件：`src/main/resources/application.properties`

关键配置项：

```properties
server.port=8080

spring.datasource.url=jdbc:mysql://localhost:3306/guiguxiaozhi?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=123456

spring.data.mongodb.uri=mongodb://localhost:27017/chat_memory_db

spring.data.redis.host=192.168.100.128
spring.data.redis.port=6379
spring.data.redis.database=6
spring.data.redis.password=123456

langchain4j.community.dashscope.chat-model.api-key=${DASH_SCOPE_API_KEY}
langchain4j.community.dashscope.embedding-model.api-key=${DASH_SCOPE_API_KEY}
pinecone.api.key=${PINECONE_API_KEY}

sky.aliyun.oss.endpoint=https://oss-cn-beijing.aliyuncs.com
sky.aliyun.oss.bucketName=java-huangma
sky.aliyun.oss.region=cn-beijing
```

需要设置的环境变量：

```bash
DASH_SCOPE_API_KEY=你的 DashScope Key
PINECONE_API_KEY=你的 Pinecone Key
OSS_ACCESS_KEY_ID=你的阿里云 OSS AccessKey ID
OSS_ACCESS_KEY_SECRET=你的阿里云 OSS AccessKey Secret
```

Windows PowerShell 示例：

```powershell
$env:DASH_SCOPE_API_KEY="你的 DashScope Key"
$env:PINECONE_API_KEY="你的 Pinecone Key"
$env:OSS_ACCESS_KEY_ID="你的 OSS AccessKey ID"
$env:OSS_ACCESS_KEY_SECRET="你的 OSS AccessKey Secret"
```

## 数据库初始化

1. 创建数据库：

```sql
CREATE DATABASE IF NOT EXISTS guiguxiaozhi DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. 执行 `sql/` 目录下已有脚本：

```text
sql/sys_user.sql
sql/user_profile.sql
sql/pair_relation.sql
```

3. 项目中还使用以下业务表，对应实体和 Mapper 已存在，但当前 `sql/` 目录未提供完整建表脚本，部署时需要按实体补齐：

- `team_profile`
- `team_member_relation`
- `statistics`
- `shopping_order`
- `appointment`

4. MongoDB 会在写入聊天记录时使用 `chat_sessions` 集合。

5. Pinecone 默认配置：

- Index：`friendmatch-index`
- Namespace：`friendmatch-namespace`

## 启动项目

### 1. 启动后端

推荐在 IDE 中运行主类：

```text
src/main/java/com/ithuangma/java/ai/langchain4j/AIAssistantApp.java
```

如果需要命令行启动，可使用 Spring Boot Maven 插件坐标运行：

```bash
mvn org.springframework.boot:spring-boot-maven-plugin:3.2.6:run
```

后端默认地址：

```text
http://localhost:8080
```

Knife4j 接口文档：

```text
http://localhost:8080/doc.html
```

### 2. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端默认地址通常为：

```text
http://localhost:5173
```

Vite 已配置代理：

- `/api` -> `http://localhost:8080`
- `/admin` -> `http://localhost:8080`
- `/websocket` -> `ws://localhost:8080`

## 前端页面

| 路由 | 页面 | 功能 |
| --- | --- | --- |
| `/login` | 登录 | 用户登录、验证码 |
| `/register` | 注册 | 用户注册 |
| `/home` | 首页 | 功能入口 |
| `/chat` | AI 聊天 | 多角色 AI 助手对话 |
| `/statistics` | 数据分析 | 注册/API 请求统计图表 |
| `/teams` | 队伍列表 | 查看、加入、匹配队伍 |
| `/teams/create` | 创建队伍 | 新建队伍 |
| `/teams/:id` | 队伍详情 | 成员、聊天、AI 回复 |
| `/user-profile` | 用户画像 | 编辑画像、头像、标签 |
| `/partners` | 伙伴匹配 | 推荐、请求、伙伴、AI 匹配助手 |
| `/chat-history` | 聊天历史 | 查询群聊/私聊历史和清理记录 |

## API 响应格式

项目主要使用统一响应结构：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

前端 `frontend/src/api/httpClient.js` 会拦截响应：

- 当 `code !== 200` 时按业务错误处理。
- 当 HTTP 状态为 `401` 时清理本地登录信息并跳转登录页。

## 开发注意事项

- `src/main/resources/application.properties` 中包含本地 MySQL、Redis、OSS 默认配置，部署前请按实际环境修改。
- Pinecone 未配置时会回退到 `InMemoryEmbeddingStore`，但 Embedding 生成仍依赖 `DASH_SCOPE_API_KEY`。
- JWT 密钥当前在应用启动时随机生成，服务重启后旧 token 会失效。
- 代码大量使用 Lombok 注解，但当前 `pom.xml` 中未显式看到 Lombok 依赖；如果命令行编译失败，需要补充 Lombok 依赖或确认 IDE 已注入。
- 当前 `pom.xml` 没有显式声明 `spring-boot-maven-plugin`，命令行可用完整插件坐标运行，或补充插件后使用 `mvn spring-boot:run`。
- `sql/` 目录只提供部分建表脚本，完整部署需要根据实体补齐其余表。
- 前端上传头像时调用 `/admin/common/upload`，该路径不在 `/api/**` JWT 拦截规则内，但前端仍会附带 token。

## 测试

后端测试位于：

```text
src/test/java/com/ithuangma/java/ai/langchain4j
```

覆盖方向包括：

- LLM 调用
- Prompt 测试
- RAG 测试
- Embedding 测试
- MongoDB CRUD
- 预约工具
- 向量初始化

运行测试：

```bash
mvn test
```

如果本地没有配置 DashScope、MongoDB、MySQL、Redis、Pinecone 等依赖服务，部分集成测试可能无法通过。

## 许可证

项目根目录包含 `LICENSE` 文件，请以其中内容为准。
