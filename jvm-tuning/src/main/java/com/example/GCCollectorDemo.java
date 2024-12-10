package com.example;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 垃圾收集器性能对比Demo
 * 运行方式：
 * 1. Parallel GC: 
 *    java -XX:+UseParallelGC -Xms512m -Xmx512m -XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:gc_parallel.log GCCollectorDemo
 *    jdk9+: java -XX:+UseParallelGC -Xms512m -Xmx512m -Xlog:gc*=info:file=gc_parallel.log -cp target/classes com.example.GCCollectorDemo
 * 2. CMS GC: 
 *    java -XX:+UseConcMarkSweepGC -Xms512m -Xmx512m -XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:gc_cms.log GCCollectorDemo
 * 3. G1 GC: 
 *    java -XX:+UseG1GC -Xms512m -Xmx512m -XX:MaxGCPauseMillis=200 -XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:gc_g1.log GCCollectorDemo
 * 4. ZGC(jdk11+):
 *    java -XX:+UseZGC -Xms512m -Xmx512m -Xlog:gc*=info:file=gc_zgc.log GCCollectorDemo
 */
public class GCCollectorDemo {
    // 模拟对象大小范围
    private static final int MIN_SIZE = 1024; // 1KB
    private static final int MAX_SIZE = 1024 * 1024; // 1MB

    // 用于存储对象的列表
    private static List<byte[]> objects = new ArrayList<>();
    
    // 模拟不同生命周期的对象
    private static List<byte[]> longLivedObjects = new ArrayList<>();
    
    private static Random random = new Random();

    public static void main(String[] args) throws InterruptedException {
        System.out.println("Starting GC Collector Demo...");
        System.out.println("VM Options: " + System.getProperty("sun.java.command"));
        
        // 创建一些长期存活的对象（老年代对象）
        createLongLivedObjects();
        
        // 模拟应用程序的正常运行
        for (int i = 0; i < 100; i++) {
            // 创建一批对象
            allocateObjects();
            
            // 清理一些对象，模拟对象被回收
            clearSomeObjects();
            
            // 每轮暂停一小段时间
            Thread.sleep(100);
        }
        
        System.out.println("Demo completed.");
    }
    
    // 创建长期存活的对象
    private static void createLongLivedObjects() {
        for (int i = 0; i < 10; i++) {
            longLivedObjects.add(new byte[MAX_SIZE]); // 每个对象1MB
        }
        System.out.println("Created long-lived objects: " + longLivedObjects.size());
    }
    
    // 分配新对象
    private static void allocateObjects() {
        int objectCount = random.nextInt(100) + 50; // 每次分配50-150个对象
        for (int i = 0; i < objectCount; i++) {
            int size = random.nextInt(MAX_SIZE - MIN_SIZE) + MIN_SIZE;
            objects.add(new byte[size]);
        }
    }
    
    // 清理一些对象
    private static void clearSomeObjects() {
        int removeCount = objects.size() / 2;
        for (int i = 0; i < removeCount; i++) {
            objects.remove(0);
        }
    }
}
