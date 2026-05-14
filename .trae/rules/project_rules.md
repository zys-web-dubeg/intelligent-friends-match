项目级Java后端开发规范

强制技术栈

框架：Spring Boot 3.x；ORM：MyBatis-Plus 3.5.x；工具：Lombok、Swagger3、Validation、SLF4J
测试：JUnit 5、Mockito；构建：Maven；数据库：MySQL 8.0+

标准包结构

com.xxx.project
├── controller    # REST接口层，对外提供API
├── service       # 业务层，接口+实现类分离
├── mapper        # 数据访问层，MyBatis-Plus Mapper接口
├── entity        # 数据库实体类，配MP+Lombok注解
├── dto           # 请求数据传输对象，接收前端参数
├── vo            # 视图对象，返回前端数据
├── config        # 项目配置类，如Swagger、MP分页、跨域配置
├── exception     # 自定义异常+全局异常处理器
└── util          # 通用工具类

核心代码规范

1. 命名规范：类大驼峰，方法/变量小驼峰，常量全大写下划线分隔，数据库表/字段下划线命名

2. 实体类：必加@TableName、@TableId等MP注解，禁用手动getter/setter

3. Controller层：@RestController开发，加Swagger接口注解，参数用@Valid

4. Service层：业务接口与实现分离，@Transactional控制事务，区分读写事务

5. 通用规则：禁用System.out.println，统一用日志打印；异常全局捕获处理；核心接口单元测试覆盖率≥80%

AI代码生成要求

1. 按表DDL生成代码时，必包含Entity、Mapper、Service、Controller、单元测试全套CRUD

2. 分页查询使用MyBatis-Plus分页插件，接口参数校验齐全

3. 代码注释完整，符合项目规范，可直接集成到项目使用