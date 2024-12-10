package com.example;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author laiu
 * 这是一个展示正确内存管理的示例程序
 * 使用软引用和限制集合大小来避免内存泄漏
 */
public class MemoryManagementDemo {
    // 使用软引用存储大对象
    private static List<SoftReference<byte[]>> list = new ArrayList<>();
    // 限制最大缓存对象数量
    private static final int MAX_CACHE_SIZE = 100;
    // 使用原子计数器跟踪创建的对象总数
    private static AtomicInteger totalCreated = new AtomicInteger(0);
    // 使用原子计数器跟踪被GC回收的对象数
    private static AtomicInteger totalReclaimed = new AtomicInteger(0);

    public static void main(String[] args) throws InterruptedException {
        System.out.println("开始运行内存管理示例程序...");
        System.out.println("当前JVM的最大堆内存: " + 
            Runtime.getRuntime().maxMemory() / 1024 / 1024 + "MB");
        
        try {
            while (true) {
                // 创建1MB的字节数组
                byte[] data = new byte[1024 * 1024];
                totalCreated.incrementAndGet();
                
                // 使用软引用包装对象
                SoftReference<byte[]> ref = new SoftReference<>(data);
                
                // 限制集合大小
                if (list.size() >= MAX_CACHE_SIZE) {
                    list.remove(0);
                }
                list.add(ref);
                
                // 检查有多少对象被回收了
                int reclaimed = 0;
                for (SoftReference<byte[]> item : list) {
                    if (item.get() == null) {
                        reclaimed++;
                    }
                }
                totalReclaimed.addAndGet(reclaimed);
                
                // 每10MB输出一次状态
                if (totalCreated.get() % 10 == 0) {
                    System.out.printf("状态报告：已创建 %d MB, 已回收 %d MB, 当前缓存大小 %d%n",
                        totalCreated.get(), totalReclaimed.get(), list.size());
                    Thread.sleep(100); // 添加延迟，方便观察
                }
                
                // 清理已被回收的引用
                list.removeIf(item -> item.get() == null);
            }
        } catch (OutOfMemoryError e) {
            System.out.println("发生内存溢出错误！");
            System.out.printf("总共创建了: %d MB的内存，回收了: %d MB%n",
                totalCreated.get(), totalReclaimed.get());
            throw e;
        }
    }
}
