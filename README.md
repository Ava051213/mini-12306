# mini-12306

参考 nageoffer/12306，用于学习 Spring Boot、MyBatis / MyBatis-Plus、MySQL、事务与并发控制。

## 功能与范围

- 用户注册、登录、当前用户查询和退出登录。
- 乘车人新增、查询、修改、删除及账号隔离。
- 已被订单引用的乘车人不能删除，保留历史订单中的乘车人快照。
- 按日期和固定起终站查询示例车次。
- 车站字典查询与车次经停站查询（按站序返回到发时刻）。
- 按上车站、下车站和日期查询经过该区间的车次，并返回已配置的席别票价与区间可售座位数。
- 车厢、座位与座位段状态模型，按相邻站段记录可售状态。
- 区间座位购票：服务端按区间与席别分配具体座位，保存车厢号、座位号和成交金额快照。
- 未支付与已支付订单都占用座位段；取消与超时关单只释放该订单实际持有的段。
- 同一座位可分别出售相邻区间，重叠区间不能重复出售；座位售票与数量售票按运行实例互斥。
- 余票查询、创建订单、订单列表分页和详情。
- 服务端按全程区间与席别取价下单，保存席别与成交金额快照；旧下单入口保留无金额行为。
- 模拟支付、取消订单及恢复库存。
- 条件扣减防止超卖，事务保证订单与库存的一致性。
- 下单幂等键防止同一次购买意图重复创建订单和扣减库存。
- 支付超时关单：下单 30 分钟未支付自动关闭并恢复库存。

仍采用单体应用，一笔订单只含一位乘车人和一张票。启用座位售票的运行实例
按区间分配具体座位并按段占用；其余实例保留整趟数量库存和旧下单入口。
不接入真实支付，退款、多乘车人、用户指定座位尚未实现。

参考项目：https://github.com/nageoffer/12306

## 环境

- Java 17
- Maven 3.9.x（本地使用 3.9.14）
- Docker 与 Docker Compose
- Spring Boot 4.0.8
- MyBatis-Plus 3.5.17（`mybatis-plus-spring-boot4-starter`，保留既有手写 Mapper）
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

全新空库可以直接启动应用，Flyway 会依次执行正式 V1、V3 到 V8 创建表并调整结构。已有数据库不能直接
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
两张表，只建结构、不写入任何站点或经停数据。正式 V6 新增区间票价
`train_trip_interval_prices`；V7 为订单与幂等表增加购买模式及订单快照列；
V8 新增车厢 `train_carriages`、座位 `train_seats` 与座位段状态
`train_seat_segments`，只建结构、不写入车厢或座位数据。测试库按 V1–V8
迁移后是十二张业务表和一张 `flyway_schema_history` 管理表。

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

- `POST /api/orders` 与 `POST /api/orders/priced` 均要求 `Idempotency-Key` 请求头，1–64 位 ASCII 字母、数字、下划线或连字符，区分大小写；缺失或格式错误返回 400。
- 键限定在当前会话账号和创建订单操作内，两个下单入口共用同一作用域。相同键与相同购买模式、trainTripId、passengerId（计价入口还包括 seatType）返回同一订单：首次 201，重试 200；同键改动任一参数或跨入口复用返回 409。
- 重试返回原订单及查询时状态；原订单已支付或取消也不重新创建，重新购票须换新键。
- 失败不保留幂等结果，同一键可重试；成功映射与订单长期保留，暂不引入清理任务。

正式 Flyway V3 创建 MySQL 幂等表，以 `(user_id, idempotency_key)` 联合主键限定账号作用域；
V7 再补 `purchase_mode` 与 `seat_type`，用于区分两个入口并比对参数。
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
此列表使用 MyBatis-Plus `BaseMapper<Station>` 与 `LambdaQueryWrapper`，
在数据库按 `code` 升序查询，再转换为响应 DTO。实体映射 `stations`、
数据库自增主键 `id` 及 `region_name` 列；未开放通用 CRUD 接口。
本轮只迁移这条字典列表查询，经停 JOIN、站码计数、首末站查询及其他业务手写 SQL 保留。

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
  `departure`、`arrival`、`departureTime`、`arrivalTime`、`seatClassList`。
- 时刻是完整 `LocalDateTime`，保留日期以便跨日校验；原项目输出 `HH:mm`。
- 排序为上车站发车时刻、车次号、车次实例 id，同一秒也不会乱序。
- 起终站相同返回 400“起点和终点不能相同”；code 不在字典返回 400
  “出发地或目的地不存在”；站点合法但无车次返回 200 和空 `trainList`；
  缺参或日期格式错误返回 400。
- 返回已配置的席别、区间票价与区间可售座位数 `quantity`；不含品牌与可售状态，
  也没有 `Result` 包装。`quantity` 由座位段状态实时统计，与
  `ticket_inventory.remaining_tickets`（整趟数量）不是一个口径；下单仍按整趟扣减。
- 与原项目的差异：原项目用预计算的 `t_train_station_relation`，该表没有日期列，
  查询不带日期；本版直接查经停表并按上车站日期过滤。
- `GET /api/trains` 保持原样，仍按固定起终点和始发日期查询，本模块未改动。

## 席别与区间票价

`GET /api/ticket-service/ticket/query` 的每个车次元素在原有六字段后新增
`seatClassList`，例如：

```json
{"trainId":"7","trainNumber":"G35","departure":"北京南","arrival":"南京南",
 "departureTime":"2026-11-03T07:00:00","arrivalTime":"2026-11-03T09:30:00",
 "seatClassList":[{"type":0,"price":550.00},{"type":2,"price":180.00}]}
```

- `type` 是席别编码：0 商务座、1 一等座、2 二等座。代码侧集中定义在
  `SeatType` 枚举，数据库用 `CHECK (seat_type IN (0, 1, 2))` 兜底。
- 上面这段是 V8 之前的真实响应；V8 之后每个元素还带 `quantity`，含义见
  “车厢与座位模型”。
- `price` 是元金额，两位小数，由整数“分”精确换算（`BigDecimal.valueOf(cents, 2)`），
  不做舍入。原项目是 `price / 100` 保留一位小数，123.45 会变成 123.5。
- 元素按 `type` 升序；只返回已配置票价的席别。整段未配价时车次保留、
  `seatClassList` 为 `[]`，空数组不代表零元或售罄。
- 不返回 `quantity`、`candidate`，不复用整趟库存，也不提供票价写入接口。
- 计价维度是“运行实例 + 上车站序 + 下车站序 + 席别”，四者合起来唯一：

```text
train_trip_interval_prices
  (train_trip_id, departure_sequence, arrival_sequence, seat_type)  UNIQUE
  price_cents INT NOT NULL CHECK (price_cents > 0)
  CHECK (arrival_sequence > departure_sequence)
  FK (train_trip_id, departure_sequence) -> train_trip_stops (train_trip_id, stop_sequence)
  FK (train_trip_id, arrival_sequence)   -> train_trip_stops (train_trip_id, stop_sequence)
```

- 两个站序就是 `train_trip_stops.stop_sequence`，即这趟车运行里的第几站；
  价格行引用别的车次的站序会被复合外键拒绝。
- 查询时先按区间取车次，再用一条
  `WHERE (train_trip_id, departure_sequence, arrival_sequence) IN (...)` 批量取价，
  不逐车次查。该条件正好落在唯一键前缀上。
- 与原项目的差异：原表 `t_train_station_price` 只有 `train_id` 单列索引、没有唯一键，
  价格可为 NULL，起终站存站名，`pageListTicketQueryV1` 对每个车次单独查一次价。
  本版按站序引用、四列唯一、价格非空为正，并一次批量读取。
- 迁移 V6 只建表，不回填；既有车次不会自动获得票价，也不会进入订单金额。

2026-10-31 用户使用真实 `curl.exe` 请求验证 G35（实例 408，2026-11-03）：
北京南 VNP→南京南 NKH 返回 200，席别 0/1/2 分别为 123.45/56.78/30.00 元；
北京南 VNP→宁波 NGH 同样返回 200、保留车次，`seatClassList` 为 `[]`。
上述金额仅为本地演示值，不是铁路实际票价；本次证据覆盖两个成功查询，
不代表真实 HTTP 错误分支、并发、区间库存或订单已验证。

## 车厢与座位模型

V8 建三张表，按运行实例（`train_trips.id`）归属，不按车号模板派生：

```text
train_carriages   (train_trip_id, carriage_number) UNIQUE
train_seats       (train_trip_id, carriage_id, seat_number) UNIQUE
                  FK (train_trip_id, carriage_id) -> train_carriages
                  CHECK seat_type IN (0, 1, 2)
train_seat_segments (train_trip_id, seat_id, segment_sequence) UNIQUE
                  FK (train_trip_id, seat_id) -> train_seats
                  FK (train_trip_id, segment_sequence) -> train_trip_stops
                  CHECK segment_sequence > 0
                  CHECK seat_status IN ('AVAILABLE', 'OCCUPIED')
```

- 段 `segment_sequence = s` 表示区间 `[s, s+1)`，一个座位在一个运行实例里
  有「站数 − 1」行。段号外键只保证起点站真实存在，终点站是否存在由数据准备负责。
- 除三个业务唯一键外，`train_carriages` 与 `train_seats` 各有一个
  `(train_trip_id, id)` 辅助唯一键，专门作为上面两个复合外键的引用目标。
  三条 CHECK 为 `chk_train_seats_seat_type`、`chk_train_seat_segments_sequence`、
  `chk_train_seat_segments_status`。
- 计算可售数时，所需段数取「下车站序 − 上车站序」，因此要求站序连续。经停表允许
  `1、2、10、11` 这类不连续站序，此时 `2→10` 虽是相邻两站，却会被要求 8 段，
  结果保守少算为 0。当前不对不连续站序做特殊处理，也不重编号历史数据。
- 可售数量：区间 `[a, b)` 内每一段都 `AVAILABLE` 的座位才计入。查询按
  「区间 + 座位」分组后要求 `COUNT(*)` 等于跨度 `b - a`，用等号而非大于等于；
  因此缺一行会少算（少卖），不会误判成可售（超卖）。
- 重叠判定：座位已被 `[c, d)` 占用时，再卖 `[a, b)` 冲突当且仅当 `a < d` 且
  `c < b`，共端点（`b == c`）不冲突，所以相邻区间可以复用同一座位。
- 查询响应的每个席别元素为 `type`、`price`、`quantity`。未配置票价的席别不出现；
  配置了票价但没有座位数据时 `quantity` 为 0，表示不可售，不是未知。
- 与原项目差异：原仓库 `t_seat` 按「座位 × 全部 OD 组合」物化一行且没有日期列，
  余票靠 `count(*)` 加 Redis 计数；本版按相邻段存状态并实时统计，不引入缓存。

本模块的查询只读；示例车厢与座位需手工 SQL 准备。V9 之后，启用座位售票的
实例已按下单区间占用段并在取消或关单时按订单释放，见“区间座位购票与释放”；
未启用的实例仍按 `ticket_inventory` 整趟扣减。查询显示的 `quantity` 在座位实例
上与下单同源，在数量实例上不代表下单一定能成交。索引未经 `EXPLAIN` 验证，
也没有性能测试。开发库已于 2026-11-02 迁到 V8，依据是用户提供的启动日志
（`Current version ... 8`、`up to date`）与迁移历史查询输出；助手未连库复核。

## 计价下单与金额快照

`POST /api/orders/priced` 接收 `trainTripId`、`passengerId`、`seatType`（0 商务座、
1 一等座、2 二等座），要求登录与 `Idempotency-Key`；首次创建 201，同键重试 200。
请求体不接收金额，客户端额外携带 `amount` 等字段会被忽略，不参与定价。

服务端取该运行实例的最小与最大站序，核对首站名与末站名等于 `train_trips`
的起终站、首站发车时刻与末站到达时刻等于车次的到发时刻，再按
（运行实例＋首站序＋末站序＋席别）精确查价。不累加相邻区间价格，不复用
部分区间价或其他运行实例的价格，也不接受客户端报价。

失败分支：车次不存在 404；乘车人不存在或不属于当前账号 404；
经停缺失、不足两站或与车次起终站/时刻不一致 409“该车次全程经停资料不完整，无法下单”；
所选席别在全程区间未配价 409“该车次全程席别未配置票价”；库存缺失 404、
余票不足 409 沿用原有语义。失败不写订单、不扣库存、不留幂等占位。

订单保存全程上下车站序、上下车站码与站名、上下车到发时刻、`seat_type`
和 `amount_cents`（正整数分）。创建、列表、详情、支付和取消响应新增
`seatType` 与 `amount`（元，两位小数，`BigDecimal.valueOf(cents, 2)`）。
旧入口创建的订单和历史订单这两个字段为 `null`，表示当时未记录，
不能解释为零元或默认席别。

幂等键比较包含购买模式，旧键拿到新入口、或同键改席别均返回 409。
成功重试直接返回原订单与当时金额，即使票价已改或已删除，也不重新定价、
不再扣票。支付、取消和超时关单只改状态，不修改快照；取消与关单仍只恢复一次库存。

三席别仍共用 `ticket_inventory` 的整趟数量。席别目前只参与定价与快照，
没有按席别容量、车厢座位或区间复用；查票报价不锁定，也没有报价有效期。
这些是本地过渡方案，不是原仓库的完整库存实现。

迁移 V7 为订单表和幂等表新增购买模式列及订单快照列。旧记录按 `LEGACY`
且快照全空保留，不回填席别或金额；数据库约束区分“旧单快照必须全空”与
“计价单快照必须完整”。

## 区间座位购票与释放

`POST /api/orders/seat` 接收 `trainTripId`、`passengerId`、`fromStation`、
`toStation`（均为 `stations.code`）和 `seatType`，要求登录与 `Idempotency-Key`；
首次创建 201，同键成功重试 200。请求不接收金额或座位号。

区间解析：按站码在同一运行实例的经停中定位上下车站，要求方向正确、两站各自
只出现一次、站序连续、上车点有发车时刻且下车点有到达时刻。同站、反向、站码
不存在或不在经停返回 400；同一车次多次经过所购车站返回 400“该车次多次经过
所购车站”；缺时刻或站序不连续返回 409“该车次经停或座位资料不完整”。

定价：按（实例、上车站序、下车站序、席别）精确读取票价，不累加相邻区间、
不回退其他实例或席别；缺价返回 409“该区间席别未配置票价”。订单保存实际
读取的金额与完整行程快照。

分座：在该实例、该席别中选第一张「区间每一段都空闲且段行齐全」的座位，
按车厢号、座位号、座位 id 排序。没有候选时区分三种原因：该席别未配置座位、
座位段行不完整、该区间已无可用座位。

占用：订单先插入，再用条件更新把目标区间内的段改为 `OCCUPIED` 并写入
`order_id`；影响行数必须等于区间跨度，否则整体回滚。段状态只有 `AVAILABLE`
与 `OCCUPIED`：未支付和已支付都占用，支付不释放座位，也不改金额与行程快照。

释放：取消与超时关单按购买模式分支。座位订单执行
`UPDATE ... SET seat_status = 'AVAILABLE', order_id = NULL WHERE order_id = ?`，
影响行数必须等于该单跨度；旧订单仍按 `ticket_inventory` 恢复一张。只有状态
条件更新成功的那一次才释放，重复取消、重复关单和并发竞争不会多释放。

幂等：作用域仍是账号＋幂等键，比较字段包含实例、乘车人、席别与起止站码。
同键换区间、换席别或跨入口返回 409；成功重试直接返回原订单，不查价、不分座、
不占段，票价删除或座位已占都不影响重试。

订单新增 `carriageNumber`、`seatNumber` 快照。创建、列表、详情、支付、取消
响应都带上下车站码与站名、到发时刻、席别、车厢号、座位号和金额；旧订单这些
字段为 `null`，不表示分配了默认座位。

### 数量售票与座位售票的切换

`train_trips.sales_mode` 取 `QUANTITY`（默认）或 `SEAT`。座位入口只接受
`SEAT` 实例；旧两个入口在 `SEAT` 实例上创建新订单返回 409。两个方向都在
车次行锁下检查，同一次运行不会用两套库存出售。

切换是受控运维步骤，没有管理接口。使用带守卫的单语句，存在待支付或已支付
订单时影响 0 行、模式不变：

```sql
UPDATE train_trips
SET sales_mode = 'SEAT'
WHERE id = ?
  AND sales_mode = 'QUANTITY'
  AND NOT EXISTS (
      SELECT 1 FROM ticket_orders o
      WHERE o.train_trip_id = ?
        AND o.status IN ('PENDING_PAYMENT', 'PAID')
  );
```

切换前需准备车厢、座位、段行与区间票价；配置不齐会在下单时报结构错误，不会
超卖。`SEAT` 为单向，本版不提供回退。

V9 新增 `sales_mode`、订单座位列与 `seat_id` 复合外键、幂等起止站码、段归属
列与配对 CHECK，并把本轮之前手工标记的 `OCCUPIED` 段重置为 `AVAILABLE`
（此前没有订单持有段）。表数量不变。

## 测试

保持本地 MySQL 运行，在项目根目录执行：

```powershell
mvn test
```

测试通过 `test` profile 连接本地 `mini_12306_test`，使用
`MYSQL_TEST_PASSWORD` 和受限账号。执行前检查 `.env` 已配置该密码，
且 MySQL 测试库与用户已按上文创建。

测试启动会核实预期数据源配置和实际数据库/用户；不匹配时在迁移前阻止
应用上下文启动。测试会检查 Flyway 版本历史（迁移版本 1–9）、十三张表（含 Flyway
历史表）、订单列表索引、支付期限列非空和关单扫描索引的列顺序，以及车站 code
唯一键、经停 `(train_trip_id, stop_sequence)` 唯一键和经停的两个外键，
票价表四列唯一键、两个复合外键与三条 CHECK。
订单表与幂等表还检查购买模式列非空、快照列可空，以及
`chk_ticket_orders_*`、`chk_order_idempotency_*` 约束名单。
车厢、座位与座位段检查唯一键、复合外键、CHECK 名称与启用状态；
V9 之后还检查订单座位列可空、座位复合外键与索引、段归属外键，
以及 `sales_mode` 非空。
集成测试会创建随机账号、乘车人、车次、库存和订单，部分数据会保留；
测试专用 V2 只在首次迁移时插入固定参考车次和初始库存，不会每次运行时
重置库存。

覆盖账号隔离、订单状态转换、库存回滚及固定并发场景，
以及订单分页、越界空页、分页参数校验、账号总量隔离、下单幂等、
支付超时关单、乘车人删除、车站经停查询、区间车次查询、席别区间票价
和计价下单。

计价下单用例覆盖：服务端按全程区间与席别取价并写完整快照；客户端携带金额
不改变成交价；部分区间价、其他席别价都不串；两个实例同站序同席别配 5.55 与
7.77 两档价时各自取价，第三个同站序同席别但无全程价的实例仍 409 且无副作用；
123.45 与 5.55 的两位小数精度；经停缺失、单站、起终站被改时 409 且不扣库存、不留订单与幂等记录；
改价与删价后同键仍返回原金额；同键换席别、跨入口复用返回 409；旧入口保持
无金额且旧幂等可重试；创建、列表、详情、支付、取消响应快照一致；支付与取消
不改快照，取消和超时关单只恢复一次库存；用 spy 在真实 INSERT 前抛异常时，
库存、订单与幂等占位一起回滚（这不等于真实写入后回滚已专项验证）；
8 线程同键只成一单，10 线程抢 3 张票只成功 3 单。

2026-10-31 用户先运行 `mvn "-Dtest=PricedOrderIntegrationTests" test`，9 项通过；
随后 `mvn test` 全量 116 项通过，0 失败、0 错误、0 跳过（原有 106 项、
新增结构断言 1 项、计价下单 9 项）。按主对话复核意见补强跨实例串价用例后，
22:15–22:17 重跑定向与全量，结果相同。证据为控制台输出与
`target/surefire-reports` 下 13 份 XML，合计 116 项。

车站经停用例覆盖字典字段与 code 升序、经停乱序插入后仍按数值站序返回、
跨日时刻与停留分钟、首末站空值、未知车次 404、旧车次空数组、
`trainId` 缺失/非数字/非正数 400、匿名访问不创建会话，
以及重复站序、重复站点 code、错误外键和站序非正数的约束失败。

区间车次用例覆盖始发到终到、相邻站与中途站上下车、跨日到发时刻、
反向查询为空、同站 400、上车站日期口径（跨日车次只命中上车站当天）、
乱序插入后按发车时刻与车次号排序、合法站点无车次的空数组、
未知 code 400、缺参与格式错误 400 且不回显原始输入，
以及匿名访问不创建会话；未配置票价时，车次保留且 `seatClassList` 为空数组。

席别票价用例覆盖三类席别按 `type` 升序、123.45/56.78/30.00 的两位小数精度、
同一车次不同区间互不串价、同车号不同运行实例各取己价、整段未配价返回空数组、
部分配价只含已配置席别，以及重复、反向或同站、本趟不存在的站序、
非法席别和非正价格被数据库约束拒绝。

关单相关用例覆盖期限写入、未到期不关单、到期关单与库存只恢复一次、
过期后支付被拒、截止时刻边界、过期后不可取消、已支付或已取消不被关单、
幂等重试返回已关闭订单、批次扫描以及 8 线程同单只关闭一次。
测试不等待 30 分钟，而是直接改写 `pay_deadline` 制造过期状态；
批次用例会把测试库中至多 100 张历史待支付单一起关闭。

2026-11-02 用户运行 `mvn test` 全量 124 项通过、0 失败、0 错误、0 跳过
（原 116 项，新增座位 7 项与结构断言 1 项）。座位用例覆盖跨段必须全空才可售、
相邻区间复用同一座位、缺段行导致少算、无座位配置返回 0、多席别分别计数并按
`type` 升序，以及唯一键、状态 CHECK、段号外键与跨运行实例车厢复合外键的拒绝。
结构断言同步为迁移版本 1–8、十三张表（含 Flyway 历史表）。

## 当前边界

本项目用于本地学习。业务日期时间统一按北京时间（UTC+08:00）解释，
使用 MySQL DATETIME 和 Java LocalDateTime 保存与传递。
MySQL 默认时区已配置为 +08:00；修正前生成的历史时间尚未迁移。

2026-10-30 用户核对开发库迁移结构后，将 Flyway baseline 登记为版本 5。此前
开发库已有的 `pay_deadline` 值均等于 `created_at` 加 30 分钟；311 笔待支付单
在登记前均已过期，用户同意按超时规则处理。普通配置启动后，用户观察到这
311 笔全部转为 `EXPIRED`；订单总数仍为 396。登记 baseline 时站点表、经停表
与幂等记录均为 0，baseline 本身不会写入示例车站或经停数据；此后用户已手工
准备 G35 及五站经停（见上节），幂等记录数量在该时点之后未再查询。

开发库 `mini_12306` 已于 2026-10-31 迁到 V6。此前用户在 IDE 里手工建过同名
空表，首次迁移因“表已存在”失败；删除该空表和失败记录后由 Flyway 重建并登记。
迁移后首次核对时票价表为 0 行；后续用户提供的真实 HTTP 响应已显示
G35 北京南→南京南的三类演示票价，而北京南→宁波仍返回空 `seatClassList`
（见“席别与区间票价”）。2026-10-31 21:48 迁移前核对票价表为 3 行，21:54 迁移后复核仍为 3 行；仍没有票价写入接口。
开发库已于 2026-10-31 执行 V7（版本 6→7），迁移前备份在用户目录；
迁移后核对新列 11 个、两表 CHECK 10 条，订单、幂等、票价与余票计数均未变化。
全程演示票价尚未补充；已有的北京南→南京南三类演示票价仍保留（3 行），
408 全程计价仍会返回“未配置票价”。`ticket_inventory` 仍是整趟数量，下单仍按整趟扣减；区间可售座位数已能查询，
但座位占用、并发锁定与释放尚未实现。开发库已于 2026-11-02 迁到 V8（依据用户提供的
启动日志与查询输出，未做恢复演练）；迁移前备份 278208 字节位于用户目录。
计价订单保存席别与金额快照，旧入口和历史订单对应字段仍为空。

超时关单只做到单实例定时扫描，没有补偿任务、消息队列或多实例协调；
停机期间到期的订单依赖下次启动后的扫描。关单日志只记录订单 ID，
未针对日志脱敏做专项检查。

已统一 MVC 错误响应，保留 message 字段与原有业务 HTTP 状态码；
通过虚构输入验证参数校验错误响应及当前配置下的控制台日志不回显敏感输入。
该验证不代表所有日志来源均已脱敏。

浏览器接入和部署所需的 CSRF 防护与 Cookie 配置尚待完善。

乘车人一旦被订单引用就无法删除，本版不提供先处理订单再删除的流程；
删除与下单并发时没有专项用例，只由外键约束兜底。

2026-11-03：区间座位购票、占座与释放已在代码中实现，测试库与开发库均迁到
V9，全量回归 144 项通过。开发库备份后迁移，并用新建的 D9001 演示实例走了
真实 HTTP 闭环：全程二等座数量 3→2→3，取消后座位释放。
座位入口只对 `sales_mode = 'SEAT'` 的实例开放，旧入口在这些实例上禁止创建
新订单；`QUANTITY` 实例保持整趟数量扣减。查询接口未按模式区分口径：座位实例
上 `quantity` 与下单同源，数量实例上仍是座位段计数，可能与该实例的
`ticket_inventory` 不一致。

座位未引入 `LOCKED` 状态，未支付与已支付都由段行的 `OCCUPIED` 表示；没有
多乘车人、退款、用户指定座位、Redis 或消息队列。切换实例需要手工执行带守卫
的 `UPDATE`，并要求没有待支付或已支付订单；本轮要求站序连续，不连续站序会
保守拒绝。`train_seat_segments` 的 `order_id` 归属由 CHECK 约束保证，未做
跨订单释放的专项压力测试。
