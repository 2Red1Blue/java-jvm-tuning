package com.example;

import java.util.ArrayList;
import java.util.List;

/**
 * @author laiu
 * 这是一个演示内存泄漏的示例程序
 * 可以用来学习JVM内存分析和调优
 */
public class MemoryLeakDemo {
    private static List<byte[]> list = new ArrayList<>();

    public static void main(String[] args) throws InterruptedException {
        System.out.println("开始运行内存泄漏演示程序...");
        System.out.println("当前JVM的最大堆内存: " + 
            Runtime.getRuntime().maxMemory() / 1024 / 1024 + "MB");

        int count = 0;
        try {
            while (true) {
                // 每次分配1MB的内存
                list.add(new byte[1024 * 1024]);
                count++;
                
                if (count % 10 == 0) {
                    System.out.println("已分配内存: " + count + "MB");
                    Thread.sleep(100); // 添加延迟，方便观察
                }
            }
        } catch (OutOfMemoryError e) {
            System.out.println("发生内存溢出错误！");
            System.out.println("总共分配了: " + count + "MB 的内存");
            throw e;
        }
    }
}
