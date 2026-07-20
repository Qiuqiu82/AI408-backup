# AI408 后端接口文档

## 1. 文档信息

| 项目 | 内容 |
| --- | --- |
| 项目 | AI408 408 考研刷题系统 |
| API 版本 | `/api/v1` |
| 文档版本 | V1.0 |
| 服务端口 | `8080` |
| 默认本地地址 | `http://127.0.0.1:8080` |
| 数据格式 | JSON；文件上传使用 `multipart/form-data`；AI 讲解使用 SSE |
| 认证方式 | JWT Bearer Token |

本文档与当前后端 Controller、DTO 和业务实现保持一致，用于前端重构、Stitch 原型联调、接口测试和上线验收。

## 2. Swagger 访问方式

启动后端后访问：

- Swagger UI：`http://127.0.0.1:8080/swagger-ui.html`
- OpenAPI JSON：`http://127.0.0.1:8080/v3/api-docs`
- OpenAPI YAML：`http://127.0.0.1:8080/v3/api-docs.yaml`

Swagger UI 已配置 JWT Bearer 鉴权：

1. 先调用认证接口获取 `accessToken`。
2. 点击 Swagger UI 右上角的 `Authorize`。
3. 输入 `Bearer <accessToken>`，或按 Swagger UI 提示输入 token。
4. 再调用需要认证的业务接口。

以下接口不需要 Bearer Token：

- `/api/v1/auth/**`
- `/swagger-ui.html`
- `/swagger-ui/**`
- `/v3/api-docs/**`
- `/actuator/health`
- `/files/**`
- `/templates/**`

管理员接口即使在 Swagger 中成功完成 JWT 鉴权，还需要当前用户具有 `ADMIN` 角色。

## 3. 通用约定

### 3.1 请求地址

开发环境：

```text
http://127.0.0.1:8080
```

前端请求完整地址示例：

```text
http://127.0.0.1:8080/api/v1/subjects
```

生产环境建议通过反向代理配置统一 API 域名，前端不要把生产域名硬编码在业务组件中。

### 3.2 统一响应结构

除 SSE 和静态文件外，所有接口都使用以下结构：

```json
{
  "code": "200",
  "message": "处理成功",
  "data": {}
}
```

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `code` | string | 业务状态码；成功固定为 `200` |
| `message` | string | 服务端提示消息 |
| `data` | object/array/null | 业务数据；失败时通常为 `null` |

注意：部分业务异常会使用 HTTP 200 返回，但 `code` 不为 `200`。前端必须同时判断 HTTP 状态和业务 `code`，不能只判断 HTTP 200。

### 3.3 认证请求头

需要登录的接口发送：

```http
Authorization: Bearer <accessToken>
Content-Type: application/json
Accept: application/json
```

文件上传接口不应手动设置 `Content-Type: application/json`，由浏览器自动生成 multipart boundary。

### 3.4 分页请求

题目、错题和收藏分页请求统一使用：

```json
{
  "data": {
    "page": {
      "pageSize": 20,
      "pageIndex": 1
    },
    "params": {}
  }
}
```

分页响应：

```json
{
  "code": "200",
  "message": "处理成功",
  "data": {
    "pageIndex": 1,
    "pageSize": 20,
    "pageCount": 4,
    "recordCount": 63,
    "records": []
  }
}
```

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `pageIndex` | integer | 当前页，从 1 开始 |
| `pageSize` | integer | 每页数量 |
| `pageCount` | integer | 总页数 |
| `recordCount` | integer | 总记录数 |
| `records` | array | 当前页记录 |

### 3.5 时间、布尔值和空值

- 时间字段返回字符串，前端不要依赖固定的展示格式，应统一格式化。
- 布尔字段返回 JSON `true`/`false`。
- 不存在的数据可能返回 `null`、空字符串、空数组，前端需要按字段语义处理。
- 题目选项 key 通常为 `A`、`B`、`C`、`D`，答案数组使用字符串数组。

## 4. 认证流程

### 4.1 验证码登录

1. `POST /api/v1/auth/send-code`
2. 用户输入邮箱验证码。
3. `POST /api/v1/auth/login`
4. 保存返回的 `accessToken`、`refreshToken`、`expiresIn` 和 `user`。
5. 如果 `user.hasPassword=false`，跳转 `/account/password`。

### 4.2 密码登录

1. `POST /api/v1/auth/password-login`
2. 保存返回的 token 和用户信息。
3. 根据 `user.hasPassword` 决定是否进入密码设置页。

邮箱不存在、未设置密码和密码错误统一返回 `40003 LOGIN_FAILED`，避免暴露账号状态。

### 4.3 Token 刷新

access token 过期后调用 `POST /api/v1/auth/refresh`。刷新成功后替换本地 access token 和 refresh token；刷新失败则清空会话并跳转登录页。

### 4.4 登录方式 claim

JWT 中包含登录方式 claim：

| claim | 值 | 含义 |
| --- | --- | --- |
| `authMethod` | `code` | 当前会话来自邮箱验证码登录，可用于免旧密码重置 |
| `authMethod` | `password` | 当前会话来自密码登录，修改已有密码需要旧密码 |

refresh token 换发的新 token 会延续原登录方式。

---

## 5. 认证接口

## 5.1 发送邮箱验证码

```http
POST /api/v1/auth/send-code
Content-Type: application/json
```

请求：

```json
{
  "data": {
    "email": "user@example.com",
    "scene": "login"
  }
}
```

请求字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `data.email` | string | 邮箱登录时是 | 合法邮箱，最长 120 |
| `data.mobile` | string | 手机号登录时是 | 兼容旧手机号链路，最长 20 |
| `data.scene` | string | 否 | 默认 `login`，最长 20 |

当前上线配置使用邮箱登录。前端发送邮箱时不要同时发送 `mobile`。

成功响应：

```json
{
  "code": "200",
  "message": "处理成功",
  "data": {
    "expireSeconds": 300
  }
}
```

| 返回字段 | 类型 | 说明 |
| --- | --- | --- |
| `expireSeconds` | integer | 验证码有效期，当前配置默认 300 秒 |

常见失败：`40004 EMAIL_INVALID`、`40005 CODE_RATE_LIMIT`、`40006 INVITE_REQUIRED`、`40007 MAIL_CONFIG_INVALID`。

## 5.2 邮箱验证码登录

```http
POST /api/v1/auth/login
Content-Type: application/json
```

请求：

```json
{
  "data": {
    "email": "user@example.com",
    "code": "123456",
    "deviceId": "web-browser-001",
    "clientType": "web"
  }
}
```

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `data.email` | string | 邮箱登录时是 | 用户邮箱 |
| `data.mobile` | string | 手机号登录时是 | 兼容旧手机号链路 |
| `data.code` | string | 是 | 验证码，最长 12 |
| `data.deviceId` | string | 否 | 客户端设备标识，最长 100 |
| `data.clientType` | string | 否 | 默认 `web`，最长 20 |

成功响应：

```json
{
  "code": "200",
  "message": "处理成功",
  "data": {
    "accessToken": "eyJ...",
    "refreshToken": "eyJ...",
    "expiresIn": 7200,
    "user": {
      "id": "u_xxx",
      "mobile": null,
      "email": "user@example.com",
      "nickname": null,
      "avatarUrl": null,
      "role": "user",
      "createdAt": "2026-07-19 10:00:00",
      "hasPassword": false,
      "wrongBookAutoRemoveEnabled": false,
      "wrongBookAutoRemoveThreshold": 1
    }
  }
}
```

### UserDTO 字段

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | string | 用户 ID |
| `mobile` | string/null | 手机号，邮箱用户通常为空 |
| `email` | string/null | 邮箱 |
| `nickname` | string/null | 昵称 |
| `avatarUrl` | string/null | 头像地址 |
| `role` | string | 当前角色，普通用户通常为 `user`，管理员为 `admin` |
| `createdAt` | string | 创建时间 |
| `hasPassword` | boolean | 是否已经设置密码 |
| `wrongBookAutoRemoveEnabled` | boolean | 错题答对后是否自动移除 |
| `wrongBookAutoRemoveThreshold` | integer | 自动移除阈值，支持 1 或 3 |

## 5.3 邮箱密码登录

```http
POST /api/v1/auth/password-login
Content-Type: application/json
```

请求：

```json
{
  "data": {
    "email": "user@example.com",
    "password": "your-password",
    "deviceId": "web-browser-001",
    "clientType": "web"
  }
}
```

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `data.email` | string | 是 | 合法邮箱，最长 120 |
| `data.password` | string | 是 | 最长 64；设置密码时要求 8-64 |
| `data.deviceId` | string | 否 | 最长 100 |
| `data.clientType` | string | 否 | 默认 `web` |

成功响应与验证码登录相同，`user.hasPassword` 应为 `true`。

失败响应示例：

```json
{
  "code": "40003",
  "message": "登录失败",
  "data": null
}
```

## 5.4 刷新 Token

```http
POST /api/v1/auth/refresh
Content-Type: application/json
```

请求：

```json
{
  "data": {
    "refreshToken": "eyJ..."
  }
}
```

| 字段 | 类型 | 必填 |
| --- | --- | --- |
| `data.refreshToken` | string | 是 |

成功响应为新的 `AuthTokens`，字段结构与登录接口一致。

---

## 6. 用户接口

所有用户接口都需要 JWT。

## 6.1 获取当前用户

```http
GET /api/v1/users/me
Authorization: Bearer <accessToken>
```

响应 `data` 为 `UserDTO`，见 [5.2 UserDTO 字段](#userdto-字段)。

## 6.2 更新当前用户

```http
PATCH /api/v1/users/me
Content-Type: application/json
Authorization: Bearer <accessToken>
```

请求：

```json
{
  "data": {
    "nickname": "408 学员",
    "avatarUrl": "https://example.com/avatar.png",
    "wrongBookAutoRemoveEnabled": true,
    "wrongBookAutoRemoveThreshold": 3
  }
}
```

所有字段均可选，适合前端只提交发生变化的字段。

| 字段 | 类型 | 规则 |
| --- | --- | --- |
| `nickname` | string/null | 最长 50 |
| `avatarUrl` | string/null | 最长 255 |
| `wrongBookAutoRemoveEnabled` | boolean/null | 是否启用自动移除 |
| `wrongBookAutoRemoveThreshold` | integer/null | 只支持 1 或 3 |

响应：更新后的 `UserDTO`。

## 6.3 设置或修改密码

```http
PATCH /api/v1/users/me/password
Content-Type: application/json
Authorization: Bearer <accessToken>
```

首次设置：

```json
{
  "data": {
    "newPassword": "new-password-123"
  }
}
```

密码登录会话修改已有密码：

```json
{
  "data": {
    "currentPassword": "old-password-123",
    "newPassword": "new-password-123"
  }
}
```

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| `data.currentPassword` | string | 条件必填 | 已有密码且当前会话为密码登录时必填 |
| `data.newPassword` | string | 是 | 8-64 位 |

验证码登录会话可以不传 `currentPassword` 直接重置密码。响应为更新后的 `UserDTO`。

## 6.4 获取学习摘要

```http
GET /api/v1/users/me/study-summary
Authorization: Bearer <accessToken>
```

响应：

```json
{
  "code": "200",
  "message": "处理成功",
  "data": {
    "answeredCount": 20,
    "correctCount": 15,
    "wrongCount": 5,
    "favoriteCount": 3,
    "todayWrongCount": 2,
    "todayFavoriteCount": 1,
    "progressRate": 31,
    "sessionSeconds": 3600
  }
}
```

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `answeredCount` | integer | 已答题数 |
| `correctCount` | integer | 答对题数 |
| `wrongCount` | integer | 当前错题数 |
| `favoriteCount` | integer | 当前收藏数 |
| `todayWrongCount` | integer | 今日新增错题数 |
| `todayFavoriteCount` | integer | 今日新增收藏数 |
| `progressRate` | integer | 学习进度百分比 |
| `sessionSeconds` | integer | 累计练习时长，单位秒 |

---

## 7. 题库接口

题目内容属于公共题库，但所有题库接口仍要求登录，以便返回当前用户的答题、错题和收藏状态。

## 7.1 查询科目列表

```http
GET /api/v1/subjects
Authorization: Bearer <accessToken>
```

响应：

```json
{
  "code": "200",
  "message": "处理成功",
  "data": [
    {
      "subjectCode": "CN",
      "subjectName": "计算机网络",
      "shortName": "计网",
      "totalCount": 44,
      "doneCount": 10,
      "wrongCount": 2
    }
  ]
}
```

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `subjectCode` | string | 科目编码 |
| `subjectName` | string | 科目全称 |
| `shortName` | string | 前端短名称 |
| `totalCount` | integer | 公共题库题目总数 |
| `doneCount` | integer | 当前用户已做题数 |
| `wrongCount` | integer | 当前用户该科目错题数 |

科目列表必须以接口返回为准，不能依赖固定数组。

## 7.2 分页查询题目

```http
POST /api/v1/questions/page
Content-Type: application/json
Authorization: Bearer <accessToken>
```

请求示例：

```json
{
  "data": {
    "page": {
      "pageSize": 20,
      "pageIndex": 1
    },
    "params": {
      "subjectCode": "CN",
      "questionType": "single",
      "keyword": "TCP",
      "tag": "可靠传输",
      "newType": 1,
      "questionStatus": "wrong",
      "inWrongBook": 1,
      "inFavorites": 1
    }
  }
}
```

分页字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `data.page.pageSize` | integer | 每页数量，建议前端使用 20 |
| `data.page.pageIndex` | integer | 页码，从 1 开始 |

筛选字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `subjectCode` | string | 科目编码 |
| `keyword` | string | 匹配标题、题干、科目、题目编码和题型 |
| `questionType` | string | `single`、`multiple`、`essay` |
| `tag` | string | 标签关键词 |
| `newType` | integer | 新题型标志，通常使用 0/1 |
| `questionStatus` | string | 用户题目状态，如 `new`、`correct`、`wrong` |
| `inWrongBook` | integer | 是否在错题本，通常使用 0/1 |
| `inFavorites` | integer | 是否已收藏，通常使用 0/1 |

题目摘要字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | string | 题目 ID |
| `subjectCode` | string | 科目编码 |
| `questionType` | string | 题型 |
| `title` | string | 题目标题 |
| `tag` | string | 主标签 |
| `newType` | boolean | 是否新题型 |
| `questionStatus` | string | 当前用户答题状态 |
| `favoriteImportance` | integer | 收藏等级 0/1/2 |
| `inWrongBook` | boolean | 是否在当前用户错题本 |

## 7.3 查询题目详情

```http
GET /api/v1/questions/{id}?view=practice&sessionId=sess_xxx
Authorization: Bearer <accessToken>
```

Query 参数：

| 参数 | 必填 | 说明 |
| --- | --- | --- |
| `view` | 否 | 默认 `practice`；传 `review` 时返回答案和解析 |
| `sessionId` | 否 | 练习会话 ID；传入后校验题目是否属于该会话 |

题目详情字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | string | 题目 ID |
| `subjectCode` | string | 科目编码 |
| `subjectName` | string | 科目名称 |
| `questionType` | string | `single`/`multiple`/`essay` |
| `title` | string | 标题 |
| `stem` | string | 题干 |
| `stemImageUrl` | string | 题目图片地址 |
| `options` | array | 选项对象数组，元素为 `{key,text}` |
| `answer` | string[] | 答案；练习视图通常不返回，复盘视图返回 |
| `analysis` | string | 解析；练习视图通常不返回，复盘视图返回 |
| `steps` | string[] | 步骤题步骤 |
| `tags` | string[] | 标签 |
| `newType` | boolean | 是否新题型 |
| `note` | string | 当前用户笔记 |

---

## 8. 练习接口

## 8.1 创建练习会话

```http
POST /api/v1/practice/sessions
Content-Type: application/json
Authorization: Bearer <accessToken>
```

普通科目练习：

```json
{
  "data": {
    "mode": "sequence",
    "subjectCode": "CN",
    "limit": 20,
    "source": "home"
  }
}
```

指定题目重做：

```json
{
  "data": {
    "mode": "sequence",
    "questionIds": ["q_001", "q_002"],
    "limit": 2,
    "source": "practice-review-wrong-retry"
  }
}
```

错题本练习：

```json
{
  "data": {
    "mode": "wrongBook",
    "limit": 20,
    "source": "mistakes-wrong-book"
  }
}
```

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `data.mode` | string | 是 | `sequence`、`memorize`、`wrongBook`、`favorites` |
| `data.subjectCode` | string | 否 | 普通练习科目；`MOCK` 或空表示全题库 |
| `data.limit` | integer | 否 | 默认 20 |
| `data.questionIds` | string[] | 否 | 指定题目时使用，后端按题目 ID 过滤 |
| `data.source` | string | 否 | 前端来源标识，用于追踪进入方式 |

返回：

```json
{
  "code": "200",
  "message": "处理成功",
  "data": {
    "sessionId": "sess_xxx",
    "mode": "sequence",
    "status": "progressing",
    "subjectCode": "CN",
    "totalCount": 20,
    "currentQuestionId": "q_001",
    "answeredCount": 0,
    "questionBriefList": [
      {
        "questionId": "q_001",
        "orderNo": 1,
        "questionStatus": "new",
        "newType": false
      }
    ],
    "currentQuestion": {
      "id": "q_001",
      "questionType": "single",
      "title": "题目标题"
    }
  }
}
```

## 8.2 获取练习会话

```http
GET /api/v1/practice/sessions/{id}
Authorization: Bearer <accessToken>
```

返回结构与创建练习会话相同。接口只允许当前用户读取自己的会话。

常见错误：

- `40403 SESSION_NOT_FOUND`：会话不存在或不属于当前用户。
- `40901 SESSION_FINISHED`：会话已结束，不能继续提交。

## 8.3 提交单选/多选答案

```http
POST /api/v1/practice/sessions/{id}/answers
Content-Type: application/json
Authorization: Bearer <accessToken>
```

请求：

```json
{
  "data": {
    "questionId": "q_001",
    "answer": ["A"],
    "elapsedSeconds": 25
  }
}
```

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `data.questionId` | string | 是 | 当前会话中的题目 ID |
| `data.answer` | string[] | 是 | 单选一个元素，多选多个元素 |
| `data.elapsedSeconds` | integer | 否 | 当前题耗时 |

返回：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `questionId` | string | 题目 ID |
| `isCorrect` | boolean | 是否正确 |
| `questionStatus` | string | `correct` 或 `wrong` |
| `correctAnswer` | string[] | 正确答案 |
| `analysis` | string | 标准解析 |
| `inWrongBook` | boolean | 提交后是否进入错题本 |
| `nextQuestionId` | string | 后端建议的下一题 ID |

背题模式不应调用此接口进行普通判题，前端可通过题目详情或会话数据展示答案。

## 8.4 更新步骤题完成状态

```http
PATCH /api/v1/practice/sessions/{id}/essay-steps
Content-Type: application/json
Authorization: Bearer <accessToken>
```

请求：

```json
{
  "data": {
    "questionId": "q_essay_001",
    "steps": [true, false, true]
  }
}
```

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `data.questionId` | string | 是 | 步骤题 ID |
| `data.steps` | boolean[] | 是 | 长度必须与题目步骤数量一致 |

返回：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `questionId` | string | 题目 ID |
| `stepStatus` | boolean[] | 保存后的步骤状态 |
| `allDone` | boolean | 是否全部完成 |
| `questionStatus` | string | 当前题目状态 |

## 8.5 结束练习

```http
POST /api/v1/practice/sessions/{id}/finish
Authorization: Bearer <accessToken>
```

请求体为空。

返回：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `sessionId` | string | 练习会话 ID |
| `durationSeconds` | integer | 练习用时 |
| `answeredCount` | integer | 已答数 |
| `correctCount` | integer | 正确数 |
| `wrongCount` | integer | 错误数 |
| `accuracy` | integer | 正确率百分比 |
| `reviewId` | string | 复盘 ID，目前与会话关联 |

## 8.6 获取练习复盘

```http
GET /api/v1/practice/sessions/{id}/review
Authorization: Bearer <accessToken>
```

返回：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `accuracy` | integer | 本次正确率 |
| `durationSeconds` | integer | 本次用时 |
| `answeredCount` | integer | 已答数 |
| `wrongCount` | integer | 错题数 |
| `wrongQuestionIds` | string[] | 错题 ID |
| `wrongQuestions` | array | 错题详情 |
| `weakPoints` | string[] | 薄弱点标签 |
| `subjectStats` | array | 按科目统计 |

`subjectStats` 元素：

```json
{
  "subjectCode": "CN",
  "subjectName": "计算机网络",
  "correctCount": 8,
  "wrongCount": 2
}
```

`wrongQuestions` 元素包含题目内容、图片、选项、步骤、用户答案、步骤状态、正确答案、是否正确和解析。

---

## 9. 个人错题与收藏接口

## 9.1 分页查询错题本

```http
POST /api/v1/me/wrong-book/page
Content-Type: application/json
Authorization: Bearer <accessToken>
```

请求：

```json
{
  "data": {
    "page": {
      "pageSize": 20,
      "pageIndex": 1
    },
    "params": {
      "subjectCode": "CN",
      "questionType": "single",
      "keyword": "TCP",
      "groupBy": "subject"
    }
  }
}
```

`params` 字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `subjectCode` | string | 科目筛选 |
| `questionType` | string | 题型筛选 |
| `keyword` | string | 题目标题/标签关键词 |
| `groupBy` | string | 分组方式扩展字段 |

记录字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `questionId` | string | 题目 ID |
| `title` | string | 题目标题 |
| `subjectName` | string | 科目名称 |
| `tag` | string | 标签 |
| `wrongAt` | string | 进入错题本的时间 |

## 9.2 查询错题统计

```http
GET /api/v1/me/wrong-book/stats
Authorization: Bearer <accessToken>
```

响应：

```json
{
  "code": "200",
  "message": "处理成功",
  "data": {
    "totalWrongCount": 5,
    "todayWrongCount": 2,
    "answeredCount": 20,
    "wrongRate": 25,
    "wrongQuestionIds": ["q_001", "q_002"],
    "todayWrongQuestionIds": ["q_003"]
  }
}
```

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `totalWrongCount` | integer | 当前错题总数 |
| `todayWrongCount` | integer | 今日新增错题数 |
| `answeredCount` | integer | 已答题数 |
| `wrongRate` | integer | 错题占已答题比例 |
| `wrongQuestionIds` | string[] | 当前全部错题 ID |
| `todayWrongQuestionIds` | string[] | 今日新增错题 ID |

## 9.3 分页查询收藏题目

```http
POST /api/v1/me/favorites/page
Content-Type: application/json
Authorization: Bearer <accessToken>
```

请求结构与错题分页相同。

记录字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `questionId` | string | 题目 ID |
| `title` | string | 题目标题 |
| `subjectName` | string | 科目名称 |
| `favoriteImportance` | integer | 收藏等级 1/2 |
| `favoriteAt` | string | 收藏时间 |

## 9.4 更新题目状态

```http
PATCH /api/v1/me/question-states/{questionId}
Content-Type: application/json
Authorization: Bearer <accessToken>
```

请求示例：

```json
{
  "data": {
    "favoriteImportance": 2,
    "note": "注意 TCP 三次握手",
    "inWrongBook": true
  }
}
```

所有字段可选，至少传一个需要更新的字段。

| 字段 | 类型 | 规则 |
| --- | --- | --- |
| `favoriteImportance` | integer/null | 0、1、2 |
| `note` | string/null | 最长 5000 |
| `inWrongBook` | boolean/null | 是否在错题本 |

返回：

```json
{
  "questionId": "q_001",
  "favoriteImportance": 2,
  "inWrongBook": true,
  "note": "注意 TCP 三次握手"
}
```

## 9.5 清空错题本

```http
DELETE /api/v1/me/wrong-book
Authorization: Bearer <accessToken>
```

返回：

```json
{
  "code": "200",
  "message": "处理成功",
  "data": {
    "clearedCount": 5
  }
}
```

## 9.6 清空收藏

```http
DELETE /api/v1/me/favorites
Authorization: Bearer <accessToken>
```

返回结构与清空错题本相同，`clearedCount` 表示清除的收藏数量。

---

## 10. 模拟考试接口

## 10.1 生成模拟试卷

```http
POST /api/v1/exams/papers
Content-Type: application/json
Authorization: Bearer <accessToken>
```

请求：

```json
{
  "data": {
    "limit": 25
  }
}
```

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `data.limit` | integer | 否 | 默认 25，最大 50 |

返回：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `paperId` | string | 试卷 ID |
| `durationSeconds` | integer | 考试时长，当前为 10800 秒，即 180 分钟 |
| `generatedAt` | string | 生成时间 |
| `totalCount` | integer | 题目总数 |
| `currentQuestionId` | string | 默认当前题目 |
| `questions` | array | 试卷题目 |

试卷题目字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `questionId` | string | 题目 ID |
| `orderNo` | integer | 题号 |
| `subjectCode` | string | 科目编码 |
| `subjectName` | string | 科目名称 |
| `questionType` | string | 题型 |
| `title` | string | 标题 |
| `stem` | string | 题干 |
| `stemImageUrl` | string | 题目图片 |
| `options` | array | 选项 |
| `steps` | string[] | 步骤题步骤 |
| `tags` | string[] | 标签 |
| `newType` | boolean | 是否新题型 |
| `favoriteImportance` | integer | 当前用户收藏等级 |

考试中前端不能使用该响应中的答案或解析字段，因为试卷接口不返回答案解析。

## 10.2 提交模拟考试

```http
POST /api/v1/exams/records
Content-Type: application/json
Authorization: Bearer <accessToken>
```

请求：

```json
{
  "data": {
    "questionIds": ["q_001", "q_002"],
    "durationSeconds": 3200,
    "answers": [
      {
        "questionId": "q_001",
        "answer": ["A"],
        "stepStatus": []
      },
      {
        "questionId": "q_002",
        "answer": ["B", "D"],
        "stepStatus": []
      }
    ]
  }
}
```

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `data.questionIds` | string[] | 是 | 本次试卷全部题目 ID |
| `data.durationSeconds` | integer | 否 | 实际用时 |
| `data.answers` | array | 是 | 每道题的答案 |
| `answers[].questionId` | string | 是 | 题目 ID |
| `answers[].answer` | string[] | 否 | 单选/多选答案 |
| `answers[].stepStatus` | boolean[] | 否 | 步骤题完成状态 |

返回考试记录摘要：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `recordId` | string | 考试记录 ID |
| `score` | integer | 百分制分数 |
| `totalCount` | integer | 总题数 |
| `answeredCount` | integer | 已答数量 |
| `correctCount` | integer | 正确数量 |
| `wrongCount` | integer | 错误数量 |
| `durationSeconds` | integer | 实际用时 |
| `submittedAt` | string | 交卷时间 |

## 10.3 查询考试记录列表

```http
GET /api/v1/exams/records
Authorization: Bearer <accessToken>
```

响应 `data` 为考试记录摘要数组。

## 10.4 查询考试记录详情

```http
GET /api/v1/exams/records/{id}
Authorization: Bearer <accessToken>
```

响应字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `recordId` | string | 记录 ID |
| `score` | integer | 分数 |
| `totalCount` | integer | 总题数 |
| `answeredCount` | integer | 已答数 |
| `correctCount` | integer | 正确数 |
| `wrongCount` | integer | 错误数 |
| `durationSeconds` | integer | 用时 |
| `submittedAt` | string | 提交时间 |
| `wrongQuestionIds` | string[] | 错题 ID |
| `wrongQuestions` | array | 错题回顾 |

`wrongQuestions` 元素包含题目内容、图片、选项/步骤、用户答案、步骤状态、正确答案、正确性和解析。

---

## 11. AI 讲解接口

## 11.1 流式生成题目讲解

```http
POST /api/v1/ai/explanations/stream
Content-Type: application/json
Accept: text/event-stream
Authorization: Bearer <accessToken>
```

请求：

```json
{
  "data": {
    "sessionId": "sess_xxx",
    "questionId": "q_001",
    "userAnswer": ["A"]
  }
}
```

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `data.sessionId` | string | 是 | 当前练习会话 ID |
| `data.questionId` | string | 是 | 当前题目 ID |
| `data.userAnswer` | string[] | 是 | 用户当前答案；背题时可以为空数组 |

SSE 事件示例：

```text
event: delta
data: {"type":"delta","content":"这道题的关键在于..."}

event: delta
data: {"type":"delta","content":"首先分析选项 A..."}

event: done
data: {"type":"done","content":""}
```

失败事件：

```text
event: error
data: {"type":"error","content":"AI 服务调用失败"}
```

前端处理要求：

- `delta` 按顺序追加到当前答案区域。
- `done` 结束 loading。
- `error` 结束 loading 并允许重试。
- 切换题目时取消或忽略旧请求，避免内容串题。
- AI 失败不影响题目标准答案和解析展示。

---

## 12. 管理员题库接口

所有管理员接口需要 JWT，并要求当前用户具有 `ADMIN` 角色。

## 12.1 获取导入模板

```http
GET /api/v1/admin/questions/template
Authorization: Bearer <accessToken>
```

响应：

```json
{
  "code": "200",
  "message": "处理成功",
  "data": {
    "templateUrl": "/templates/ai408-question-template.xlsx",
    "version": "v2"
  }
}
```

前端需要使用资源 URL 配置拼接完整 API 地址后再下载。

## 12.2 提交题库导入任务

```http
POST /api/v1/admin/questions/import
Content-Type: multipart/form-data
Authorization: Bearer <accessToken>
```

表单字段：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `file` | file | 是 | `.xlsx` 或 `.csv`，后端限制文件大小 |
| `importType` | string | 否 | `append` 追加；`replace` 替换；默认 `append` |

curl 示例：

```bash
curl -X POST "http://127.0.0.1:8080/api/v1/admin/questions/import" \
  -H "Authorization: Bearer <accessToken>" \
  -F "file=@questions.xlsx" \
  -F "importType=append"
```

响应为导入任务：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `jobId` | string | 任务 ID |
| `status` | string | 初始通常为 `pending` |
| `totalCount` | integer | 总行数 |
| `successCount` | integer | 成功数 |
| `failedCount` | integer | 失败数 |
| `errorFileUrl` | string/null | 错误文件地址 |
| `updatedAt` | string | 更新时间 |

## 12.3 查询导入任务

```http
GET /api/v1/admin/questions/imports/{jobId}
Authorization: Bearer <accessToken>
```

建议前端每 2 秒轮询一次，直到 `status` 为 `success` 或 `failed`。

任务状态：

| 状态 | 说明 |
| --- | --- |
| `pending` | 任务已创建，等待处理 |
| `running` | 正在读取、校验或写入 |
| `success` | 任务成功完成，可能存在部分失败行 |
| `failed` | 任务失败 |

当 `errorFileUrl` 非空时，前端提供错误文件下载入口。

---

## 13. 文件资源接口

文件资源不是 JSON API，通常直接返回文件或静态资源。

| 资源 | 地址示例 | 用途 |
| --- | --- | --- |
| 模板文件 | `/templates/ai408-question-template.xlsx` | 下载题库模板 |
| 导入错误文件 | `/files/imports/...` | 下载失败行文件 |
| 题目图片 | `/files/...` 或 COS URL | 展示题目图片 |

如果后端返回相对路径，前端应使用统一的 `resolveApiAssetUrl` 方法拼接 API Base URL，禁止在页面组件内重复拼接。

---

## 14. 错误码

### 14.1 认证和权限

| code | 名称 | message | 前端处理 |
| --- | --- | --- | --- |
| `40001` | `MOBILE_INVALID` | 手机号格式错误 | 修正手机号 |
| `40002` | `CODE_INVALID` | 验证码错误或已过期 | 重新输入或重新发送 |
| `40003` | `LOGIN_FAILED` | 登录失败 | 统一提示，不区分账号状态 |
| `40004` | `EMAIL_INVALID` | 邮箱格式不正确 | 修正邮箱 |
| `40005` | `CODE_RATE_LIMIT` | 验证码发送太频繁，请稍后再试 | 等待后重试 |
| `40006` | `INVITE_REQUIRED` | 当前仅开放邀请用户登录 | 提示当前登录策略 |
| `40007` | `MAIL_CONFIG_INVALID` | 邮件服务未配置，无法发送验证码 | 提示联系管理员 |
| `40100` | `UNAUTHORIZED` | 未登录 | 刷新 token 或登录 |
| `40101` | `TOKEN_EXPIRED` | 登录已过期 | 刷新 token |
| `40102` | `REFRESH_TOKEN_INVALID` | 刷新令牌无效 | 清理会话并登录 |
| `40300` | `FORBIDDEN` | 无权访问 | 返回首页或显示无权限 |

### 14.2 资源和业务

| code | 名称 | message | 前端处理 |
| --- | --- | --- | --- |
| `40400` | `NOT_FOUND` | 资源不存在 | 返回上级页面 |
| `40401` | `IMPORT_JOB_NOT_FOUND` | 导入任务不存在 | 停止轮询并提示 |
| `40402` | `QUESTION_NOT_FOUND` | 题目不存在 | 刷新题库或重建会话 |
| `40403` | `SESSION_NOT_FOUND` | 练习会话不存在 | 清理会话并重新创建 |
| `40404` | `REVIEW_NOT_FOUND` | 复盘结果不存在 | 显示无复盘记录 |
| `40405` | `USER_NOT_FOUND` | 用户不存在 | 清理登录态 |
| `40900` | `CONFLICT` | 资源冲突 | 刷新当前数据 |
| `40901` | `SESSION_FINISHED` | 练习会话已结束 | 跳转复盘或新建练习 |

### 14.3 参数、导入和 AI

| code | 名称 | message | 前端处理 |
| --- | --- | --- | --- |
| `42200` | `VALIDATION_FAILED` | 参数校验失败 | 定位字段错误 |
| `42202` | `IMPORT_FORMAT_INVALID` | 文件格式不正确 | 选择 xlsx/csv |
| `42203` | `ANSWER_FORMAT_INVALID` | 答案格式不正确 | 保留选择并重试 |
| `42204` | `STEP_LENGTH_INVALID` | 步骤数量不匹配 | 刷新题目后重试 |
| `42205` | `STATE_INVALID` | 状态参数不合法 | 检查收藏/错题参数 |
| `42206` | `FILE_EMPTY` | 文件为空 | 重新选择文件 |
| `50300` | `AI_PROVIDER_ERROR` | AI 服务调用失败 | 保留标准解析并重试 |
| `999` | `INTERNAL_ERROR` | 处理失败 | 通用失败和重试 |

### 14.4 HTTP 状态注意事项

- 业务异常通常 HTTP 200 + 非 `200` 业务 code。
- 未认证可能返回 HTTP 401。
- 无权限通常返回 HTTP 403。
- 前端 HTTP 封装层需要同时读取 HTTP status 和 JSON body。
- SSE 请求不能按普通 JSON 响应解析，应读取 `response.body`。

---

## 15. 前端联调约定

### 15.1 API 封装

建议前端按业务模块拆分 API：

- `auth.js`：认证接口。
- `users.js`：当前用户、密码和学习摘要。
- `questions.js`：科目、题目分页和详情。
- `practice.js`：练习会话、答题、结束和复盘。
- `state.js`：错题、收藏和题目状态。
- `exam.js`：模拟考试和考试记录。
- `ai.js`：SSE AI 讲解。
- `admin.js`：模板和题库导入。

页面组件不直接拼接接口 URL，也不直接处理 token 刷新。

### 15.2 用户切换时的缓存处理

登录成功或切换账号时清除：

- 当前练习 session ID。
- 当前题目 ID。
- 待创建练习请求。
- 当前练习模式。
- 当前题目、答案和 AI 文本等页面内存缓存。

默认科目重置为 `CN`。如果后端返回科目列表中没有 CN，则使用返回列表的第一个科目。

### 15.3 题库共享与状态隔离

前端不应因为当前用户没有 `ai408_user_question_state` 记录而显示“没有题库”。正确逻辑是：

1. 调用 `/api/v1/subjects` 获取公共题库科目。
2. 调用学习摘要接口获取当前用户统计。
3. 调用练习接口创建当前用户的会话。
4. 用户答题后才产生用户题目状态。

### 15.4 练习请求上下文

从复盘、错题本或考试详情进入练习时，前端使用待执行请求传递：

```json
{
  "mode": "sequence",
  "subjectCode": "CN",
  "questionIds": ["q_001", "q_002"],
  "limit": 2,
  "source": "practice-review-wrong-retry",
  "title": "重做本次错题"
}
```

该对象是前端 UI 状态，不是后端 API 的独立资源；进入练习页后转换为创建练习会话请求。

## 16. OpenAPI 使用建议

- 前端重构前先用 Swagger UI 验证接口真实返回，不要仅依赖页面旧逻辑。
- 使用 `Authorize` 后可直接测试所有需要登录的接口。
- 对文件上传接口使用 Swagger 的 multipart 文件选择器。
- SSE 接口建议使用浏览器或前端页面测试，Swagger UI 主要用于查看 schema 和接口描述。
- 如果接口字段变更，先修改 DTO 和 Controller 注解，再同步更新本文档和前端 API 封装。
- 任何数据库字段变化仍必须通过 Flyway migration，不通过 Swagger 文档直接修改数据结构。

