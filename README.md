# mini-12306

参考 nageoffer/12306，用于学习 Spring Boot、MyBatis、MySQL、事务与并发控制。

## 功能与范围

- 用户注册、登录、当前用户查询和退出登录。
- 乘车人新增、查询、修改及账号隔离。
- 按日期和固定起终站查询示例车次。
- 余票查询、创建订单、订单列表分页和详情。
- 模拟支付、取消订单及恢复库存。
- 条件扣减防止超卖，事务保证订单与库存的一致性。

第一版采用单体应用，仅支持全程票、一种席别、一位乘车人一张票。
不分配具体座位，不接入真实支付；退款和超时关单尚未实现。

参考项目：https://github.com/nageoffer/12306

## 环境

- Java 17
- Maven 3.9.x（本地使用 3.9.14）
- Docker 与 Docker Compose
- Spring Boot 4.0.8
- MySQL 8.4（由 Compose 启动）

## 首次初始化

### 1. 配置本地凭据

在项目根目录创建 `.env`，填写自己的本地数据库密码：

```properties
MYSQL_ROOT_PASSWORD=replace_with_your_local_password
```

`.env` 已被 Git 忽略，不应提交真实密码。
Docker Compose 和 Spring Boot 分别读取此文件。

### 2. 启动数据库

```powershell
docker compose up -d
docker compose logs --tail 30 mysql
```

等待 MySQL 日志出现 `ready for connections`。

数据库名为 `mini_12306`，本机端口为 `3307`，容器端口为 `3306`。
数据保存在命名卷 `mysql_data` 中。

### 3. 初始化表与示例数据

以下脚本只在空库首次执行，不能在已初始化的库中整份重跑。

```powershell
docker compose cp ./src/main/resources/db mysql:/tmp/mini-12306-db

docker compose exec mysql mysql --default-character-set=utf8mb4 -uroot -p mini_12306
```

输入 `.env` 中配置的密码，在 MySQL 命令行执行：

```sql
SELECT DATABASE();

SOURCE /tmp/mini-12306-db/users.sql;
SOURCE /tmp/mini-12306-db/passengers.sql;
SOURCE /tmp/mini-12306-db/train_trips.sql;
SOURCE /tmp/mini-12306-db/ticket_inventory.sql;
SOURCE /tmp/mini-12306-db/ticket_orders.sql;

SHOW TABLES;
exit
```

确认当前数据库为 `mini_12306` 后再执行脚本，任一步报错先停止处理。

初始化后应有五张表、四条示例车次及四条库存记录，
每趟车初始余票为 20；用户、乘车人和订单表为空。

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

先完成数据库初始化，并保持 MySQL 运行，再执行：

```powershell
mvn test
```

测试连接本地 `mini_12306` 数据库，连通性测试会断言数据库名称。
部分集成测试保留随机生成的数据，不应对生产数据库运行。

覆盖账号隔离、订单状态转换、库存回滚及固定并发场景，
以及订单分页、越界空页、分页参数校验和账号总量隔离。

## 当前边界

本项目用于本地学习。业务日期时间统一按北京时间（UTC+08:00）解释，
使用 MySQL DATETIME 和 Java LocalDateTime 保存与传递。
MySQL 默认时区已配置为 +08:00；修正前生成的历史时间尚未迁移。

已统一 MVC 错误响应，保留 message 字段与原有业务 HTTP 状态码；
通过虚构输入验证参数校验错误响应及当前配置下的控制台日志不回显敏感输入。
该验证不代表所有日志来源均已脱敏。

浏览器接入和部署所需的 CSRF 防护与 Cookie 配置尚待完善。
