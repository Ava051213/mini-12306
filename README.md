# mini-12306

参考 nageoffer/12306，用于学习 Spring Boot、MyBatis、MySQL、事务与并发控制。

## 功能与范围

- 用户注册、登录、当前用户查询和退出登录。
- 乘车人新增、查询、修改及账号隔离。
- 按日期和固定起终站查询示例车次。
- 余票查询、创建订单、订单列表分页和详情。
- 模拟支付、取消订单及恢复库存。
- 条件扣减防止超卖，事务保证订单与库存的一致性。
- 下单幂等键防止同一次购买意图重复创建订单和扣减库存。

第一版采用单体应用，仅支持全程票、一种席别、一位乘车人一张票。
不分配具体座位，不接入真实支付；退款和超时关单尚未实现。

参考项目：https://github.com/nageoffer/12306

## 环境

- Java 17
- Maven 3.9.x（本地使用 3.9.14）
- Docker 与 Docker Compose
- Spring Boot 4.0.8
- MySQL 8.4（由 Compose 启动）

## 数据库与迁移

### 本地凭据和 MySQL

在项目根目录创建 `.env`，填写自己的本地凭据：

```properties
MYSQL_ROOT_PASSWORD=replace_with_your_local_root_password
MYSQL_TEST_PASSWORD=replace_with_a_different_test_account_password
```

`.env` 已被 Git 忽略，不要提交真实密码。Compose 使用 root 凭据启动
MySQL；Spring Boot 默认配置也使用 root 连接开发库。测试使用单独账号，
密码通过 `MYSQL_TEST_PASSWORD` 提供。不要将本地凭据用于生产环境。

启动数据库并等待 MySQL 日志出现 `ready for connections`：

```powershell
docker compose up -d
docker compose logs --tail 30 mysql
```

Compose 创建开发库 `mini_12306`。本机端口为 `3307`，容器端口为 `3306`，
数据保存在命名卷 `mysql_data` 中。

### 开发库 Schema

应用默认连接 `mini_12306`，启动时 Flyway 会校验迁移历史并执行尚未应用的
正式迁移。正式迁移放在 `src/main/resources/db/migration`，使用类似
`V1__create_initial_schema.sql` 的命名方式。V1 创建五张业务表，不插入
示例车次或余票。Flyway Schema History 另有一张管理表。

全新空库可以直接启动应用，Flyway 会依次执行正式 V1、V3 创建表。已有数据库不能直接
重放建表 SQL：先备份，再将实际表结构与目标版本迁移逐项核对；一致后才可
显式执行 Flyway `baseline` 登记当前版本。Baseline 只写迁移历史，不会验证
或创建业务结构，也不会运行被登记版本的 SQL。结构不一致时先停止，不能
用 baseline 掩盖差异。`baseline-on-migrate` 保持关闭，`clean` 被禁用。

本项目开发库已经由维护者核对结构、备份并显式 baseline 到版本 1。后续
正式 Schema 变更应新增迁移文件，不修改已执行的 V1，也不要再手动运行
`src/main/resources/db` 中的旧 `SOURCE` 脚本。测试专用迁移使用了版本 2，
共享正式迁移后续从版本 3 起，并保证所有迁移目录中的版本唯一。

旧的 `src/main/resources/db/*.sql` 手动初始化脚本暂留作历史参考；Spring
SQL 初始化器已关闭，应用启动不会执行它们。它们包含建表语句，不适合在
已有库中重复运行。全新开发库通过正式迁移创建结构，不会自动获得示例车次。

### 独立测试库

测试连接 `mini_12306_test`，使用 MySQL 用户 `mini12306_test`。首次准备测试
环境时，使用 DBA/root 在 MySQL 中执行以下语句；将密码替换为与 root 不同的
本地测试密码，并将同一密码写入 `.env` 的 `MYSQL_TEST_PASSWORD`：

```sql
CREATE DATABASE mini_12306_test
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

CREATE USER 'mini12306_test'@'%'
    IDENTIFIED BY 'replace_with_a_different_test_account_password';

GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES
    ON `mini\_12306\_test`.*
    TO 'mini12306_test'@'%';
```

该账号只应获得测试库权限，不授予开发库或生产库权限。测试配置位于
`src/test/resources/application-test.yaml`，测试基类固定激活 `test` profile。
启动迁移前，测试安全配置会校验生效 URL、MySQL 用户和实际连接的数据库，
配置不匹配时阻止启动。

测试 profile 加载正式迁移和 `src/test/resources/db/test-migration` 中的
测试专用迁移。V1 创建五张业务表，测试 V2 插入四条固定参考车次及其库存；
开发 profile 不加载该测试目录。正式 V3 新增 `order_idempotency` 表，测试库按
V1/V2/V3 迁移后有六张业务表和一张 `flyway_schema_history` 管理表。

## 启动应用

```powershell
mvn spring-boot:run
```

默认地址为 `http://localhost:8080`。保持启动终端运行，
在另一个终端检查未登录响应：

```powershell
curl.exe -i http://localhost:8080/api/users/me
```

预期返回 401 和“请先登录”。

登录使用 HttpSession，客户端需要保存并发送 JSESSIONID Cookie。
在启动终端按 Ctrl+C 可停止应用。

## 下单幂等

客户端为一次购买意图生成一个键，并在该次购买的所有重试中复用；发起新的购买时换新键。

- `POST /api/orders` 要求 `Idempotency-Key` 请求头，1–64 位 ASCII 字母、数字、下划线或连字符，区分大小写；缺失或格式错误返回 400。
- 键限定在当前会话账号和创建订单操作内。相同键与相同 trainTripId/passengerId 返回同一订单：首次 201，重试 200；相同键但参数不同返回 409。
- 重试返回原订单及查询时状态；原订单已支付或取消也不重新创建，重新购票须换新键。
- 失败不保留幂等结果，同一键可重试；成功映射与订单长期保留，暂不引入清理任务。

正式 Flyway V3 创建 MySQL 幂等表，以 `(user_id, idempotency_key)` 联合主键限定账号作用域。
Service 在一个事务内通过 UPSERT 和行锁协调同键请求，将幂等映射、扣库存与订单一起提交或回滚；未修改既有 V1/V2。

独立测试库已迁移到 V3。用户运行 `mvn test` 全量 60 项通过，覆盖同键重试、参数冲突、账号隔离、请求头边界、支付/取消后的重试，以及 8 线程同键只创建一单且只扣一张票。写入失败测试在真实扣库存和插单后模拟异常，验证三者回滚及同键重新下单。
固定并发用例不等于压力测试，模拟异常不等于真实数据库故障；开发库尚无 V3 执行证据。当前实现尚未提交/推送，详细交接见 AGENTS.md。

## 订单列表分页

`GET /api/orders?page=1&size=10` 查询当前登录账号的订单，
账号身份来自 HttpSession，固定按 id 倒序排列。

- page 从 1 开始，默认 1。
- size 默认为 10，允许范围为 1–100。
- 响应包含 items、page、size、total、totalPages。
- 无订单时 items 为空数组，total 和 totalPages 均为 0。
- 超出末页时返回 200 和空 items，保留实际总量与请求页码。
- 非法分页参数返回 400，错误正文使用 message 字段，不回显非法输入。

列表响应已从直接返回数组改为分页对象，客户端需通过 items 读取订单。
数据库按当前账号执行 COUNT(*) 和 LIMIT/OFFSET 查询，
不在 Java 内存中分页；total 也只统计当前账号的订单。

现有索引 idx_ticket_orders_user_id (user_id, id) 已通过
本地 EXPLAIN 核对：第一页和第二页的列表计划均使用该索引反向扫描，
未出现 Using filesort；计数计划使用同一索引并显示 Using index。
本次未修改表结构或新增索引，执行计划结论限于本次查询和数据分布，
不代表大数据量下的性能保证。

大 OFFSET 仍有跳过记录的成本，本轮未进行大数据量性能测试。
计数与列表是两次独立查询，不保证并发新增订单时来自同一快照；
跨页查询期间新增订单也可能导致结果位置变化。

## 测试

保持本地 MySQL 运行，在项目根目录执行：

```powershell
mvn test
```

测试通过 `test` profile 连接本地 `mini_12306_test`，使用
`MYSQL_TEST_PASSWORD` 和受限账号。执行前检查 `.env` 已配置该密码，
且 MySQL 测试库与用户已按上文创建。

测试启动会核实预期数据源配置和实际数据库/用户；不匹配时在迁移前阻止
应用上下文启动。测试会检查 Flyway 版本历史、七张表以及订单列表索引。
集成测试会创建随机账号、乘车人、车次、库存和订单，部分数据会保留；
测试专用 V2 只在首次迁移时插入固定参考车次和初始库存，不会每次运行时
重置库存。

覆盖账号隔离、订单状态转换、库存回滚及固定并发场景，
以及订单分页、越界空页、分页参数校验、账号总量隔离和下单幂等。

## 当前边界

本项目用于本地学习。业务日期时间统一按北京时间（UTC+08:00）解释，
使用 MySQL DATETIME 和 Java LocalDateTime 保存与传递。
MySQL 默认时区已配置为 +08:00；修正前生成的历史时间尚未迁移。

已统一 MVC 错误响应，保留 message 字段与原有业务 HTTP 状态码；
通过虚构输入验证参数校验错误响应及当前配置下的控制台日志不回显敏感输入。
该验证不代表所有日志来源均已脱敏。

浏览器接入和部署所需的 CSRF 防护与 Cookie 配置尚待完善。
