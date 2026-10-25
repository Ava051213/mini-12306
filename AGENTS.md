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
- 按一个有明确目的的完整步骤引导：先说明目的，再集中提供操作、关键解释和预期结果，完成后统一反馈；报错时停下处理。用户于 2026-10-25 明确反感过细拆分、反复贴输出和手工搬运 SQL，优先让工具直接读取已有文件，避免重复验证同一购票流程。
- 未经用户明确允许，不替用户修改业务代码、配置或数据库，不替用户执行构建、测试、容器操作、Git 暂存、提交或推送等命令。
- 用户已授权只读检查项目，以及由助手直接维护本文件。此授权不包含提交和推送。此前库存与创建订单模块的最终交接文档提交曾获单次授权，现已完成，该例外不扩展到本次验收记录或后续提交。
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

- 自动化测试通过 MockMvc 显式传递 `MockHttpSession` 验证会话延续；2026-10-25 用户另通过真实 HTTP 客户端验证 JSESSIONID 保存、携带后访问及退出后旧 Cookie 返回 401，详见“真实 HTTP 购票闭环验收”。30 分钟自然超时仍未实测。
- 登录前已有会话的替换、超长登录密码、空白登录输入、会话对应账号已不存在、无会话时退出等分支已写入实现，但本轮未为它们补充专项测试。
- 当前 `/me`、四个乘车人入口及全部订单入口已复用 `CurrentUserSession`；乘车人、下单、订单查询、支付和取消的未登录及跨账号场景已验证，归属来自服务端会话。后续受保护接口继续遵循这一规则。
- 乘车人删除尚未实现，规则留待讨论；身份证仅做格式校验，相同内容修改、真正不存在 ID 和并发修改尚未专项验证。
- 乘车人未登录测试使用有效请求体，验证四个入口返回 401；增改接口的 `@Valid` 参数校验先于方法内身份检查，未登录且请求体无效时可能先返回 400。2026-10-25 用户已补写并完整暂存 users.sql 和 passengers.sql，五份脚本在独立空库初始化及应用余票查询验证通过；未使用全新容器或重新运行全量测试。
- 当前未实现 CSRF 防护或部署环境的 cookie 安全配置；浏览器前端接入或部署时继续完善。框架参数校验日志可包含被拒绝的密码、证件号和手机号输入；响应隔离测试不代表日志已脱敏，日志脱敏留待处理。

已确认推送到 GitHub 的提交：

- `6c60ac2` — `chore: initialize Spring Boot project`
- `9c605c2` — `chore: add Docker Compose configuration for local MySQL`
- `61e0ae8` — `chore: configure MySQL datasource and add connectivity test`
- `e8f27d2` — `feat: add user registration`
- `5ffa315` — `feat: add login, current user lookup and logout`
- `9423424` — `feat: add passenger management and account isolation`（2026-10-24 用户确认已推送，本地 `origin/main` 也已包含该提交）
- `f4ec8c3` — `feat: add train trip search by date and route`（2026-10-24 用户确认车次模块 Git 收尾完成；本地 `main` 与 `origin/main` 均包含该业务提交及其后的车次交接文档提交）
- `4a66f0b` — `feat: add ticket inventory query and transactional order creation`（2026-10-25 用户确认业务及交接文档均已推送；主对话核对本地 `main` 与 `origin/main` 均包含这两个提交）
- `625a3bf` — `feat: add order queries, simulated payment and cancellation`（2026-10-25 用户亲自提交并推送；主对话核对本地 `main` 与 `origin/main` 均包含该业务及后续交接文档提交，最小闭环验收通过；未刷新远程引用）

## 学习顺序

| 顺序 | 功能 | 建议学习目标与完成条件 |
| --- | --- | --- |
| 0 | 项目基础（已完成） | 环境、Git、容器数据库及应用实际连接验证 |
| 1 | 用户注册（已完成） | 讨论表字段与约束，走通请求、校验、持久化、响应和测试；理解重复注册及密码存储 |
| 2 | 登录与用户信息（最小闭环验收通过） | BCrypt 验证、HttpSession 登录态、当前用户查询、退出及 401 验证 |
| 3 | 乘车人（最小闭环验收通过） | 账号归属、基本增查改及五项集成测试已完成；删除规则另行讨论 |
| 4 | 车次查询（最小闭环验收通过） | 通过仓库中的 SQL 初始化示例车次，按日期和固定起终点查询；库存留到下一模块 |
| 5 | 库存与创建订单（最小闭环验收通过，已推送） | 条件扣减、事务回滚、账号归属及并发场景通过；全量 31 项通过 |
| 6 | 订单后续流程（最小闭环验收通过，已推送） | 当前账号查询、模拟支付、取消恢复、回滚及并发互斥已验证；全量 44 项通过，业务提交 625a3bf |
| 7 | 完善与进阶 | 结合已实现流程评估异常处理、并发问题、索引及部署，不提前堆砌框架 |

- 一次模块对话以一个可验证的完整功能为目标；“用户模块”等范围过大时继续细分。
- 第一版采用单体应用和简化业务规则；未来是否增加复杂区间售票、真实支付、前端或分布式组件，需结合学习目标另行讨论。
- 前端范围尚未确定；第一轮真实 HTTP 验收已使用 PowerShell 的 Invoke-WebRequest、Invoke-RestMethod 和 curl.exe，暂未选定图形化接口调试工具。

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

本轮车次查询定向阅读已核验（2026-10-24，读取参考仓库 `main` 原始源码，未固定提交版本）：

- 查询代码位于 `services/ticket-service/src/main/java/org/opengoofy/index12306/biz/ticketservice/`。`controller/TicketController.java` 的 `GET /api/ticket-service/ticket/query` 接收 `TicketPageQueryReqDTO`，实际委托 `TicketService.pageListTicketQueryV1`；不要仅根据同时存在的 V2 方法推断当前入口使用 V2。
- `dto/req/TicketPageQueryReqDTO.java` 包含 fromStation、toStation、departureDate、departure、arrival；departureDate 使用 `@DateTimeFormat(pattern = "yyyy-MM-dd")`，DTO 继承公共 PageRequest。命名包含分页不代表已核验的方法实际执行数据库分页。
- `service/impl/TicketServiceImpl.java` 的 V1 查询先调用查询责任链，再查询站点代码到地区的缓存映射；缓存未命中时由 StationMapper 读取站点。区间列表缓存未命中时，以 startRegion、endRegion 条件调用 TrainStationRelationMapper.selectList，并通过 TrainMapper.selectById 加载车次资料，最后组装并按发车时间排序。
- `dao/mapper/TrainStationRelationMapper.java` 继承 MyBatis-Plus BaseMapper，查询条件由 Service 的 Wrapper 构造；`dao/entity/TrainStationRelationDO.java` 映射 `t_train_station_relation`，包含 trainId、起终站、起终地区、始发/终到标识及出发/到达时间。
- V1 查询还读取区间席别价格、Redis 余票并在缺失时调用 SeatMarginCacheLoader；本轮未追踪加载器和查询责任链实现。已读取的 V1 方法主体没有将 departureDate 放入区间 Mapper 的筛选条件，不能据此宣称原项目通过日期条件直接查出每日车次，也不能仅凭此断言全项目忽略日期。
- 教学从 Controller 与请求 DTO 两个文件开始，再沿 V1 的区间 Mapper 调用解释 Service 和数据访问。精简版实际实现、字段及接口见下方“车次查询模块完成情况”；沿用全程票范围，库存扣减和订单留到后续模块。

### 车次查询模块完成情况（最小闭环验收通过）

- 用户亲手实现按出发日期、固定起点和终点查询预录入全程示例车次；代码位于 `src/main/java/com/mia/mini12306/train/`，集成测试为 `TrainTripIntegrationTests`。不需要登录，不查询或修改库存，不创建订单。
- 用户在 IDEA 数据库控制台创建 `train_trips` 表并录入示例数据；对应脚本已保存在 `src/main/resources/db/train_trips.sql`。字段为有符号自增 BIGINT `id`、`VARCHAR(20) train_number`、两个 `VARCHAR(50)` 起终站、`DATETIME departure_at/arrival_at`。主键为 `id`；唯一约束 `(train_number, departure_at)`，查询索引 `(departure_station, arrival_station, departure_at)`，检查约束要求到达时间晚于出发时间。
- SQL 脚本包含 2026-11-01 上海虹桥至杭州东两趟、同日上海虹桥至南京南一趟，以及 2026-11-02 上海虹桥至杭州东一趟。脚本用于首次初始化，当前 `CREATE TABLE` 和普通 `INSERT` 不支持对已有数据重复执行；本地数据库运行过一次后不要整份重跑。新环境先执行脚本再运行集成测试。
- 接口为 `GET /api/trains?departureDate=yyyy-MM-dd&fromStation=...&toStation=...`。缺失/格式错误的查询参数及相同起终站返回 400；有效但无匹配车次返回 200 和空数组；正常结果按出发时间升序返回。无效参数的框架默认响应正文未统一为项目的 `message` 格式。
- `TrainTripController` 绑定并校验请求 DTO，将实体转换为响应 DTO；`TrainTripService` 把 LocalDate 转成当天起点和次日零点；`TrainTripMapper` 用起终站相等条件及 `[dayStart, nextDayStart)` 半开时间范围查询，并通过 SQL 列别名映射实体。时间范围避免午夜边界重复归日，也允许发车时间范围利用索引。
- 四项集成测试覆盖正常查询及排序、无匹配结果、错误日期格式、缺少起点、相同起终点。测试连接本地 MySQL，依赖已由 SQL 脚本录入的固定种子数据，不清理车次记录。
- 2026-10-24 用户运行 `mvn "-Dtest=TrainTripIntegrationTests" test`：4 项通过，0 失败、0 错误、0 跳过；随后运行 `mvn test`：基础 2 项、乘车人 5 项、车次 4 项、登录 4 项、注册 4 项，共 19 项通过，0 失败、0 错误、0 跳过，BUILD SUCCESS。全量输出时间为 15:06:45 +08:00；对应 Surefire XML 报告于 15:06:38–15:06:45 生成。本轮由用户运行测试，助手未代跑。
- 助手只读核对本模块源文件、SQL、测试和全量构建输出；未发现阻断本次查询闭环的问题。`git diff --check` 未报告空白错误（只有 AGENTS.md 的 LF/CRLF 提示）。本轮未执行数据库操作、构建、测试或 Git 写命令，也未提交或推送业务代码。
- 2026-10-24 主对话只读验收通过，范围为本地学习阶段按出发日期和固定起终点查询全程示例车次。核对 Controller、Service、Mapper、请求与响应 DTO、异常、建表及示例数据脚本和四项测试，未发现阻断该最小闭环的问题；复核五份现有 Surefire XML：全量 19 项通过，0 失败、0 错误、0 跳过。最新 src 文件修改时间为当日 15:03:38，早于本轮报告。本次未代跑测试，未执行 SQL，也未连接数据库重新核验实际表结构。
- 用户已重新暂存并提交完整的 9 个业务、测试和 SQL 文件，提交为 `f4ec8c3` — `feat: add train trip search by date and route`，并已完成文档提交和推送。2026-10-24 库存模块开始时只读核对：`main` 与本地 `origin/main` 指向相同的车次交接文档提交，均包含该业务提交，工作区干净；用户确认 Git 收尾已完成，本次未刷新远程引用。此前暂存区为空壳或空 SQL 的问题已解决；助手没有更改暂存区。
- 当前边界：未测试真实 HTTP 客户端或部署场景；无效参数响应体格式沿用框架默认值；初始化脚本不能重复应用；未接入数据库迁移工具。午夜边界、缺少日期或终点、空白站名尚无专项测试；排序测试的示例数据本身按时间插入，尚未专门构造乱序数据验证排序。库存、席别和订单流程不属于本模块。

### 余票查询与创建订单模块（最小闭环验收通过，已推送）

- 用户于 2026-10-24 至 10-25 亲手实现余票查询与创建订单，沿用全程票、一种席别、一位乘车人一张票。代码位于 `src/main/java/com/mia/mini12306/inventory/` 与 `order/`，测试为 TicketInventoryIntegrationTests、TicketOrderIntegrationTests。支付、取消、库存恢复及订单查询 HTTP 接口留到下一模块。
- 参考阅读基于 2026-10-24 的 `nageoffer/12306` main 原始源码，未固定 SHA（GitHub 提交查询 API 限流）。ticket-service 的 `controller/TicketController` 普通购票入口调用 `TicketServiceImpl.purchaseTicketsV1`，另有独立 V2 入口；V1 先校验责任链、车次级 Redisson 锁，再通过代理调用带本地事务的 executePurchaseTickets，选座、写未支付车票并远程创建订单，账号来自 UserContext。选座器和校验链内部未追踪，不能据此确认原项目乘车人归属校验或跨服务整体原子性。
- 原项目余票查询 V1 使用 Redis 区间/席别余票，缺失时调用 `service/cache/SeatMarginCacheLoader`。其 selectSeatMargin 以车次、席别、AVAILABLE 状态、起终站条件调用 SeatMapper.selectCount；Mapper 继承 BaseMapper，SeatDO 映射 t_seat。另读 SeatServiceImpl.lockSeat，按具体座位及相关区间更新为 LOCKED，但未核验选座器到该方法的完整路径。本版使用 MySQL 数量库存，不引入 Redis、Redisson 或选座。
- 原项目订单链：ticket-service 的 TicketOrderRemoteService 通过 Feign 调用 order-service 的 `/api/order-service/order/ticket/create`，TicketOrderController 委托 OrderServiceImpl.createTicketOrder；其本地事务生成订单号、写主单/订单项/乘车人关联并发送延迟关单消息。OrderStatusEnum.PENDING_PAYMENT 编码为 0，OrderDO 映射 t_order。本版借鉴待支付语义，使用字符串状态，不引入消息或远程调用。
- 库存脚本 `src/main/resources/db/ticket_inventory.sql`：train_trip_id 为有符号 BIGINT 主键并外键关联 train_trips；total_tickets、remaining_tickets 为非空 INT，CHECK 保证总数非负、余票非负且不超过总数。INSERT SELECT 以车次号及发车时间定位四条已有示例车次，各初始化 20 张，不依赖固定自增 ID。用户已执行并提供关联查询及 SHOW CREATE TABLE，助手核对一致。
- 订单脚本 `src/main/resources/db/ticket_orders.sql`：有符号自增 BIGINT id；user_id、train_trip_id、passenger_id 均非空并设外键，无级联删除；passenger_name VARCHAR(50)、passenger_id_card VARCHAR(18) 保存下单快照；status VARCHAR(20) 非空且无默认；created_at/updated_at 由数据库生成，后者随更新维护；联合索引 (user_id, id)。用户已执行，SHOW CREATE TABLE 与设计一致，建表后初始 COUNT 为 0。
- 两份 SQL 为首次初始化用途，不可整份重复执行。新环境需先具备 users、passengers、train_trips，再创建库存和订单表；train_trips 的脚本已在仓库，users/passengers 的建表文件仍未补齐。本模块不重跑旧车次脚本或重置已有库存。
- 公开接口 `GET /api/trains/{trainTripId}/inventory` 返回 200 及 trainTripId、remainingTickets；售罄返回 0，车次不存在或未配置库存返回 404/message，非正数或非数字路径参数返回 400。Controller 使用 @Positive 和 MVC 内建方法校验；响应不暴露总库存。
- 下单接口 `POST /api/orders`：请求只含两个 @NotNull/@Positive 的 Long 字段 trainTripId、passengerId。Controller 通过 CurrentUserSession.requireUser 从服务端会话取得账号，调用注入的 Service；成功返回 201，字段为 id、trainTripId、passengerId、status、createdAt，不暴露 userId 或姓名/证件快照。未登录 401；乘车人不存在或越权统一 404“乘车人不存在”；库存缺失 404；库存不足 409“余票不足”；无效 ID 400。
- TicketOrderService.create 为 public 方法，使用 Spring @Transactional。先调用 PassengerService.findById(userId, passengerId)，其 SQL 同时限定乘车人 ID 和账号 ID；再检查库存记录存在。可售判断由单条 `UPDATE ticket_inventory SET remaining_tickets = remaining_tickets - 1 WHERE train_trip_id = ? AND remaining_tickets > 0` 的影响行数决定，不能依赖此前普通 SELECT 的余票值。
- 条件扣减成功后，从服务端账号及查到的乘车人构造订单，状态固定 OrderStatus.PENDING_PAYMENT。MyBatis 默认枚举处理器按枚举名称存取字符串；INSERT 回填主键，再按 id/userId 查回数据库时间。INSERT 影响行数不为 1 或查回为空均抛运行时异常，未吞异常；扣减和写单处于同一数据库事务。
- 当前不含金额、票价、幂等键或同乘车人同车次去重。重复请求按独立订单处理，每笔成功订单扣一张；订单 ID 直接作为本版标识。后续支付语义与金额另行讨论。
- 库存 4 项测试：匿名正数查询/字段限制/不创建会话、售罄 0、库存缺失与车次不存在、0/负数/非数字 ID。通过 JdbcTemplate 创建 2099 年随机独立车次及库存；仅库存查询测试使用测试层 @Transactional 回滚新增数据，不修改查询种子。
- 订单 8 项测试：正常下单与落库快照/归属/状态/时间/余票减一；最后一张售出后再次下单 409 且无多扣/多单；跨账号乘车人 404（即使请求账号有同证件自己的记录）；未登录 401 且不创建会话；两个 ID 各自 null/0/负数 400；乘车人不存在、库存未配置或车次不存在 404；写入异常回滚；并发不超卖。
- 订单测试通过真实注册、登录、添加乘车人取得会话及归属数据，使用独立随机车次/库存，不加测试层事务。测试账号、乘车人、车次、库存和成功订单会保留，不清理用户记录或示例种子。
- 回滚测试通过 @MockitoSpyBean 包装真实 TicketOrderMapper，仅在该用例拦截 insert：确认真实 Service 事务已开启、同事务内余票从 2 变为 1，再抛模拟 DataIntegrityViolationException；异常离开 Service 且事务结束后，确认余票恢复为 2、订单为 0。此前曾因 Mapper 字段误用 @Autowired 触发 NotAMockException，改为 spy 后通过；模拟在每项测试后默认重置。此用例验证运行时写入异常回滚，不是实际数据库约束报错，也未验证真实 INSERT 成功后的后续异常能回滚订单行。
- 并发测试：8 个线程用 ready/start 两个 CountDownLatch 协调开始，各自调用注入的 Service 代理开启独立事务，争抢 3 张票。通过 Future 收集结果，只允许库存不足异常；设置等待期限并在 finally 关闭线程池。断言恰好 3 成功、5 不足、余票 0、订单 3，且归属和待支付状态正确。此为本地固定并发场景，不代表生产压力测试或所有调度已穷尽。
- 定向验证：用户于 2026-10-24 16:51:53 运行库存测试 4 项通过；订单测试逐步扩展，于 2026-10-25 00:08:14 运行 8 项全部通过。助手只读核对各轮源码、用户输出和对应 Surefire XML，未代跑 Maven。
- 最终全量回归：用户运行 `mvn test` 于 2026-10-25 00:19:56 +08:00 完成，基础 2、注册 4、登录 4、乘车人 5、车次 4、库存 4、订单 8，共 31 项通过，0 失败、0 错误、0 跳过，BUILD SUCCESS。助手核对用户附件输出及七份 Surefire XML 一致，报告时间 00:19:43–00:19:56；本次未代跑测试或操作数据库。
- 主对话于 2026-10-25 只读验收通过：核对本模块全部 Java 实现、两个 SQL、两份测试及复用的会话和乘车人归属调用链，未发现阻断本地学习阶段最小闭环的问题。核实条件扣减与写单同属 public Service 方法上的 Spring 事务；回滚测试观察真实扣减后模拟写入异常，并在事务结束后检查恢复；并发测试仅接受库存不足这一失败原因，核对成功数、余票及已提交订单数。
- 主对话复核七份现有 Surefire XML 为全量 31 项通过，0 失败、0 错误、0 跳过，报告时间为 2026-10-25 00:19:43–00:19:56；最新 src 修改时间为当日 00:07:02，按文件时间未发现报告生成后的源码或测试修改。业务提交的 `git diff --check` 无空白错误。本次未执行构建、测试、SQL 或数据库连接；实际约束报错及其他未验证项仍以下述边界为准。
- 其他边界：普通业务异常沿用 message 格式，框架参数校验响应尚未统一；@Valid 先于 Controller 方法内会话检查，未登录且请求体无效可能先返回 400。未覆盖真实 HTTP cookie、生产部署、真实数据库写入约束错误、并发修改乘车人资料/删除记录、大规模负载或重复请求幂等；既有日志脱敏及 CSRF/cookie 配置边界仍适用。
- 操作记录：除用户单次明确授权助手修改正常下单测试的两段查询结果非空断言外，业务、SQL、测试均由用户手写，构建和测试由用户执行。该单次编辑授权已用完，不扩展到后续编辑或代跑命令。助手维护本文件并已纠正车次 f4ec8c3 的旧提交状态，最终随本模块文档提交。
- Git 记录：用户重新暂存完整内容并检查后，亲自提交 `4a66f0b` — `feat: add ticket inventory query and transactional order creation`，包含完整的 19 个业务/SQL/测试文件、1211 行新增；两个 SQL 均有内容，原空壳暂存问题已解决。最终交接及车次旧状态纠正已按单次授权单独作文档提交。2026-10-25 用户确认业务与文档均已推送，主对话核实两者均包含在本地 main 和 origin/main 中，验收开始前工作区干净；本次未刷新远程引用，也未执行 Git 写操作。

### 订单查询、模拟支付与取消模块（最小闭环验收通过，已推送）

- 2026-10-25 用户开始本模块，目标为当前登录账号的订单列表、详情、模拟支付和取消恢复库存；退款与超时关单不引入。所有操作必须限定会话账号，需验证合法状态转换、重复请求、支付与取消互斥、重复取消只恢复一次库存、取消与恢复的同事务回滚，以及并发支付/取消和既有功能回归。
- 开始时只读核对：main 与本地 origin/main 同指 a1dc49d，包含业务提交 4a66f0b 及库存模块交接文档；仅本文件有未提交修改，为主对话验收记录。本轮未刷新远程引用，未代跑构建、测试、SQL、容器或 Git 写操作；既有未提交记录保留，随本模块最终交接由用户审阅处理。
- 模块开始时已核对本地订单 Controller、Service、Mapper、DTO、实体及状态枚举，库存查询/条件扣减及表脚本、共享会话组件和已有订单测试。当时订单仅有创建入口与 PENDING_PAYMENT 状态，Mapper 已有同时限定 id/user_id 的查询，可用于后续扩展；尚无支付、取消或恢复库存实现。
- 第一组参考阅读基于当日 nageoffer/12306 main 原始源码，未固定 SHA：order-service 的 controller/TicketOrderController 将 queryTicketOrderByOrderSn/pageTicketOrder 委托给 service/impl/OrderServiceImpl；详情按 orderSn 查主单及订单项，已读方法未加账号条件；列表以请求 DTO 的 userId 和状态条件构造 Wrapper，按 orderTime 倒序并查询订单项。dao/mapper/OrderMapper 继承 BaseMapper，具体筛选在 Service 构造；dto/req/TicketOrderPageQueryReqDTO 确有 userId 字段。未核验外围对身份的验证或覆盖，不能据此推断完整鉴权。本版明确从已有 CurrentUserSession 取得账号，查询条件包含 user_id。
- 第二组参考阅读已核验取消链：ticket-service 的 TicketController.cancelTicketOrder 委托 TicketServiceImpl.cancelTicketOrder，通过 Feign TicketOrderRemoteService 调 order-service 的 TicketOrderController.cancelTickOrder，再到 OrderServiceImpl.cancelTickOrder。订单侧先查订单并要求 PENDING_PAYMENT，再取得订单号相关 Redisson 锁，更新主单与订单项为 CLOSED；该方法有本地事务。已读更新 Wrapper 仅按订单号，状态检查在取锁前，不能照搬为本版并发互斥方案。
- 取消成功后的直接恢复分支仅在 ticketAvailabilityCacheUpdateType 不为 binlog 时进入：TicketServiceImpl 远程查询订单详情，调用 SeatServiceImpl.unlock，再回补令牌桶与 Redis 区间/席别余票。unlock 按车次、车厢、座位及相关区间构造 Wrapper，调用继承 BaseMapper 的 SeatMapper 更新状态为 AVAILABLE。binlog 分支实现、令牌桶内部和外围鉴权尚未追踪；不能把订单侧本地事务视为涵盖远程调用及 Redis 恢复的整体事务。GitHub 树 API 当次限流，路径与方法通过 raw 源码核验，未固定 SHA。
- 第三组参考阅读已核验支付结果链：pay-service 的 PayController.pay 调 PayServiceImpl.commonPay 创建待支付支付单；PayServiceImpl.callbackPay 更新支付单，仅 TRADE_SUCCESS 时调用 PayResultCallbackOrderSendProduce 发送事件。order-service 的 mq/consumer/PayResultCallbackOrderConsumer.onMessage 有幂等与本地事务注解，依次调用 OrderServiceImpl.statusReversal（主单和订单项改为 ALREADY_PAID）与 payCallbackOrder（写支付时间、渠道）。后两方法按订单号更新；statusReversal 的锁键与取消方法不同，不能把这些锁推定为支付/取消共享互斥。支付渠道回调入口、验签及完整消息可靠性未核验，不据此宣称跨服务整体原子性。
- 本模块已由用户亲手实现，业务代码位于 order 包及 inventory/mapper/TicketInventoryMapper.java，测试为 TicketOrderIntegrationTests。本轮未修改表结构、SQL 脚本或配置。
- 接口：GET /api/orders 返回当前账号列表，按 id 倒序、暂不分页、无记录返回 []；GET /api/orders/{id} 返回所属订单详情；POST /api/orders/{id}/pay 与 /cancel 均无请求体，成功返回 200 及 OrderResponse。四个入口均从 CurrentUserSession 取得账号；路径 ID 使用 @Positive。未登录 401，不存在或他人订单统一 404“订单不存在”，无效/非数字 ID 400。
- OrderResponse 包含 id、trainTripId、passengerId、下单时的 passengerName 快照、status、createdAt、updatedAt，不含 userId 或证件；乘车人后续改名不改变订单快照。创建响应继续沿用 OrderCreateResponse。
- 状态规则：PENDING_PAYMENT 可转为 PAID 或 CANCELLED；支付不再扣库存，取消恢复一张。同目标重复请求返回 200 且无额外副作用；已支付取消、已取消支付返回 409，消息分别为“当前订单状态不允许取消/支付”。仅模拟系统内状态，不引入金额、支付渠道、退款或超时关单；重复创建仍产生不同订单，未实现下单幂等键。
- TicketOrderMapper.updateStatusIfPending 以 id、user_id 和 PENDING_PAYMENT 为条件更新，支付与取消竞争同一行。Service 的两个 public 方法使用 Spring @Transactional；条件更新后通过 findByIdAndUserIdForUpdate 锁定读取最新状态，区分不存在、重复成功与相反终态冲突，避免普通快照读造成误判。
- 仅取消条件更新成功的请求调用 restoreOneIfNotFull；SQL 在 remaining_tickets < total_tickets 时加一。恢复返回非 1 时抛 IllegalStateException("Cancelled order inventory could not be restored")，使取消状态回滚；订单状态与库存恢复属于同一个数据库事务。重复取消在恢复前返回。
- 新增 13 项测试，加上原创建订单 8 项，共 21 项订单测试。查询 4 项覆盖空列表、倒序、详情、姓名快照、敏感字段隔离、未登录、跨账号（含伪造 userId）、不存在/无效 ID 和读取不改变数据；状态操作 5 项覆盖正常支付/取消、重复请求、相反终态冲突、未登录、三种状态的跨账号操作及不存在/无效 ID。使用同车次两笔订单，使取消恢复后仍有容量，防止库存上限掩盖重复恢复错误。
- 取消回滚 2 项：其一在真实状态更新与真实库存恢复后抛模拟 DataIntegrityViolationException，退出 Service 事务后确认订单全字段及库存恢复；其二在事务内确认已取消，再模拟库存恢复返回 0，验证 IllegalStateException 及订单回滚。两项均在无测试层事务的情况下调用真实 Service 代理，校验 HTTP 查询与数据库一致；不等同于真实数据库约束故障或实际库存缺失/满额场景。
- 回滚测试修正记录：助手最初示例对 MyBatis Mapper spy 使用 invocation.callRealMethod()，于 03:04:32 触发 MockitoException（18 项中 1 失败），未执行预定故障。用户改为注入 SqlSessionTemplate，在 spy 中以 TicketInventoryMapper.class.getName()+".restoreOneIfNotFull" 和 trainTripId 参数执行已注册的同一 Mapper SQL，复用当前 Spring 事务，真实恢复后再抛模拟异常；03:09:48 重跑 18 项全通过。此修正仅涉及测试，未因此修改业务逻辑。
- 并发 2 项：8 线程同时取消同一订单，全部成功且只恢复一张；另以 4 支付/4 取消竞争同一订单，只允许具体 OrderStateConflictException 作为失败，同一目标 4 成功、另一目标 4 冲突。PAID 终态库存 8、恢复 0 次，CANCELLED 终态库存 9、恢复 1 次；均检查另一笔订单全字段不变及订单数量。线程用 ready/start 门闩、独立 Service 代理事务、带期限的 Future 和 finally 关闭线程池；本地固定场景不代表压力测试或穷尽调度。
- 测试通过真实注册、登录、乘车人和下单 HTTP 流程创建随机账号及独立的 2099 年车次/库存；测试层不加 @Transactional，测试记录保留，不自动清理。状态与库存结果同时通过数据库和 HTTP 查询核对。
- 定向验证最终于 2026-10-25 03:19:25 +08:00 完成，订单 21 项全通过。最终全量回归由用户运行 mvn test，于 2026-10-25 03:22:37 +08:00 完成：基础 2、注册 4、登录 4、乘车人 5、车次 4、库存 4、订单 21，共 44 项通过，0 失败、0 错误、0 跳过，BUILD SUCCESS。助手核对附件输出与七份 Surefire XML 一致，报告时间 03:22:19–03:22:37；最新 src 修改为 03:18:43，按文件时间未发现测试后改动。
- 模块只读复核未发现阻断本地学习闭环的问题，范围含 Controller、DTO、异常映射、Service 事务、条件更新/锁定读取、库存恢复和各轮新增测试。助手仅维护本文件，未代改业务/SQL/测试、代跑 Maven 或操作数据库/容器/Git 写命令。
- 2026-10-25 主对话独立只读验收通过：核对全部新增业务实现、共享会话组件、库存和订单 SQL 及新增 13 项测试。查询和状态操作均限定服务端账号；支付与取消以同一订单的待支付条件更新竞争，随后锁定读取最新状态；仅取消成功方恢复库存，恢复非 1 行或异常会回滚。测试实际检查同车次两单留下的容量、重复操作与另一订单不变；并发仅接受状态冲突作为预期失败；回滚用例在真实状态与库存写入后模拟异常，再检查事务外数据。未发现阻断本地学习阶段最小闭环的问题。
- 主对话复核七份现有 Surefire XML：基础 2、注册 4、登录 4、乘车人 5、车次 4、库存 4、订单 21，共 44 项通过，0 失败、0 错误、0 跳过；报告时间 2026-10-25 03:22:19–03:22:37。最新 src 修改时间为 03:18:43，按文件时间未发现报告生成后修改。业务提交的 git diff --check 无输出。本次未重跑构建或测试，未执行 SQL 或连接数据库核验实际表结构；验收结论受下列验证边界限制。
- 验证边界：列表未分页；无真实支付、退款或超时关单；并发只覆盖本地固定场景；故障为模拟运行时异常/零行结果。真实 HTTP Cookie 的后续验证见下节，CSRF/cookie 部署配置、日志脱敏、框架 400 响应未统一等边界继续适用。
- Git 记录：用户重新暂存完整的 10 个业务/测试文件及本文件，原 4 个 AM 新文件已变为完整暂存的 A；git diff --cached --check 无输出。用户亲自提交 625a3bf — feat: add order queries, simulated payment and cancellation，共 11 个文件、1316 行新增、14 行删除，并成功推送 origin main（a1dc49d..625a3bf）。此后用户已提交并推送最终交接文档；主对话验收开始时核对 main 与本地 origin/main 指向相同的后续文档提交，均包含 625a3bf，工作区干净。本次未刷新远程引用或执行 Git 写操作；验收新增修改仅涉及本文件，不维护自身文档提交编号。

### 真实 HTTP 购票闭环验收（通过）

- 2026-10-25 09:07–09:26 +08:00，用户亲自运行 docker compose ps 确认 MySQL 容器运行，再通过 mvn spring-boot:run 启动应用，日志显示 Tomcat 在 8080 端口启动。用户使用另一个 PowerShell 终端发送真实 HTTP 请求；助手依据用户贴出的命令和响应核验，未代发请求或运行应用、容器、测试、SQL。
- 匿名 GET /api/users/me 返回 401/message，未见 Set-Cookie。新账号注册返回 201，登录返回 200；Invoke-WebRequest 的 SessionVariable 保存到客户端会话对象，Cookie 名为 JSESSIONID、Path 为 /、HttpOnly 为 True。携带 WebSession 查询 /me 返回相同账号的 200 响应；文档不保存 Cookie 值或演示密码。
- 用户新增虚构乘车人并查询，保存返回 ID；公开查询 2026-11-01 上海虹桥至杭州东，得到 G101 与 G103。PowerShell 首次自动解码响应时中文乱码，经 Invoke-WebRequest.RawContentStream.ToArray() 取得字节并按 UTF-8 解码后中文正确，本次未据此修改代码或数据库。Invoke-RestMethod 返回 JSON 数组直接赋值再索引；此前外包 @() 在本机造成额外数组层，已调整示例用法。
- 本次 G101（trainTripId=1）实际初始余票为 20。创建订单 199 返回 201/PENDING_PAYMENT，余票降为 19；模拟支付返回 200/PAID，重复支付仍 PAID，余票均为 19。再创建订单 200，余票降为 18；取消返回 200/CANCELLED，恢复为 19；重复取消仍 CANCELLED，余票保持 19。
- 当前账号列表按 200、199 倒序返回 CANCELLED、PAID，订单 200 详情同为 CANCELLED。已支付订单 199 再取消、已取消订单 200 再支付均返回 409；再查询确认两单状态及余票 19 未变。
- 用户退出前将原 Cookie 保存在终端变量中，退出返回 204；随后使用 curl.exe 明确发送该旧 Cookie，GET /api/users/me 与 GET /api/orders 均返回 401/message“请先登录”。验证了服务器会话失效，未记录或展示 Cookie 值。
- 本轮真实 HTTP 最小购票闭环验收通过；未重新运行全量测试，最近自动化结果仍为 44 项通过。本轮不包含真实 HTTP 跨账号攻击、浏览器行为、30 分钟自然超时或新环境初始化验证。演示账号、乘车人、两笔订单保留，G101 余票为 19；不要再假定示例库存仍为 20，也不为还原演示而手动改库。
- 后续待核对：演示响应的无偏移 createdAt/updatedAt 与终端本地时钟存在约 8 小时差异，HTTP Date 头使用 GMT 本身正常；数据库会话时区、DATETIME 存储约定及展示语义尚未核验，不凭当前输出直接改时区或历史时间数据。

### 初始化脚本补齐与独立空库验证（通过）

- 2026-10-25 用户提供本地 users 与 passengers 的 SHOW CREATE TABLE 结果，并亲手创建 src/main/resources/db/users.sql 与 passengers.sql。助手只读核对工作区内容与实际表结构一致：字段、默认时间、用户名唯一约束、账号与证件联合唯一约束、外键、InnoDB 和 utf8mb4_0900_ai_ci 均保留；未复制现有库的 AUTO_INCREMENT 计数，也未包含用户数据。
- 两文件最初为 AM，暂存区为空文件；用户补末尾换行并重新 git add 后，两文件均为 A。助手复核暂存统计为 users.sql 12 行、passengers.sql 17 行，共 29 行，原空壳暂存问题已解决。助手未修改 SQL 或暂存区。
- 2026-10-25 09:40 用户在原 MySQL 容器创建独立 mini_12306_verify 库，确认 DATABASE() 及 SHOW TABLES 为空。随后通过 docker compose cp 将 db 目录放入容器 /tmp/mini-12306-db，用 mysql --default-character-set=utf8mb4 连接验证库，依次 SOURCE users.sql、passengers.sql、train_trips.sql、ticket_inventory.sql、ticket_orders.sql。输出全部成功，车次和库存各新增 4 行，SHOW TABLES 显示五张表；未重建或修改原 mini_12306 库。
- 09:48 用户以 Maven 启动参数临时覆盖 server.port=8081 及数据源 URL 指向 mini_12306_verify，保留原配置文件。curl.exe 查询 http://localhost:8081/api/trains/1/inventory 返回 200 和 remainingTickets=20；日志确认 Hikari 建立真实数据库连接。用户随后正常停止该应用，日志为 Graceful shutdown complete、连接池关闭及 BUILD SUCCESS。该 BUILD SUCCESS 为启动命令正常退出，不是重新执行 44 项测试。
- 本轮验收范围为同一 MySQL 容器内独立空库的脚本初始化和应用公开余票查询；未在新容器/新数据卷验证，也未在验证库重跑全量测试或购票流程。验证库及容器 /tmp 下脚本副本仍保留，不代清理。当前全量测试的连通性断言要求库名 mini_12306，不能直接宣称它已支持任意验证库。
- 用户已亲手编写并调整根目录 README.md。助手只读核对项目范围、环境版本、凭据占位符、Compose 端口/卷、五份 SOURCE 的顺序、应用启动、会话及测试说明，与当前实现和已验证操作一致，未发现阻断使用的问题。用户删去了具体测试日期/数量和验收历史，详细证据继续留在本文件，无需复制回 README。
- README.md 当前为 AM，暂存区仍是空文件，完整正文在工作区；两份 SQL 已完整暂存。提交前由用户将 README.md、AGENTS.md 及两份 SQL 一并重新暂存并核对差异；助手未暂存或提交。

## 当前停留点与交接规则

- 当前阶段：注册（`e8f27d2`）、登录（`5ffa315`）、乘车人（`9423424`）、车次查询（`f4ec8c3`）、余票查询与创建订单（`4a66f0b`）及订单查询/模拟支付/取消恢复（`625a3bf`）均已提交并推送，最小闭环均已由主对话验收通过。最新用户全量回归为 2026-10-25 03:22:37 完成的 44 项通过，主对话已复核现有报告。第一版业务、真实 HTTP 购票演示及独立空库初始化/应用余票查询均验收通过；完整新容器环境尚未验证。
- 2026-10-24 库存模块开始时只读检查：`main` 与本地 `origin/main` 指向相同提交，工作区干净，车次业务提交包含完整的 9 个源码、测试和 SQL 文件；用户确认车次模块已完成 Git 收尾。本轮未刷新远程引用，未重新执行测试。
- 当前停留点：2026-10-25 独立空库验证已通过，8081 验证应用已正常停止，README 已由用户编写且只读核对通过。AGENTS.md 有未提交验收记录，users.sql/passengers.sql 已完整暂存，README 的暂存内容仍为空，四文件均尚未提交或推送。原开发库 G101 余票为 19，验证库对应余票为 20；不混淆两库状态。
- 下一步由用户重新暂存完整的 README、AGENTS 及两份 SQL，核对差异并先拟写规范英文提交说明，再完成提交和推送。暂不重复购票或完整测试；时间语义核对仍待后续小步骤。具体操作继续由用户亲手执行，未经另行授权不代操作数据库、应用或 Git，暂不新增分布式组件或扩大业务范围。
- 此前车次旧状态纠正及库存模块最终交接文档的单次暂存/提交授权已执行完成，本次验收仅只读检查并维护本文件，不包含新的暂存、提交或推送。之前正常下单测试两段非空断言的单次编辑授权也已用完。不主动创建或向其他对话发送任务。
- 助手本次未修改业务、SQL 或测试文件，未执行构建、测试、数据库、容器或 Git 写操作。继续小步教学，后续示例代码添加简短英文注释，已有代码无需补注释；文档不维护自身提交编号，以免循环修改。
- 主对话给模块对话的任务应包含：目标、已知前提、范围、需要讨论的决定，以及完成后的验证要求。
- 模块对话结束时更新本文件：实际完成的功能、关键设计决定、验证命令及结果、已知问题、下一步；有业务提交时记录其编号。未验证或未推送的状态必须明确标注。
- 不复制完整聊天日志；保留继续学习必需的信息。进度改变时替换过期的“当前停留点”，避免累积互相冲突的指令。
