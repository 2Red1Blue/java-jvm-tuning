package com.example.threadpool.examples;

import cn.hutool.core.thread.ThreadUtil;
import com.google.common.util.concurrent.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.stereotype.Component;

import java.nio.channels.Channel;
import java.sql.Connection;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ThirdPartyThreadPoolExample {

    // Guava线程池 - 适合处理复杂的异步任务链和事件处理
    private final ListeningExecutorService guavaExecutor;
    
    // Apache Commons Pool - 适合管理重量级资源对象
    private final GenericObjectPool<DatabaseConnection> connectionPool;
    private final GenericObjectPool<NettyClientConnection> channelConnectPool;

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

        channelConnectPool = new GenericObjectPool<>(new NettyClientConnectionFactory());
    }

    // Guava示例1：订单处理流程
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


    // Guava示例2：异步数据聚合
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



    // Apache Commons Pool示例1：数据库连接管理
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


    // Apache Commons Pool示例：Netty客户端连接管理
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

    // 数据库连接类
    private static class DatabaseConnection {
        private final String id = "DB-Conn-" + System.currentTimeMillis();
        private final Connection connection; // 实际的数据库连接

        public DatabaseConnection(String url, String username, String password) {
            // 初始化实际的数据库连接
            this.connection = createRealConnection(url, username, password);
        }

        public void executeQuery(String sql) {
            // 执行实际的数据库操作
        }

        private Connection createRealConnection(String url, String username, String password) {
            // 创建实际的数据库连接
            return null; // 示例代码省略实现
        }
    }

    // 数据库连接工厂
    private static class DatabaseConnectionFactory extends BasePooledObjectFactory<DatabaseConnection> {
        @Override
        public DatabaseConnection create() {
            return new DatabaseConnection("jdbc:mysql://localhost:3306/db", "user", "pass");
        }

        @Override
        public PooledObject<DatabaseConnection> wrap(DatabaseConnection conn) {
            return new DefaultPooledObject<>(conn);
        }

        @Override
        public boolean validateObject(PooledObject<DatabaseConnection> p) {
            // 验证连接是否有效
            return true; // 示例代码省略实现
        }
    }

    // Netty客户端连接类
    private static class NettyClientConnection {
        private final String id = "Netty-Conn-" + System.currentTimeMillis();
        private final Channel channel; // 实际的Netty通道

        public NettyClientConnection(String host, int port) {
            // 初始化实际的Netty连接
            this.channel = createRealConnection(host, port);
        }

        public void sendData(String data) {
            // 发送数据
        }

        private Channel createRealConnection(String host, int port) {
            // 创建实际的Netty连接
            return null; // 示例代码省略实现
        }
    }

    // Netty客户端连接工厂
    private static class NettyClientConnectionFactory extends BasePooledObjectFactory<NettyClientConnection> {
        @Override
        public NettyClientConnection create() {
            return new NettyClientConnection("localhost", 8080);
        }

        @Override
        public PooledObject<NettyClientConnection> wrap(NettyClientConnection conn) {
            return new DefaultPooledObject<>(conn);
        }

        @Override
        public boolean validateObject(PooledObject<NettyClientConnection> p) {
            // 验证连接是否有效
            return true; // 示例代码省略实现
        }
    }

    // Hutool线程池
    public void hutoolThreadPoolExample() {
        ExecutorService executor = ThreadUtil.newExecutor(2, 4);
        executor.submit(() -> {
            log.info("Hutool线程池执行任务");
        });
        executor.shutdown();
    }


    private void processBatch(DatabaseConnection conn, List<String> batch) {

    }

    private List<String> getBatch(List<String> dataItems, int i, int size) {
        return null;
    }
    @Data
    public class Order {
        private String id;
    }
    @Data
    public class OrderResult{
        private String id;

    }

    @Data
    public class UserProfile{
        private String id;
    }

    @Data
    @AllArgsConstructor
    public class UserAggregateData{
        private UserProfile userProfile;
        private List<Order> orders;
        private CreditScore creditScore;

    }
    @Data
    public class UserCreditScore{

    }
    @Data
    public class UserCredit{

    }
    @Data
    public class CreditScore{

    }
    private void sendNotifications(List<OrderResult> results) {

    }

    private void updateOrderStatuses(List<OrderResult> results) {

    }
    private void handleBatchProcessingFailure(List<Order> orders) {

    }

    private OrderResult processOrder(Order order) {
        return null;
    }

    private void updateUserDashboard(UserAggregateData data) {
    }
    private CreditScore getUserCredit(String userId) {
        return null;
    }

    private List<Order> getUserOrders(String userId) {
        return null;
    }

    private UserProfile getUserProfile(String userId) {
        return null;
    }
}

