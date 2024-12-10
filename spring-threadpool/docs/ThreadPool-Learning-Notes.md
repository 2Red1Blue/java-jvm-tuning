# Spring Boot 线程池学习笔记

## 学习目标
1. 理解Spring Boot中线程池的概念和作用
2. 掌握不同类型的线程池配置方法
3. 学习线程池的最佳实践

## 线程池类型及特点

### Java基础线程池
1. 单线程池（SingleThreadExecutor）
   - 只有一个工作线程
   - 保证任务按照提交顺序执行（FIFO）
   - 适用于需要保证顺序的场景

2. 固定线程池（FixedThreadPool）
   - 固定数量的工作线程
   - 任务队列无界
   - 适用于负载较重的服务器，可控制线程数量

3. 缓存线程池（CachedThreadPool）
   - 按需创建新线程
   - 空闲线程会被回收（默认60秒）
   - 适用于执行大量短期异步任务

4. 定时任务线程池（ScheduledThreadPool）
   - 支持定时及周期性任务执行
   - 可设置核心线程数
   - 适用于需要定期执行的任务

### Spring ThreadPoolTaskExecutor
Spring的ThreadPoolTaskExecutor是对Java ThreadPoolExecutor的封装，具有以下特点：

1. 更好的Spring集成
   - 可通过配置文件配置
   - 支持Spring的生命周期管理
   - 可以方便地注入到Spring组件中

2. 增强的功能
   - 支持任务优先级
   - 提供线程池运行状态监控
   - 支持优雅关闭
   - 可配置线程名前缀，方便调试

3. 配置更灵活
   - 核心线程数
   - 最大线程数
   - 队列容量
   - 线程存活时间
   - 拒绝策略
   - 等待任务完成的设置

4. 使用建议
   - 在Spring环境中优先使用ThreadPoolTaskExecutor
   - 可以通过配置文件统一管理线程池参数
   - 建议设置有界队列避免OOM
   - 根据实际需求选择合适的拒绝策略

## ThreadPoolTaskExecutor vs ThreadPoolExecutor

### 主要区别
1. 生命周期管理
   - ThreadPoolTaskExecutor：
     * 集成Spring生命周期
     * 支持优雅关闭
     * 可以等待任务完成后再关闭
   - ThreadPoolExecutor：
     * 需要手动管理生命周期
     * 需要自己实现关闭逻辑

2. 配置方式
   - ThreadPoolTaskExecutor：
     * 支持Spring配置文件配置
     * 可以通过@Value注入参数
     * 提供更友好的配置方法
   - ThreadPoolExecutor：
     * 需要手动创建和配置
     * 配置相对复杂
     * 需要自己实现ThreadFactory

3. 功能扩展
   - ThreadPoolTaskExecutor：
     * 提供任务装饰器
     * 支持任务优先级
     * 提供更多监控指标
   - ThreadPoolExecutor：
     * 基础功能实现
     * 需要自己扩展额外功能

### @Async注解使用
1. 配置方式
   ```java
   @EnableAsync  // 在配置类上启用异步
   @Configuration
   public class AsyncConfig {
       @Bean("threadPoolTaskExecutor")
       public ThreadPoolTaskExecutor threadPoolTaskExecutor() {
           // 配置线程池
       }
   }
   ```

2. 使用方式
   ```java
   @Async("threadPoolTaskExecutor")
   public CompletableFuture<String> asyncMethod() {
       // 异步方法实现
   }
   ```

3. 注意事项
   - @Async方法必须是public
   - 返回值应该是void或Future类型
   - 在同一个类中调用@Async方法无效
   - 异常处理需要特别注意

4. 最佳实践
   - 指定线程池名称避免使用默认线程池
   - 使用CompletableFuture获取异步结果
   - 合理设置线程池参数
   - 添加异常处理机制

### AsyncConfigurer接口
AsyncConfigurer接口是Spring提供的用于配置异步执行的接口，它提供了两个重要方法：

1. 配置方式
```java
@Configuration
public class AsyncExecutorConfig implements AsyncConfigurer {
    @Value("${thread-pool.core-pool-size}")
    private int corePoolSize;
    
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(25);
        executor.setThreadNamePrefix("async-");
        executor.initialize();
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) -> {
            // 处理未捕获的异步异常
        };
    }
}
```

2. 特点
   - 提供全局的异步执行器配置
   - 支持统一的异常处理
   - 可以通过配置文件注入参数
   - 适合需要统一管理异步配置的场景

### CompletableFuture

1. 基本使用
```java
// 创建异步任务
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
    return "结果";
}, executor);

// 处理结果
future.thenAccept(result -> {
    System.out.println(result);
});
```

2. 常用方法
```java
// 组合多个异步任务
CompletableFuture<String> future1 = CompletableFuture.supplyAsync(() -> "Hello");
CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> "World");
CompletableFuture<String> combined = future1.thenCombine(future2, (s1, s2) -> s1 + " " + s2);

// 异常处理
future.exceptionally(throwable -> "默认值")
      .thenAccept(System.out::println);

// 等待多个任务完成
CompletableFuture.allOf(future1, future2).join();
```

3. 使用建议
   - 指定自定义线程池
   - 合理设置超时时间
   - 正确处理异常
   - 避免阻塞操作

## 线程池实现方式比较

### 1. Spring @Bean方式
```java
@Bean
public ThreadPoolTaskExecutor threadPoolTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(corePoolSize);
    executor.setMaxPoolSize(maxPoolSize);
    executor.setQueueCapacity(queueCapacity);
    executor.setThreadNamePrefix("spring-thread-");
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    executor.setWaitForTasksToCompleteOnShutdown(true);
    return executor;
}
```
优点：
- Spring容器管理生命周期
- 支持配置文件配置
- 可以被其他组件注入
- 支持优雅关闭

适用场景：
- Spring应用程序
- 需要统一配置管理
- 需要Spring生命周期管理

### 2. AsyncConfigurer方式
优点：
- 提供全局异步配置
- 统一的异常处理
- 更好的Spring集成

适用场景：
- 需要全局异步配置
- 需要统一异常处理
- @Async注解使用场景

### 3. Guava线程池
优点：
- 更强大的Future实现
- 更好的异常处理
- 链式调用支持
- 丰富的工具类

适用场景：
- 复杂的异步任务链
- 需要更好的Future功能
- 需要更强的异常处理

### 4. Hutool线程池
优点：
- 使用简单
- API友好
- 快速创建
- 工具类丰富

适用场景：
- 简单的异步任务
- 快速开发
- 工具类使用

### 5. Apache Commons Pool
优点：
- 对象池管理
- 配置灵活
- 性能优化
- 资源复用

适用场景：
- 需要对象池管理
- 资源重用场景
- 连接池管理

### 选择建议
1. 一般Spring项目：使用@Bean配置ThreadPoolTaskExecutor
2. 全局异步配置：使用AsyncConfigurer
3. 复杂异步任务链：考虑Guava
4. 简单异步任务：可以使用Hutool
5. 对象池需求：使用Commons Pool

## 定时任务实现

### Spring Task实现方式
1. @Scheduled注解方式
   ```java
   @Scheduled(fixedRate = 5000)  // 固定速率执行
   public void fixedRateTask() {
       // 任务代码
   }

   @Scheduled(cron = "0 0 12 * * ?")  // Cron表达式
   public void cronTask() {
       // 任务代码
   }
   ```

2. TaskScheduler方式
   ```java
   @Autowired
   private TaskScheduler taskScheduler;

   // Cron触发器
   taskScheduler.schedule(
       () -> { /* 任务代码 */ },
       new CronTrigger("0/10 * * * * ?")
   );

   // 固定延迟触发器
   taskScheduler.scheduleWithFixedDelay(
       () -> { /* 任务代码 */ },
       TimeUnit.SECONDS.toMillis(30)
   );
   //如定时发送心跳检测
   public void startHeartbeatCheck() {  
    taskScheduler.scheduleWithFixedDelay(  
        () -> {  
            try {  
                performHeartbeatCheck();  
            } catch (Exception e) {  
                log.error("心跳检测失败", e);  
            }  
        },  
        TimeUnit.SECONDS.toMillis(30) // 每30秒执行一次  
    );  
}
   ```

### 配置说明
1. 启用定时任务
   ```java
   @Configuration
   @EnableScheduling
   public class SchedulerConfig {
       @Bean
       public TaskScheduler taskScheduler() {
           ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
           scheduler.setPoolSize(5);
           scheduler.setThreadNamePrefix("scheduled-task-");
           return scheduler;
       }
   }
   ```

2. Cron表达式说明
   - 秒 分 时 日 月 周
   - "0 0 12 * * ?" 每天12点执行
   - "0/5 * * * * ?" 每5秒执行一次
   - "0 0/30 * * * ?" 每30分钟执行一次

## 第三方框架线程池实现

### Guava线程池
适用场景：

- 处理复杂的异步任务链（如订单处理流程）

- 需要组合多个异步操作的结果（如用户数据聚合）

- 事件驱动型的异步处理

- 需要细粒度控制线程行为和异常处理
使用回调方式与netty相似
```java
ThreadFactory threadFactory = new ThreadFactoryBuilder()
.setNameFormat("guava-pool-%d")
.setDaemon(true)
.setPriority(Thread.NORM_PRIORITY)
.setUncaughtExceptionHandler((thread, ex) ->
	log.error("Thread error: {}", thread.getName(), ex))
.build();

ListeningExecutorService service = MoreExecutors.listeningDecorator(
    new ThreadPoolExecutor(10, 10, 0L, TimeUnit.MILLISECONDS,
        new LinkedBlockingQueue<Runnable>(1000),
        threadFactory));

// 使用方式
ListenableFuture<Result> future = service.submit(task);
Futures.addCallback(future, new FutureCallback<Result>() {
    public void onSuccess(Result result) { ... }
    public void onFailure(Throwable t) { ... }
});
```

#### 示例
一、订单处理流程
```java
private final ListeningExecutorService guavaExecutor;
public ThirdPartyThreadPoolExample() {  
    // 初始化Guava线程池  
    ThreadFactory threadFactory = new ThreadFactoryBuilder()  
            .setNameFormat("business-process-pool-%d")  
            .setDaemon(true)  
            .setPriority(Thread.NORM_PRIORITY)  
            .setUncaughtExceptionHandler((thread, ex) ->   
                log.error("业务处理线程异常: {}", thread.getName(), ex))  
            .build();  
  
    guavaExecutor = MoreExecutors.listeningDecorator(  
            new ThreadPoolExecutor(5, 20,  
                    60L, TimeUnit.SECONDS,  
                    new LinkedBlockingQueue<>(1000),  
                    threadFactory,  
                    new ThreadPoolExecutor.CallerRunsPolicy()));
                    }
                    
public void processOrders(List<Order> orders) {  
    List<ListenableFuture<OrderResult>> futures = orders.stream()  
        .map(order -> guavaExecutor.submit(() -> processOrder(order)))  
        .collect(Collectors.toList());  
  
    // 批量处理订单结果  
    ListenableFuture<List<OrderResult>> allFutures = Futures.allAsList(futures);  
      
    Futures.addCallback(allFutures, new FutureCallback<>() {  
        @Override  
        public void onSuccess(List<OrderResult> results) {  
            // 批量更新订单状态  
            updateOrderStatuses(results);  
            // 发送通知  
            sendNotifications(results);  
        }  
  
  
        @Override  
        public void onFailure(Throwable t) {  
            log.error("批量订单处理失败", t);  
            // 处理失败情况  
            handleBatchProcessingFailure(orders);  
        }  
  
    }, guavaExecutor);  
}
```

二、异步数据聚合
```java
public void aggregateUserData(String userId) {  
    // 并行获取用户各种数据  
    ListenableFuture<UserProfile> profileFuture = guavaExecutor.submit(() -> getUserProfile(userId));  
    ListenableFuture<List<Order>> ordersFuture = guavaExecutor.submit(() -> getUserOrders(userId));  
    ListenableFuture<CreditScore> creditFuture = guavaExecutor.submit(() -> getUserCredit(userId));  
  
    // 组合多个异步结果  
    ListenableFuture<UserAggregateData> aggregateFuture = Futures.whenAllComplete(  
            profileFuture, ordersFuture, creditFuture)  
            .call(() -> {  
                return new UserAggregateData(  
                    profileFuture.get(),  
                    ordersFuture.get(),  
                    creditFuture.get()  
                );  
            }, guavaExecutor);  
  
    // 处理最终的聚合结果  
    Futures.addCallback(aggregateFuture,   
        new FutureCallback<UserAggregateData>() {  
            @Override  
            public void onSuccess(UserAggregateData data) {  
                updateUserDashboard(data);  
            }  
  
  
            @Override  
            public void onFailure(Throwable t) {  
                log.error("用户数据聚合失败: {}", userId, t);  
            }  
        }, guavaExecutor);  
}
```
### Hutool线程池
可用于自定义线程池时简化步骤
```java
// 创建
ExecutorService executor = ThreadUtil.newExecutor(2, 4);

// 执行异步任务
ThreadUtil.execute(() -> {
    // 任务代码
});

// 创建定时任务
CronUtil.schedule("*/2 * * * * *", (Task) () -> {
    // 任务代码
});
```

### Apache Commons Pool
适用场景：
- 管理重量级资源对象（如数据库连接）
- 管理网络连接（如Netty客户端连接）
- 需要精确控制资源池大小和行为的场景
- 需要对资源进行复用和生命周期管理
```java
GenericObjectPoolConfig config = new GenericObjectPoolConfig();
config.setMaxTotal(10);
config.setMaxIdle(5);

ObjectPool<MyPoolableObject> pool = new GenericObjectPool<>(
    new MyPoolableObjectFactory(), config);
    
// 使用连接
Connection conn = pool.borrowObject();
try {

// 使用连接

} finally {
	pool.returnObject(conn);
}
```

#### 示例
一、数据库连接管理
```java
private final GenericObjectPool<DatabaseConnection> connectionPool;
public ThirdPartyThreadPoolExample() {
	// 初始化数据库连接池配置
        GenericObjectPoolConfig<DatabaseConnection> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal(20);
        poolConfig.setMaxIdle(10);
        poolConfig.setMinIdle(5);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestWhileIdle(true);
        poolConfig.setTimeBetweenEvictionRuns(Duration.ofMinutes(1));
        poolConfig.setMinEvictableIdleTime(Duration.ofMinutes(5));

        connectionPool = new GenericObjectPool<>(new DatabaseConnectionFactory(), poolConfig);
}

public void performDatabaseOperations(String sql) throws Exception {  
    DatabaseConnection conn = null;  
    try {  
        conn = connectionPool.borrowObject();  
        conn.executeQuery(sql);  
    } finally {  
        if (conn != null) {  
            connectionPool.returnObject(conn);  
        }  
    }  
}  
  
// Apache Commons Pool示例2：批量数据处理  
public void processBatchData(List<String> dataItems) throws Exception {  
    List<DatabaseConnection> connections = new ArrayList<>();  
    try {  
        // 获取多个连接用于并行处理  
        for (int i = 0; i < Math.min(dataItems.size(), 5); i++) {  
            connections.add(connectionPool.borrowObject());  
        }  
  
        // 使用连接池中的连接并行处理数据  
        ExecutorService executor = Executors.newFixedThreadPool(connections.size());  
        List<CompletableFuture<Void>> futures = new ArrayList<>();  
  
        for (int i = 0; i < connections.size(); i++) {  
            DatabaseConnection conn = connections.get(i);  
            List<String> batch = getBatch(dataItems, i, connections.size());  
              
            futures.add(CompletableFuture.runAsync(() -> {  
                try {  
                    processBatch(conn, batch);  
                } catch (Exception e) {  
                    log.error("批处理失败", e);  
                    throw new RuntimeException(e);  
                }  
            }, executor));  
        }  
  
        // 等待所有批处理完成  
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();  
        executor.shutdown();  
    } finally {  
        // 归还所有连接  
        for (DatabaseConnection conn : connections) {  
            connectionPool.returnObject(conn);  
        }  
    }  
}
```

二、netty客户端连接
```java
private final GenericObjectPool<NettyClientConnection> channelConnectPool;

channelConnectPool = new GenericObjectPool<>(new NettyClientConnectionFactory());

public void manageNettyConnections() throws Exception {  
    NettyClientConnection conn = null;  
    try {  
        conn = channelConnectPool.borrowObject();  
        conn.sendData("业务消息");  
    } finally {  
        if (conn != null) {  
            channelConnectPool.returnObject(conn);  
        }  
    }  
}
```

## 线程池使用总结

### 1. 线程池配置选择
- Spring环境：优先使用@Bean配置ThreadPoolTaskExecutor
- 全局异步配置：实现AsyncConfigurer接口
- 特殊性能要求：使用private static final方式

### 2. 异步任务处理
- 使用@Async + @Retryable处理可重试的异步任务
- 配置全局异常处理器
- 使用CompletableFuture处理异步结果

### 3. 定时任务处理
- 简单定时任务：使用@Scheduled注解
- 动态定时任务：使用TaskScheduler
- 复杂定时任务：考虑使用Quartz,xxl-job等专业调度框架

### 4. 监控和管理
- 配置线程池监控指标
- 实现优雅关闭
- 合理设置队列大小和拒绝策略

### 5. 常见问题避免
- 防止线程池满载
- 避免任务队列无限增长
- 处理异步任务超时
- 正确处理异常传播
