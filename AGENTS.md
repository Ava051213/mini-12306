# mini-12306 项目交接

## 使用方式

- 新对话先读取本文件，再检查与当前任务相关的源码及 Git 状态。以实际文件和用户最新指示为准，发现记录过期时及时纠正。
- 当前主对话负责整体规划、模块顺序和进度协调；模块对话负责引导一个边界明确的功能。
- 不假定可以访问其他对话的完整历史。交接所需的决定、验证结果和下一步写在本文件中。
- 本文件纳入 Git 版本管理。暂时只维护这一份交接文件，避免重复记录。

## 学习与操作约定

- 用户的目标是亲手走通每个流程，并理解设计、代码、测试和 Git 操作。
- 使用中文交流。用户有一些 Java 和 Maven 基础，已表示熟悉 Maven 父子模块概念；无需重复讲解，可通过阅读真实 POM 验证并衔接依赖分析。基础仍不能成为跳过具体用法解释的理由。
- 用户偏好较快、连贯的引导，不要频繁停下来提理解检查题；直接解释当前步骤与下一步，用户遇到不理解的地方会主动提问。
- 自乘车人 Service 完成后的步骤起，后续示例代码在关键意图处添加简短英文注释；不要求回头为此前代码补注释。
- 每次推进一个小步骤：说明目的，提供操作方法，解释关键内容与预期结果，再根据用户反馈继续。
- 未经用户明确允许，不替用户修改业务代码、配置或数据库，不替用户执行构建、测试、容器操作、Git 暂存、提交或推送等命令。
- 用户已授权只读检查项目，以及由助手直接维护本文件。此授权不扩展到其他文件，也不包含提交和推送本文件。
- 不删除文件或替用户清理环境。提出精简建议时先解释文件用途。
- 不读取或展示 `.env` 的密钥值，不把密码等私密信息写入交接文档。
- 提交说明使用规范英文，通常采用 `type: description`。先让用户尝试拟写，再帮助修改；必要时用正文补充原因或验证内容。
- 不主动创建或向其他对话发送任务，除非用户明确要求。
- 学习业务模块时，结合参考项目进行定向阅读：先看整体结构，再沿一条真实业务调用链追踪 Controller、业务处理和数据访问，最后讨论模块依赖与修改影响。每次只安排少量、与当前知识点相关的文件，并解释阅读顺序；不要求用户先读完整仓库。
- 对照原项目和精简版时，明确区分“原项目实际采用的设计”“为第一版选择的简化方案”和“尚未实现或尚未决定的部分”，不把原项目的复杂度直接搬入精简版。

## 项目目标与边界

- 参考项目：<https://github.com/nageoffer/12306>。
- 已确定：从零手写精简版，边做边讲。参考原项目不代表直接复制其代码、架构或全部功能。
- 原项目的具体实现和版本要求需要实际查证，不凭记忆推断；本文件记录的是本项目现状。
- 第一版范围和总体架构已由用户同意，见下方“第一版目标与架构”。字段、接口和具体实现由对应模块对话逐步讨论，不在开工前一次性定死。

## 第一版目标与架构

### 验收场景

第一版以一条能够实际运行和测试的购票闭环为目标：用户注册并登录，添加一位乘车人，查询某天的车次和余票，为该乘车人创建订单；下单后余票减少，模拟支付或取消订单后，能查询到正确的订单状态，取消时余票恢复。

### 简化范围

- 先支持预先录入的示例车次和日期，不接入铁路 12306 的外部接口或实时数据。
- 第一版只售卖车次固定起终点之间的全程票，避免引入区间库存复用和跨站座位分配；中途站购票留待后续扩展。
- 第一版只支持一种席别，每笔订单只包含一位乘车人和一张票；库存按数量管理，不分配具体座位号。
- 支付只模拟本系统内的状态变化，不接入真实支付渠道。
- 取消订单与库存恢复纳入第一版流程；退款、支付超时关单等规则后续再评估。
- 上述限制是控制第一版复杂度的范围，不代表完整铁路售票规则。

### 架构与职责

- 采用单体应用：一个 Spring Boot 服务和一个 MySQL 数据库。第一版不拆分微服务，也不引入网关、Redis 或消息队列。
- 在应用内部按业务职责组织用户、乘车人、车次查询、库存和订单相关代码。
- 逐步学习分层职责：Controller 处理 HTTP 请求与响应，Service 承载业务流程，MyBatis Mapper 负责数据库访问。具体包结构随第一个业务模块一起设计。
- 业务模块是对业务职责的划分；Controller、Service、Mapper 是代码内部按职责分层，两者不要混为一谈。

### 模块边界

| 模块 | 第一版职责 |
| --- | --- |
| 用户 | 注册、登录及当前用户信息 |
| 乘车人 | 管理用户可用于购票的乘车人 |
| 车次查询 | 查询预先录入的车次、日期和简单乘车区间 |
| 库存 | 查询余票、下单扣减库存、取消时恢复库存 |
| 订单 | 创建、查询、取消订单及模拟支付状态流转 |

模块对话可以把模块继续拆小，例如先完成注册，再处理登录。数据表字段、重复请求处理、订单状态细节等在相关模块中讲解并确认。

## 已核对的技术与文件

截至 2026-10-23（数据库版本沿用此前实查记录）：

| 项目 | 当前情况 |
| --- | --- |
| 项目坐标 | `com.mia:mini-12306:0.0.1-SNAPSHOT` |
| Java / Spring Boot | Java 17 / Spring Boot 4.0.8 |
| Web | `spring-boot-starter-webmvc` |
| 持久层 | MyBatis Spring Boot Starter 4.0.1；用户注册、按用户名及 ID 查询已实现 |
| 其他依赖 | MySQL Connector/J、Lombok、Spring Security Crypto（BCrypt）、Spring Boot Validation；Lombok 已配置注解处理器 |
| 数据库 | Compose 镜像 `mysql:8.4`；此前实际查询版本为 8.4.11 |
| 数据库连接 | 本机 `localhost:3307` → 容器 `3306`，数据库 `mini_12306` |
| 数据持久化 | Compose 命名卷 `mysql_data`，挂载 `/var/lib/mysql` |
| 凭据 | 根目录 `.env` 中的 `MYSQL_ROOT_PASSWORD`；已被 Git 忽略 |
| 分支 / 远程 | `main` / `git@github.com:Ava051213/mini-12306.git` |

- `pom.xml`：项目依赖、Java 版本、构建和 Lombok 注解处理配置。
- `compose.yaml`：本地 MySQL 服务及 `restart: unless-stopped` 配置。
- `src/main/resources/application.yaml`：通过 `optional:file:.env[.properties]` 导入本地凭据；使用 root 连接数据库。当前 JDBC 参数用于本地开发，不作为部署配置范例。
- `src/main/java/com/mia/mini12306/Mini12306Application.java`：Spring Boot 启动入口。
- `src/test/java/com/mia/mini12306/Mini12306ApplicationTests.java`：包含上下文启动测试及数据库连通性测试。
- 连通性测试用 `JdbcTemplate` 执行 `SELECT 1`、`SELECT DATABASE()`，断言结果分别为 `1`、`mini_12306`。这两项测试验证应用启动与数据库连接；注册业务另由 `UserRegistrationIntegrationTests` 验证。
- `users` 表已由用户在 IDEA 数据库控制台创建并通过 `SHOW CREATE TABLE` 核验；使用 `utf8mb4_0900_ai_ci`，用户名唯一约束不区分大小写。
- 用户注册代码位于 `src/main/java/com/mia/mini12306/user/`：`UserController`、`UserRegistrationService`、`UserMapper`、`UserAccount`、请求和响应 DTO、重复用户名异常及异常处理器。
- 注册请求用户名限制为 3–50 位 ASCII 字母、数字或下划线；密码限制为 8–72 个字符且 UTF-8 编码不超过 72 字节。密码使用 BCrypt 哈希后存储，响应不包含哈希。
- 注册接口为 `POST /api/users/register`，成功返回 201；重复用户名返回 409；参数校验失败返回 400。
- `config/PasswordEncoderConfig.java` 声明 BCrypt `PasswordEncoder` Bean，注册与登录服务通过构造器共同注入；注册使用 `encode`，登录使用 `matches`。
- 登录相关代码：`UserLoginService`、`dto/UserLoginRequest`、`dto/UserProfileResponse`、`InvalidCredentialsException`、`NotLoggedInException`，HTTP 会话处理位于 `controller/UserController`。
- `POST /api/users/login` 成功返回 200 和公开资料；账号不存在、密码不匹配或密码超过 72 个 UTF-8 字节均抛认证失败异常并返回 401。请求 DTO 使用 `@NotBlank`，空白或缺失字段由参数校验处理。
- 登录状态采用 Servlet `HttpSession`：认证成功后使已有会话失效，再建立新会话；只保存 `Long` 类型的 `LOGIN_USER_ID`，空闲超时为 1800 秒。没有引入 JWT、Redis 或完整 Spring Security 过滤器链。
- `GET /api/users/me` 使用 `getSession(false)` 读取会话用户 ID，再通过 Service 和 `UserMapper.findById` 查询；没有有效登录身份或用户不存在时返回 401。该 SQL 不读取密码哈希。
- `POST /api/users/logout` 使当前会话失效并返回 204；没有会话时同样返回 204，不删除账号，不退出其他独立会话。
- 登录及当前用户资料统一返回 `UserProfileResponse`（`id`、`username`、`createdAt`），不直接返回包含哈希的实体。
- `target/` 是构建产物，`.idea/` 是本地 IDE 配置，`HELP.md` 是生成的帮助文档；均已被 Git 忽略。

## 本地环境经验

- 用户终端使用 PowerShell，已成功使用系统 Maven 3.9.14 和 Java 17 构建项目。
- Maven Wrapper 文件保留，当前指定 Maven 3.10.0。
- IDEA 曾在使用 Wrapper 同步时出现 `ArtifactHandlerManager` 的 Guice 错误；切换到 IDEA 捆绑 Maven 后同步及驱动提示恢复正常。不能仅凭此现象断定根因。
- 本机另有 MySQL80 服务，所以项目容器使用本机端口 3307，避免与默认 3306 冲突。
- Spring 应用在项目根目录启动时可按当前相对路径读取 `.env`；Docker Compose 也使用根目录 `.env`，两者读取机制不同。
- 容器是否正在运行、数据库是否可连接，需要当次验证，不能根据以前成功启动的记录推定。

## 已完成与验证

1. 初始化 Git 仓库，通过 SSH 关联 GitHub，并完成推送。
2. 配置 Compose MySQL、持久化数据卷与端口映射，验证数据库及版本。
3. 配置应用数据源、读取本地凭据，并加入数据库连通性测试。
4. 用户注册闭环已实现：数据表、输入校验、BCrypt 密码哈希、MyBatis 持久化、成功响应和重复用户名错误处理。
5. 用户于 2026-10-22 运行 `mvn test`：6 项测试通过，0 失败、0 错误、0 跳过；覆盖应用上下文和数据库连接、注册成功及哈希验证、重复用户名、无效输入、密码超过 BCrypt UTF-8 字节上限。
6. 注册成功用例使用随机用户名写入本地开发数据库；集成测试不会清理注册记录。最新测试未再出现 MyBatis “No mapper found” 警告。
7. 用户亲自实现登录、当前用户查询和退出登录。2026-10-23 首次运行登录测试因数据库连接被拒绝而失败；随后重跑成功，未据此修改业务代码，也未确认首次拒绝连接的具体环境原因。
8. 用户于 2026-10-23 运行 `mvn "-Dtest=UserLoginIntegrationTests" test`：最初的登录闭环测试 1 项通过；随后补齐失败场景，运行 `mvn test`：10 项通过，0 失败、0 错误、0 跳过（基础 2 项、注册 4 项、登录 4 项）。助手只读核对源码与用户提供的运行输出，未代跑测试。
9. `UserLoginIntegrationTests` 验证注册后登录、携带会话查询当前用户、退出返回 204 和原会话失效、退出后无会话查询返回 401；另有错误密码、账号不存在、独立未登录查询 3 个测试。登录及查询响应不含密码/哈希字段与实际敏感值；认证失败不创建新会话。登录测试同样会保留随机注册记录。
10. 主对话于 2026-10-23 完成只读验收：核对 Controller、Service、Mapper、DTO、异常处理、共享 PasswordEncoder 及注册回归测试，未发现阻断当前最小闭环的问题，本地学习阶段验收通过。读取现有 Surefire XML 报告确认基础 2 项、注册 4 项、登录 4 项，共 10 项通过，0 失败、0 错误、0 跳过；报告文件时间为当日 09:54:11–09:54:14，按文件时间检查未发现报告生成后修改的源码或测试。本次未重新执行构建或测试，验收不包含下列尚未验证项或部署安全配置。

11. 乘车人基本增查改、共享会话身份及五项集成测试已完成。2026-10-23 用户运行 `mvn test`，全量 15 项通过，0 失败、0 错误、0 跳过；模块对话已只读核对源码、用户输出和 Surefire 报告，主对话只读验收通过，详见下节。

### 乘车人模块最小闭环验收通过

- 用户亲自完成乘车人新增、列表、详情和修改，以及共享会话身份读取；助手只读核对业务与测试源码，未发现阻断当前学习闭环的问题。业务代码位于 `src/main/java/com/mia/mini12306/passenger/`，测试为 `PassengerIntegrationTests`。
- 用户在 IDEA 数据库控制台创建 `passengers` 表，并提供 `SHOW CREATE TABLE` 核验。字段为有符号自增 `BIGINT id`、`BIGINT user_id`、`VARCHAR(50) real_name`、`VARCHAR(18) id_card`、`VARCHAR(11) phone` 及 `DATETIME created_at/updated_at`，均非空；时间由数据库生成，更新时间随记录修改。建表 SQL 尚未作为文件纳入仓库。
- 主键为 `id`；联合唯一约束 `uk_passengers_user_id_card (user_id, id_card)` 保证同账号不可重复添加同一证件，不同账号可添加相同证件；索引同时支持账号列表查询。`fk_passengers_user` 指向 `users.id`，类型均为有符号 BIGINT，无级联删除。
- `user/CurrentUserSession.java` 通过 `getSession(false)` 读取会话，检查 `LOGIN_USER_ID` 为 Long，并复用 `UserLoginService.getCurrentUser` 检查账号仍存在。`/me` 和四个乘车人入口复用该组件；登录也使用共享常量写入身份。请求 DTO 不定义 `userId`，归属来自服务端会话。
- 接口：`POST /api/passengers` 返回 201 空响应；`GET /api/passengers` 返回 200 数组，无记录时为 `[]`；`GET /api/passengers/{id}` 和 `PUT /api/passengers/{id}` 返回 200 及 `PassengerResponse`。未登录返回 401“请先登录”；他人或不存在的记录统一返回 404“乘车人不存在”；重复证件返回 409“该乘车人已添加”。错误响应使用 `message` 字段。
- `PassengerSaveRequest` 的姓名非空白且 2–50 字符；身份证非空白并匹配 `^[0-9]{17}[0-9Xx]$`；手机号非空白并匹配 `^1[3-9][0-9]{9}$`。Controller 增改入口使用 `@Valid`。身份证仅检查格式，不校验出生日期、校验码或实名。响应包含 id、姓名、完整证件、电话及创建/更新时间，不含 userId，本版向所属账号返回完整资料。
- `PassengerMapper` 新增后回填主键，列表按 `user_id` 过滤并按 id 排序，详情和修改同时限定乘车人 ID 与 user_id；修改不改变归属。`PassengerService` 从服务端账号 ID 构建实体，以 `Locale.ROOT` 将身份证转为大写，捕获新增/修改的 `DuplicateKeyException` 并转为业务 409。
- 修改先执行带归属条件的 UPDATE，再按同一账号与记录 ID 查询返回，不使用更新影响行数判断不存在，避免相同内容的修改误报。当前每次只写一条记录，未引入事务；相同内容修改、真正不存在 ID 和并发修改尚无专项测试。
- 五项乘车人测试：正常新增/空列表/列表/详情/修改及重新查询；四接口未登录 401 且不创建会话；两个独立账号列表隔离、他人详情/修改 404 且原记录全字段不变；10 组无效输入分别验证 POST/PUT 400 且原记录不变；同账号大小写归一化后的重复新增和修改冲突 409 且原记录不变。正常流程和跨账号测试特意使用不相等的账号 ID 与乘车人 ID，避免参数颠倒被相等数字掩盖。
- 集成测试通过真实注册、登录及本地 MySQL 执行，使用随机账号与显式传递的 MockHttpSession；测试账号和乘车人记录保留，不自动清理。2026-10-23 18:31 用户运行 `mvn "-Dtest=PassengerIntegrationTests" test`：5 项通过，0 失败、0 错误、0 跳过。
- 最初一次乘车人测试因数据库连接被拒绝而报错，随后重跑可连接；具体环境原因未确认。正常流程曾因测试期待小写 x、实际返回大写 X 而失败，用户已修正期望并保留小写输入验证归一化；此前 INSERT 表名和详情 Service 参数顺序的手写错误也已修正并通过测试。
- 已核验本地 Jackson Databind 3.1.5 源码：`JsonNode.asText()` 自 3.0 过时，替代为 `asString()`，`asLong()` 未过时。用户已替换测试中两处调用；最新重编译输出未再出现过时 API 提示。
- 最终全量回归：用户运行 `mvn test`，于 2026-10-23 18:43:45 +08:00 完成，基础 2 项、注册 4 项、登录 4 项、乘车人 5 项，共 15 项通过，0 失败、0 错误、0 跳过，BUILD SUCCESS。助手核对用户输出与四份 Surefire XML 一致；报告文件时间为当日 18:43:38–18:43:45，按文件时间检查未发现最后一份报告生成后修改的 src 文件。助手未代跑构建或测试。
- 主对话于 2026-10-23 只读验收通过，范围为本地学习阶段的新增、列表、详情、修改及账号隔离。核对 Controller、Service、Mapper、DTO、异常处理、共享会话组件和用户 Controller 的接入，未发现阻断该最小闭环的问题；复核四份现有 Surefire XML 为全量 15 项通过。按文件时间检查，最新 src 修改为当日 18:43:05，早于报告；本次未代跑构建或测试，也未连接数据库重新核验表结构。删除、并发与其他验证边界仍以下节为准。

### 当前验证边界与待完善项

- 当前通过 MockMvc 显式传递 `MockHttpSession` 验证会话延续；尚未通过真实 HTTP 客户端验证 `JSESSIONID` cookie 收发和旧 cookie 在退出后的行为，也未实测 30 分钟自然超时。
- 登录前已有会话的替换、超长登录密码、空白登录输入、会话对应账号已不存在、无会话时退出等分支已写入实现，但本轮未为它们补充专项测试。
- 当前 `/me` 和四个乘车人入口已复用 `CurrentUserSession`，乘车人未登录及跨账号访问已验证；后续订单等受保护接口必须同样使用服务端登录身份确定归属，不能直接信任客户端提交的用户 ID。
- 乘车人删除尚未实现，规则留待讨论；身份证仅做格式校验，相同内容修改、真正不存在 ID 和并发修改尚未专项验证。
- 乘车人未登录测试使用有效请求体，验证四个入口返回 401；增改接口的 `@Valid` 参数校验先于方法内身份检查，未登录且请求体无效时可能先返回 400。建表 SQL 尚未纳入仓库，新环境建库仍需补充可复现的脚本。
- 当前未实现 CSRF 防护或部署环境的 cookie 安全配置；浏览器前端接入或部署时继续完善。框架参数校验日志可包含被拒绝的密码、证件号和手机号输入；响应隔离测试不代表日志已脱敏，日志脱敏留待处理。

已确认推送到 GitHub 的提交：

- `6c60ac2` — `chore: initialize Spring Boot project`
- `9c605c2` — `chore: add Docker Compose configuration for local MySQL`
- `61e0ae8` — `chore: configure MySQL datasource and add connectivity test`
- `e8f27d2` — `feat: add user registration`
- `5ffa315` — `feat: add login, current user lookup and logout`

## 学习顺序

| 顺序 | 功能 | 建议学习目标与完成条件 |
| --- | --- | --- |
| 0 | 项目基础（已完成） | 环境、Git、容器数据库及应用实际连接验证 |
| 1 | 用户注册（已完成） | 讨论表字段与约束，走通请求、校验、持久化、响应和测试；理解重复注册及密码存储 |
| 2 | 登录与用户信息（最小闭环验收通过） | BCrypt 验证、HttpSession 登录态、当前用户查询、退出及 401 验证 |
| 3 | 乘车人（最小闭环验收通过） | 账号归属、基本增查改及五项集成测试已完成；删除规则另行讨论 |
| 4 | 车次和乘车区间 | 建立精简的示例数据，完成指定日期和区间的车次查询 |
| 5 | 库存与创建订单 | 通过数据库事务和条件扣减保证库存不为负，验证失败时回滚、库存不足时拒绝下单，以及并发下不超卖 |
| 6 | 订单后续流程 | 查询、取消、恢复库存和模拟支付，验证状态变化 |
| 7 | 完善与进阶 | 结合已实现流程评估异常处理、并发问题、索引及部署，不提前堆砌框架 |

- 一次模块对话以一个可验证的完整功能为目标；“用户模块”等范围过大时继续细分。
- 第一版采用单体应用和简化业务规则；未来是否增加复杂区间售票、真实支付、前端或分布式组件，需结合学习目标另行讨论。
- 前端范围和接口调试工具尚未确定。

### 与参考项目对照的学习方法

- 开始业务模块时，先在参考仓库中定位同一业务的入口和调用链，再确定本项目本次要实现的最小闭环。
- 追踪时记录入口、业务规则所在位置、数据访问位置、调用方向、跨模块依赖及失败处理；结合代码说明模块耦合来自哪里，以及一次改动可能影响哪些调用方。
- 先读 README、根 Maven 聚合配置和目录结构建立地图；业务代码按功能逐条追踪，不尝试一次看完所有模块。
- 文件路径和模块名称必须根据参考仓库当前内容核验后再交给用户。阅读以小批量为单位，讲完一组再继续。
- 参考项目的架构和技术细节是学习材料，不自动成为本项目的依赖或需求。任何额外技术均需说明它解决的问题以及是否属于第一版范围。

### 参考仓库导览记录

2026-10-22 已只读核验参考仓库 `main` 分支的 `README.md`、根 `pom.xml`、`services/pom.xml`、`frameworks/pom.xml`、`dependencies/pom.xml` 和 `services/user-service/pom.xml`。未固定提交版本；后续阅读时留意仓库变更。

- 根 `pom.xml` 的 `<packaging>pom</packaging>` 和 `<modules>` 聚合四个模块：`dependencies`、`frameworks`、`services`、`tests`。
- `dependencies/pom.xml` 通过属性和 `<dependencyManagement>` 集中管理依赖版本；根 POM 将其作为 BOM 导入。版本管理不等于将所有依赖加入每个模块。
- `frameworks/pom.xml` 聚合项目自建的公共组件，包括 base、bizs、cache、common、convention、database、designpattern、distributedid、idempotent、log、web；具体职责需再读对应代码，不凭名称推断全部实现。
- `frameworks` 在此仓库是可复用支撑代码的分组，不等于“后台系统任务”。其中既有 cache、database、web 等技术组件，也有 `bizs/user` 这类业务共享组件。定时任务或消息消费者要根据真实代码归属判断。
- `services/pom.xml` 聚合 aggregation-service、gateway-service、order-service、pay-service、ticket-service、user-service。
- `services/user-service/pom.xml` 的 parent 指向 services 聚合 POM，依赖中包含项目自建的 database、web、cache 等组件，以及 Web、分片、服务发现等外部依赖。后续按注册流程需要逐个解释，不一次引入本项目。
- README 明确提供聚合服务与微服务两种启动方式；不能仅凭目录有多个服务就断言只能以微服务运行。
- 阅读时区分三件事：`<modules>` 描述构建聚合，`<dependencies>` 描述代码所需依赖，HTTP/远程调用或消息等运行时联系需要追源码与配置；Maven 模块清单不是运行时调用图。
- 导览第一步只给用户根 `pom.xml` 和 `services/pom.xml`，观察 packaging、modules 和 parent。理解后再继续依赖管理与业务入口，保持小步推进。

用户注册流程已核验到的入口：

- 原项目 `services/user-service/src/main/java/org/opengoofy/index12306/biz/userservice/controller/UserInfoController.java` 中的 `register` endpoint 接收并校验 `UserRegisterReqDTO`，再委托给 `UserLoginService.register`。
- 具体实现位于同包路径下 `service/impl/UserLoginServiceImpl.java` 的 `register`。该方法涉及校验链、Redisson 锁、多个表写入、Redis 和布隆过滤器，远超精简版第一步所需；先理解 Controller 如何把 HTTP 请求交给 Service，再只挑业务必需的概念继续追踪。
- 参考仓库目录和代码路径来自 2026-10-22 对 `main` 的核验。仓库会变化，继续阅读时复核。

本轮登录流程定向阅读已核验（`main`，未固定提交版本）：

- `services/user-service/src/main/java/org/opengoofy/index12306/biz/userservice/controller/UserLoginController.java` 提供登录、检查登录和退出入口，委托 `UserLoginService`。
- `service/impl/UserLoginServiceImpl.java` 的 `login` 解析用户名/手机号/邮箱，再经 Mapper 查询账号；当前读取到的代码使用用户名与请求密码构造查询条件。本项目继续使用已有 BCrypt 哈希与 `matches` 校验，不照搬该查询方式。
- 该实现通过 `JWTUtil.generateAccessToken` 生成 token，将登录资料按 token 存入 `DistributedCache`，有效期 30 分钟；`checkLogin` 读缓存，`logout` 删除缓存。仅凭这几个方法不能推断所有受保护接口的完整鉴权链。
- `UserInfoController.queryUserByUsername` 接收用户名查询资料，与本项目从会话识别当前用户的 `/me` 语义不同。
- 原项目采用 JWT 与分布式缓存；本单体应用选择 Servlet 会话，不引入上述组件。Controller 负责 HTTP/会话，Service 负责凭据验证和资料查询，Mapper 负责 SQL；后续身份共享方式再按业务需要设计。

本轮乘车人流程定向阅读已核验（2026-10-23，读取 `main` 原始源码，未固定提交版本）：

- `services/user-service/src/main/java/org/opengoofy/index12306/biz/userservice/controller/PassengerController.java` 提供列表查询、新增和修改入口，委托 `PassengerService`，实现位于 `service/impl/PassengerServiceImpl.java`。
- 列表入口从 `UserContext.getUsername()` 取得用户名；Service 通过缓存读取列表，缓存未命中时使用用户名条件调用 `PassengerMapper.selectList`。
- `savePassenger` 校验姓名、证件号和手机号，再从 `UserContext` 取得用户名，写入 `PassengerDO` 后调用 Mapper 插入，成功后删除该账号的乘车人列表缓存。
- `updatePassenger` 同样取得当前用户名，更新条件同时包含用户名与请求中的乘车人 ID，完成后删除列表缓存。归属条件是本次精简版要借鉴的关键设计。
- `dao/mapper/PassengerMapper.java` 继承 MyBatis-Plus `BaseMapper<PassengerDO>`；SQL 操作条件在 Service 的 Wrapper 中构造。`PassengerDO` 映射 `t_passenger`，含用户名、姓名、证件类型/号码、优惠类型、手机号、添加日期和审核状态等字段。
- 已读取 `dto/req/PassengerReqDTO.java`、`dto/resp/PassengerRespDTO.java`。响应中证件号和手机号声明了脱敏序列化器；尚未追踪序列化器实现，不据此推断全部字段的最终返回值。
- `frameworks/bizs/user/src/main/java/org/opengoofy/index12306/frameworks/starter/user/core/UserTransmitFilter.java` 从请求头构造用户资料并写入 `UserContext`，在 finally 中清理；`UserContext` 使用线程上下文保存资料。本轮未核验网关对身份头的验证或覆盖行为，不能把该过滤器本身等同于完整认证。
- 精简版继续使用已有 Servlet 会话，通过 `CurrentUserSession` 复用身份读取；乘车人归属由服务端确定，查询和修改 SQL 均限制 user_id。不引入原项目的缓存、幂等组件或线程上下文。实际表结构、接口、实现和验证结果见“乘车人模块最小闭环验收通过”。

## 当前停留点与交接规则

- 当前阶段：注册（`e8f27d2`）与登录（`5ffa315` — `feat: add login, current user lookup and logout`）均已提交并推送。乘车人新增、列表、详情、修改及身份归属由用户亲自实现，2026-10-23 全量 15 项测试通过，主对话只读验收通过；用户已提交业务代码：`9423424` — `feat: add passenger management and account isolation`，尚未推送。
- 2026-10-23 业务提交后只读检查：main 比本地 origin/main 引用领先 1 个提交，工作区仅有本文件修改。用户已重新暂存并提交完整的 12 个业务与测试文件，此前暂存区为空壳或旧版本的问题已解决。交接文档尚未提交，需由用户单独审阅并提交；本轮未刷新远程引用。
- 当前停留点：下一步由用户拟写规范英文文档提交说明，亲自提交交接文档并推送，收尾后开启车次模块的新对话。此前数据库拒绝连接的具体环境原因未确认。
- 助手本轮仅只读检查及维护本文件，未修改业务或测试文件，未执行构建、测试、容器或 Git 写操作。不主动创建或向其他对话发送任务。继续小步教学，后续示例代码添加简短英文注释，已有代码无需补注释；文档不维护自身提交编号，以免循环修改。
- 主对话给模块对话的任务应包含：目标、已知前提、范围、需要讨论的决定，以及完成后的验证要求。
- 模块对话结束时更新本文件：实际完成的功能、关键设计决定、验证命令及结果、已知问题、下一步；有业务提交时记录其编号。未验证或未推送的状态必须明确标注。
- 不复制完整聊天日志；保留继续学习必需的信息。进度改变时替换过期的“当前停留点”，避免累积互相冲突的指令。
