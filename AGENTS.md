# AI408 Backend

## 项目概述
AI408 后端是一个面向 408 考研刷题场景的 Spring Boot 服务，负责登录鉴权、题库导入、随机练习、错题本、模拟考试、题目图片存储，以及基于 Qwen 的 AI 流式讲解。

本仓库是后端仓库，对应前端仓库位于：

- `D:\02code\AI408-front`

涉及页面交互、接口联调、字段展示异常时，需要同时参考前端仓库。

## 技术栈
- Java 17
- Spring Boot 3.3.x
- Spring Web
- Spring Security
- Spring Validation
- Spring Data JPA
- MySQL 8
- Flyway
- JWT（`jjwt`）
- Apache POI（Excel 导题）
- 腾讯云 COS SDK（题目配图上传）
- DashScope / Qwen（AI 讲解，含视觉模型）

## 项目结构
```text
src/main/java/org/example/ai408/
├── common/        # 通用响应、异常、错误码
├── config/        # 配置映射，如 AppProperties
├── controller/    # REST API 控制器
├── domain/        # JPA Entity
├── dto/           # 请求/响应 DTO
├── repository/    # JPA Repository
├── security/      # JWT 与 Spring Security 配置
├── service/       # 业务逻辑
└── util/          # 工具类

src/main/resources/
├── application.yml
└── db/migration/  # Flyway migration
```

## 核心业务模块
- 登录与用户系统
- 用户资料与学习统计
- 题库 Excel 导入
- 科目与题目查询
- 随机练习 / 背题
- 错题本 / 收藏
- 练习复盘
- 模拟考试 / 考试记录
- AI 流式讲解
- 题目配图导入与 COS 存储

## API 与数据约定
- 统一返回格式：`ApiResponse<T> { code, message, data }`
- 前端以 `code === '200'` 判断成功
- 认证采用 JWT access/refresh token
- 开发期验证码固定为 `123456`
- AI 讲解接口使用 SSE / 流式输出
- 文件资源通常返回相对路径，如 `/templates/**`、`/files/**`
- 练习会话保持 `sequence` 契约，对外接口尽量稳定

## 数据库约定
- 默认运行时数据库为 MySQL
- 所有结构变更必须通过 Flyway migration 提交
- 不允许只改 Entity、不补 migration
- 当前核心表包括：
  - `ai408_user`
  - `ai408_question`
  - `ai408_user_question_state`
  - `ai408_import_job`
  - `ai408_practice_session`
  - `ai408_session_question`
  - `ai408_exam_record`
  - `ai408_exam_record_question`

## 编码规范
- Controller 只做参数接收、权限边界和响应封装，业务逻辑放 Service
- Repository 只负责数据访问，不塞业务判断
- DTO 与 Entity 分离，不直接把 Entity 返回给前端
- 新接口保持 `/api/v1/**` 风格
- 统一使用项目已有 `ErrorCode` / `BusinessException`
- 公共字段和返回结构优先复用现有 DTO
- 题目相关字段新增时，要同步考虑：
  - 导题逻辑
  - Entity / DTO
  - 会话快照
  - AI 上下文组装
  - 前端消费字段

## 当前开发状态
- 已完成登录、刷新 token、用户资料基本链路
- 已完成题库导入、导入任务轮询、错误文件下载
- 已完成随机练习、背题模式、练习复盘
- 已完成错题本、收藏、错题重做
- 已完成模拟考试、交卷、考试记录、考试错题回顾
- 已完成 AI 流式讲解接入 Qwen
- 已完成题目配图字段、COS 上传链路、图片题展示支持
- 正在持续修正前端中文乱码、交互细节、导题边界条件

## 已知业务实现特点
- 练习与背题共用同一题目源，只是展示模式不同
- 错题本的本质仍然是“创建一轮练习会话继续做题”
- 模拟考试和普通练习相似，但记录与评分链路独立
- 图片题 AI 讲解可能切到视觉模型
- 用户答题状态主要落在 `ai408_user_question_state`

## 注意事项
- 本仓库里有一些调试脚本和临时产物，修改前先确认是否属于正式代码
- 不要删除或重写已有 Flyway migration
- 不要把临时本地路径、测试 key、调试数据硬编码进正式逻辑
- 不要擅自恢复用户已经修改的代码
- 如果后端改了字段名、返回结构或文件路径语义，要同步检查前端仓库是否受影响

## 环境变量
- `AI408_DB_URL`
- `AI408_DB_USERNAME`
- `AI408_DB_PASSWORD`
- `AI408_JWT_SECRET`
- `DASHSCOPE_API_KEY`
- `AI408_QWEN_MODEL`
- `AI408_QWEN_VISION_MODEL`
- `AI408_COS_SECRET_ID`
- `AI408_COS_SECRET_KEY`

## 本地开发建议
- 改表结构前先看 `src/main/resources/db/migration`
- 改接口前先查对应前端页面和 `src/api`
- 改错题/收藏/练习状态逻辑时，至少回归：
  - 普通练习
  - 背题模式
  - 练习复盘
  - 错题本
  - 模拟考试
- 提交前至少跑一次：
  - `mvn -q -DskipTests compile`

## 给 Codex 的建议
- 先确认当前任务只改后端，还是需要联动前端
- 优先做链路闭环，不做无关重构
- 改动前先读现有 Service / DTO / Controller，不要凭空新起一套模式
- 如果发现接口问题，其实是前端资源 URL 拼接、状态同步或页面消费问题，要明确指出，不要误判成后端 bug
