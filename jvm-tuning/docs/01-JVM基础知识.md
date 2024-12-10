# JVM基础知识与调优指南

## 1. JVM内存结构

### 1.1 堆内存（Heap）
- 年轻代（Young Generation）
  - Eden区
  - Survivor区（S0和S1）
- 老年代（Old Generation）

### 1.2 非堆内存
- 方法区（Method Area）/ 元空间（Metaspace）
- 程序计数器（Program Counter Register）
- 虚拟机栈（VM Stack）
- 本地方法栈（Native Method Stack）

## 2. 常用JVM参数

### 2.1 内存相关
```bash
# 设置堆的初始大小
-Xms<size>

# 设置堆的最大大小
-Xmx<size>

# 设置新生代大小
-Xmn<size>

# 设置元空间大小
-XX:MetaspaceSize=<size>
-XX:MaxMetaspaceSize=<size>
```

### 2.2 垃圾回收相关
```bash
# 设置垃圾收集器
-XX:+UseSerialGC
-XX:+UseParallelGC
-XX:+UseConcMarkSweepGC
-XX:+UseG1GC

# GC日志
-XX:+PrintGCDetails
-XX:+PrintGCDateStamps
-Xloggc:/path/to/gc.log
```

## 3. 性能监控工具使用

### 3.1 JDK自带工具
- jps：查看Java进程
- jstat：查看JVM统计信息
- jmap：导出堆内存快照
- jstack：查看线程堆栈

### 3.2 使用示例
```bash
# 查看Java进程
jps -l

# 查看堆内存使用情况
jstat -gcutil <pid> 1000

# 导出堆内存快照
jmap -dump:format=b,file=heap.bin <pid>

# 查看线程堆栈
jstack <pid>
```

## 4. 实践案例分析

### 4.1 内存泄漏案例
参考`MemoryLeakDemo.java`的示例程序，可以通过以下步骤进行分析：

1. 运行程序：
```bash
java -Xmx256m -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=./dump.hprof com.example.MemoryLeakDemo
```

2. 使用JVisualVM或MAT分析dump文件

3. 观察内存使用趋势和GC日志

### 4.2 调优建议
- 根据应用特点选择合适的垃圾收集器
- 合理设置堆内存大小
- 注意内存泄漏和内存溢出的预防
- 定期进行性能监控和分析
