# mini-12306 项目交接

## 使用方式

- 新对话先读取本文件，再检查与当前任务相关的源码及 Git 状态。以实际文件和用户最新指示为准，发现记录过期时及时纠正。
- 当前主对话负责整体规划、模块顺序和进度协调；模块对话负责引导一个边界明确的功能。
- 不假定可以访问其他对话的完整历史。交接所需的决定、验证结果和下一步写在本文件中。
- 本文件纳入 Git 版本管理。暂时只维护这一份交接文件，避免重复记录。
- 本文件包含历史阶段记录。接手时优先读取“学习与操作约定”“跨模型协作与文档维护”“当前停留点与交接规则”及当前模块小节；历史章节中的“当前”“尚未实现”只代表当时情况，不能覆盖较新的模块记录或实际源码。

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

## 跨模型协作与文档维护

2026-10-28 用户同意跨模型协作，并进一步明确沿用原有节奏：开头由主对话与用户确定模块目标、关键规则、方案边界和验收标准；中途由模块 Agent（用户称模型为 `deepseek-v4.1-flash`）完整引导用户完成阅读、局部设计、编码、测试编写、排错和文档维护，不要求逐步返回主对话；结束后回到主对话独立只读验收。用户仍亲手修改代码、配置和迁移，运行命令并完成 Git 操作。本约定不授权助手启动其他对话、发送任务或自动修改业务文件；由用户自行携带开头任务书与最终交接摘要。

- 主对话在开头尽量一次性明确业务契约、数据与历史迁移原则、事务与并发要求、验证标准及排除范围，用户确认后交给模块 Agent 连贯推进。每个新模块在任务书确认前只做只读准备，不进入实现；总体目标或协作方式的确认不等于具体模块业务规则已确认。
- 模块 Agent 负责模块内完整学习流程。命名、方法拆分、具体 SQL、索引、事务代理组织、测试设计和排错等，在已确认契约与方案边界内直接与用户讨论落实、记录依据，无需主对话逐项审批。发现规则缺口或方案需调整时，先向用户说明事实、选项与影响，由用户决定是否补充规则、调整范围或带回主对话；不能悄悄改变已确认契约，也不把每个技术问题默认升级给主对话。
- 模块 Agent 不假定能访问主对话历史，也不根据模型名称推定能力。先读实际文件与 Git 状态；无法读取时明确缺少哪些材料，不编造路径、源码、测试、数据库状态或其他对话的决定。
- 主对话维护全局规则、阶段路线及最终验收结论；模块 Agent 可直接维护当前模块小节的事实记录及 README 中对应功能说明，不代写“主对话验收通过”，不覆盖其他模块或既有未提交记录。README 只将已经实现的行为写为功能事实，未实现方案必须注明待定。
- 规则确认、用户完成一个完整步骤、有新的验证证据、排错后改变结论或准备结束对话时，模块 Agent 更新当前模块记录，不要求用户代写。这些范围内的局部文档维护已有授权，无需再次询问；先完成编辑并复核差异，再报告实际更新。若工具无法写入，明确说明未写入及阻碍，不把计划写成已完成。记录至少包含：当前阶段；已确认规则及确认来源；待定问题；实际修改与既有修改的区分；验证命令、执行者、时间/结果与证据来源；未覆盖边界；实际 Git 状态；下一步。
- 明确区分“建议/待确认”“已确认但未实现”“已实现未验证”“用户已运行且有证据”“主对话已验收”。给出命令不代表执行过，历史报告不证明新增改动已通过；无法归属到当前源码的旧报告要注明局限。
- 关键技术结论附文件位置与适量代码证据，并区分直接读取的事实和推断。POM 未直接声明某依赖不等于类路径中不存在；历史数据库计数不代表当前值或某状态的数量；只加列不改变表清单。锁顺序说明注明业务行锁分析范围，不据简化箭头断言已发生死锁或保证不存在死锁。
- 避免多份重复交接文件和完整聊天日志。以当前模块小节维护最新进度，仅保留继续学习和验收必需的历史；编辑前检查文档的暂存与未暂存差异，做局部更新，不覆盖其他来源的修改。文档维护不包含暂存、提交或推送。
- 模块 Agent 遇到阻碍先在模块对话中继续定位，与用户处理必要澄清，不反复要求重跑相同命令。确实无法推进时，说明“实际现象/相关文件与代码/已核对证据/影响与待决定项”，由用户决定是否中途求助；不自主联系其他 Agent。完成时更新文档并一次性交接实际改动、验证证据、未覆盖边界和 Git 状态；主对话独立检查实际文件，发现问题则交回模块修正后复核，不直接沿用模块自评。

## 项目目标与边界

- 参考项目：<https://github.com/nageoffer/12306>。
- 总目标（2026-10-29 用户明确：“总目标就是实现原仓库”）：通过用户亲手实现和理解，最终复现参考仓库的实际业务能力、设计与技术架构。后续规划以这一终点为依据，不将第一版精简闭环作为最终范围。
- 阶段策略：从零手写精简版、边做边讲是已完成的起步方式；单体、单席别、全程票等属于阶段性边界。后续根据原仓库实际源码和依赖关系逐步扩展，涉及架构与组件的具体模块仍先明确契约和验收标准，再由用户实施。
- 原仓库参考基线（2026-10-29 只读核验）：`daf3a2714c00ff263764d23ab10c2eaed2ae842d`，为本次查询时 main 指向的提交；本轮目录和源码按此 SHA 读取。后续先沿用该基线，切换版本需明确记录差异。不凭历史导览或记忆将某项功能或组件列为已核实的原项目实现；详细差距与建议路线见“原仓库复现基线与路线盘点”。
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
- 乘车人删除已实现、通过全量回归并由主对话只读验收通过，规则见“乘车人删除（第二阶段小模块，主对话只读验收通过）”；身份证仅做格式校验，相同内容修改和并发修改尚未专项验证。
- 乘车人未登录测试使用有效请求体，验证四个入口返回 401；增改接口的 `@Valid` 参数校验先于方法内身份检查，未登录且请求体无效时可能先返回 400。2026-10-25 用户已补写并完整暂存 users.sql 和 passengers.sql，五份脚本在独立空库初始化及应用余票查询验证通过；未使用全新容器或重新运行全量测试。
- 当前未实现 CSRF 防护或部署环境的 cookie 安全配置；浏览器前端接入或部署时继续完善。2026-10-25 已完成 MVC 统一错误响应与参数校验日志脱敏，虚构输入的响应及控制台日志回归通过；不代表所有日志来源或覆盖配置下均已脱敏，具体实现与验证边界见“统一错误响应与参数校验日志脱敏”。

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
- README.md 此前为 AM 且暂存区为空，用户已重新暂存四文件并完成提交 84d1e3c（chore: add database initialization scripts and setup documentation）及推送。2026-10-25 时间问题排查开始时助手只读核对工作区干净、main 与本地 origin/main 同步，最近提交为 84d1e3c；未刷新远程引用。

### 时间语义排查（新连接北京时间验证通过，历史值未迁移）

- 用户于 2026-10-25 10:11 提供只读查询：宿主机时间为 +08:00，MySQL global/session time_zone 均为 SYSTEM，system_time_zone 为 UTC，NOW() 与 UTC_TIMESTAMP() 相同。用户 324 和订单 199/200 的 DATETIME 存储值与此前 HTTP 响应一致，均比对应本地操作时间少 8 小时。
- 排查时助手核对注册 INSERT 不指定时间，由数据库 CURRENT_TIMESTAMP 生成；实体/响应使用 LocalDateTime，无显式 JSON 时区转换。连接 URL 有 serverTimezone=Asia/Shanghai，但未配置强制设置会话时区；原 Compose 未指定数据库默认时区。证据定位到数据库时间生成和无时区值的业务含义，而非响应阶段单独少了 8 小时。
- 用户已在 Compose 增加 command: ["--default-time-zone=+08:00"]，保留命名卷及 JDBC URL；基础测试新增 shouldUseBeijingTimeForDatabaseSession，通过应用实际 JdbcTemplate 连接断言 TIMESTAMPDIFF(MINUTE, UTC_TIMESTAMP(), NOW()) 为 480。采用本精简国内车次项目的北京时间 DATETIME/LocalDateTime 约定；不依靠 JSON 格式化把时间补 8 小时。助手只读核对配置与测试，未代改或代运行。
- 用户运行 mvn test 于 2026-10-25 10:23:09 +08:00 完成：基础 3、注册 4、登录 4、乘车人 5、车次 4、库存 4、订单 21，共 45 项通过，0 失败、0 错误、0 跳过，BUILD SUCCESS。助手核对用户附件及七份 Surefire XML 一致，新时区用例确已执行通过。报告时间 10:22:50–10:23:09，最新 src 修改 10:21:16，配置修改 10:16:47，按文件时间均早于报告。此轮没有另行验证新增记录的 HTTP 时间显示，已有 JDBC 会话与全量回归证据。
- git diff --check 曾发现基础测试类末尾多一个空白行，README 也仍写“时间语义仍待统一核对”。2026-10-25 用户明确单次授权助手修改这两处：已删除多余空行、保留单个结尾换行，并补充北京时间约定及历史值未迁移说明。仅排版/文档修改无需再跑全量测试；后续若改测试逻辑或配置再按影响验证。
- 改默认时区不会转换已存 DATETIME。历史自动生成的 created_at/updated_at 与人工按北京时间录入的 departure_at/arrival_at 不可一起加 8 小时；历史时间处理须单独核实记录来源、范围及转换边界，避免重复转换。当前尚未迁移历史数据。

### 统一错误响应与参数校验日志脱敏（主对话最终验收通过，已推送）

- 用户亲手完成实现、配置和测试。新增 `common/web/ErrorResponse.java` 与 `GlobalExceptionHandler.java`，删除 user、passenger、inventory、order 四个旧异常处理器及其重复响应类型；公共处理器继承 Spring `ResponseEntityExceptionHandler`。本轮未修改业务 Service、Mapper、SQL、事务、库存或订单状态流转。
- 继续使用单字段 `record ErrorResponse(String message)`，未启用 ProblemDetail，成功响应保持原状。现有业务 401、404、409 及车次相同起终点的 400 保留；车次异常现可返回“起点和终点不能相同”。重复用户名响应固定为“用户名已存在”，不再回显输入用户名；原异常类仍保留原有消息，处理器不把它用于响应或普通业务日志。
- 请求体及车次查询 DTO 的 `MethodArgumentNotValidException` 按字段名、规则类型排序，仅返回第一处错误。使用服务端字段名和固定中文提示，不读取 rejected value 或默认错误消息；内部 `passwordWithinBcryptLimit` 映射为公开字段 password，保留 BCrypt UTF-8 字节上限校验。查询日期绑定失败返回安全的格式提示。
- 重写 `handleExceptionInternal`，将框架错误正文转换为公共响应，保留框架决定的状态码和必要响应头。路径正数校验、类型转换、JSON 解析及缺少必需请求体按框架分类返回 400；未知路径 404、方法不允许 405、其他框架状态及未预期应用错误 500 不混改为统一状态。405 保留 Allow。响应已提交时不替换正文；返回值校验等服务端错误保留 5xx，未预期 500 对外仅返回“服务器内部错误”。
- 日志核对纠正：此前校验失败包含 rejected value 的 WARN 实际来自 `DefaultHandlerExceptionResolver`，并非笼统所称 DispatcherServlet。公共处理器接管预期错误；application.yaml 设置 `org.springframework.web: INFO`，抑制 MVC DEBUG/TRACE 对输入或完整异常的打印，设置 `org.springframework.web.servlet.PageNotFound: ERROR`，抑制未知路径的原始 URI WARN；405 重写入口绕过父类直接打印异常消息的 WARN。5xx 仅记录异常类型，不打印消息、cause 链或堆栈，排查信息有限。本配置不承诺覆盖其他日志来源、用户另行调高日志级别或外部日志采集。
- 新增 `GlobalExceptionHandlerIntegrationTests` 六项测试，涵盖注册短密码、乘车人非法证件/手机号、路径非正数与非数字转换、截断 JSON、登录缺失密码、下单缺失 ID、车次日期绑定失败、未知路径 404、405/Allow、测试专用端点模拟 500。检查 JSON/message 和 HTTP 状态；用明确虚构标记验证密码、证件号、手机号、路径、JSON、日期及异常消息不进入相应响应或捕获日志。测试端点和配置仅位于 src/test。增强原注册重复用户名及车次相同起终点的消息断言。
- 用户运行 `mvn "-Dtest=GlobalExceptionHandlerIntegrationTests,UserRegistrationIntegrationTests,TrainTripIntegrationTests" test` 于 2026-10-25 16:37:06 +08:00 完成，新增错误测试最初五项、注册四项、车次四项，共 13 项通过。最初只写测试骨架而无 @Test 时仅执行八项，不能视作新场景已验证。随后补足第六项及路径校验、日志断言；18:20:26 的全量运行因测试地址 api/orders 缺少开头斜杠报一项错误，修正测试地址后回归通过，未因此修改业务实现。
- 最终由用户运行 `mvn test`，于 **2026-10-25 18:22:43 +08:00** 完成：基础 3、注册 4、登录 4、乘车人 5、车次 4、库存 4、订单 21、错误响应 6，共 **51 项通过，0 失败、0 错误、0 跳过，BUILD SUCCESS**。模块对话核对用户输出与八份 Surefire XML 一致，报告时间为 18:22:19–18:22:42；最新 src 修改为 18:21:44，早于报告。现有报告的 system-out/system-err 未发现所用虚构敏感标记或 rejected value；模拟 500 仅见异常类型日志。模块对话未代跑构建、测试、数据库或容器操作。
- 模块只读核对未发现阻断本次本地学习范围的问题。本轮未重做真实 HTTP 购票演示。415/406、缺少整个请求体、JSON 字段类型不匹配、返回值校验、无静态资源映射时的 NoHandlerFound 分支、响应已提交、异步或容器在 MVC 前拒绝请求等未专项验证；当前未知路径测试覆盖当前默认 MVC 配置。原车次测试已覆盖缺失起点返回 400，尚未专项断言其统一正文。不能把当前 MockMvc 及日志证据扩大为所有 API/容器故障或全部日志的统一与脱敏保证。未登录且输入无效时仍可先返回 400；乘车人路径 ID 未新增 @Positive。
- Git 收尾已完成：用户重新暂存完整实现、配置、测试及两份文档，三个新文件由 AM 变为完整暂存的 A；助手只读核对公共处理器 231 行、新测试 214 行，暂存区与工作区一致，`git diff --cached --check` 无输出。用户亲自提交 `44d2816` — `feat: unify API error responses and prevent sensitive input disclosure`，共 12 个文件、485 行新增、138 行删除，并成功推送 origin main（9c003b5..44d2816）。助手核对本地 main 与 origin/main 均为 44d2816、工作区干净；未刷新远程引用或执行 Git 写操作。随后仅维护本文件的最终提交记录，该文档更新尚未提交。
- 用户最新明确授权模块对话“一起维护”文档，助手据此更新 README 的日志边界及本节交接，保留主对话既有记录，并替换过期当前停留点；文档已随 44d2816 提交推送。本次另更新本文件的最终 Git 状态，无须为文档修改重跑测试；本文件后续交接修改的提交推送仍由用户操作。
- 主对话于 2026-10-25 进行最终只读验收：核对 44d2816 的 12 个文件变更、当前源码、README、Git 状态及八份 Surefire XML。公共处理器是当前唯一的 MVC Advice，四个旧处理器已删除；业务异常按具体类型映射为既有的 400/401/404/409，重复用户名不回显输入，车次相同起终点有受控 400 正文。框架错误经 ResponseEntityExceptionHandler 的统一出口返回 JSON message，405 保留 Allow，未预期异常只记录类型并返回受控 500。新六项测试覆盖校验、转换、截断 JSON、404、405/Allow、500 及虚构敏感标记不出现在响应或捕获日志；复核报告为 51 项通过，0 失败、0 错误、0 跳过，时间 18:22:19–18:22:42。主对话未重新执行构建或测试，未发现阻断本模块验收的问题。

## 当前停留点与交接规则

- 当前工作（2026-10-30 23:05）：用户明确总目标为实现原仓库，第三阶段按“车站与车次经停站 → 区间车次查询 → 席别与区间票价”推进。第一模块“车站与车次经停站”已实现并由主对话独立只读验收（`779672f`）；第二模块“区间车次查询”已由用户亲手实现，用户运行全量 97 项回归通过（22:50:50），业务提交 `f0a3b77` 与文档提交 `d950898` 已推送，主对话已完成实现层独立核对，仅要求修正文档状态矛盾后收尾。开发库结构与 V1/V3/V4/V5 定义核对一致，Flyway 已登记 baseline 5；历史待支付单 311 笔已由正常调度关闭；用户已在开发库准备 G35 北京南至宁波示例运行及五站经停数据。下一步是第三阶段第三模块“席别与区间票价”，先确认契约再实施。此前“运行环境、安全与可运维性完善”及其五模块排序未确认为第三阶段路线。
- 已确认规则：总体终点改为复现原仓库；继续由用户亲手修改与运行命令，主对话规划、只读核验并维护交接文档。目标确认不授权助手代改业务、配置、测试或数据库，不授权构建、测试、数据库、容器和 Git 写操作，也不授权创建其他对话或子代理。
- 历史阶段证据（2026-10-29）：主对话读取实际文档、配置、V3/V4、订单和乘车人相关源码，并读取现有九份 Surefire XML，合计 77 项通过、0 失败、0 错误、0 跳过；报告时间为 2026-10-28 22:01:56–22:02:44，最新 src 修改为 21:58:12。这是既有运行证据复核，未重新执行测试，不构成第三阶段新增功能验收。
- 历史阶段 Git 状态（2026-10-29）：本地 main 与 origin/main 同指 `7faaf3b`，未刷新远程引用；暂存区为空，既有未暂存修改为 `.gitignore` 与本文件中的前期验收记录。
- 当前 Git 与交接（2026-10-30 23:05）：本轮只读核对本地 main 与 origin/main 同指 `d950898`；未刷新远程引用。第二模块业务与文档提交均已推送，暂存区为空，工作区仅余既有 `.gitignore` 修改及本文件在 `d950898` 之后补充的提交号记录（未提交）。业务、配置、测试、数据库和 Git 写操作仍由用户负责；主对话已获授权维护 AGENTS.md/README.md，不代用户提交或推送。开发库迁移影响评估与示例数据准备已完成，下一步是第三阶段第三模块的契约确认。
- 当前阶段：第一版业务模块与第二阶段五个模块及乘车人删除小模块均已完成本地实现、验证和主对话验收。第五模块业务提交为 `372a3b1`；乘车人删除业务提交为 `e054706`，文档收尾为 `7faaf3b`。第三模块代码与文档为 `0bf3faf`、`e427939`；第四模块为 `b3341bf`，文档收尾 `b03befd`。测试仍使用受限账号 `mini12306_test` 和独立库 `mini_12306_test`。开发库最新状态见本节“开发库 V5 baseline 与历史关单影响评估”。
- 2026-10-24 库存模块开始时只读检查：`main` 与本地 `origin/main` 指向相同提交，工作区干净，车次业务提交包含完整的 9 个源码、测试和 SQL 文件；用户确认车次模块已完成 Git 收尾。本轮未刷新远程引用，未重新执行测试。
- 历史环境记录（第四模块时期）：第四模块本地验收通过，用户运行 `mvn test` 于 2026-10-28 11:39:06 完成，60 项全部通过；业务契约、并发、失败回滚、迁移和边界见下节。业务提交 `b3341bf` 已推送，文档收尾提交为 `b03befd`。业务、配置、测试、数据库与 Git 写操作仍由用户负责。开发库的备份位于用户目录 `C:\Users\34248\mini12306-backups\20261027_132508\mini_12306_before_flyway.sql`，大小 234709 字节且有 mysqldump 完成标记；尚未演练恢复。开发库 baseline 操作前计数为 users=574、passengers=505、trips=377、inventory=352、orders=396、remaining tickets 总和=706；操作后用户查询确认相同，`flyway_schema_history` 只有 version=1、description=`<< Flyway Baseline >>`、type=BASELINE、checksum 空、success=1。用户运行 `mvn spring-boot:run` 的日志确认连到 `mini_12306`、Flyway 校验成功、当前版本 1、无待执行迁移，Tomcat 8080 启动成功；MySQL 8.4 高于 Flyway 已验证版本 8.1 的警告仍存在。用户是否已按 Ctrl+C 停止该进程未确认。
- 第二阶段路线已于 2026-10-25 经用户确认，按五个边界明确的模块推进：第一，统一错误响应与参数校验日志脱敏；第二，订单列表分页与索引分析；第三，独立测试库与数据库版本迁移（Flyway）；第四，下单幂等；第五，支付超时关单。小件穿插：乘车人删除（2026-10-28 已完成实现与全量回归）、历史时间迁移评估。浏览器前端与部署作为后续可选方向，接入时同步处理 CSRF、Cookie 和环境配置；继续保持单体，按具体问题决定是否增加组件。
- 第一、二模块完成情况分别见对应章节。第三模块决定与验证记录见下节。README 与本文件由助手根据实际配置和验证结果维护；当前 README 已更新 Flyway、测试数据库准备、迁移和测试数据保留说明。第三模块代码与文档提交分开；后续模块对话开始前须检查暂存区、工作区和完整差异，保留既有提交，不做 Git 写操作。

### 原仓库复现基线与路线盘点（2026-10-29，只读完成，第三阶段顺序已确认）

- 已确认目标及来源：用户先明确“总目标就是实现原仓库”，随后同意先核验原仓库、建立差距清单和阶段顺序。精简版是起点；原项目实际具有的业务、前端、公共组件与架构均纳入长期对照，不能把单体或暂不使用中间件当成永久排除项。用户仍亲手实现，助手不代跑命令或修改业务文件。
- 固定参考：[nageoffer/12306 @ daf3a2714c00ff263764d23ab10c2eaed2ae842d](https://github.com/nageoffer/12306/tree/daf3a2714c00ff263764d23ab10c2eaed2ae842d)。GitHub commits API 返回提交时间 `2026-03-12T14:45:22Z`、主题 `docs(readme): 优化 Ragent AI 链接文本显示`；本轮目录 API 返回 `truncated=false`。本次是学习与盘点基线，不表示已运行或验收原项目。
- 核验方式：助手只读联网获取固定 SHA 的 README、根与相关子 POM、目录、聚合库建表脚本及定向源码；随后核对本地 TrainTrip、TrainTripMapper、OrderCreateRequest、TicketOrder、TicketInventoryMapper 和 POM。只读网络访问已获工具授权；未克隆仓库、安装依赖、编译或启动原项目。部分 raw 文件读取超时，改经 GitHub contents API 读取；末次补查目录遭 API 限流，未读取的实现不作完成结论。

原仓库已核验事实与本地差距（路径均相对于各自仓库；接口或依赖存在不等于完整业务已验证）：

| 方向 | 原仓库证据 | 当前 mini-12306 与剩余差距 |
| --- | --- | --- |
| 站点与区间查询 | `RegionStationController`、`TrainStationController`、`TrainStationServiceImpl`；聚合 SQL 有 `t_region/t_station/t_train_station/t_train_station_relation`；`TicketServiceImpl.pageListTicketQueryV1` 查询站点关系 | 本地 `train_trips` 只有固定起终点与发到时间，查询 Mapper 按这两个字段过滤；尚无独立站点、经停顺序或中途站查询 |
| 席别与区间票价 | `resources/db/12306-springboot.sql` 的 `t_train_station_price` 含 train_id、departure、arrival、seat_type、price；TicketServiceImpl 读取票价 | 本地只有一种席别，订单和查询没有票价或金额；金额单位、服务端定价和历史订单如何过渡待模块确认 |
| 车厢、座位与区间库存 | 聚合 SQL 的 `t_carriage/t_seat/t_ticket`；`SeatServiceImpl`、`TrainSeatTypeSelector` 调用及 `StationCalculateUtil.throughStation/takeoutStation` | 本地按车次减数量，不分座、不记录乘车区间；尚未实现同座位的区间占用、复用或选座。选座器的完整策略分支仍需下一轮细读 |
| 多乘车人与订单明细 | `PurchaseTicketReqDTO.passengers/chooseSeats/departure/arrival`；`TicketOrderCreateReqDTO.ticketOrderItems`；`TicketOrderItemCreateReqDTO` 含席别、座位和 amount；OrderServiceImpl 写主单、明细与乘车人关联 | 本地 OrderCreateRequest 只收一位 passengerId，TicketOrder 是单票订单；需主子订单、整单原子性、金额、区间与座位快照及状态规则 |
| 用户体系与身份 | `UserInfoController` 有资料修改/注销等入口；`UserLoginServiceImpl` 含用户名/手机/邮箱登录、JWT、缓存登录资料、注册锁与 BloomFilter；网关 TokenValidateGatewayFilterFactory 解析 Authorization | 本地是用户名+BCrypt 与 HttpSession，具备账号隔离；多身份登录、资料与注销、跨服务身份传播尚未实现。网关源码存在注销后 token 有效性 TODO，不能将其视为已解决；保留本地密码与隐私保护要求 |
| 支付与退款 | `PayServiceImpl.commonPay/callbackPay` 写支付单并发送支付成功消息；退款 Controller、RefundServiceImpl 及退款消息文件存在 | 本地仅模拟订单状态，暂无支付单、渠道回调或退款。原 PayServiceImpl.commonRefund 返回 null，RefundServiceImpl 也保留 TODO；需逐链确认实际能力，不据接口名称宣称退款端到端完成；渠道开发与外部账号授权后续讨论 |
| 缓存与消息 | TicketServiceImpl 的 Redis/Redisson、缓存加载、令牌桶调用；OrderServiceImpl 创建订单时发送 RocketMQ 延迟关单消息；支付结果及关单消费者在目录中存在 | 本地 MySQL 幂等和条件更新、定时关单已实现，但尚无 Redis、消息和跨系统一致性；后续需要缓存一致性、重试/消费幂等及故障验证，不直接照搬本地事务结论 |
| 模块、网关与分片 | 根 POM 聚合 dependencies/frameworks/services/tests；services 含 aggregation/gateway/user/ticket/order/pay；aggregation POM 引入四业务模块；分片算法和配置文件存在，聚合 SQL 有多张分表 | 本地单 Maven 模块、单进程、单库；需公共组件、业务模块组织、网关、服务通信及分片。两种启动方式由原 README 明示，聚合不等于没有中间件；分片路由规则和完整运行配置本轮未细读 |
| 前端与运行支撑 | `console-vue/package.json` 声明 Vue 3、Ant Design Vue、Axios；services POM 声明 Actuator/Prometheus；ticket POM 声明 Nacos/OpenFeign/Sentinel/XXL-JOB/Hippo4j 等 | 本地暂无前端和这些支撑。前端页面完成度、监控配置和上述依赖在具体链路中的生效方式仍待核验，不能由依赖声明推断已运行 |

阶段依赖顺序（第三阶段的三个模块顺序已获用户同意，后续阶段仍为规划建议；具体模块须确认任务书）：

1. 第三阶段先扩展查询业务基础：车站与经停站 → 区间车次查询 → 席别与区间票价。原理是先有站点顺序与合法区间，才能定义区间票价、座位占用和多乘车人购买。此阶段不提前宣称中途站已经可以买票。
2. 随后扩展完整购票模型：车厢和具体座位 → 区间占用/复用 → 多乘车人主子订单 → 选座策略。分小模块保留可验证闭环，支付、取消、超时、幂等及旧订单适配必须随模型改变回归；不能让查询支持区间后仍按全程数量库存出售区间票。
3. 逐步建立原项目的公共组件与用户体系：Maven 业务模块、通用响应/分页/异常、责任链/策略、持久层与 ID、Redis 缓存、JWT 与身份上下文、多身份登录及资料/注销。公共能力在出现真实调用方时抽取；网关接入时验证身份可信边界，不将客户端 userId 作为购买归属。
4. 完善支付单、渠道适配、支付结果回调、退款与前端；再推进消息驱动的支付同步/超时关单和消费幂等、重试与补偿。前端可在所需 API 稳定后穿插接入；浏览器安全同时处理。按源码实际链路识别原项目 TODO 和需补齐的部分。
5. 完成目标架构与运行方式：聚合服务组织 → 网关和微服务通信/治理 → 分库分表 → 购票高并发优化及缓存/令牌桶方案 → 原仓库功能覆盖复核、性能和故障演练。缓存、消息、公共组件可为后续模块提前准备，具体先后按完整链路契约调整；完整复现时两种启动方式和相关组件都应核验，不把依赖齐全当作完成。

第三阶段三个模块的学习目标与验收轮廓（顺序已同意，具体契约按模块确认）：

- 车站与车次经停站：理解独立站点、车次运行实例、站序和到发时刻；实现站点/经停查询。验收真实数据持久化、稳定按站序返回、跨日时刻与合法性、非法/不存在参数、既有固定起终点流程回归。编号、唯一约束、数据来源及旧车次回填方式需任务书确认。
- 区间车次查询：依据同车次的站序判断起终站与方向，返回对应上下车时刻。验收正常中途站、反向/同站/不经停、日期与跨日边界、排序和空结果；按乘车站日期还是始发日期筛选需明确确认。本模块不包含区间下单或复用当前全程余票作为区间余票。
- 席别与区间票价：理解车次+区间+席别的价格维度及服务端定价。验收多席别和多区间的正确价格、缺失票价处理、金额单位与精度边界及既有接口兼容；是否同轮进入订单金额快照需任务书决定，不能由客户端提交金额决定成交价。

- 运行准备：开发库 V3/V4 执行、历史时间与订单影响、备份恢复仍是待验证事项。默认启动会自动迁移且启用调度，先完成只读评估，备份、恢复、迁移或数据处理需用户明确授权并由用户执行；这项准备不代替业务复现主线。源码补充：V4 只回填期限，实际 EXPIRED 与库存恢复由调度条件更新完成；每轮最多 100 单，是否到期取数据库 NOW()，不能将所有历史订单总数当作首次关闭数量。关单 WARN 传入异常对象，README“日志只记录订单 ID”的描述不足以证明异常链脱敏，后续应按源码与新证据修正。
- 技术版本待决定：固定原仓库 BOM 是 Java 17、Spring Boot 3.0.7、Spring Cloud 2022.0.3；本地是 Java 17、Boot 4.0.8。是否调整本地基线、如何适配原项目中间件和持久层依赖尚未确认，不自动降级，也不默认原依赖兼容 Boot 4。
- 状态区分：用户已确认总目标、只读盘点、第三阶段三个模块顺序及首模块任务书；参考版本及上述定向源码已由主对话读取；后续模块具体业务规则和后续阶段顺序仍需展开；没有新增业务实现、用户执行验证或第三阶段模块验收。原仓库本身未构建、启动或测试，完整配置、选座各策略、前端各页面和全部 TODO 尚未穷尽。
- 实际修改与 Git：本轮仅助手局部维护 AGENTS.md，保留该文件的既有验收与目标记录及 `.gitignore` 修改；README、业务、配置和测试未改。开始时本地 main 与 origin/main 同指 `7faaf3b`、暂存区为空；未刷新远程引用，未暂存、提交或推送。
- 下一步：首个业务模块“车站与车次经停站”的任务书已确认，由用户自行携带任务书交模块对话；继续采用少量定向源码阅读，不要求用户一次读完整仓库，主对话结束时独立只读验收。

### 车站与车次经停站（第三阶段第一模块，已实现并通过本地全量回归，待主对话验收）

- 当前阶段及确认来源：2026-10-29 主对话只读核对本地 TrainTrip、Controller、Service、V1 车次表、测试 V2、基础结构断言和测试基类，并补查原接口/DTO后给出完整任务书；用户在主对话总结接口、时间、旧数据及成功正文阶段差异并询问是否推进后，明确回复“可以”。以下任务书契约已确认；“业务、迁移及测试尚未实现”是 2026-10-29 任务书确认时的历史状态，实施结果见本节 2026-10-30 的实现状态与验证记录。模块 Agent 可在边界内引导用户连贯实施，无需把命名、方法拆分或每条 SQL 带回主对话审批；用户仍亲手修改并运行命令。
- 目标：建立可复用车站资料和车次的有序经停时刻，为下模块区间查询提供可信数据；用户理解独立站点、运行实例、站序与到发时间的关系，完成数据库 → Mapper → Service → Controller → 集成测试的查询闭环。
- 原项目定向阅读（固定 `daf3a2714c00ff263764d23ab10c2eaed2ae842d`）：先看 `resources/db/12306-springboot.sql` 的 t_station/t_train/t_train_station，再沿 `services/ticket-service/src/main/java/org/opengoofy/index12306/biz/ticketservice/` 下的 `controller/TrainStationController.java` → `service/impl/TrainStationServiceImpl.java` → `dao/mapper/TrainStationMapper.java`/实体与 SQL 对照；原服务的 listTrainStationQuery 已读，Mapper 内部需模块对话继续核验。RegionStationController 只用于说明站点字典的调用方，不要求一次实现地区检索；本地模型不声称与原表逐字段相同。
- 运行实例与旧数据：沿用 train_trips.id 表示本项目一次带日期的车次运行，不在本模块重建列车模板/开行日历。保留现有车次和订单 ID、固定起终点字段及查询/购票契约；不删除、重编号或重置既有业务数据，不凭两个端点推测历史中间站。已有车次没有配置经停表时，其经停查询返回 200/[]，由后续受控数据准备显式补齐。
- 已确认数据模型：新增车站字典（id、唯一站点 code、name，以及原站点响应需要的 spell/regionName）和经停表（车次实例引用、车站引用、正整数站序、到站/发车 LocalDateTime）。同车次站序不能重复；一个车次是否允许重复经过同一站需对照原始路线语义确认，当前原源码证据不足，不预设唯一约束。字典站名不作为全局唯一标识。具体表/列名、长度、主键、外键、CHECK 与索引由模块 Agent 与用户落实，不允许通过 Java 排序代替 SQL 明确排序；独立城市字典、拼音检索和维护页面留到后续。
- 已确认经停时间：完整时刻表至少有始发站和终到站；始发到站时间可空、发车时间必填，终到发车时间可空、到站时间必填，中途站两者均有且到站不晚于发车。按站序的相邻站须先发后到，允许跨日，用完整北京时间 DATETIME/LocalDateTime；第一站/最后站的站名及有效到发时刻分别与 train_trips 的起终点和到发时刻一致。跨行完整性由数据准备/导入校验保证，不声称单个 CHECK 可以保证全部规则。残缺记录不应伪装为完整可售线路。
- 原 API/DTO 补查事实（2026-10-29，同一 SHA）：RegionStationController 提供 `GET /api/ticket-service/station/all`，StationQueryRespDTO 为 name/code/spell/regionName；TrainStationController 提供 `GET /api/ticket-service/train-station/query?trainId=...`，TrainStationQueryRespDTO 为 sequence/departure/arrivalTime/departureTime/stopoverTime，sequence 在原项目为 String；原成功正文使用 Result 包装。地区/站点搜索另有 `GET /api/ticket-service/region-station/query`，请求含 queryType/name，本模块暂不纳入。
- 已确认HTTP契约：沿用上述原仓库 station/all 与 train-station/query 两条路径，trainId 在本阶段对应本地 train_trips.id。站点元素沿原 name/code/spell/regionName，固定 code 升序；经停元素沿原 sequence/departure/arrivalTime/departureTime/stopoverTime，按数据库数值站序升序，输出 sequence 字符串，departure 为站名；中途站停留分钟由到发时刻得到，首末站 stopoverTime 为空。成功正文暂沿本地现有数组风格，原 Result 成功包装留到公共响应对齐时处理，明确这不是原 API 的完全兼容实现。合法不存在车次返回404/message“车次不存在”；已存在但未配置经停返回200/[]；缺失、非正数或不能转换为Long的 trainId 返回受控400/message。查询不要求登录，不创建会话，不写业务数据，沿用统一错误响应。
- 数据准备与迁移：只新增版本迁移，不修改已执行的 V1、测试 V2、V3/V4；当前下一个正式版本可用 V5，写入前重查两迁移目录避免版本冲突。主迁移仅创建本模块结构，不批量回填旧车次或编造站点 code/线路。测试使用显式的独立车次/站点/经停夹具，不重放 V2、不改原四条参考车次或库存；至少一组含中途站及跨日。无需为每次测试新增固定种子迁移；演示数据来源和开发库导入作为后续明确步骤。
- 验收要求：用户亲自运行本模块定向测试及全量 mvn test；助手只读核对源码、用户输出和归属于当前源码的报告。测试覆盖字典字段与顺序、经停字段与真正乱序插入后仍按数值站序返回（包括2/10等防止字符串误排）、跨日时刻、未知车次404、旧车次空数组、缺失/非法ID400、匿名访问不创建会话。迁移/结构测试验证新增表、关键唯一约束和外键，以及 Flyway 历史；任务书确认时，基础测试精确断言版本1/2/3/4与七张表，新增结构必须补齐这些断言，不能删断言或只改成 contains 来掩盖结构差异。数据库重复站序、重复站点code、错误引用等约束应有具体失败结果证据。新测试数量不预定，既有77项不能被跳过或削弱。
- 排除范围：本模块不开放车站或经停的写入管理接口，不改区间搜索、不出售中途站票、不做区间库存、席别票价、多乘车人订单、选座、Redis或服务拆分；不升级/降级 Boot。数据准备需要遵守上述完整性规则，公开查询之外不顺带扩展管理端。
- 运行准备边界：可以优先在已有受保护的 test profile 下验证新增迁移和测试，不把开发库先迁移作为一切源码学习的前置条件。使用默认配置启动应用前，先完成开发库 V3/V4/新增迁移与关单首次扫描的只读影响评估；备份恢复、开发库迁移、数据补齐或改写必须由用户明确同意并亲自执行，不以启动应用的方式绕过确认。
- 授权和交接：用户亲手写代码、配置、迁移、夹具与测试，并执行命令和 Git 操作；模块 Agent 在任务书确认后引导完整模块，可维护本模块记录与 README 中已实现且有证据的功能，不代写主对话验收结论。发现必须改变上述契约或旧数据原则时先与用户讨论；结束时交接实际变更、验证证据、未覆盖项与Git状态，主对话独立只读验收。
- 开发库 V5 baseline 与历史关单影响（2026-10-30，用户执行数据库操作，主对话核对源码和用户输出）：用户提供的 `SHOW CREATE TABLE` 与正式 V1/V3/V4/V5 对应结构一致；V3 幂等表、V5 站点/经停表行数均为 0。期限差异查询结果为 0；登记前 311 笔待支付单的 `pay_deadline` 均已过期，逐车次库存恢复容量检查均为 `CAPACITY_OK`。用户新备份 `C:\Users\34248\mini12306-backups\mini_12306_before_v5_baseline_20261030.sql`，长度 247115 字节，时间 2026-10-30 16:10:54；未做恢复演练。
- Flyway 历史核对时，用户先前查询曾得到版本 1 BASELINE，之后在 `mini_12306` 查不到历史表且 `DROP TABLE` 返回未知表错误；用户确认没有执行 DROP，原因未查明。用户随后以临时参数 `baseline-on-migrate=true`、`baseline-version=5`、`order.timeout-close.enabled=false` 启动应用；数据库历史显示仅一条版本 5、类型 BASELINE、success=1。该次 baseline 没有执行 V3/V4/V5 SQL。用户移除临时参数后正常启动应用，先观察到 100 单过期，随后 311 笔全部转为 `EXPIRED`；最终状态为 CANCELLED=45、PAID=40、EXPIRED=311，总订单数 396。此证据验证了本次单实例调度处理，不覆盖停机补偿、多实例并发或长期运行；普通应用当前是否仍在运行未确认。
- 实现状态（2026-10-30，用户亲手完成，模块 Agent 只读核对）：新增正式迁移 `src/main/resources/db/migration/V5__create_station_and_train_trip_stop.sql`；新增 `station/` 包 8 个文件：`Station`、`TrainTripStopRow`（JOIN 投影）、`mapper/StationQueryMapper`、`dto/StationResponse`、`dto/TrainStopResponse`、`StationQueryService`、`controller/StationController`、`TrainTripNotFoundException`；`TrainTripMapper` 新增 `countById`；`GlobalExceptionHandler` 的 404 列表加入 `TrainTripNotFoundException`（模块决定把异常放 station 包，Service 同包免 import）；`Mini12306ApplicationTests` 版本断言改为 1–5、表清单补两张，并新增站点 code 唯一键、经停 (trip, sequence) 唯一键、经停两个外键三组断言。
- 结构与约束：`stations(id, code, name, spell, region_name)`，`code` 唯一、站名不唯一；`train_trip_stops(id, train_trip_id, station_id, stop_sequence, arrival_at, departure_at)`，`(train_trip_id, stop_sequence)` 唯一，`stop_sequence` 用 INT（不用原项目的 varchar，避免 10 排在 2 前），外键到 `train_trips` 与 `stations`，CHECK 要求站序为正、至少一个时刻、到站不晚于发车；未对 `(train_trip_id, station_id)` 设唯一键，重复经过同一站仍开放。
- 接口与服务行为：`GET /api/ticket-service/station/all` 按 `code` 升序返回 `name/code/spell/regionName`，不返回 id；`GET /api/ticket-service/train-station/query?trainId=` 以 `train_trips.id` 为 trainId，SQL 按数值站序 `ORDER BY`，返回 `sequence/departure/arrivalTime/departureTime/stopoverTime`，`sequence` 输出字符串、`departure` 为站名、首末站 `stopoverTime` 为 null；时刻返回完整 LocalDateTime（ISO-8601），与原项目 `HH:mm` 有意不同，否则跨日不可验证。Service 先用 `countById` 判 404“车次不存在”，存在但未配置经停返回 200/[]；trainId 缺失、非正数、非数字分别由框架绑定与 `@Positive` 拦截为受控 400；两个接口不要求登录、不创建会话、不写数据。
- 验证记录（用户执行）：`mvn "-Dtest=StationQueryIntegrationTests" test` 于 2026-10-30 14:46:47 完成，11 项通过；`mvn test` 于 14:48:13 完成，**88 项通过，0 失败、0 错误、0 跳过，BUILD SUCCESS**（基础 5、错误响应 6、库存 4、订单 38、乘车人删除 7、乘车人 5、车站经停 11、车次 4、登录 4、注册 4）。日志确认 test profile、独立库 `mini_12306_test`、Flyway 校验 5 个迁移、当前版本 5 无待执行。模块 Agent 核对 10 份 Surefire XML 合计 88 项、报告时间 14:47:32–14:48:13，最新 src 修改为 14:45:59，早于报告生成；未代跑构建或测试。
- 排错记录：首次定向运行 11 项中 `shouldRejectNonPositiveStopSequence` 失败，期望 `DataIntegrityViolationException`，实际 `UncategorizedSQLException`。原因是 MySQL 的 CHECK 违规报 SQLSTATE HY000、错误码 3819，Spring 不能归类为完整性违规；改为断言父类 `DataAccessException` 并校验消息含 `chk_train_trip_stops_sequence` 后通过。结论：用 `DataIntegrityViolationException` 兜底不能覆盖 CHECK 违规，当前没有写入经停的业务路径，无实际影响；后续若有写入接口需另行处理。
- 用户提供的开发库数据准备结果（2026-10-30）：车次实例 `id=408`，G35，北京南至宁波，运行日期 2026-11-03，库存 20/20；`train_trip_stops` 有站序 1–5：北京南、济南西、南京南、杭州东、宁波，到发时刻与任务书示例一致。该输出证明数据库查询结果，不证明真实 HTTP 查询或区间购票能力。
- 未覆盖边界：没有站点与经停的写入接口、管理端或导入脚本；首末站与 `train_trips` 一致、相邻站先发后到、同车次是否允许重复经停，均只由数据准备保证，未做库级或应用级校验；未用真实 HTTP 客户端验证两个接口，只有 MockMvc；未做数据量、执行计划或真实浏览器验证；区间查询、中途售票、席别票价均为后续模块；测试新增的随机站点与车次记录保留在测试库，不清理。
- Git 状态：本模块开始时本地 `main` 与 `origin/main` 同指 `7faaf3b`，暂存区为空。业务提交 `85574f3`（`feat: add station dictionary and train trip stop queries`，13 个文件、615 行新增、2 行删除，8 个 Java 文件与 V5 迁移均为完整内容）；文档提交 `872c0aa`（`docs: record station and train trip stop completion`，2 个文件、130 行新增、24 行删除）；已推送 `7faaf3b..872c0aa`。提交前发现此前 `git add` 只把新文件暂存成空骨架（4 行／0 行），已重新暂存真实内容才提交。提交后工作区仅余既有 `.gitignore` 修改，未提交。
- 下一步（2026-10-30 当时状态，已被后续实现取代）：第三阶段第二模块“区间车次查询”进入契约确认；当时业务实现与测试尚未开始。该模块随后由用户亲手完成并通过全量回归，见“区间车次查询（第三阶段第二模块，已实现并通过本地全量回归，待主对话验收）”小节。

### 区间车次查询（第三阶段第二模块，已实现并通过本地全量回归，待主对话验收）

- 当前阶段及确认来源：2026-10-30 主对话确认模块任务书；用户以“直接开始吧”要求跳过详细说明进入实施，随后对四条契约默认值未提异议（站点用 code、日期按上车站发车时刻、新增原路径并保留 /api/trains、四类状态码），并在每步完成后以“可以”确认继续。模块 Agent 引导阅读与编码，用户亲手写全部业务代码、测试并运行命令；助手只读核对源码与用户输出。
- 原仓库只读事实（固定 `daf3a2714c00ff263764d23ab10c2eaed2ae842d`）：`TicketController` 的 `GET /api/ticket-service/ticket/query` 直接以 `TicketPageQueryReqDTO` 绑定查询串并返回 `Result<TicketPageQueryRespDTO>`；该 DTO 中 `fromStation`/`toStation` 注释为“出发地 Code”，`departureDate` 为 `Date` + `@DateTimeFormat(yyyy-MM-dd)`，另有 `departure`/`arrival`（站名）并继承 `PageRequest`。过滤器链 order 10 校验日期不早于今天（“出发日期不能小于当前日期”）与起终站不同（“出发地和目的地不能相同”），order 20 校验两个 code 存在于地区/车站字典（“出发地或目的地不存在”）。`pageListTicketQueryV1` 把 code 映射为 `regionName`，按 `start_region`/`end_region` 查 `t_train_station_relation`；`TrainStationRelationMapper` 仅继承 `BaseMapper`，无自定义 SQL；关系表无日期列，查询不带日期，结果在 Java 中按 `HH:mm` 字符串用 `TimeStringComparator` 排序。
- 因此原仓库无法区分“始发日期”与“上车站日期”，该口径由本地自定，不宣称照搬。
- 已确认契约：站点标识用 `stations.code`；`departureDate` 按上车站发车时刻落在 `[date 00:00, 次日 00:00)` 过滤；新增 `GET /api/ticket-service/ticket/query` 并保留 `/api/trains`；响应为 `{"trainList":[...]}`，元素 `trainId`（字符串）、`trainNumber`、`departure`、`arrival`、`departureTime`、`arrivalTime`（完整 `LocalDateTime`），不加 `Result` 包装，不含席别、票价、区间余票、品牌与可售状态；同站 400“起点和终点不能相同”，未知 code 400“出发地或目的地不存在”，合法站点无车次 200 与空 `trainList`，缺参或日期格式错 400。本模块不新增迁移，V5 结构已够用。
- 实现（用户亲手写入）：`train/TrainTripInterval`（JOIN 投影，与 `TrainTrip` 分开）；`TrainTripMapper.findIntervalsByCodesAndDepartureDay`；`StationQueryMapper.countByCodes`；`train/dto/TicketQueryRequest`、`TicketListResponse`、`TicketPageQueryResponse`；`train/StationCodeNotFoundException`；`TrainTripService.searchInterval`（构造器新增 `StationQueryMapper` 依赖，原 `search` 未改）；`train/controller/TicketQueryController`；`GlobalExceptionHandler` 的 400 处理器注解加入新异常并把参数类型改为 `RuntimeException`、方法名改为 `handleInvalidTrainSearch`；新增 `TicketQueryIntegrationTests` 9 项。
- 区间判定（`TrainTripMapper` 的 SQL）：`train_trip_stops` 自连接为 a、b，要求同一 `train_trip_id` 且 `b.stop_sequence > a.stop_sequence`；两次 JOIN `stations` 取站名（fs 为上车站、ts 为下车站）；日期条件写在 `a.departure_at`；`ORDER BY a.departure_at, t.train_number, t.id`。终到站 `departure_at` 为 NULL，会被日期条件自然排除，不会作为上车站成交。命中完全由数据库完成，Java 不做整批经停的排序或过滤。
- 判断顺序：先比两个 code 是否相同（报“起点和终点不能相同”），再查字典计数（`< 2` 报“出发地或目的地不存在”），最后才做区间 JOIN；与原仓库 order 10 先于 order 20 一致。
- 排错记录：首次手工验证时模块 Agent 让用户只把 `ts.code` 改为 `JGK` 并预期 0 行，实际返回 1 行；原因是出发站仍是 北京南（站序 1）→ 济南西（站序 2）为正向，预期写错。真正的反向需 `fs.code='NKH'` 与 `ts.code='JGK'`，用户随后确认为 0 行。业务代码未因此修改。
- 验证记录（用户执行）：`mvn "-Dtest=TicketQueryIntegrationTests" test` 于 2026-10-30 22:46:58 完成，9 项通过；`mvn test` 于 22:50:50 完成，97 项通过，0 失败、0 错误、0 跳过，BUILD SUCCESS（基础 5、错误响应 6、库存 4、订单 38、乘车人删除 7、乘车人 5、车站经停 11、区间车次 9、车次 4、登录 4、注册 4）。日志确认 test profile、独立库 `mini_12306_test`、Flyway 校验 5 个迁移、当前版本 5 无待执行。助手核对 11 份 Surefire XML 合计 97 项、报告时间 22:50:12–22:50:50，`find src -newer` 无输出；未代跑构建或测试。
- 测试覆盖：始发到终到、相邻站、中途站上下车；跨日到发时刻；反向查询 0 行；同站 400；上车站日期口径（中间站 12-21 发车不被 12-20 命中，始发站 12-20 发车不被 12-21 命中）；乱序插入后按发车时刻与车次号确定性排序；合法站点无车次空数组；未知 code 400；缺参、格式错 400 且不回显原始输入；匿名访问不创建会话；响应不含 `seatClassList`。夹具全部自建，不依赖开发库 408，也不改 V2 四条参考车次与库存。
- 未覆盖边界：只有 MockMvc，没有真实 HTTP 客户端或开发库 408 的实机调用；未做 `EXPLAIN` 或索引评估（现有 `uk_stations_code` 与 `idx_train_trip_stops_station` 支撑该写法，执行计划未验证）；首末站与 `train_trips` 的一致性仍只由数据准备保证；同一车次重复经过同一站时区间语义未定义也未测试；不涉及区间下单、区间余票、座位复用、席别与票价；`ticket_inventory.remaining_tickets` 仍只代表整趟数量。主对话 2026-10-30 独立核对另指出三个未专项覆盖的边界：跨日时刻的午夜精确边界（如 00:00:00 归属前一天还是当天）、排序第三键（同一时刻且同车次号时按实例 id）没有对应用例、终到站作为上车站被排除没有对应用例；三者均未在源码中发现相应错误。
- 主对话独立核对（2026-10-30）：只读核对 SQL、Service 判断顺序、400 处理器参数、响应字段、两次提交范围与 9 项测试夹具，并实读 11 份 Surefire XML 共 97 项、失败/错误/跳过均为 0、报告 22:50:12–22:50:50、最新源码修改 22:41:10，认定业务实现与现有测试符合本次契约；同时指出 README 与 AGENTS.md 中的状态矛盾，要求先修正文档再完成整体验收。两处矛盾已由模块 Agent 修正：README 的“站点表和经停表当前均为空”改为标注 baseline 登记时点为空、之后已准备 G35 数据且幂等记录数量未再查询；本文件当前交接、Git 状态与第一模块的“下一步”均改为反映 `d950898` 与第二模块已完成。修正后未重跑测试，主对话整体验收结论尚未给出。
- Git 状态：本模块开始时本地 `main` 与 `origin/main` 同指 `779672f`，暂存区为空。用户于 2026-10-30 亲自提交并推送两次：`f0a3b77` — `feat: query trips by boarding and alighting stations`，11 个文件、541 行新增、4 行删除，7 个新文件均以完整内容入库（`TrainTripInterval` 18 行、测试类 338 行）；`d950898` — `docs: record interval ticket query completion`，2 个文件、71 行新增、19 行删除；已推送 `779672f..d950898`，本地 `main` 与 `origin/main` 同指 `d950898`。提交前索引里仍是 4 行空骨架（与第一模块同一坑），重新 `git add` 后行数正确。开工前已有的 `.gitignore` 修改继续留在工作区，未提交。本条提交编号记录为提交之后的文档更新，尚未提交。
- 下一步：文档状态矛盾已修正（未重跑测试，也未改变业务代码），交主对话完成整体只读验收；验收通过后可进入第三阶段第三模块“席别与区间票价”。

### 乘车人删除（第二阶段小模块，主对话只读验收通过）

- 当前阶段：2026-10-28 由模块 Agent 引导、用户亲手完成实现、测试与全量回归，并由主对话完成独立只读验收。业务提交 `e054706` 与文档提交 `7faaf3b` 均已推送。
- 已确认契约（主对话任务书，用户转交）：接口 `DELETE /api/passengers/{id}`，成功 204；身份只来自 HttpSession，未登录 401；记录不存在与他人记录统一 404 且响应不可区分；已被 `ticket_orders` 引用的乘车人返回 409；删除带账号归属条件；不新增软删除；不改动订单、支付、取消、库存、幂等和支付超时逻辑。
- 外键事实（用户提供两个库的 `SHOW CREATE TABLE ticket_orders`，2026-10-28）：开发库 `mini_12306` 与测试库 `mini_12306_test` 的 `fk_ticket_orders_passenger` 均未声明 ON DELETE，实际为 RESTRICT，`passenger_id` 为 NOT NULL。被引用的乘车人因此无法硬删除，历史订单快照与引用关系得以保留。
- 实际修改（用户亲手写入）：新增 `passenger/PassengerInUseException.java`（409，消息“该乘车人已被订单引用，无法删除”）；`PassengerMapper` 新增 `countOrdersByPassengerId` 与 `deleteByIdAndUserId`（DELETE 同时限定 id 与 user_id 并返回影响行数）；`PassengerService` 新增 `@Transactional delete`；`PassengerController` 新增 `@DeleteMapping("/{id}")` + `@ResponseStatus(NO_CONTENT)`；`GlobalExceptionHandler.handleConflict` 的异常列表加入新异常，复用 `message` 字段。
- 删除流程：先 `findById(userId, id)` 定 404 语义，再查引用计数抛 409，再执行删除；影响行数非 1 抛 `PassengerNotFoundException`；捕获 `DataIntegrityViolationException` 转 409 作为并发下单兜底。这是本项目首个带 `@Transactional` 的乘车人 CRUD 方法。
- 判定顺序与安全：必须先归属（404）再引用（409）。顺序反转时，他人记录被引用返回 409、未被引用返回 404，会泄漏“该 ID 存在且有人下过单”，违反契约。
- 并发与事务边界：普通 SELECT 是快照读，不阻塞并发插单，因此“检查通过→执行删除”之间存在窗口，由 RESTRICT 外键拦截并映射为 409。未使用 `SELECT ... FOR UPDATE`，避免与下单事务产生新的锁等待关系；事务不消除竞态，只保证异常时整体回滚。上述为业务行锁分析，未做死锁或并发注入验证。
- 测试：新增 `PassengerDeletionIntegrationTests`，7 项。覆盖成功删除后详情 404／列表仅剩另一条／数据库行数为 0；未登录 401、不创建会话、记录不变；他人记录 404 且原记录逐字段不变；不存在 ID 404；重复删除首次 204 再次 404；真实下单后被引用返回 409 且乘客、订单快照、订单状态、库存均不变；`not-a-number` 400 与 `0` 404。未修改或放宽既有断言，未使用跳过注解；复用真实注册、登录、下单流程与独立测试库，测试记录保留不清理。
- 验证记录（用户执行）：`mvn "-Dtest=PassengerDeletionIntegrationTests" test` 于 2026-10-28 22:00:55 +08:00 完成，7 项通过；`mvn test` 于 22:02:44 完成，**77 项通过，0 失败、0 错误、0 跳过，BUILD SUCCESS**（基础 5、错误响应 6、库存 4、订单 38、乘车人删除 7、乘车人 5、车次 4、登录 4、注册 4）。日志确认 test profile、独立库 `mini_12306_test`、Flyway 版本 4、无待执行迁移。助手核对 9 份 Surefire 报告的数量与时间（22:01:56–22:02:44）一致，`find src -newer` 无输出，未发现报告生成后修改源码；助手未代跑构建或测试。
- 未覆盖边界：删除与下单同时发生的竞态只由外键兜底，无专项并发用例；未用真实 HTTP 客户端验证该接口；未在开发库执行删除；删除后 ID 不复用、历史订单无法再关联到有效乘客属于既有设计，未做数据修复；`DataIntegrityViolationException` 未按错误码 1451 精确筛选，该 DELETE 上其他完整性错误的处理未验证。
- 主对话验收结论（2026-10-28）：**通过，无阻断问题**。实际源码确认归属查询先于引用计数，外部账号和不存在记录统一走 404；删除 SQL 同时限定 `id` 与 `user_id`；`@Transactional` 负责本次删除流程的回滚边界，不能消除检查与并发下单之间的竞态，外键 RESTRICT 才是并发引用的最终兜底。7 项新增测试均有具体业务结果断言，无 `@Disabled`、`@Ignore` 或无条件跳过；9 份 Surefire 报告合计 77 项通过，报告时间为 22:01:56–22:02:44，未发现报告生成后源码或测试修改。README 删除小节与当前实现及未覆盖边界一致。
- Git 状态：用户于 2026-10-28 亲自提交并推送两次：`e054706` — `feat: allow owners to delete passengers without order references`，6 个文件、444 行新增（新增异常 8 行、测试类 384 行、Mapper／Service／Controller／GlobalExceptionHandler 修改），两个新文件按完整内容入库，不再是空骨架；`7faaf3b` — `docs: record passenger deletion completion`，AGENTS.md 与 README.md 共 47 行新增、10 行删除。推送后本地 `main` 与 `origin/main` 同指 `7faaf3b`。本条提交编号记录为提交之后的文档更新，尚未提交。助手未做任何 Git 写操作。
- 下一步：乘车人删除小模块已完成验收。后续可进入第二阶段收尾评估或新的进阶模块；开发库 V3/V4 执行、历史订单影响评估、真实 HTTP、并发竞态与真实数据库故障仍按各模块边界单独安排，不因本次验收自动完成。

### 支付超时关单（第五模块，主对话只读验收通过，已提交推送）

- 当前阶段：2026-10-28 用户授权模块 Agent 全权负责本模块构建。模块 Agent 提交完整设计清单，用户以“可以”确认后进入实现；用户完成代码、迁移、测试和全量回归，业务提交 `372a3b1` 已推送，主对话已完成独立只读验收。未验证边界仍见本节。
- 已确认前提：继续采用单体 Spring Boot + MySQL、HttpSession 身份和真实 MySQL 集成测试；用户亲手实现与运行命令；沿用前四模块的账号隔离、错误响应、订单分页和下单幂等契约。现有订单状态为 PENDING_PAYMENT、PAID、CANCELLED，TicketOrder 还没有支付截止字段；只读搜索未发现调度启用或关单任务实现。
- 本次只读复核补充：现有 Surefire XML 的类路径包含 spring-context 7.0.9，基础调度 API 可由该框架提供，不能由未声明独立调度依赖推断项目缺少该能力；实际尚未启用相关任务。基础测试精确断言迁移版本 1/2/3、七张表及账号订单索引；新增迁移须按最终方案更新版本断言，仅加列或索引不改变表清单，字段和新索引应按需求另设断言。orders=396 仅为此前 baseline 前后的订单总数快照，不是当前待支付订单数。本轮未连接数据库核实历史订单分布。
- 已确认契约：支付期限固定 30 分钟并由下单时刻起算；使用数据库 `NOW()` 判定；超时终态为 `EXPIRED`；支付同时要求待支付状态和期限未到；取消逻辑不改；幂等重试命中已关单订单返回原单；关单以独立事务批量处理，每批最多 100 单，库存只在状态真正迁移时恢复，测试 profile 关闭调度。
- 当前验证基线：第四模块已有八份报告共 60 项通过，完成时间为 2026-10-28 11:39:06 +08:00；这不是第五模块验证。开发库 V3 执行、备份恢复演练、旧时间迁移、全新空库重放等既有边界仍未验证，不因本模块准备而自动扩大授权。
- Git 与文档：本轮开始只读检查显示本地 main 与 origin/main 对应文档提交 b03befd，只有 AGENTS.md 的既有主对话验收记录未提交，暂存区为空；本轮继续局部维护本文件。后续 Agent 必须重新检查实际状态，不覆盖这些内容，不假设仍然干净或提交未变。
- 当前阶段：2026-10-28 业务、配置、迁移与测试均由用户亲手完成，全量回归通过；业务提交 `372a3b1` 已提交并推送，主对话独立验收通过。按跨模型约定，模块自查不代替主对话独立验收。
- 已确认契约（用户 2026-10-28 以“可以”确认模块 Agent 提交的设计）：期限固定 30 分钟、由下单时刻起算并落库；判定统一使用数据库 `NOW()`；超时终态新增 `EXPIRED`，不复用 `CANCELLED`；支付的条件更新同时要求 `status = 'PENDING_PAYMENT'` 与 `pay_deadline > NOW()`；取消逻辑不改，`EXPIRED` 因条件更新影响 0 行返回 409“当前订单状态不允许取消”；查询与列表正常返回 `EXPIRED`；幂等重试命中已被关单的订单返回 200 和原单、不释放键；库存只由真正完成状态迁移的那一次恢复，恢复失败整笔回滚；单实例 fixedDelay 扫描、每批 100 单、按 `pay_deadline` 升序、每单独立事务、单笔失败不影响同批其他订单；test profile 关闭调度。
- 已完成修改（用户亲手修改）：正式迁移 `V4__add_order_payment_deadline.sql`（可空列 → 回填 `created_at + 30 分钟` → 收紧 NOT NULL → 新增 `idx_ticket_orders_status_deadline (status, pay_deadline)`）；`OrderStatus` 增 `EXPIRED`；`TicketOrder` 增 `payDeadline`；`TicketOrderMapper` 的 INSERT 用 `DATE_ADD(NOW(), INTERVAL 30 MINUTE)` 生成期限，四处 SELECT 补别名映射，新增 `updateStatusIfPendingAndNotExpired`、`findExpiredIds`、`expireIfPendingAndOverdue`、`findTrainTripIdById`；`TicketOrderService.pay` 改用带期限的条件更新并对 `PENDING_PAYMENT`／`EXPIRED` 统一返回“订单已超时关闭，无法支付”，新增 `@Transactional closeExpired(Long)`；新增 `OrderTimeoutCloseScheduler`（`@ConditionalOnProperty` + `@Scheduled(fixedDelayString)`，批大小 100，单笔异常仅记 WARN）；`Mini12306Application` 加 `@EnableScheduling`；`OrderResponse`、`OrderCreateResponse` 增 `payDeadline`；`application-test.yaml` 增 `order.timeout-close.enabled: false`；`Mini12306ApplicationTests` 版本断言改为 1/2/3/4 并新增列非空与新索引断言；`TicketOrderIntegrationTests` 新增 10 项用例与 `orderStatus`、`moveDeadlineToPast` 两个辅助方法。
- 事务与锁顺序：`create` 为幂等行 → 库存行 → 新订单行；`cancel` 与 `closeExpired` 均为订单行 → 库存行；`pay` 只涉及订单行。关单先做“状态 + 期限”条件更新，仅影响 1 行者读取 `train_trip_id` 并恢复库存，恢复返回非 1 行时抛异常回滚状态更新。上述为业务行锁分析，不构成死锁不存在的证明，也未做死锁注入验证。
- 验证记录（用户执行）：`mvn -Dtest=Mini12306ApplicationTests test` 于 14:26:49 使测试库迁移到 v4，5 项通过；`mvn -Dtest=TicketOrderIntegrationTests test` 于 14:33:43 与 14:47:38 各 28 项通过；`mvn test` 于 15:29:48 首次为 70 项中 1 失败（`shouldCloseExpiredOrdersInScheduledBatch` 三张单仍为 `PENDING_PAYMENT`），判断为用例受测试库历史待支付单干扰——`findExpiredIds` 按期限升序 `LIMIT 100`，刚设为一分钟前的三张被历史单挤出批次；改用固定 `2000-01-01` 期限后 `mvn test` 于 15:33:45 为 **70 项通过，0 失败、0 错误、0 跳过，BUILD SUCCESS**（基础 5、错误响应 6、库存 4、订单 38、乘车人 5、车次 4、登录 4、注册 4）。批次用例会顺带关闭测试库中至多 100 张历史待支付单并各恢复 1 张库存。
- 未验证事项与已知边界：开发库仍停在版本 1，V3、V4 未在开发库执行；历史待支付单在开发库的实际数量与时间分布未查询，按约定默认“全部回填、首次扫描即过期”。调度循环用直接构造 `OrderTimeoutCloseScheduler` 驱动，未验证真实定时触发、进程停机期间的积压补偿与跨实例并发；8 线程用例是固定并发场景，不是压力测试或穷尽调度；恢复库存失败导致回滚的分支没有专项用例；日志脱敏未针对关单 WARN 做检查。
- 最近核对的 Git 状态：用户于 2026-10-28 亲自提交 `372a3b1` — `feat: close unpaid orders after the payment window`，14 个文件、692 行新增、22 行删除，其中两个新文件按完整内容入库（`OrderTimeoutCloseScheduler.java` 51 行、`V4__add_order_payment_deadline.sql` 15 行），不存在只提交骨架的情况；已推送 `b03befd..372a3b1`。首次 `git push` 因 `github.com:22` 连接超时失败，改用 `git push origin main` 成功。提交后本地 `main` 与 `origin/main` 同指 `372a3b1`，工作区干净。本节关于提交哈希与推送状态的补充为提交之后的文档更新，尚未提交。
- 主对话验收结论：通过，无阻断问题。未验证边界为开发库 V3/V4 执行、历史待支付订单影响、真实定时触发、停机补偿、跨实例并发、恢复库存失败专项和关单日志脱敏；这些不改变本地学习范围内的实现验收结论。

### 下单幂等（主对话只读验收通过，业务与模块文档已推送）

- 最终结果：用户运行 `mvn test`，于 **2026-10-28 11:39:06 +08:00** 完成，基础 5、注册 4、登录 4、乘车人 5、车次 4、库存 4、订单 28、错误响应 6，共 **60 项通过，0 失败、0 错误、0 跳过，BUILD SUCCESS**。主对话重新解析八份现有 Surefire XML，数量与结果一致，报告文件时间为 11:38:37–11:39:06；日志确认 `test` profile、独立库 `mini_12306_test` 及 Flyway 当前版本 3，测试安全配置要求受限账号 `mini12306_test`。纠正此前源码时间记录：`OrderIdempotencyMapper.java` 最后修改为 11:49:36，晚于报告；其余 src 文件最新修改为 11:23:01。主对话只读使用 `javap -v -p` 检查 01:43:34 生成的现有 Mapper class，三个方法签名、SQL、Mapper/SQL/Param 注解与当前源码一致，该接口没有方法体，未发现影响本次验收的行为差异；未据此声称报告覆盖后续重新编译。此前仅执行 `mvn` 因未指定目标退出，未运行测试；后续完整命令已通过。
- 用户于 2026-10-27 确认并亲手实现契约：`POST /api/orders` 必须携带 `Idempotency-Key` 请求头，1–64 位 ASCII 字母、数字、下划线或连字符，区分大小写，不裁剪；缺失或格式不符返回 400。请求体仍为 trainTripId/passengerId，账号仅来自 HttpSession。键限定在当前账号与创建订单操作内；不同账号同键隔离，不同键表示独立下单意图。
- 同账号同键且两个请求 ID 相同，首次创建返回 201，重复返回 200 和原订单及查询时状态；原单已支付或取消也不重新创建，取消不释放键。成功键的任一请求 ID 不同返回 409“幂等键已用于不同的下单请求”，不回显参数或键。
- 只保留成功创建的映射。业务校验、售罄或事务写入异常时，幂等占位、订单和库存一起回滚，同键可以重试，失败键不绑定参数。客户端改变下单意图时仍应换新键。成功映射与订单长期保留，暂无清理策略、删除订单或过期复用机制；保持单体 MySQL，不引入 Redis、消息队列或第三方幂等库。
- 调用链：Controller 校验请求头/请求体并从 CurrentUserSession 读取账号；Service.create 在一个 Spring 事务内调用 OrderIdempotencyMapper.reserve，以无副作用 UPSERT 竞争唯一键，再通过 findForUpdate 锁定读取；先比较原始参数，已关联则锁定读取所属订单并直接返回，否则验证乘车人归属、查询并条件扣减库存、插入并读取订单，最后 attachOrder。占位不能单独提交；OrderCreateResult 的 created 区分 HTTP 201/200。同键请求等待前一事务提交或回滚，只有实际创建方扣票。
- 正式迁移为 `src/main/resources/db/migration/V3__create_order_idempotency.sql`，未修改已执行 V1 或测试 V2。新表 `order_idempotency` 使用联合主键 `(user_id, idempotency_key)`，键为 ASCII/`ascii_bin`，保存原始请求 ID；可空 order_id 带唯一索引，账号和订单有外键。测试库主迁移与测试迁移合并按 V1/V2/V3 执行，基础测试验证成功版本 1/2/3 及七张表。开发库此前 baseline 到 1，后续只加载正式 V3；目前无开发库执行 V3 的证据。`application-test.yaml` 仅在 `src/test/resources` 且已跟踪，当前不存在旧主资源配置的暂存/未跟踪冲突。
- 测试复用 registerAndLogin、createPassenger、createTrip、createInventory 等辅助方法，新增 submitOrder 与 assertIdempotencyMapping；新用例使用独立随机账号、2099 年车次和库存，不修改 V2 固定参考数据。三项基本专项覆盖首次/同参重试、分别改变两个 ID 的冲突、不同键独立创建、售罄后返回原单、跨账号同键隔离、乘车人校验/售罄失败无映射、取消后旧键返回 CANCELLED，以及失败键恢复库存后成功重试。
- 同键并发专项使用 8 线程、同账号/同键/同请求争抢最后一张票，各线程独立调用真实 Service 代理事务；全部返回同一订单，恰一项 created=true，最终库存 0/订单 1/映射唯一，spy 验证扣库存与插订单各一次，再经 HTTP 重试返回 200。使用 ready/start 门闩、有期限的 Future 与 finally 关闭线程池，无测试层事务；原不同键争抢库存测试保留。
- 请求头专项覆盖缺失、空串、空白、前后空格、非法字符、中文、65 位输入均返回安全 400 且无库存/订单/映射写入；1 位、64 位及 A/a 不同键成功，重复返回同一订单，并验证已支付后旧键返回 PAID。写异常专项在真实扣库存及 SqlSessionTemplate 执行真实 INSERT 后抛模拟 DataIntegrityViolationException，在事务外核对库存恢复、订单和占位均无残留，再用同一键成功创建与重复返回原单。
- 定向验证：`mvn "-Dtest=Mini12306ApplicationTests,GlobalExceptionHandlerIntegrationTests,TicketOrderIntegrationTests" test` 于 01:19:09 为 37 项/1 失败，基础 5 与订单 26 通过；请求头 @Pattern 使空请求体校验进入 HandlerMethodValidationException，原受控字段提示回归失败。用户补齐 handleHandlerMethodValidationException，复用安全字段消息，直接参数仍通用 400、返回值校验保留安全 500；`mvn "-Dtest=GlobalExceptionHandlerIntegrationTests" test` 于 01:43:52 为 6 项全通过。日志中的受控 IllegalStateException ERROR 来自已有模拟 500 测试。
- `mvn "-Dtest=TicketOrderIntegrationTests#shouldCreateOnlyOneOrderForConcurrentSameKey" test` 于 01:54:24 为 1 项通过；`mvn "-Dtest=TicketOrderIntegrationTests#shouldValidateIdempotencyKeyHeader+shouldRollBackInventoryWhenOrderInsertFails" test` 于 11:24:06 为 2 项全通过。后一命令此前因 Mockito 括号错误在模拟设置阶段失败，用户修正为 `.when(ticketOrderMapper).insert(...)`，并把首次调用的新随机键改为同一个 key；后续定向及全量均通过。
- 最终只读核验覆盖 Controller/DTO、Service 事务、Mapper SQL、迁移、错误映射、测试及配置，未发现阻断本地学习范围的问题。验证边界：8 线程固定场景不等于压力测试或穷尽并发调度；写异常为真实写入后模拟运行时故障，不是实际数据库约束/网络故障；本轮没有重新运行真实 HTTP 购票演示，也没有在全新空库重放 V1/V2/V3。成功键长期占用，测试随机业务记录保留；开发库 V3 执行仍未确认。
- Git 结果：用户亲自提交 `b3341bf` — `feat: add idempotent order creation`，共 13 个文件、807 行新增、27 行删除，并成功推送 origin main（`e427939..b3341bf`）；随后亲自提交文档收尾 `b03befd` — `docs: record order idempotency completion`。助手只读核对业务提交包含五个完整新增文件（包括 19 行 V3），当前本地 `main` 与 `origin/main` 同指 `b03befd`，暂存和未暂存差异在验收前均为空；此前新增文件只暂存骨架的问题已解决。助手未代改源码或迁移，未代跑构建/测试/数据库/容器或执行 Git 写操作。
- 主对话最终验收：2026-10-28 复核根目录文档、实际源码、配置、两个提交与现有八份报告，未发现阻断本地学习范围的问题，第四模块验收通过。验收前工作区干净，本地 HEAD 与 origin/main 均为 `b03befd23bb6922185f5555ec79a7a26248c6cdc`；未刷新远程引用。README 的功能与边界描述与当前实现一致，本轮仅维护本文件，更新尚未提交。成功映射无限期保留；开发库 V3、全新空库重放、压力与真实数据库故障仍未验证。除已有边界外，同键不同参数的同时竞争、幂等重试同时发生支付/取消，以及 attachOrder 影响行数异常等防御分支尚无专项测试；现有八线程用例通过 Service 事务代理并发，HTTP 行为由 MockMvc 顺序请求验证，不能称为八路真实 HTTP 并发。
- 下一步为第五模块“支付超时关单”的规则与方案讨论，尚未授权实现。讨论项：支付期限与精确截止条件、超时状态及接口行为、数据库时间口径、支付/取消/关单竞争和库存仅恢复一次、历史待支付订单处理、应用停机后的补偿扫描及测试方式。候选方案为单体定时分批扫描、每单独立事务、状态与期限条件更新和库存恢复同事务，后续新增正式 V4；这些均为建议，未成为已确认契约。不创建其他对话，不代改业务、配置或测试，不执行构建/测试/数据库/容器及 Git 写操作。

### 独立测试库与数据库版本迁移（Flyway，代码已提交，文档已更新）

- 依赖按实际 POM 与解析结果核对：Spring Boot 4.0.8 管理 `spring-boot-starter-flyway`；Flyway Core/MySQL 扩展实际为 11.14.1，MySQL Connector/J 为 9.7.0；本地 MySQL 8.4 可执行迁移，但 Flyway 日志警告其最新已验证 MySQL 版本为 8.1。
- `src/main/resources/db/migration/V1__create_initial_schema.sql` 只建五张业务表：users、passengers、train_trips、ticket_inventory、ticket_orders；不插入车次或库存种子数据。`src/test/resources/db/test-migration/V2__insert_test_reference_data.sql` 只用于测试参考车次及对应库存。测试 profile 的 locations 同时含主迁移和测试迁移；主配置 SQL initializer 关闭，避免旧脚本重复运行。
- 测试 profile 连接 `localhost:3307/mini_12306_test`，用户 `mini12306_test`，密码从 `.env` 中 `MYSQL_TEST_PASSWORD` 读取；应用默认 profile 仍连接开发库 `mini_12306` 与 root。测试通过父类固定激活 test profile，并导入测试安全配置。安全配置检查生效 URL/用户名/Flyway 设置；迁移前又从实际连接核实 `DATABASE()` 和 `CURRENT_USER()`，数据库不对即阻止启动。负向验证中用户用 Maven 属性注入 `127.0.0.1:1/not_allowed`，启动在配置检查阶段以 `Unsafe test database configuration; startup blocked` 被拦截，没有连接或迁移。
- 测试库 Flyway 配置关闭 baseline-on-migrate、禁用 clean、启用 validate；测试 V1 在空库执行成功，再执行 V2 插入固定参考数据。重复启动时停在版本 2，没有重放 V2 或重置库存。自动化测试检查版本 1/2 均有成功历史记录且无失败记录、实际六张表清单，以及 `ticket_orders` 联合索引列顺序 `(user_id, id)`。
- 开发库既有五表结构经用户提供的 SHOW CREATE TABLE 输出与 V1 对照一致。执行 baseline 前用户确认 schema 为 `mini_12306`；备份开发库到仓库外个人目录。使用 Flyway Maven Plugin 11.14.1 对明确指定的开发库执行显式 baseline version 1，未运行 V1 建表或测试 V2。baseline 后用户核对迁移历史为一条 BASELINE version 1；操作前后业务记录计数和库存总余量一致。正常配置下 `mvn spring-boot:run` 再次校验迁移并显示开发库版本 1 up to date。
- 用户验证：`mvn "-Dtest=Mini12306ApplicationTests" test` 最新 5 项通过；此前 `mvn "-Dtest=TrainTripIntegrationTests" test` 4 项通过，确认 V2 数据可支撑固定车次查询；最新 `mvn test` 全量 55 项通过，0 失败、0 错误、0 跳过，BUILD SUCCESS。全量日志显示 test profile 连接 `mini_12306_test`，Flyway 版本 2 已是最新。README 已根据当前配置更新，未为文档编辑重跑测试。
- 范围及未覆盖：测试账号在数据库层没有开发库权限；固定测试参考数据由不可变 V2 提供，其他集成测试创建的随机业务数据会保留。备份已导出但没有在隔离库进行恢复演练；没有重新连接核对所有开发库业务记录内容，基线前后仅对比表记录数及余票总和。自动结构断言仅覆盖表清单与订单索引，非全部字段/外键/CHECK 的自动比对；Flyway validate 也不全面检测实际表结构漂移。正式目录后续迁移须避开已被测试迁移占用的版本 2，采用全局不重复版本（例如 3），不可在两目录添加相同版本。代码提交为 `0bf3faf`，文档与代码分开提交；第四模块开始时曾核对本地超前两个提交，随后复查本地 `main` 与 `origin/main` 均为 `e427939`，未刷新远程引用。

### 订单列表分页与索引分析（主对话只读验收通过，已推送）

- 用户实现并提交推送 `14410a1`（`feat: add pagination to account order lists`）；提交含六个文件。主对话核对本地 `HEAD` 与 `origin/main` 均为 `14410a1446e0443db3edd12247622be18267922e`。README 记录分页契约、索引计划和边界。
- `GET /api/orders` 按 HttpSession 当前账号查询，`page` 从 1 开始且默认 1，`size` 默认 10、范围 1–100，固定 `id DESC`。响应含 `items/page/size/total/totalPages`；空列表页数为 0，越界页返回 200、空 items、实际总量和请求页码。Mapper 以账号条件分别执行 `COUNT(*)` 与 `LIMIT/OFFSET`，Service 使用 `long` 计算偏移；创建、支付、取消、库存和事务流程未改。
- 测试覆盖默认值、空列表、第一页/多页/部分末页、越界空页、大页码偏移、参数错误、账号订单和总数隔离，以及伪造 `userId` 不覆盖会话身份。用户于 2026-10-25 23:58:24 +08:00 在当前提交运行 `mvn test`，53 项通过，0 失败、0 错误、0 跳过，BUILD SUCCESS；主对话核对八份 Surefire XML 的数目与结果一致，时间为 23:57:48–23:58:23。为解决先前报告早于 Mapper 文件时间的疑点，另核对当前 Mapper 编译产物时间为 23:51:13，晚于源文件时间 23:34:15，且早于本次测试报告；Maven 输出编译阶段均为 up to date。本次未代跑构建或测试。
- 用户在 IDEA 查询控制台对第一页、第二页和按账号计数执行 `EXPLAIN FORMAT=TRADITIONAL`：均使用 `idx_ticket_orders_user_id (user_id, id)`；列表计划为反向索引扫描且无 `Using filesort`，计数显示 `Using index`。证据来自用户提供的结果与 README 记录，本轮未连接数据库复核。结论只适用于本次查询和数据分布，不构成大数据量保证；大 OFFSET 有跳过成本，COUNT 与列表是独立查询，并发新增时不保证同一快照，跨页新增会影响位置；未做大数据量性能测试。
- 验收未发现阻断问题。此结论限于源码、用户提供的测试输出、Surefire 报告和 EXPLAIN 结果；未重跑数据库查询、未做性能测试。用户负责业务代码与数据库操作，主对话仅只读核验并维护本文件；本文件更新未提交。
- 此前车次旧状态纠正及库存模块最终交接文档的单次暂存/提交授权已执行完成，本次验收仅只读检查并维护本文件，不包含新的暂存、提交或推送。之前正常下单测试两段非空断言的单次编辑授权也已用完。不主动创建或向其他对话发送任务。
- 文档操作记录：主对话此前的路线确认与第一模块任务记录已保留；第一模块对话根据用户最新明确授权更新 README 与本文件的完成记录。业务、配置和测试均由用户编写及运行，助手未代跑构建、测试、数据库、容器或 Git 写操作。教学继续围绕完整目的解释请求、失败位置和断言，用户需要时给出具体写法，避免仅凭照写代码或测试通过推定已理解；后续示例代码添加简短英文注释。文档不维护自身提交编号，以免循环修改。
- 主对话给模块对话的任务应包含：目标、已知前提、范围、需要讨论的决定，以及完成后的验证要求。
- 模块对话结束时更新本文件：实际完成的功能、关键设计决定、验证命令及结果、已知问题、下一步；有业务提交时记录其编号。未验证或未推送的状态必须明确标注。
- 不复制完整聊天日志；保留继续学习必需的信息。进度改变时替换过期的“当前停留点”，避免累积互相冲突的指令。
