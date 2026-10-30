# mini-12306

参考 nageoffer/12306，用于学习 Spring Boot、MyBatis、MySQL、事务与并发控制。

## 功能与范围

- 用户注册、登录、当前用户查询和退出登录。
- 乘车人新增、查询、修改、删除及账号隔离。
- 已被订单引用的乘车人不能删除，保留历史订单中的乘车人快照。
- 按日期和固定起终站查询示例车次。
- 车站字典查询与车次经停站查询（按站序返回到发时刻）。
- 按上车站、下车站和日期查询经过该区间的车次（只查车次，不含票价与区间余票）。
- 余票查询、创建订单、订单列表分页和详情。
- 模拟支付、取消订单及恢复库存。
- 条件扣减防止超卖，事务保证订单与库存的一致性。
- 下单幂等键防止同一次购买意图重复创建订单和扣减库存。
- 支付超时关单：下单 30 分钟未支付自动关闭并恢复库存。

第一版采用单体应用，仅支持全程票、一种席别、一位乘车人一张票。
不分配具体座位，不接入真实支付；退款尚未实现。

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

全新空库可以直接启动应用，Flyway 会依次执行正式 V1、V3、V4、V5 创建表并调整结构。已有数据库不能直接
重放建表 SQL：先备份，再将实际表结构与目标版本迁移逐项核对；一致后才可
显式执行 Flyway `baseline` 登记当前版本。Baseline 只写迁移历史，不会验证
或创建业务结构，也不会运行被登记版本的 SQL。结构不一致时先停止，不能
用 baseline 掩盖差异。`baseline-on-migrate` 保持关闭，`clean` 被禁用。

截至 2026-10-30，开发库现有结构已与正式 V1、V3、V4、V5 迁移定义核对一致，
Flyway 历史已登记 baseline 版本 5。Baseline 只登记当前版本；此次登记没有执行
V3、V4、V5 SQL。后续正式 Schema 变更应新增迁移文件，不修改已登记的迁移，
也不要再手动运行 `src/main/resources/db` 中的旧 `SOURCE` 脚本。测试专用迁移
使用版本 2，开发库不加载该迁移。

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
开发 profile 不加载该测试目录。正式 V3 新增 `order_idempotency` 表；正式 V4
为 `ticket_orders` 增加 `pay_deadline` 列和 `(status, pay_deadline)` 索引，
不新增表。正式 V5 新增车站字典 `stations` 与车次经停 `train_trip_stops`
两张表，只建结构、不写入任何站点或经停数据。测试库按 V1/V2/V3/V4/V5
迁移后是八张业务表和一张 `flyway_schema_history` 管理表。

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
固定并发用例不等于压力测试，模拟异常不等于真实数据库故障；开发库尚无 V3 执行证据。实现已由用户提交并推送为 `b3341bf`（`feat: add idempotent order creation`），详细交接见 AGENTS.md。

## 支付超时关单

订单创建时写入支付期限，为下单时刻加 30 分钟。期限用数据库函数计算，
与 `created_at` 同源，并通过响应的 `payDeadline` 字段返回。

- 期限一律用数据库 `NOW()` 判定，不受应用时钟影响。
- 超过期限后支付返回 409“订单已超时关闭，无法支付”，
  即使关单任务还没执行也一样；边界取 `pay_deadline > NOW()`，等于截止时刻即不可支付。
- 关单任务按期限升序分批处理，每批最多 100 单，默认每 60 秒一轮，
  使用 fixedDelay 避免任务堆积，单笔失败只回滚该笔并在下一轮重试。
- 关单把状态改为 `EXPIRED` 并恢复 1 张库存，两步在同一事务内。
  只有真正把状态从 `PENDING_PAYMENT` 改成 `EXPIRED` 的那一次才恢复库存，
  恢复失败整笔回滚。
- 已支付、已取消的订单不会因期限到达被改动。
- 关单不释放幂等键；同键重试返回 200 和原订单，状态为 `EXPIRED`。
- 取消逻辑不受期限影响，已关闭订单再取消返回 409。
- test profile 用 `order.timeout-close.enabled=false` 关闭调度，
  测试直接调用 Service 方法验证业务结果。

第一版是单实例扫描：停机期间到期的订单在下次启动后由扫描补处理，
没有单独的补偿任务；多实例同时扫描时，条件更新保证同一订单只会被关闭一次。

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

## 乘车人删除

`DELETE /api/passengers/{id}` 删除当前登录账号名下的乘车人，成功返回 204 空响应。

- 账号身份只来自 HttpSession；未登录返回 401。
- 记录不存在与属于其他账号统一返回 404“乘车人不存在”，两种情况的响应完全一致。
- 已被订单引用的乘车人返回 409“该乘车人已被订单引用，无法删除”。
- 重复删除时第二次返回 404；删除影响行数不是 1 时按记录不存在处理。
- 删除 SQL 同时限定 id 与 user_id，即使前置校验失效也无法删除他人记录。

实际数据库中 `fk_ticket_orders_passenger` 未声明 ON DELETE，行为为 RESTRICT。
Service 先做归属校验和引用检查以给出明确的 404／409 语义，再执行删除；
两步之间的并发下单由外键拦截，同样映射为 409。删除方法使用单独事务，
但事务不阻止并发插入订单，最终一致性由外键保证。本版为硬删除，不引入软删除。

已被订单引用的乘车人只能修改资料，不能删除。订单查询不 JOIN `passengers`，
历史订单继续显示下单时保存的姓名和证件快照。

## 车站与车次经停站

`GET /api/ticket-service/station/all` 返回全部车站，按 `code` 升序，
元素为 `name`、`code`、`spell`、`regionName`，不返回数据库 id。
`GET /api/ticket-service/train-station/query?trainId=...` 返回一个车次的经停顺序，
`trainId` 对应 `train_trips.id`，即一次带日期的车次运行。

- 元素为 `sequence`、`departure`、`arrivalTime`、`departureTime`、`stopoverTime`。
- 按数据库数值站序升序返回，`sequence` 以字符串输出以避免与原契约不一致。
- `departure` 是站名；`stopoverTime` 是到发之间的分钟数，首末站为 null。
- 时刻返回完整 `LocalDateTime`（如 `2026-12-21T01:05:00`），保留日期以便跨日校验；
  原项目只输出 `HH:mm`，此处为有意差异。
- 车次不存在返回 404“车次不存在”；车次存在但未配置经停返回 200 和空数组。
- `trainId` 缺失、非正数或不能转换为 Long 返回受控 400，不回显原始输入。
- 两个接口都不要求登录、不创建会话、不写业务数据，错误统一走 MVC 错误响应。

数据结构：`stations` 以 `code` 唯一，站名不唯一；`train_trip_stops` 以
`(train_trip_id, stop_sequence)` 唯一，`stop_sequence` 为正整数（INT），
外键指向 `train_trips` 与 `stations`，CHECK 要求至少一个时刻且到站不晚于发车。
未对 `(train_trip_id, station_id)` 加唯一键，同一车次重复经过同一站仍可录入。

跨行规则不由单个 CHECK 保证：首末站与 `train_trips` 的起终点和到发时刻一致、
相邻站先发后到、同车次是否允许重复经停，都由数据准备与导入校验负责。
当前没有站点或经停的写入接口，也没有导入脚本。2026-10-30 用户在开发库
手工准备了 G35（北京南至宁波，2026-11-03）及北京南、济南西、南京南、
杭州东、宁波五站的经停数据；这是受控示例数据，不是导入功能或区间售票能力。
拼音/地区检索和中途站售票属于后续模块。

## 区间车次查询

`GET /api/ticket-service/ticket/query?departureDate=2026-11-03&fromStation=VNP&toStation=NKH`
按上车站、下车站和日期查询经过该区间的车次，路径与原项目一致。

- `fromStation`、`toStation` 是 `stations.code`，不是站名；站名不唯一。
- `departureDate` 按上车站的发车时刻过滤，区间为 `[当日 00:00, 次日 00:00)`。
  不是始发日期：始发 23:30、次日 00:25 到中途站的车次，属于中途站的次日。
- 区间命中由同一条 `train_trip_id` 的两条经停记录决定，要求上车站
  `stop_sequence` 小于下车站；SQL 自连接完成，Java 不拉整批经停再排序过滤。
- 响应正文是 `{"trainList": [...]}`，元素为 `trainId`（字符串）、`trainNumber`、
  `departure`、`arrival`、`departureTime`、`arrivalTime`。
- 时刻是完整 `LocalDateTime`，保留日期以便跨日校验；原项目输出 `HH:mm`。
- 排序为上车站发车时刻、车次号、车次实例 id，同一秒也不会乱序。
- 起终站相同返回 400“起点和终点不能相同”；code 不在字典返回 400
  “出发地或目的地不存在”；站点合法但无车次返回 200 和空 `trainList`；
  缺参或日期格式错误返回 400。
- 不含席别、票价、区间余票、品牌与可售状态，也没有 `Result` 包装。
  `ticket_inventory.remaining_tickets` 仍是整趟数量，不代表任意区间余票。
- 与原项目的差异：原项目用预计算的 `t_train_station_relation`，该表没有日期列，
  查询不带日期；本版直接查经停表并按上车站日期过滤。
- `GET /api/trains` 保持原样，仍按固定起终点和始发日期查询，本模块未改动。

## 测试

保持本地 MySQL 运行，在项目根目录执行：

```powershell
mvn test
```

测试通过 `test` profile 连接本地 `mini_12306_test`，使用
`MYSQL_TEST_PASSWORD` 和受限账号。执行前检查 `.env` 已配置该密码，
且 MySQL 测试库与用户已按上文创建。

测试启动会核实预期数据源配置和实际数据库/用户；不匹配时在迁移前阻止
应用上下文启动。测试会检查 Flyway 版本历史、九张表、订单列表索引、
支付期限列非空和关单扫描索引的列顺序，以及车站 code 唯一键、
经停 `(train_trip_id, stop_sequence)` 唯一键和经停的两个外键。
集成测试会创建随机账号、乘车人、车次、库存和订单，部分数据会保留；
测试专用 V2 只在首次迁移时插入固定参考车次和初始库存，不会每次运行时
重置库存。

覆盖账号隔离、订单状态转换、库存回滚及固定并发场景，
以及订单分页、越界空页、分页参数校验、账号总量隔离、下单幂等、
支付超时关单、乘车人删除、车站经停查询和区间车次查询。当前全量为 97 项通过。

车站经停用例覆盖字典字段与 code 升序、经停乱序插入后仍按数值站序返回、
跨日时刻与停留分钟、首末站空值、未知车次 404、旧车次空数组、
`trainId` 缺失/非数字/非正数 400、匿名访问不创建会话，
以及重复站序、重复站点 code、错误外键和站序非正数的约束失败。

区间车次用例覆盖始发到终到、相邻站与中途站上下车、跨日到发时刻、
反向查询为空、同站 400、上车站日期口径（跨日车次只命中上车站当天）、
乱序插入后按发车时刻与车次号排序、合法站点无车次的空数组、
未知 code 400、缺参与格式错误 400 且不回显原始输入，
以及匿名访问不创建会话、响应不含席别字段。

关单相关用例覆盖期限写入、未到期不关单、到期关单与库存只恢复一次、
过期后支付被拒、截止时刻边界、过期后不可取消、已支付或已取消不被关单、
幂等重试返回已关闭订单、批次扫描以及 8 线程同单只关闭一次。
测试不等待 30 分钟，而是直接改写 `pay_deadline` 制造过期状态；
批次用例会把测试库中至多 100 张历史待支付单一起关闭。

## 当前边界

本项目用于本地学习。业务日期时间统一按北京时间（UTC+08:00）解释，
使用 MySQL DATETIME 和 Java LocalDateTime 保存与传递。
MySQL 默认时区已配置为 +08:00；修正前生成的历史时间尚未迁移。

2026-10-30 用户核对开发库迁移结构后，将 Flyway baseline 登记为版本 5。此前
开发库已有的 `pay_deadline` 值均等于 `created_at` 加 30 分钟；311 笔待支付单
在登记前均已过期，用户同意按超时规则处理。普通配置启动后，用户观察到这
311 笔全部转为 `EXPIRED`；订单总数仍为 396。站点表和经停表当前均为空，
幂等记录也为 0；baseline 不会自动写入示例车站或经停数据。

超时关单只做到单实例定时扫描，没有补偿任务、消息队列或多实例协调；
停机期间到期的订单依赖下次启动后的扫描。关单日志只记录订单 ID，
未针对日志脱敏做专项检查。

已统一 MVC 错误响应，保留 message 字段与原有业务 HTTP 状态码；
通过虚构输入验证参数校验错误响应及当前配置下的控制台日志不回显敏感输入。
该验证不代表所有日志来源均已脱敏。

浏览器接入和部署所需的 CSRF 防护与 Cookie 配置尚待完善。

乘车人一旦被订单引用就无法删除，本版不提供先处理订单再删除的流程；
删除与下单并发时没有专项用例，只由外键约束兜底。
