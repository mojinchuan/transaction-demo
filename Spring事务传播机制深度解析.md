# Spring事务传播机制深度解析：REQUIRED vs NESTED vs REQUIRES_NEW实战指南

## 前言

在Spring开发中，事务管理是保证数据一致性的核心机制。而事务的传播行为（Propagation Behavior）更是面试和实际开发中的高频考点。今天我们就通过一个完整的实战项目，深入理解**REQUIRED**、**NESTED**和**REQUIRES_NEW**三种传播类型的区别，并解决在实际使用中遇到的各种坑。

## 一、什么是事务传播机制？

事务传播机制定义了**当一个事务方法被另一个事务方法调用时，事务应该如何传播**。Spring提供了7种传播行为：

### Spring的7种事务传播类型

| 传播类型 | 说明 | 使用频率 |
|---------|------|----------|
| **REQUIRED** | 如果当前存在事务，则加入该事务；否则创建新事务（默认） | ⭐⭐⭐⭐⭐ |
| **SUPPORTS** | 如果当前存在事务，则加入该事务；否则以非事务方式执行 | ⭐⭐ |
| **MANDATORY** | 如果当前存在事务，则加入该事务；否则抛出异常 | ⭐ |
| **REQUIRES_NEW** | 总是创建新事务，如果当前存在事务，则挂起当前事务 | ⭐⭐⭐ |
| **NOT_SUPPORTED** | 以非事务方式执行，如果当前存在事务，则挂起当前事务 | ⭐ |
| **NEVER** | 以非事务方式执行，如果当前存在事务，则抛出异常 | ⭐ |
| **NESTED** | 如果当前存在事务，则在嵌套事务中执行（使用保存点）；否则创建新事务 | ⭐⭐ |

### 🍚 生活化类比：下班回家吃饭

为了让你更好地理解这7种传播类型，我们来用一个**下班回家吃饭**的场景来类比：

假设你下班回家，老婆在家做饭。不同的传播类型就像不同的"吃饭策略"：

#### 0️⃣ REQUIRED - "反正就是要吃饭"
> **如果老婆做饭了，你就吃老婆做的饭；如果老婆没有做饭，你就自己做饭吃。**
> 
> 💡 **核心：** 反正你就是要吃饭（反正要在事务中运行）

#### 1️⃣ SUPPORTS - "随缘吃饭"
> **如果老婆做饭了，你就吃老婆做的饭；如果老婆没有做饭，你就不吃了。**
> 
> 💡 **核心：** 不一定非要吃饭（不一定非要在事务中运行）

#### 2️⃣ MANDATORY - "家暴男式吃饭"
> **非要吃老婆做的饭，老婆要是没有做饭，你就大发脾气！**
> 
> 💡 **核心：** 必须在已有事务中运行，否则抛异常（典型的家暴男😂）

#### 3️⃣ REQUIRES_NEW - "独立做饭"
> **劳资非要吃自己做的饭，就算老婆把饭做好了，我也不吃老婆做的！**
> 
> 💡 **核心：** 总要开启新事务，挂起现有事务（我行我素）

#### 4️⃣ NOT_SUPPORTED - "绝食抗议"
> **劳资就是不吃饭，就算老婆把饭做好了，我也不吃！**
> 
> 💡 **核心：** 以非事务方式运行，挂起现有事务（坚决不吃饭）

#### 5️⃣ NEVER - "暴躁绝食"
> **老子就是不吃饭，如果老婆把饭做好了，我还要发脾气！**
> 
> 💡 **核心：** 必须以非事务方式运行，如果存在事务则抛异常（暴躁老哥😤）

#### 6️⃣ NESTED - "分碗吃饭"
> **如果老婆做饭了，你就吃老婆做的饭，但是单独盛到自己碗里，避免把自己的口腔微生物传染到桌上的菜碗；如果老婆没有做饭，你就自己做饭吃。**
> 
> 💡 **核心：** 在嵌套事务中运行（使用保存点），与REQUIRED的区别是把异常影响限制在嵌套事务中（讲究卫生🥗）

**其中最常用的是：**
- **REQUIRED**（默认）：适合大多数场景
- **REQUIRES_NEW**：需要独立事务的场景
- **NESTED**：需要隔离内层异常的场景

本文重点讲解这三种最常用的传播类型。

## 二、核心问题：方法A调用方法B，异常处理对回滚的影响

这是我们最关心的场景：

```
方法A（@Transactional）
  ├─ 插入数据1
  ├─ 调用方法B（@Transactional）
  │   ├─ 插入数据2
  │   └─ 抛出异常（或不抛出）
  └─ 插入数据3（可能执行，可能不执行）
```

**关键问题：**
1. 如果方法A捕获了方法B的异常，数据会怎样？
2. 如果方法A不捕获异常，数据会怎样？
3. REQUIRED、NESTED和REQUIRES_NEW有什么区别？

---

## 三、技术栈详解：从JDBC到Spring Data JPA

在深入事务之前，我们先理清Java持久化技术的演进关系。

### 3.1 持久化技术层次图

```
应用层
  ├── Spring Data JPA (最高层抽象)
  │   └── 基于 JPA 规范
  │       └── 实现：Hibernate、EclipseLink等
  │           └── 底层：JDBC
  │
  ├── MyBatis (独立的ORM框架)
  │   └── 底层：JDBC
  │
  └── Spring JDBC (JDBC的轻量级封装)
      ├── JdbcTemplate
      └── NamedParameterJdbcTemplate
          └── 底层：原生JDBC

基础层
  └── JDBC (Java Database Connectivity)
      └── 数据库驱动 (MySQL Driver、H2 Driver等)
          └── 数据库 (MySQL、H2、Oracle等)
```

### 3.2 各技术详解

#### 1️⃣ **JDBC (Java Database Connectivity)**
- **性质**：Java官方标准API（java.sql包）
- **作用**：提供Java程序访问数据库的统一接口
- **特点**：
  - ✅ 所有Java数据库操作的基础
  - ✅ 轻量级，性能最好
  - ❌ 需要手动编写SQL
  - ❌ 需要手动处理结果集映射
  - ❌ 代码冗长，容易出错

**示例代码：**
```java
// 原生JDBC写法 - 繁琐
Connection conn = dataSource.getConnection();
PreparedStatement ps = conn.prepareStatement(
    "SELECT * FROM account WHERE id = ?"
);
ps.setLong(1, 1L);
ResultSet rs = ps.executeQuery();
Account account = new Account();
if (rs.next()) {
    account.setId(rs.getLong("id"));
    account.setName(rs.getString("name"));
    account.setBalance(rs.getDouble("balance"));
}
rs.close();
ps.close();
conn.close();
```

#### 2️⃣ **Spring JDBC**
Spring对JDBC的轻量级封装，简化了资源管理和异常处理。

##### **JdbcTemplate**
- 使用位置参数（`?`）
- 自动管理连接、语句和结果集的关闭
- 统一的异常体系

```java
// JdbcTemplate写法
String sql = "INSERT INTO account (name, balance) VALUES (?, ?)";
jdbcTemplate.update(sql, "张三", 1000.0);

// 查询
List<Account> accounts = jdbcTemplate.query(
    "SELECT * FROM account", 
    new BeanPropertyRowMapper<>(Account.class)
);
```

##### **NamedParameterJdbcTemplate** ⭐推荐
- 使用命名参数（`:name`），更清晰
- 支持Map和对象作为参数源
- 特别适合多参数SQL

```java
// NamedParameterJdbcTemplate写法
String sql = "INSERT INTO account (name, balance) VALUES (:name, :balance)";
Map<String, Object> params = new HashMap<>();
params.put("name", "张三");
params.put("balance", 1000.0);
namedJdbcTemplate.update(sql, params);

// 或使用SqlParameterSource
SqlParameterSource params = new MapSqlParameterSource()
    .addValue("name", "张三")
    .addValue("balance", 1000.0);
namedJdbcTemplate.update(sql, params);
```

**对比：**
| 特性 | JdbcTemplate | NamedParameterJdbcTemplate |
|------|--------------|---------------------------|
| 参数类型 | 位置参数 `?` | 命名参数 `:name` |
| 可读性 | 一般 | ✅ 更好 |
| 参数复用 | ❌ 困难 | ✅ 轻松 |
| 适用场景 | 简单SQL | 复杂SQL、多参数 |

#### 3️⃣ **JPA (Java Persistence API)**
- **性质**：Java官方ORM规范（JSR 338），不是具体实现
- **作用**：定义对象关系映射的标准接口
- **特点**：
  - ✅ 标准化，可移植性强
  - ✅ 面向对象操作，无需写SQL
  - ✅ 自动管理实体生命周期
  - ❌ 学习曲线较陡
  - ❌ 复杂查询不够灵活

**常见实现：**
- **Hibernate**（最流行，Spring Boot默认）
- EclipseLink
- OpenJPA

```java
// JPA写法
@PersistenceContext
EntityManager em;

Account account = em.find(Account.class, 1L); // 根据ID查询
account.setBalance(2000.0);
em.merge(account); // 更新
```

#### 4️⃣ **Spring Data JPA**
- **性质**：Spring对JPA的进一步抽象和简化
- **作用**：减少样板代码，提供Repository模式
- **特点**：
  - ✅ 几乎不需要写实现代码
  - ✅ 方法名自动生成查询
  - ✅ 支持分页、排序
  - ✅ 与Spring生态完美集成
  - ⚠️ 底层还是JPA（通常是Hibernate）

```java
// Spring Data JPA写法
public interface AccountRepository extends JpaRepository<Account, Long> {
    // 方法名自动生成SQL，无需实现
    List<Account> findByName(String name);
    List<Account> findByBalanceGreaterThan(Double balance);
    
    // 自定义查询
    @Query("SELECT a FROM Account a WHERE a.balance > :balance")
    List<Account> findRichAccounts(@Param("balance") Double balance);
}

// 使用
@Autowired
AccountRepository repo;

List<Account> accounts = repo.findByName("张三");
```

#### 5️⃣ **MyBatis**
- **性质**：独立的持久层框架（Apache基金会）
- **作用**：SQL映射框架，介于JDBC和全自动ORM之间
- **特点**：
  - ✅ SQL可控性强
  - ✅ 灵活性高，适合复杂查询
  - ✅ 学习成本低
  - ✅ 性能好
  - ❌ 需要手动编写SQL（XML或注解）
  - ❌ 不是JPA标准，不可移植

```java
// MyBatis Mapper接口
@Mapper
public interface AccountMapper {
    @Select("SELECT * FROM account WHERE id = #{id}")
    Account findById(Long id);
    
    @Insert("INSERT INTO account(name, balance) VALUES(#{name}, #{balance})")
    void insert(Account account);
    
    @Update("UPDATE account SET balance = #{balance} WHERE id = #{id}")
    void update(Account account);
}

// 使用
@Autowired
AccountMapper mapper;

Account account = mapper.findById(1L);
```

### 3.3 技术选型对比

| 特性 | JDBC | Spring JDBC | JPA/Hibernate | Spring Data JPA | MyBatis |
|------|------|-------------|---------------|-----------------|---------|
| **抽象级别** | 最低 | 低 | 高 | 最高 | 中 |
| **SQL控制** | 完全控制 | 完全控制 | 自动生成 | 自动生成 | 手动编写 |
| **学习成本** | 低 | 低 | 高 | 中 | 低 |
| **开发效率** | 低 | 中 | 高 | 最高 | 中 |
| **性能** | 最好 | 很好 | 较好 | 较好 | 好 |
| **灵活性** | 最高 | 很高 | 较低 | 较低 | 高 |
| **可移植性** | 好 | 好 | 好 | 好 | 差 |
| **适用场景** | 极致性能 | 简单项目 | 企业应用 | 快速开发 | 复杂SQL |

### 3.4 选型建议

- **小型项目/简单CRUD** → Spring Data JPA（开发效率最高）
- **复杂SQL/报表系统** → MyBatis（SQL可控）
- **高性能要求** → Spring JDBC（轻量级）
- **微服务/快速迭代** → Spring Data JPA
- **传统企业系统** → MyBatis（国内生态成熟）
- **学习/理解底层** → 从JDBC开始

---

## 四、Spring事务管理器详解

### 4.1 事务管理器层次结构

```
PlatformTransactionManager (接口 - Spring事务管理核心)
    ├── DataSourceTransactionManager (JDBC事务管理器)
    ├── JpaTransactionManager (JPA事务管理器)
    ├── HibernateTransactionManager (Hibernate事务管理器)
    └── ... 其他实现
```

### 4.2 PlatformTransactionManager

**性质**：Spring事务管理的核心接口

**核心方法：**
```java
public interface PlatformTransactionManager {
    // 获取事务状态
    TransactionStatus getTransaction(TransactionDefinition definition);
    
    // 提交事务
    void commit(TransactionStatus status);
    
    // 回滚事务
    void rollback(TransactionStatus status);
}
```

**作用**：
- 统一的事务管理抽象
- 屏蔽底层事务实现差异
- 支持声明式事务（@Transactional）

### 4.3 DataSourceTransactionManager

**适用场景**：直接使用JDBC或Spring JDBC的项目

**特点：**
- ✅ 基于JDBC Connection的事务管理
- ✅ 完全支持NESTED传播（通过Savepoint）
- ✅ 轻量级，性能好
- ❌ 不支持JPA EntityManager

**配置示例：**
```java
@Configuration
@EnableTransactionManagement
public class TransactionConfig {
    
    @Bean
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        DataSourceTransactionManager tm = new DataSourceTransactionManager();
        tm.setDataSource(dataSource);
        tm.setNestedTransactionAllowed(true); // 启用嵌套事务
        return tm;
    }
}
```

### 4.4 JpaTransactionManager

**适用场景**：使用JPA/Hibernate的项目

**特点：**
- ✅ 基于JPA EntityManager的事务管理
- ✅ 与JPA生命周期集成
- ⚠️ NESTED支持有限（需要特殊配置）
- ❌ 配置复杂

**配置示例：**
```java
@Configuration
@EnableTransactionManagement
public class TransactionConfig {
    
    @Bean
    public PlatformTransactionManager transactionManager(
            EntityManagerFactory entityManagerFactory) {
        JpaTransactionManager tm = new JpaTransactionManager();
        tm.setEntityManagerFactory(entityManagerFactory);
        
        // 尝试启用保存点支持（不一定成功）
        HibernateJpaDialect jpaDialect = new HibernateJpaDialect();
        jpaDialect.setPrepareConnection(true);
        tm.setJpaDialect(jpaDialect);
        
        return tm;
    }
}
```

### 4.5 事务管理器对比

| 特性 | DataSourceTransactionManager | JpaTransactionManager |
|------|------------------------------|----------------------|
| **适用技术** | JDBC、Spring JDBC | JPA、Hibernate |
| **NESTED支持** | ✅ 完全支持 | ⚠️ 部分支持 |
| **配置复杂度** | 简单 | 复杂 |
| **性能** | 更好 | 略差 |
| **功能丰富度** | 基础 | 丰富 |
| **推荐使用** | Spring JDBC项目 | JPA项目 |

### 4.6 为什么选择DataSourceTransactionManager？

在我们的项目中，选择`DataSourceTransactionManager`的原因：

1. **完全支持NESTED传播**：通过JDBC Savepoint机制
2. **简单可靠**：配置少，不易出错
3. **性能好**：没有JPA的额外开销
4. **适合演示**：专注于事务传播，不涉及ORM复杂性

---

## 五、REQUIRED传播类型实战

### 5.1 测试场景1：捕获异常

```java
@Transactional(propagation = Propagation.REQUIRED)
public String testRequiredWithCatch() {
    // 插入第一条数据
    accountDao.insert(new Account("REQUIRED_CATCH_A", 1000.0));
    
    try {
        methodB_Required(); // REQUIRED传播
    } catch (RuntimeException e) {
        System.out.println("捕获到异常: " + e.getMessage());
    }
    
    // ❌ 这行会报错：Transaction silently rolled back
    accountDao.insert(new Account("REQUIRED_CATCH_B", 2000.0));
}

@Transactional(propagation = Propagation.REQUIRED)
public void methodB_Required() {
    accountDao.insert(new Account("REQUIRED_METHOD_B", 500.0));
    throw new RuntimeException("方法B异常");
}
```

**测试结果：**
```json
{
  "success": false,
  "error": "Transaction silently rolled back because it has been marked as rollback-only"
}
```

**结论：** ❌ 即使捕获了异常，整个事务也会回滚

**原因分析：**
1. REQUIRED传播下，方法A和方法B共享**同一个事务**
2. 方法B抛出异常时，事务被标记为`rollback-only`
3. 即使方法A捕获了异常，事务状态已无法改变
4. 任何后续的数据库操作都会失败

### 5.2 测试场景2：不捕获异常

```java
@Transactional(propagation = Propagation.REQUIRED)
public String testRequiredWithoutCatch() {
    accountDao.insert(new Account("REQUIRED_NO_CATCH_A", 1000.0));
    
    methodB_Required(); // 抛出异常，不捕获
    
    // ❌ 这行不会执行
    accountDao.insert(new Account("REQUIRED_NO_CATCH_B", 2000.0));
}
```

**测试结果：**
```json
{
  "success": false,
  "error": "方法B发生异常 - REQUIRED传播",
  "data": []  // 所有数据都回滚
}
```

**结论：** ❌ 所有数据都回滚

### 5.3 REQUIRED传播总结

| 场景 | 结果 | 原因 |
|------|------|------|
| 内层异常 + 外层catch | ❌ 全部回滚 | 事务标记为rollback-only |
| 内层异常 + 外层不catch | ❌ 全部回滚 | 异常传播导致回滚 |
| 内层正常 + 外层异常 | ❌ 全部回滚 | 同一事务，同生共死 |

**核心特点：**
- ✅ 简单直接，适合大多数场景
- ✅ 保证事务的原子性
- ❌ 无法隔离内层异常
- ❌ 一旦标记rollback-only，无法恢复

**记忆口诀：** REQUIRED = **同生共死**

---

## 六、NESTED传播类型实战

### 6.1 什么是保存点（Savepoint）？

NESTED传播的核心是**数据库保存点机制**：

```
事务开始
  ├─ 插入数据1
  ├─ 【创建保存点SP1】
  ├─ 插入数据2（嵌套事务）
  ├─ 抛出异常
  └─ 【回滚到保存点SP1】← 只回滚数据2，数据1保留
```

### 6.2 测试场景1：捕获异常

```java
@Transactional(propagation = Propagation.REQUIRED)
public String testNestedWithCatch() {
    // 插入第一条数据
    accountDao.insert(new Account("NESTED_CATCH_A", 1000.0));
    
    try {
        methodB_Nested(); // NESTED传播
    } catch (RuntimeException e) {
        System.out.println("捕获到异常: " + e.getMessage());
    }
    
    // ✅ 可以正常执行
    accountDao.insert(new Account("NESTED_CATCH_B", 2000.0));
}

@Transactional(propagation = Propagation.NESTED)
public void methodB_Nested() {
    accountDao.insert(new Account("NESTED_METHOD_B", 500.0));
    throw new RuntimeException("方法B异常");
}
```

**测试结果：**
```json
{
  "success": true,
  "message": "NESTED with catch - 只有account1和account2应该保存，methodB的数据应该回滚",
  "data": [
    {"id": 1, "name": "NESTED_CATCH_A", "balance": 1000.0},
    {"id": 3, "name": "NESTED_CATCH_B", "balance": 2000.0}
  ]
}
```

**结论：** ✅ 方法A的数据保存，方法B的数据回滚

**原因分析：**
1. 方法A开启事务
2. 方法B执行前创建**保存点**
3. 方法B抛出异常，回滚到保存点（只回滚方法B的操作）
4. 方法A捕获异常，继续执行
5. 方法A提交事务，保存点之前的数据生效

### 6.3 测试场景2：不捕获异常

```java
@Transactional(propagation = Propagation.REQUIRED)
public String testNestedWithoutCatch() {
    accountDao.insert(new Account("NESTED_NO_CATCH_A", 1000.0));
    
    methodB_Nested(); // 抛出异常，不捕获
    
    // ❌ 这行不会执行
    accountDao.insert(new Account("NESTED_NO_CATCH_B", 2000.0));
}
```

**测试结果：**
```json
{
  "success": false,
  "error": "方法B发生异常 - NESTED传播",
  "data": []  // 所有数据都回滚
}
```

**结论：** ❌ 所有数据都回滚

**原因分析：**
虽然NESTED可以回滚到保存点，但异常会继续传播到方法A，导致外层事务也回滚。

### 6.4 测试场景3：内层正常，外层异常

```java
@Transactional(propagation = Propagation.REQUIRED)
public String testNestedOuterException() {
    accountDao.insert(new Account("NESTED_OUTER_A", 1000.0));
    
    methodB_Nested_Normal(); // NESTED传播，正常执行
    
    accountDao.insert(new Account("NESTED_OUTER_B", 2000.0));
    
    // 外层抛出异常
    throw new RuntimeException("方法A异常");
}

@Transactional(propagation = Propagation.NESTED)
public void methodB_Nested_Normal() {
    accountDao.insert(new Account("NESTED_B_NORMAL", 500.0));
    // 正常执行，不抛出异常
}
```

**测试结果：**
```json
{
  "success": false,
  "error": "方法A发生异常 - 外层异常导致全部回滚",
  "data": []  // 所有数据都回滚
}
```

**结论：** ❌ 所有数据都回滚（包括内层正常执行的数据）

**原因分析：**
1. 方法B的数据虽然在保存点之后提交
2. 但外层事务最终回滚，所有操作都失效
3. **保存点只能向内回滚，不能向外隔离**

### 6.5 NESTED传播总结

| 场景 | 结果 | 原因 |
|------|------|------|
| 内层异常 + 外层catch | ✅ 内层回滚，外层保存 | 保存点机制生效 |
| 内层异常 + 外层不catch | ❌ 全部回滚 | 异常传播到外层 |
| 内层正常 + 外层异常 | ❌ 全部回滚 | 外层回滚影响所有 |

**核心特点：**
- ✅ 可以隔离内层异常（通过保存点）
- ✅ 外层可以继续执行
- ❌ 不能隔离外层异常
- ⚠️ 需要数据库支持保存点（H2、MySQL、PostgreSQL都支持）

**记忆口诀：** NESTED = **内外有别，但不能逆天**

**类比理解：**
> 保存点就像游戏中的存档点
> - 你可以读档回到之前的状态（内层回滚）
> - 但如果游戏本身崩溃了（外层异常），所有进度都会丢失

---

## 七、REQUIRES_NEW传播类型实战

### 7.1 什么是独立事务？

REQUIRES_NEW传播的核心是**完全独立的事务**：

```
外层事务（挂起）
  └─ 内层事务（独立）
      ├─ 插入数据
      ├─ 提交/回滚（不影响外层）
  └─ 外层事务（恢复）
      └─ 继续执行
```

### 7.2 测试场景1：内层正常，外层异常

```java
@Transactional(propagation = Propagation.REQUIRED)
public String testRequiresNewOuterException() {
    // 外层事务
    accountDao.insert(new Account("REQ_NEW_OUTER_A", 1000.0));
    
    // 内层独立事务（外层事务被挂起）
    methodB_RequiresNew_Normal();
    
    // 外层事务（恢复）
    accountDao.insert(new Account("REQ_NEW_OUTER_B", 2000.0));
    
    // 外层抛出异常
    throw new RuntimeException("方法A异常");
}

@Transactional(propagation = Propagation.REQUIRES_NEW)
public void methodB_RequiresNew_Normal() {
    accountDao.insert(new Account("REQ_NEW_B_NORMAL", 500.0));
    // 正常执行，独立事务提交
}
```

**测试结果：**
```json
{
  "success": false,
  "error": "方法A发生异常 - 外层异常导致外层回滚，但内层已提交",
  "data": [
    {"id": 1, "name": "REQ_NEW_B_NORMAL", "balance": 500.0}
  ]
}
```

**结论：** ✅ 方法B的数据保存，方法A的数据回滚

**原因分析：**
1. 方法A开启事务T1
2. 方法B执行时，**挂起T1，创建新事务T2**
3. 方法B执行完成，**T2独立提交**
4. 恢复T1，继续执行
5. 方法A抛出异常，**T1回滚**
6. T2已提交，不受T1回滚影响

### 7.3 测试场景2：内层异常，外层捕获

```java
@Transactional(propagation = Propagation.REQUIRED)
public String testRequiresNewInnerExceptionWithCatch() {
    accountDao.insert(new Account("REQ_NEW_CATCH_A", 1000.0));
    
    try {
        methodB_RequiresNew_Exception(); // 独立事务，抛出异常
    } catch (RuntimeException e) {
        System.out.println("捕获到异常: " + e.getMessage());
    }
    
    // ✅ 可以正常执行
    accountDao.insert(new Account("REQ_NEW_CATCH_B", 2000.0));
}

@Transactional(propagation = Propagation.REQUIRES_NEW)
public void methodB_RequiresNew_Exception() {
    accountDao.insert(new Account("REQ_NEW_B_EXCEPTION", 500.0));
    throw new RuntimeException("方法B异常");
}
```

**测试结果：**
```json
{
  "success": true,
  "message": "REQUIRES_NEW with catch - 外层数据保存，内层数据回滚",
  "data": [
    {"id": 1, "name": "REQ_NEW_CATCH_A", "balance": 1000.0},
    {"id": 2, "name": "REQ_NEW_CATCH_B", "balance": 2000.0}
  ]
}
```

**结论：** ✅ 方法A的数据保存，方法B的数据回滚

**原因分析：**
1. 方法B的独立事务回滚（不影响外层）
2. 方法A捕获异常，继续执行
3. 方法A的外层事务正常提交

### 7.4 测试场景3：内层异常，外层不捕获

```java
@Transactional(propagation = Propagation.REQUIRED)
public String testRequiresNewInnerExceptionWithoutCatch() {
    accountDao.insert(new Account("REQ_NEW_NO_CATCH_A", 1000.0));
    
    methodB_RequiresNew_Exception(); // 抛出异常，不捕获
    
    // ❌ 这行不会执行
    accountDao.insert(new Account("REQ_NEW_NO_CATCH_B", 2000.0));
}
```

**测试结果：**
```json
{
  "success": false,
  "error": "方法B发生异常 - REQUIRES_NEW传播",
  "data": []  // 所有数据都回滚
}
```

**结论：** ❌ 所有数据都回滚

**原因分析：**
1. 方法B的独立事务回滚
2. 异常传播到方法A
3. 方法A的外层事务也回滚

### 7.5 REQUIRES_NEW传播总结

| 场景 | 结果 | 原因 |
|------|------|------|
| 内层异常 + 外层catch | ✅ 内层回滚，外层保存 | 事务完全独立 |
| 内层异常 + 外层不catch | ❌ 全部回滚 | 异常传播到外层 |
| 内层正常 + 外层异常 | ✅ 内层保存，外层回滚 | 事务完全独立 |

**核心特点：**
- ✅ 创建完全独立的事务
- ✅ 内层提交后不受外层影响
- ✅ 可以隔离内层异常
- ❌ 不能阻止异常传播（除非捕获）
- ⚠️ 性能开销较大（需要挂起/恢复事务）

**记忆口诀：** REQUIRES_NEW = **各自为政，互不干涉**

**类比理解：**
> REQUIRES_NEW就像开两个独立的银行账户
> - 账户A存钱（外层事务）
> - 账户B存钱（内层独立事务）
> - 账户B的钱存进去就确定了，不受账户A影响

---

## 八、三种传播类型终极对比

### 8.1 综合对比表

| 场景 | REQUIRED | NESTED | REQUIRES_NEW |
|------|----------|--------|--------------|
| **内层异常+外层catch** | ❌ 全部回滚<br>(rollback-only) | ✅ 内层回滚<br>外层保存 | ✅ 内层回滚<br>外层保存 |
| **内层异常+外层不catch** | ❌ 全部回滚 | ❌ 全部回滚 | ❌ 全部回滚 |
| **内层正常+外层异常** | ❌ 全部回滚 | ❌ 全部回滚 | ✅ 内层保存<br>外层回滚 |
| **事务关系** | 同一事务 | 保存点 | 独立事务 |
| **性能开销** | 最小 | 较小 | 较大 |
| **使用频率** | ⭐⭐⭐⭐⭐ | ⭐⭐ | ⭐⭐⭐ |

### 8.2 选择指南

**使用REQUIRED的场景：**
- ✅ 大多数业务场景
- ✅ 需要保证原子性
- ✅ 简单的CRUD操作

**使用NESTED的场景：**
- ✅ 需要隔离内层异常
- ✅ 希望外层能继续执行
- ✅ 数据库支持保存点

**使用REQUIRES_NEW的场景：**
- ✅ 日志记录（无论主事务是否成功）
- ✅ 审计追踪
- ✅ 发送通知/邮件
- ✅ 需要立即提交的场景

### 8.3 决策流程图

```
方法A调用方法B，方法B需要事务吗？
  ├─ 否 → 不使用@Transactional
  └─ 是 ↓
    
方法B的异常应该影响方法A吗？
  ├─ 是 → 使用REQUIRED（默认）
  └─ 否 ↓
    
方法B需要立即提交吗？
  ├─ 是 → 使用REQUIRES_NEW
  └─ 否 ↓
    
只需要隔离内层异常？
  ├─ 是 → 使用NESTED
  └─ 否 → 使用REQUIRED
```

---

## 九、完整测试命令

### 9.1 启动应用

```bash
mvn spring-boot:run
```

### 9.2 测试REQUIRED传播

```bash
# 清空数据
curl -X DELETE http://localhost:8080/api/transaction/clear

# 测试1：REQUIRED + catch异常（全部回滚）
echo "=== 测试1: REQUIRED + catch ==="
curl http://localhost:8080/api/transaction/required/with-catch

# 查看数据（应该为空）
curl http://localhost:8080/api/transaction/accounts
```

### 9.3 测试NESTED传播

```bash
# 清空数据
curl -X DELETE http://localhost:8080/api/transaction/clear

# 测试2：NESTED + catch异常（只有外层保存）
echo "=== 测试2: NESTED + catch ==="
curl http://localhost:8080/api/transaction/nested/with-catch

# 查看数据（应该有NESTED_CATCH_A和NESTED_CATCH_B）
curl http://localhost:8080/api/transaction/accounts

# 清空数据
curl -X DELETE http://localhost:8080/api/transaction/clear

# 测试3：NESTED + 不catch异常（全部回滚）
echo "=== 测试3: NESTED + 不catch ==="
curl http://localhost:8080/api/transaction/nested/without-catch

# 清空数据
curl -X DELETE http://localhost:8080/api/transaction/clear

# 测试4：NESTED + 外层异常（全部回滚）
echo "=== 测试4: NESTED + 外层异常 ==="
curl http://localhost:8080/api/transaction/nested/outer-exception
```

### 9.4 测试REQUIRES_NEW传播

```bash
# 清空数据
curl -X DELETE http://localhost:8080/api/transaction/clear

# 测试5：REQUIRES_NEW + 内层正常，外层异常（只有内层保存）
echo "=== 测试5: REQUIRES_NEW + 外层异常 ==="
curl http://localhost:8080/api/transaction/requires-new/outer-exception

# 查看数据（应该只有REQ_NEW_B_NORMAL）
curl http://localhost:8080/api/transaction/accounts

# 清空数据
curl -X DELETE http://localhost:8080/api/transaction/clear

# 测试6：REQUIRES_NEW + 内层异常，外层捕获（只有外层保存）
echo "=== 测试6: REQUIRES_NEW + 内层异常 + catch ==="
curl http://localhost:8080/api/transaction/requires-new/inner-exception-catch

# 查看数据（应该有REQ_NEW_CATCH_A和REQ_NEW_CATCH_B）
curl http://localhost:8080/api/transaction/accounts

# 清空数据
curl -X DELETE http://localhost:8080/api/transaction/clear

# 测试7：REQUIRES_NEW + 内层异常，外层不捕获（全部回滚）
echo "=== 测试7: REQUIRES_NEW + 内层异常 + 不catch ==="
curl http://localhost:8080/api/transaction/requires-new/inner-exception-no-catch

# 查看数据（应该为空）
curl http://localhost:8080/api/transaction/accounts
```

### 9.5 H2控制台

访问 http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:mem:testdb`
- 用户名: `sa`
- 密码: （空）

---

## 十、常见问题与陷阱

### 10.1 自注入问题

**问题：** Service内部方法调用，事务不生效

**错误示例：**
```java
@Service
public class MyService {
    @Transactional
    public void methodA() {
        methodB(); // ❌ 直接调用，事务不生效
    }
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void methodB() {
        // ...
    }
}
```

**解决方案：** 使用`@Lazy`自注入
```java
@Service
public class MyService {
    @Autowired
    @Lazy
    private MyService self;
    
    @Transactional
    public void methodA() {
        self.methodB(); // ✅ 通过代理调用，事务生效
    }
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void methodB() {
        // ...
    }
}
```

### 10.2 JPA不支持NESTED

**问题：** 使用JpaTransactionManager时，NESTED传播报错

**解决方案：** 
1. 使用DataSourceTransactionManager
2. 或配置HibernateJpaDialect（不一定成功）
3. 或改用REQUIRES_NEW

### 10.3 H2数据库获取自增ID

**问题：** `SCOPE_IDENTITY()`函数不存在

**解决方案：** 使用GeneratedKeyHolder
```java
GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
namedJdbcTemplate.update(sql, params, keyHolder);
Long id = keyHolder.getKey().longValue();
```

### 10.4 rollback-only陷阱

**问题：** REQUIRED传播中，捕获异常后仍然报错

**原因：** 事务已被标记为rollback-only

**解决方案：**
1. 使用REQUIRES_NEW代替NESTED
2. 或在捕获异常后手动清除rollback-only标记（不推荐）

---

## 十一、最佳实践总结

### 11.1 技术选型建议

**持久化技术：**
- 快速开发 → Spring Data JPA
- 复杂SQL → MyBatis
- 简单项目 → Spring JDBC
- 学习理解 → 从JDBC开始

**事务管理器：**
- Spring JDBC → DataSourceTransactionManager
- JPA项目 → JpaTransactionManager
- 需要NESTED → DataSourceTransactionManager

### 11.2 传播类型选择

- **90%场景** → REQUIRED（默认）
- **需要隔离内层异常** → NESTED或REQUIRES_NEW
- **需要独立提交** → REQUIRES_NEW
- **日志/审计** → REQUIRES_NEW

### 11.3 编码规范

1. ✅ 优先使用声明式事务（@Transactional）
2. ✅ Service层控制事务，DAO层不控制
3. ✅ 避免在循环中频繁开启事务
4. ✅ 合理设置事务超时时间
5. ✅ 注意自注入问题（使用@Lazy）
6. ✅ 单元测试要覆盖事务场景

### 11.4 性能优化

1. 缩小事务范围（只包裹必要的代码）
2. 避免在事务中进行远程调用
3. 避免在事务中进行耗时操作
4. 合理使用REQUIRES_NEW（开销较大）
5. 批量操作放在一个事务中

---

## 十二、结语

通过这个项目，我们不仅深入理解了Spring事务传播机制的本质，还掌握了：

✅ **持久化技术栈**：从JDBC到Spring Data JPA的完整演进  
✅ **事务管理器**：PlatformTransactionManager及其实现  
✅ **三种传播类型**：REQUIRED、NESTED、REQUIRES_NEW的实战应用  
✅ **常见问题**：自注入、rollback-only、保存点支持等  
✅ **最佳实践**：技术选型、编码规范、性能优化  

**核心要点回顾：**
- REQUIRED = **同生共死**（同一事务）
- NESTED = **内外有别，但不能逆天**（保存点）
- REQUIRES_NEW = **各自为政，互不干涉**（独立事务）

希望这篇文章能帮助你更好地理解Spring事务传播机制，在实际开发中做出正确的技术选型！

---

**源码地址：** [GitHub链接]

**欢迎点赞、收藏、转发！** 🎉

**参考资料：**
- Spring Framework官方文档
- JPA规范（JSR 338）
- Hibernate用户指南
- 《Spring实战》第5版
