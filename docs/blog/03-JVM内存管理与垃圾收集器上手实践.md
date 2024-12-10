# JVM内存管理与垃圾收集器上手实践

## 1. JVM内存结构
### 1.1 堆内存（Heap）
- 年轻代（Young Generation）
  - Eden区：新对象分配的区域
  - Survivor区（From和To）：存活对象在年轻代中的中转站
- 老年代（Old Generation）：长期存活的对象

### 1.2 非堆内存
- 方法区（Method Area）/元空间（Metaspace）：存储类信息、常量、静态变量等
- 程序计数器（Program Counter Register）：记录当前线程执行的字节码位置
- 虚拟机栈（VM Stack）：存储局部变量表、操作数栈等
- 本地方法栈（Native Method Stack）：为本地方法服务

## 2. 垃圾回收机制
### 2.1 垃圾识别算法
- 引用计数法
  - 优点：实现简单，判定效率高
  - 缺点：无法解决循环引用问题
- 可达性分析
  - GC Roots：线程栈变量、静态变量、JNI引用等
  - 标记所有从GC Roots可达的对象

### 2.2 垃圾收集算法
- 标记-清除算法（Mark-Sweep）
  - 标记存活对象
  - 清除未标记对象
  - 缺点：产生内存碎片
- 复制算法（Copying）
  - 将内存分为两块
  - 存活对象复制到另一块
  - 整块清理
  - 适用于存活对象少的场景（如新生代）
- 标记-整理算法（Mark-Compact）
  - 标记存活对象
  - 将存活对象向一端移动
  - 清理边界外内存
  - 适用于存活对象多的场景（如老年代）

### 2.3 分代收集
- 新生代：复制算法
- 老年代：标记-整理或标记-清除算法

## 3. 垃圾收集器
### 3.1 新生代收集器
- Serial：单线程收集器
- ParNew：Serial的多线程版本
- Parallel Scavenge：关注吞吐量

### 3.2 老年代收集器
- Serial Old：Serial的老年代版本
- Parallel Old：Parallel Scavenge的老年代版本
- CMS：并发标记清除，关注停顿时间

### 3.3 全局收集器
- G1：区域化分代式
- ZGC：低延迟垃圾收集器

## 4. 实践与调优
### 4.1 JVM参数
```bash
# 内存大小设置
-Xms：初始堆大小
-Xmx：最大堆大小
-Xmn：新生代大小
-XX:MetaspaceSize：元空间初始大小
-XX:MaxMetaspaceSize：元空间最大大小

# GC日志
-XX:+PrintGCDetails
-XX:+PrintGCDateStamps
-Xloggc:gc.log

# 垃圾收集器选择
-XX:+UseG1GC：使用G1收集器
-XX:+UseConcMarkSweepGC：使用CMS收集器
-XX:+UseZGC：使用ZGC收集器
```

### 4.2 监控工具
- jstat：查看GC统计信息
- jmap：导出堆转储文件
- jstack：查看线程堆栈
- VisualVM：可视化监控工具
- JMC（Java Mission Control）：性能分析工具

### 4.3 调优建议
1. 合理设置堆大小
   - 避免堆过小导致频繁GC
   - 避免堆过大导致GC停顿时间过长

2. 选择合适的垃圾收集器
   - 注重吞吐量：Parallel收集器
   - 注重响应时间：CMS或G1收集器
   - 超低延迟：ZGC

3. GC日志分析
   - 关注GC频率
   - 关注停顿时间
   - 分析内存分配速率

4. 内存泄漏防范
   - 注意集合类使用
   - 及时释放不用的对象
   - 使用弱引用或软引用

## 5. 垃圾收集器对比实验

### 5.1 实验环境
```bash
# JDK版本
java version "11.0.25" 2024-10-15
OpenJDK Runtime Environment (build 11.0.25+8-post-Ubuntu-1ubuntu122.04)
OpenJDK 64-Bit Server VM (build 11.0.25+8-post-Ubuntu-122.04, mixed mode, sharing)

# 操作系统
WSL2 (Ubuntu 22.04)
```

### 5.2 测试代码
```java
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
        // 创建一些长期存活的对象（老年代对象）
        createLongLivedObjects();
        
        // 模拟应用程序的正常运行
        for (int i = 0; i < 100; i++) {
            // 创建一批对象
            allocateObjects();
            // 清理一些对象，模拟对象被回收
            clearSomeObjects();
            Thread.sleep(100);
        }
    }
}
```

将`pom.xml`文件中的maven插件主类配置更换成当前示例程序`GCCollectorDemo`
```xml
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-jar-plugin</artifactId>
                <version>3.2.0</version>
                <configuration>
                    <archive>
                        <manifest>
                            <addClasspath>true</addClasspath>
                            <mainClass>com.example.GCCollectorDemo</mainClass>
                        </manifest>
                    </archive>
                </configuration>
            </plugin>
```
### 5.3 Parallel收集器分析

#### 5.3.1 运行参数
```bash
# JDK8及之前版本
java -XX:+UseParallelGC -Xms512m -Xmx512m -XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:gc_parallel.log GCCollectorDemo

# JDK9及以后版本
java -XX:+UseParallelGC -Xms512m -Xmx512m -Xlog:gc*=info:file=gc_parallel.log -cp target/classes com.example.GCCollectorDemo
```

#### 5.3.2 GC日志分析
```
[0.010s][info][gc] Using Parallel
[0.011s][info][gc,heap,coops] Heap address: 0x00000000e0000000, size: 512 MB, Compressed Oops mode: 32-bit
[0.287s][info][gc,start     ] GC(0) Pause Young (Allocation Failure)
[0.306s][info][gc,heap      ] GC(0) PSYoungGen: 131494K->21479K(153088K)
[0.306s][info][gc,heap      ] GC(0) ParOldGen: 0K->51110K(349696K)
[0.307s][info][gc,metaspace ] GC(0) Metaspace: 481K(4864K)->481K(4864K) NonClass: 444K(4352K)->444K(4352K) Class: 37K(512K)->37K(512K)
[0.307s][info][gc           ] GC(0) Pause Young (Allocation Failure) 128M->70M(491M) 19.644ms
[0.307s][info][gc,cpu       ] GC(0) User=0.06s Sys=0.03s Real=0.02s
[0.615s][info][gc,start     ] GC(1) Pause Young (Allocation Failure)
[0.621s][info][gc,heap      ] GC(1) PSYoungGen: 152676K->21497K(153088K)
[0.622s][info][gc,heap      ] GC(1) ParOldGen: 51110K->87913K(349696K)
...

[10.658s][info][gc             ] GC(87) Pause Young (Allocation Failure) 210M->157M(455M) 6.081ms
[10.659s][info][gc,cpu         ] GC(87) User=0.02s Sys=0.00s Real=0.01s
[10.763s][info][gc,start       ] GC(88) Pause Young (Allocation Failure)
[10.768s][info][gc,heap        ] GC(88) PSYoungGen: 114797K->57512K(116736K)
[10.768s][info][gc,heap        ] GC(88) ParOldGen: 104050K->120909K(349696K)
[10.769s][info][gc,metaspace   ] GC(88) Metaspace: 486K(4864K)->486K(4864K) NonClass: 449K(4352K)->449K(4352K) Class: 37K(512K)->37K(512K)
[10.769s][info][gc             ] GC(88) Pause Young (Allocation Failure) 213M->174M(455M) 6.330ms
[10.769s][info][gc,cpu         ] GC(88) User=0.03s Sys=0.00s Real=0.01s
[10.876s][info][gc,start       ] GC(89) Pause Young (Allocation Failure)
[10.888s][info][gc,heap        ] GC(89) PSYoungGen: 116243K->57712K(116736K)
[10.889s][info][gc,heap        ] GC(89) ParOldGen: 120909K->145758K(349696K)
[10.889s][info][gc,metaspace   ] GC(89) Metaspace: 486K(4864K)->486K(4864K) NonClass: 449K(4352K)->449K(4352K) Class: 37K(512K)->37K(512K)
[10.890s][info][gc             ] GC(89) Pause Young (Allocation Failure) 231M->198M(455M) 13.825ms
[10.890s][info][gc,cpu         ] GC(89) User=0.04s Sys=0.00s Real=0.02s
[10.996s][info][gc,start       ] GC(90) Pause Young (Allocation Failure)
[11.003s][info][gc,heap        ] GC(90) PSYoungGen: 116569K->57393K(116736K)
[11.004s][info][gc,heap        ] GC(90) ParOldGen: 145758K->185737K(349696K)
[11.004s][info][gc,metaspace   ] GC(90) Metaspace: 486K(4864K)->486K(4864K) NonClass: 449K(4352K)->449K(4352K) Class: 37K(512K)->37K(512K)
[11.004s][info][gc             ] GC(90) Pause Young (Allocation Failure) 256M->237M(455M) 8.927ms
[11.005s][info][gc,cpu         ] GC(90) User=0.05s Sys=0.00s Real=0.01s
[11.107s][info][gc,heap,exit   ] Heap
[11.107s][info][gc,heap,exit   ]  PSYoungGen      total 116736K, used 76185K [0x00000000f5580000, 0x0000000100000000, 0x0000000100000000)
[11.108s][info][gc,heap,exit   ]   eden space 58880K, 31% used [0x00000000f5580000,0x00000000f67d9d00,0x00000000f8f00000)
[11.108s][info][gc,heap,exit   ]   from space 57856K, 99% used [0x00000000f8f00000,0x00000000fc70c788,0x00000000fc780000)
[11.109s][info][gc,heap,exit   ]   to   space 57856K, 0% used [0x00000000fc780000,0x00000000fc780000,0x0000000100000000)
[11.109s][info][gc,heap,exit   ]  ParOldGen       total 349696K, used 185737K [0x00000000e0000000, 0x00000000f5580000, 0x00000000f5580000)
[11.110s][info][gc,heap,exit   ]   object space 349696K, 53% used [0x00000000e0000000,0x00000000eb562440,0x00000000f5580000)
[11.110s][info][gc,heap,exit   ]  Metaspace       used 487K, capacity 4553K, committed 4864K, reserved 1056768K
[11.111s][info][gc,heap,exit   ]   class space    used 37K, capacity 410K, committed 512K, reserved 1048576K
```
![[Pasted image 20241208131640.png]]
以一次典型的Young GC为例：
```
[11.003s][info][gc,heap] GC(90) PSYoungGen: 116569K->57393K(116736K)
[11.004s][info][gc,heap] GC(90) ParOldGen: 145758K->185737K(349696K)
[11.004s][info][gc             ] GC(90) Pause Young (Allocation Failure) 256M->237M(455M) 8.927ms
```

1. **GC触发原因**：
   - `Pause Young (Allocation Failure)`表示新生代空间不足
   - 这是最常见的GC触发原因，即Eden区满了无法分配新对象

2. **内存变化**：
   - 新生代（PSYoungGen）：116MB -> 57MB
   - 老年代（ParOldGen）：142MB -> 181MB
   - 总堆内存：256MB -> 237MB
   - 本次GC释放了约19MB内存
   - 约39MB对象从新生代晋升到老年代

3. **GC性能指标**：
   ```
   User=0.05s Sys=0.00s Real=0.01s
   ```
   - User时间 > Real时间，说明充分利用了多线程并行收集
   - Sys：内核态CPU时间0秒
   - Real：实际耗时0.01秒
   - User > Real说明利用了多线程并行收集
   - 单次GC暂停时间仅8.927ms，性能表现良好

4. **内存分布**：
```
PSYoungGen total 116736K:
- eden space: 58880K, 31% used    (新对象分配区域)
- from space: 57856K, 99% used    (当前使用的Survivor区)
- to space: 57856K, 0% used       (下次GC时使用的Survivor区)

ParOldGen total 349696K:
- object space: 349696K, 53% used (老年代使用率53%)

Metaspace:
- used 487K, capacity 4553K       (元空间使用情况)
```
   - 新生代采用经典的Eden + 2个Survivor区布局
   - Survivor区使用率较高，说明对象存活率较高

#### 5.3.3 Parallel收集器特点总结

1. **优点**：
   - 高吞吐量：多线程并行收集，CPU利用率高
   - 自适应调节：会根据运行情况自动调整新生代大小、晋升阈值等
   - 适合后台计算型应用

2. **缺点**：
   - 所有收集动作都会造成STW（Stop-The-World）
   - 不适合对响应时间要求很高的场景

3. **适用场景**：
   - 后台批处理
   - 科学计算
   - 大数据处理
   - 不需要太多交互的应用


### 5.4 CMS收集器分析

#### 5.4.1 运行参数
```bash
java -XX:+UseConcMarkSweepGC -Xms512m -Xmx512m -Xlog:gc*=info:file=gc_cms.log GCCollectorDemo
```

#### 5.4.2 GC日志分析
```
[0.012s][info][gc] Using Concurrent Mark Sweep
[0.012s][info][gc,heap,coops] Heap address: 0x00000000e0000000, size: 512 MB, Compressed Oops mode: 32-bit
[0.180s][info][gc,start     ] GC(0) Pause Young (Allocation Failure)
[0.181s][info][gc,task      ] GC(0) Using 11 workers of 18 for evacuation
[0.198s][info][gc,heap      ] GC(0) ParNew: 139632K->17470K(157248K)
[0.198s][info][gc,heap      ] GC(0) CMS: 0K->78999K(349568K)
[0.198s][info][gc,metaspace ] GC(0) Metaspace: 488K(4864K)->488K(4864K) NonClass: 451K(4352K)->451K(4352K) Class: 37K(512K)->37K(512K)
[0.199s][info][gc           ] GC(0) Pause Young (Allocation Failure) 136M->94M(494M) 18.914ms
[0.199s][info][gc,cpu       ] GC(0) User=0.14s Sys=0.01s Real=0.01s
[0.509s][info][gc,start     ] GC(1) Pause Young (Allocation Failure)
[0.519s][info][gc,task      ] GC(1) Using 11 workers of 18 for evacuation
[0.535s][info][gc,heap      ] GC(1) ParNew: 157246K->17455K(157248K)
[0.536s][info][gc,heap      ] GC(1) CMS: 78999K->135198K(349568K)
[0.536s][info][gc,metaspace ] GC(1) Metaspace: 493K(4864K)->493K(4864K) NonClass: 456K(4352K)->456K(4352K) Class: 37K(512K)->37K(512K)
[0.536s][info][gc           ] GC(1) Pause Young (Allocation Failure) 230M->149M(494M) 26.878ms
[0.537s][info][gc,cpu       ] GC(1) User=0.12s Sys=0.02s Real=0.03s
[0.846s][info][gc,start     ] GC(2) Pause Young (Allocation Failure)
[0.847s][info][gc,task      ] GC(2) Using 11 workers of 18 for evacuation
[0.866s][info][gc,heap      ] GC(2) ParNew: 156736K->17471K(157248K)
[0.866s][info][gc,heap      ] GC(2) CMS: 135198K->192239K(349568K)
[0.867s][info][gc,metaspace ] GC(2) Metaspace: 493K(4864K)->493K(4864K) NonClass: 456K(4352K)->456K(4352K) Class: 37K(512K)->37K(512K)
[0.867s][info][gc           ] GC(2) Pause Young (Allocation Failure) 285M->204M(494M) 20.477ms
[0.867s][info][gc,cpu       ] GC(2) User=0.19s Sys=0.00s Real=0.02s
[0.868s][info][gc,start     ] GC(3) Pause Initial Mark
[0.868s][info][gc           ] GC(3) Pause Initial Mark 205M->205M(494M) 0.369ms
[0.868s][info][gc,cpu       ] GC(3) User=0.00s Sys=0.00s Real=0.00s
[0.869s][info][gc           ] GC(3) Concurrent Mark
[0.869s][info][gc,task      ] GC(3) Using 5 workers of 5 for marking
[0.870s][info][gc           ] GC(3) Concurrent Mark 1.685ms
[0.871s][info][gc,cpu       ] GC(3) User=0.01s Sys=0.00s Real=0.01s
[0.871s][info][gc           ] GC(3) Concurrent Preclean
[0.871s][info][gc           ] GC(3) Concurrent Preclean 0.353ms
[0.871s][info][gc,cpu       ] GC(3) User=0.00s Sys=0.00s Real=0.00s
[0.871s][info][gc           ] GC(3) Concurrent Abortable Preclean
[1.279s][info][gc,start     ] GC(4) Pause Young (Allocation Failure)
[1.279s][info][gc,task      ] GC(4) Using 11 workers of 18 for evacuation
[1.291s][info][gc,heap      ] GC(4) ParNew: 157247K->17470K(157248K)
[1.291s][info][gc,heap      ] GC(4) CMS: 192239K->216923K(349568K)
[1.291s][info][gc,metaspace ] GC(4) Metaspace: 493K(4864K)->493K(4864K) NonClass: 456K(4352K)->456K(4352K) Class: 37K(512K)->37K(512K)
[1.292s][info][gc           ] GC(4) Pause Young (Allocation Failure) 341M->228M(494M) 12.541ms
[1.292s][info][gc,cpu       ] GC(4) User=0.11s Sys=0.00s Real=0.02s
[1.501s][info][gc,start     ] GC(5) Pause Young (Allocation Failure)
[1.501s][info][gc,task      ] GC(5) Using 11 workers of 18 for evacuation
[1.519s][info][gc,heap      ] GC(5) ParNew: 156545K->17465K(157248K)
[1.520s][info][gc,heap      ] GC(5) CMS: 216923K->265853K(349568K)
[1.520s][info][gc,metaspace ] GC(5) Metaspace: 493K(4864K)->493K(4864K) NonClass: 456K(4352K)->456K(4352K) Class: 37K(512K)->37K(512K)
[1.521s][info][gc           ] GC(5) Pause Young (Allocation Failure) 364M->276M(494M) 19.895ms
[1.521s][info][gc,cpu       ] GC(5) User=0.19s Sys=0.01s Real=0.02s
[1.521s][info][gc           ] GC(3) Concurrent Abortable Preclean 649.551ms
[1.521s][info][gc,cpu       ] GC(3) User=0.32s Sys=0.01s Real=0.65s
[1.522s][info][gc,start     ] GC(3) Pause Remark
[1.523s][info][gc           ] GC(3) Pause Remark 292M->292M(494M) 0.656ms
[1.523s][info][gc,cpu       ] GC(3) User=0.00s Sys=0.00s Real=0.00s
[1.523s][info][gc           ] GC(3) Concurrent Sweep
[1.524s][info][gc           ] GC(3) Concurrent Sweep 0.622ms
[1.524s][info][gc,cpu       ] GC(3) User=0.00s Sys=0.00s Real=0.00s
[1.524s][info][gc           ] GC(3) Concurrent Reset
[1.526s][info][gc           ] GC(3) Concurrent Reset 1.456ms
[1.526s][info][gc,cpu       ] GC(3) User=0.00s Sys=0.00s Real=0.00s
[1.526s][info][gc,heap      ] GC(3) Old: 192239K->115299K(349568K)
[1.730s][info][gc,start     ] GC(6) Pause Young (Allocation Failure)
[1.730s][info][gc,task      ] GC(6) Using 11 workers of 18 for evacuation
[1.742s][info][gc,heap      ] GC(6) ParNew: 156457K->17454K(157248K)
[1.742s][info][gc,heap      ] GC(6) CMS: 115299K->200805K(349568K)
[1.742s][info][gc,metaspace ] GC(6) Metaspace: 493K(4864K)->493K(4864K) NonClass: 456K(4352K)->456K(4352K) Class: 37K(512K)->37K(512K)
[1.742s][info][gc           ] GC(6) Pause Young (Allocation Failure) 265M->213M(494M) 12.326ms
[1.743s][info][gc,cpu       ] GC(6) User=0.12s Sys=0.00s Real=0.01s
[1.743s][info][gc,start     ] GC(7) Pause Initial Mark
[1.743s][info][gc           ] GC(7) Pause Initial Mark 217M->217M(494M) 0.378ms
[1.744s][info][gc,cpu       ] GC(7) User=0.00s Sys=0.00s Real=0.00s
[1.744s][info][gc           ] GC(7) Concurrent Mark
[1.744s][info][gc,task      ] GC(7) Using 5 workers of 5 for marking
[1.745s][info][gc           ] GC(7) Concurrent Mark 0.877ms
[1.745s][info][gc,cpu       ] GC(7) User=0.00s Sys=0.00s Real=0.00s
[1.745s][info][gc           ] GC(7) Concurrent Preclean
[1.745s][info][gc           ] GC(7) Concurrent Preclean 0.243ms
[1.745s][info][gc,cpu       ] GC(7) User=0.00s Sys=0.00s Real=0.00s
[1.745s][info][gc           ] GC(7) Concurrent Abortable Preclean
...

[10.830s][info][gc,task        ] GC(52) Using 11 workers of 18 for evacuation
[10.842s][info][gc,heap        ] GC(52) ParNew: 139583K->17471K(157248K)
[10.842s][info][gc,heap        ] GC(52) CMS: 68669K->159563K(349568K)
[10.843s][info][gc,metaspace   ] GC(52) Metaspace: 493K(4864K)->493K(4864K) NonClass: 456K(4352K)->456K(4352K) Class: 37K(512K)->37K(512K)
[10.843s][info][gc             ] GC(52) Pause Young (Allocation Failure) 203M->172M(494M) 12.412ms
[10.843s][info][gc,cpu         ] GC(52) User=0.13s Sys=0.00s Real=0.01s
[10.843s][info][gc,start       ] GC(53) Pause Initial Mark
[10.843s][info][gc             ] GC(53) Pause Initial Mark 173M->173M(494M) 0.166ms
[10.843s][info][gc,cpu         ] GC(53) User=0.00s Sys=0.00s Real=0.00s
[10.843s][info][gc             ] GC(53) Concurrent Mark
[10.844s][info][gc,task        ] GC(53) Using 5 workers of 5 for marking
[10.844s][info][gc             ] GC(53) Concurrent Mark 0.878ms
[10.844s][info][gc,cpu         ] GC(53) User=0.00s Sys=0.00s Real=0.00s
[10.844s][info][gc             ] GC(53) Concurrent Preclean
[10.845s][info][gc             ] GC(53) Concurrent Preclean 0.237ms
[10.845s][info][gc,cpu         ] GC(53) User=0.00s Sys=0.00s Real=0.00s
[10.845s][info][gc             ] GC(53) Concurrent Abortable Preclean
[10.944s][info][gc             ] GC(53) Concurrent Abortable Preclean 99.403ms
[10.945s][info][gc,cpu         ] GC(53) User=0.00s Sys=0.00s Real=0.10s
[10.945s][info][gc,start       ] GC(53) Pause Remark
[10.946s][info][gc             ] GC(53) Pause Remark 185M->185M(494M) 0.918ms
[10.947s][info][gc,cpu         ] GC(53) User=0.00s Sys=0.00s Real=0.00s
[10.947s][info][gc             ] GC(53) Concurrent Sweep
[10.948s][info][gc             ] GC(53) Concurrent Sweep 0.670ms
[10.948s][info][gc,cpu         ] GC(53) User=0.00s Sys=0.00s Real=0.00s
[10.948s][info][gc             ] GC(53) Concurrent Reset
[10.949s][info][gc             ] GC(53) Concurrent Reset 0.651ms
[10.949s][info][gc,cpu         ] GC(53) User=0.00s Sys=0.00s Real=0.00s
[10.950s][info][gc,heap        ] GC(53) Old: 159563K->58518K(349568K)
[10.950s][info][gc,heap,exit   ] Heap
[10.950s][info][gc,heap,exit   ]  par new generation   total 157248K, used 30022K [0x00000000e0000000, 0x00000000eaaa0000, 0x00000000eaaa0000)
[10.950s][info][gc,heap,exit   ]   eden space 139776K,   8% used [0x00000000e0000000, 0x00000000e0c41b68, 0x00000000e8880000)
[10.951s][info][gc,heap,exit   ]   from space 17472K,  99% used [0x00000000e8880000, 0x00000000e998ffe0, 0x00000000e9990000)
[10.951s][info][gc,heap,exit   ]   to   space 17472K,   0% used [0x00000000e9990000, 0x00000000e9990000, 0x00000000eaaa0000)
[10.951s][info][gc,heap,exit   ]  concurrent mark-sweep generation total 349568K, used 58518K [0x00000000eaaa0000, 0x0000000100000000, 0x0000000100000000)
[10.951s][info][gc,heap,exit   ]  Metaspace       used 495K, capacity 4553K, committed 4864K, reserved 1056768K
[10.951s][info][gc,heap,exit   ]   class space    used 37K, capacity 410K, committed 512K, reserved 1048576K

```
以一次完整的CMS周期为例（GC(3)到GC(7)），我们可以看到CMS的主要阶段：

1. **Young GC和Initial Mark（初始标记）**：
```
[0.868s][info][gc,start     ] GC(3) Pause Initial Mark
[0.868s][info][gc           ] GC(3) Pause Initial Mark 205M->205M(494M) 0.369ms
```
- STW阶段
- 标记GC Roots直接引用的对象
- 耗时仅0.369ms

2. **Concurrent Mark（并发标记）**：
```
[0.869s][info][gc           ] GC(3) Concurrent Mark
[0.869s][info][gc,task      ] GC(3) Using 5 workers of 5 for marking
[0.870s][info][gc           ] GC(3) Concurrent Mark 1.685ms
```
- 并发阶段，使用5个线程进行标记
- 耗时1.685ms

3. **Concurrent Preclean（并发预清理）**：
```
[0.871s][info][gc           ] GC(3) Concurrent Preclean
[0.871s][info][gc           ] GC(3) Concurrent Preclean 0.353ms
```
- 并发阶段
- 耗时0.353ms

4. **Concurrent Abortable Preclean（可中断的预清理）**：
```
[0.871s][info][gc           ] GC(3) Concurrent Abortable Preclean
[1.521s][info][gc           ] GC(3) Concurrent Abortable Preclean 649.551ms
```
- 并发阶段
- 耗时较长（649.551ms），这是正常的，因为这个阶段是可中断的

5. **Final Remark（最终标记）**：
```
[1.522s][info][gc,start     ] GC(3) Pause Remark
[1.523s][info][gc           ] GC(3) Pause Remark 292M->292M(494M) 0.656ms
```
- STW阶段
- 完成标记工作
- 耗时0.656ms

6. **Concurrent Sweep（并发清除）**：
```
[1.523s][info][gc           ] GC(3) Concurrent Sweep
[1.524s][info][gc           ] GC(3) Concurrent Sweep 0.622ms
```
- 并发阶段
- 清除未被标记的对象
- 耗时0.622ms

7. **Concurrent Reset（并发重置）**：
```
[1.524s][info][gc           ] GC(3) Concurrent Reset
[1.526s][info][gc           ] GC(3) Concurrent Reset 1.456ms
```
- 并发阶段
- 重置CMS算法相关的内部数据结构
- 耗时1.456ms

整个周期的效果：
```
[1.526s][info][gc,heap      ] GC(3) Old: 192239K->115299K(349568K)
```
- 老年代空间从192239K减少到115299K
- 总容量349568K
- 释放了约75MB的内存

整体现象：
```
[10.950s][info][gc,heap,exit   ] Heap
[10.950s][info][gc,heap,exit   ]  par new generation   total 157248K, used 30022K [0x00000000e0000000, 0x00000000eaaa0000, 0x00000000eaaa0000)
[10.950s][info][gc,heap,exit   ]   eden space 139776K,   8% used [0x00000000e0000000, 0x00000000e0c41b68, 0x00000000e8880000)
[10.951s][info][gc,heap,exit   ]   from space 17472K,  99% used [0x00000000e8880000, 0x00000000e998ffe0, 0x00000000e9990000)
[10.951s][info][gc,heap,exit   ]   to   space 17472K,   0% used [0x00000000e9990000, 0x00000000e9990000, 0x00000000eaaa0000)
[10.951s][info][gc,heap,exit   ]  concurrent mark-sweep generation total 349568K, used 58518K [0x00000000eaaa0000, 0x0000000100000000, 0x0000000100000000)
[10.951s][info][gc,heap,exit   ]  Metaspace       used 495K, capacity 4553K, committed 4864K, reserved 1056768K
[10.951s][info][gc,heap,exit   ]   class space    used 37K, capacity 410K, committed 512K, reserved 1048576K
```
1. Heap 概述

`[10.950s][info][gc,heap,exit   ] Heap`

这一行表示垃圾回收已完成，并提供了堆内存的状态信息。

2. 年轻代（ParNew Generation）

`[10.950s][info][gc,heap,exit   ]  par new generation   total 157248K, used 30022K [0x00000000e0000000, 0x00000000eaaa0000, 0x00000000eaaa0000)`

- **总大小**：年轻代的总内存为 157248K。
- **已使用**：当前已使用 30022K。
- **内存地址范围**：年轻代的内存地址从 `0x00000000e0000000` 到 `0x00000000eaaa0000`。

3. Eden 空间

`[10.950s][info][gc,heap,exit   ]   eden space 139776K,   8% used [0x00000000e0000000, 0x00000000e0c41b68, 0x00000000e8880000)`

- **总大小**：Eden 空间的总大小为 139776K。
- **使用率**：当前使用率为 8%，表示在 Eden 空间中仅使用了少量内存。
- **内存地址范围**：Eden 的内存地址从 `0x00000000e0000000` 到 `0x00000000e8880000`。

4. 从空间（From Space）和到空间（To Space）

`[10.951s][info][gc,heap,exit   ]   from space 17472K,  99% used [0x00000000e8880000, 0x00000000e998ffe0, 0x00000000e9990000) [10.951s][info][gc,heap,exit   ]   to   space 17472K,   0% used [0x00000000e9990000, 0x00000000e9990000, 0x00000000eaaa0000)`

- **从空间（From Space）**：
    
    - **总大小**：17472K。
    - **使用率**：99%，意味着几乎所有的内存都在使用中。这是因为在进行年轻代 GC 之前，许多对象被复制到老年代，导致从空间的使用量接近满。
    - **内存地址范围**：从 `0x00000000e8880000` 到 `0x00000000e9990000`。
- **到空间（To Space）**：
    
    - **总大小**：17472K。
    - **使用率**：0%，表示在当前 GC 之后到空间是空的。
    - **内存地址范围**：从 `0x00000000e9990000` 到 `0x00000000eaaa0000`。

 5. 老年代（Concurrent Mark-Sweep Generation）
 
`[10.951s][info][gc,heap,exit   ]  concurrent mark-sweep generation total 349568K, used 58518K [0x00000000eaaa0000, 0x0000000100000000, 0x0000000100000000)`

- **总大小**：老年代的总内存为 349568K。
- **已使用**：当前已使用 58518K，表示老年代中的存活对象数量。
- **内存地址范围**：老年代的内存地址从 `0x00000000eaaa0000` 到 `0x0000000100000000`。

6. Metaspace

`[10.951s][info][gc,heap,exit   ]  Metaspace       used 495K, capacity 4553K, committed 4864K, reserved 1056768K`

- **已使用**：Metaspace 当前使用了 495K，尤其是用于存储类的元数据。
- **容量**：Metaspace 的容量为 4553K，表示在此限制下可以使用的最大内存量。
- **已提交**：已提交的内存量为 4864K，表示当前在物理内存中为 Metaspace 分配的内存。
- **保留**：保留的内存量为 1056768K，表示 JVM 为 Metaspace 保留的最大内存。

 7. 类空间（Class Space）

`[10.951s][info][gc,heap,exit   ]   class space    used 37K, capacity 410K, committed 512K, reserved 1048576K`

- **已使用**：类空间当前使用了 37K。
- **容量**：类空间的容量为 410K。
- **已提交**：类空间已提交的内存量为 512K。
- **保留**：类空间为 1048576K，表示 JVM 为类空间保留的内存。

#### 5.4.3 有趣的现象(怎么GC后还变大了呢)
```
[10.621s][info][gc             ] GC(51) Pause Full (Allocation Failure) 393M->67M(494M) 7.533ms
[10.621s][info][gc,heap        ] GC(50) ParNew: 157163K->0K(157248K)
[10.622s][info][gc,heap        ] GC(50) CMS: 245374K->68669K(349568K)
[10.622s][info][gc,metaspace   ] GC(50) Metaspace: 493K(4864K)->493K(4864K) NonClass: 456K(4352K)->456K(4352K) Class: 37K(512K)->37K(512K)
[10.622s][info][gc             ] GC(50) Pause Young (Allocation Failure) 393M->67M(494M) 8.446ms
[10.622s][info][gc,cpu         ] GC(50) User=0.01s Sys=0.00s Real=0.01s
[10.622s][info][gc,heap        ] GC(49) Old: 245374K->68669K(349568K)
[10.830s][info][gc,start       ] GC(52) Pause Young (Allocation Failure)
[10.830s][info][gc,task        ] GC(52) Using 11 workers of 18 for evacuation
[10.842s][info][gc,heap        ] GC(52) ParNew: 139583K->17471K(157248K)
[10.842s][info][gc,heap        ] GC(52) CMS: 68669K->159563K(349568K)
...
```
1. **GC 事件的基本信息**：
    
    - `GC(51) Pause Full (Allocation Failure) 393M->67M(494M) 7.533ms` 表明这是第 51 次 GC，因内存分配失败触发了 Full GC，堆内存从 393M 降到 67M，经历了约 7.533 毫秒的暂停。
    - `GC(50)` 的信息显示了并行年轻代（ParNew）和并发标记-清除（CMS）的内存使用情况。
2. **内存使用情况**：
    
    - `GC(50) ParNew: 157163K->0K(157248K)`：年轻代（Young Generation）的内存使用情况，GC 前使用了 157163K，GC 后使用为 0K，表示所有存活的对象都被移动到老年代中。
    - `GC(50) CMS: 245374K->68669K(349568K)`：老年代（Old Generation）在 GC 前使用了 245374K，GC 后使用 68669K，总大小为 349568K，表示经过 GC 后，老年代释放了部分内存。
3. **GC 52 的情况**：
    
    - 在 `GC(52)` 中，年轻代再次被触发，使用了 11 个工作线程进行对象的转移。
    - `ParNew: 139583K->17471K(157248K)`：年轻代的使用情况，GC 前使用了 139583K，GC 后使用了 17471K，显示年轻代释放了大量内存。
    - `CMS: 68669K->159563K(349568K)`：这里是关键的部分。虽然前面 CMS 在 GC 前是 68669K，但在 GC 后变为 159563K。这意味着在此 GC 过程中，老年代的内存使用量增加了。

**为什么 CMS 变大了？**

这种情况通常发生在以下几种情况下：

1. **对象的短期存活**：
    
    - 在 Full GC 后，许多对象可能被转移到老年代。这些对象可能在年轻代被清理后又被创建，因此老年代的使用量可能会增加。
2. **生存对象的转移**：
    
    - 在年轻代 GC 后，存活下来的对象会被转移到老年代。如果有大量对象在年轻代中存活，则会导致老年代使用量增加。
3. **内存分配压力**：
    
    - 如果应用程序在 GC 后仍然有较高的内存需求，老年代可能需要分配更多的内存来满足这些需求，从而导致老年代的使用量增加。


#### 5.4.4 CMS收集器特点总结

1. **优点**：
   - 并发收集，大部分工作与用户线程同时进行
   - 低停顿时间（从日志可见，STW阶段如Initial Mark仅0.369ms，Remark仅0.656ms）
   - 分阶段处理，每个阶段职责明确

2. **缺点**：
   - 对CPU资源敏感（需要额外的CPU资源来执行并发收集）
   - 可能产生浮动垃圾（并发清理阶段新产生的垃圾）
   - 可能发生Concurrent Mode Failure（需要降级为Serial Old收集器）

3. **适用场景**：
   - 需要低延迟的Web应用
   - 对响应时间要求高的系统
   - 具有足够CPU资源的服务器环境

4. **调优建议**：
   - 根据日志观察，CMS在本例中表现良好，STW时间都控制在1ms左右
   - 可以通过调整CMSInitiatingOccupancyFraction来控制CMS启动时机
   - 注意监控Concurrent Mode Failure的发生

#### 5.4.5 cms为什么淘汰了
1. **性能问题**：
    
    - CMS 的主要问题是其在处理大量垃圾时的性能下降，尤其是在遇到大量短生命周期对象时，可能会导致频繁的 Full GC（完全垃圾回收），影响应用性能。
2. **内存碎片**：
    
    - CMS 可能会导致内存碎片问题，尤其是在长时间运行的应用中，内存的分配和回收可能会变得不均衡，从而导致可用内存的减少。
3. **替代收集器的引入**：
    
    - Java 8 引入了新的垃圾回收器，例如 G1 垃圾回收器（Garbage-First Garbage Collector），它在处理大内存应用时具有更好的性能。G1 旨在减少停顿时间，更好地处理碎片化问题，并且适应大多数应用程序的需求。

### 5.5 G1收集器分析
**G1 垃圾收集器的特点**：

1. **分区堆**：
    
    - G1 将堆内存分为多个小的区域（region），每个区域可以是 Eden、Survivor 或 Old 区域。这样可以更灵活地管理内存。
2. **增量收集**：
    
    - 与传统的 CMS 不同，G1 可以在需要时进行增量式回收，而不是一次性进行全面的收集。这有助于减少长时间的停顿。
3. **可预测的停顿时间**：
    
    - G1 的设计目标之一是尽量控制垃圾回收的停顿时间，使其更加可预测。用户可以通过设置 `-XX:MaxGCPauseMillis` 参数来定义最大停顿时间目标。
4. **并行和并发**：
    
    - G1 垃圾收集器支持多线程并行处理，同时也在标记阶段中使用并发操作，减少了应用程序的停顿时间。
5. **适应性**：
    
    - G1 垃圾收集器会根据应用程序的运行情况动态调整其收集策略，以优化性能。

#### 5.5.1 运行参数
```bash
java -XX:+UseG1GC -Xms512m -Xmx512m -Xlog:gc*=info:file=gc_g1.log GCCollectorDemo
//jdk9+:
java -XX:+UseG1GC -
Xms512m -Xmx512m -Xlog:gc*=info:file=gc_g1.log -cp target/classes com.example.GCCollectorD
emo
```

#### 5.5.2 GC日志分析
```
[0.010s][info][gc,heap] Heap region size: 1M
[0.013s][info][gc     ] Using G1
[0.013s][info][gc,heap,coops] Heap address: 0x00000000e0000000, size: 512 MB, Compressed Oops mode: 32-bit
[0.014s][info][gc,cds       ] Mark closed archive regions in map: [0x00000000ffe00000, 0x00000000ffe6bff8]
[0.015s][info][gc,cds       ] Mark open archive regions in map: [0x00000000ffc00000, 0x00000000ffc47ff8]
[0.187s][info][gc,start     ] GC(0) Pause Young (Normal) (G1 Evacuation Pause)
[0.188s][info][gc,task      ] GC(0) Using 12 workers of 18 for evacuation
[0.192s][info][gc,phases    ] GC(0)   Pre Evacuate Collection Set: 0.0ms
[0.193s][info][gc,phases    ] GC(0)   Evacuate Collection Set: 3.9ms
[0.193s][info][gc,phases    ] GC(0)   Post Evacuate Collection Set: 0.3ms
[0.193s][info][gc,phases    ] GC(0)   Other: 1.1ms
[0.193s][info][gc,heap      ] GC(0) Eden regions: 25->0(21)
[0.193s][info][gc,heap      ] GC(0) Survivor regions: 0->4(4)
[0.193s][info][gc,heap      ] GC(0) Old regions: 2->17
[0.193s][info][gc,heap      ] GC(0) Humongous regions: 119->99
[0.193s][info][gc,metaspace ] GC(0) Metaspace: 458K(4864K)->458K(4864K) NonClass: 421K(4352K)->421K(4352K) Class: 37K(512K)->37K(512K)
[0.193s][info][gc           ] GC(0) Pause Young (Normal) (G1 Evacuation Pause) 144M->118M(512M) 6.246ms
[0.193s][info][gc,cpu       ] GC(0) User=0.01s Sys=0.01s Real=0.01s
[0.303s][info][gc,start     ] GC(1) Pause Young (Normal) (G1 Evacuation Pause)
[0.303s][info][gc,task      ] GC(1) Using 12 workers of 18 for evacuation
[0.309s][info][gc,phases    ] GC(1)   Pre Evacuate Collection Set: 0.0ms
[0.309s][info][gc,phases    ] GC(1)   Evacuate Collection Set: 5.2ms
[0.309s][info][gc,phases    ] GC(1)   Post Evacuate Collection Set: 0.2ms
[0.309s][info][gc,phases    ] GC(1)   Other: 0.3ms
[0.309s][info][gc,heap      ] GC(1) Eden regions: 21->0(21)
[0.309s][info][gc,heap      ] GC(1) Survivor regions: 4->4(4)
[0.310s][info][gc,heap      ] GC(1) Old regions: 17->38
[0.310s][info][gc,heap      ] GC(1) Humongous regions: 178->126
[0.310s][info][gc,metaspace ] GC(1) Metaspace: 460K(4864K)->460K(4864K) NonClass: 423K(4352K)->423K(4352K) Class: 37K(512K)->37K(512K)
[0.311s][info][gc           ] GC(1) Pause Young (Normal) (G1 Evacuation Pause) 217M->166M(512M) 7.809ms
[0.311s][info][gc,cpu       ] GC(1) User=0.02s Sys=0.01s Real=0.00s
[0.415s][info][gc,start     ] GC(2) Pause Young (Normal) (G1 Evacuation Pause)
[0.415s][info][gc,task      ] GC(2) Using 12 workers of 18 for evacuation
[0.420s][info][gc,phases    ] GC(2)   Pre Evacuate Collection Set: 0.0ms
[0.420s][info][gc,phases    ] GC(2)   Evacuate Collection Set: 3.9ms
[0.420s][info][gc,phases    ] GC(2)   Post Evacuate Collection Set: 0.2ms
[0.420s][info][gc,phases    ] GC(2)   Other: 0.4ms
[0.420s][info][gc,heap      ] GC(2) Eden regions: 21->0(21)
[0.420s][info][gc,heap      ] GC(2) Survivor regions: 4->4(4)
[0.420s][info][gc,heap      ] GC(2) Old regions: 38->59
[0.420s][info][gc,heap      ] GC(2) Humongous regions: 173->108
[0.420s][info][gc,metaspace ] GC(2) Metaspace: 463K(4864K)->463K(4864K) NonClass: 426K(4352K)->426K(4352K) Class: 37K(512K)->37K(512K)
[0.420s][info][gc           ] GC(2) Pause Young (Normal) (G1 Evacuation Pause) 234M->169M(512M) 5.205ms
[0.420s][info][gc,cpu       ] GC(2) User=0.02s Sys=0.00s Real=0.00s
[0.626s][info][gc,start     ] GC(3) Pause Young (Concurrent Start) (G1 Humongous Allocation)
[0.627s][info][gc,task      ] GC(3) Using 12 workers of 18 for evacuation
[0.631s][info][gc,phases    ] GC(3)   Pre Evacuate Collection Set: 0.0ms
[0.631s][info][gc,phases    ] GC(3)   Evacuate Collection Set: 3.9ms
[0.631s][info][gc,phases    ] GC(3)   Post Evacuate Collection Set: 0.2ms
[0.631s][info][gc,phases    ] GC(3)   Other: 0.7ms
[0.631s][info][gc,heap      ] GC(3) Eden regions: 19->0(21)
[0.632s][info][gc,heap      ] GC(3) Survivor regions: 4->4(4)
[0.632s][info][gc,heap      ] GC(3) Old regions: 59->75
[0.632s][info][gc,heap      ] GC(3) Humongous regions: 171->82
[0.632s][info][gc,metaspace ] GC(3) Metaspace: 463K(4864K)->463K(4864K) NonClass: 426K(4352K)->426K(4352K) Class: 37K(512K)->37K(512K)
[0.632s][info][gc           ] GC(3) Pause Young (Concurrent Start) (G1 Humongous Allocation) 250M->159M(512M) 5.622ms
[0.632s][info][gc,cpu       ] GC(3) User=0.02s Sys=0.00s Real=0.00s
[0.632s][info][gc           ] GC(4) Concurrent Cycle
[0.632s][info][gc,marking   ] GC(4) Concurrent Clear Claimed Marks
[0.632s][info][gc,marking   ] GC(4) Concurrent Clear Claimed Marks 0.095ms
[0.632s][info][gc,marking   ] GC(4) Concurrent Scan Root Regions
[0.633s][info][gc,marking   ] GC(4) Concurrent Scan Root Regions 0.421ms
[0.633s][info][gc,marking   ] GC(4) Concurrent Mark (0.633s)
[0.633s][info][gc,marking   ] GC(4) Concurrent Mark From Roots
[0.633s][info][gc,task      ] GC(4) Using 5 workers of 5 for marking
[0.634s][info][gc,marking   ] GC(4) Concurrent Mark From Roots 0.837ms
[0.634s][info][gc,marking   ] GC(4) Concurrent Preclean
[0.634s][info][gc,marking   ] GC(4) Concurrent Preclean 0.113ms
[0.634s][info][gc,marking   ] GC(4) Concurrent Mark (0.633s, 0.634s) 1.181ms
[0.634s][info][gc,start     ] GC(4) Pause Remark
[0.636s][info][gc,stringtable] GC(4) Cleaned string and symbol table, strings: 46 processed, 1 removed, symbols: 95 processed, 0 removed
[0.636s][info][gc            ] GC(4) Pause Remark 200M->160M(512M) 2.037ms
[0.636s][info][gc,cpu        ] GC(4) User=0.01s Sys=0.01s Real=0.00s
[0.637s][info][gc,marking    ] GC(4) Concurrent Rebuild Remembered Sets
[0.637s][info][gc,marking    ] GC(4) Concurrent Rebuild Remembered Sets 0.350ms
[0.637s][info][gc,start      ] GC(4) Pause Cleanup
[0.637s][info][gc            ] GC(4) Pause Cleanup 160M->160M(512M) 0.174ms
[0.637s][info][gc,cpu        ] GC(4) User=0.00s Sys=0.00s Real=0.00s
[0.637s][info][gc,marking    ] GC(4) Concurrent Cleanup for Next Mark
[0.639s][info][gc,marking    ] GC(4) Concurrent Cleanup for Next Mark 1.650ms
[0.639s][info][gc            ] GC(4) Concurrent Cycle 7.110ms
...


```

1. **初始化信息**：
```
[0.010s][info][gc] Using G1
[0.013s][info][gc,heap,coops] Heap address: 0x00000000e0000000, size: 512 MB, Compressed Oops mode: 32-bit
```
- G1将堆空间划分为多个大小相等的Region
- 每个Region大小为1M（Heap region size: 1M）

2. **Young GC过程**（以GC(0)为例）：
```
[0.187s][info][gc,start     ] GC(0) Pause Young (Normal) (G1 Evacuation Pause)
[0.188s][info][gc,task      ] GC(0) Using 12 workers of 18 for evacuation
[0.192s][info][gc,phases    ] GC(0)   Pre Evacuate Collection Set: 0.0ms
[0.193s][info][gc,phases    ] GC(0)   Evacuate Collection Set: 3.9ms
[0.193s][info][gc,phases    ] GC(0)   Post Evacuate Collection Set: 0.3ms
[0.193s][info][gc,phases    ] GC(0)   Other: 1.1ms
```
- 使用12个并发线程进行垃圾收集
- 整个过程分为三个主要阶段：
  - Pre Evacuate：准备阶段
  - Evacuate：转移存活对象
  - Post Evacuate：收尾工作

3. **内存变化**：
```
[0.193s][info][gc,heap      ] GC(0) Eden regions: 25->0(21)
[0.193s][info][gc,heap      ] GC(0) Survivor regions: 0->4(4)
[0.193s][info][gc,heap      ] GC(0) Old regions: 2->17
[0.193s][info][gc,heap      ] GC(0) Humongous regions: 119->99
```
- Region分为四类：Eden、Survivor、Old和Humongous
- 括号中的数字表示目标容量
- Humongous regions用于存储大对象

##### 5.5.2.2 详细的gc过程
```
[10.533s][info][gc            ] GC(99) Pause Young (Concurrent Start) (G1 Humongous Allocation) 245M->186M(512M) 5.629ms
[10.534s][info][gc,cpu        ] GC(99) User=0.01s Sys=0.00s Real=0.01s
[10.534s][info][gc            ] GC(100) Concurrent Cycle
[10.534s][info][gc,marking    ] GC(100) Concurrent Clear Claimed Marks
[10.534s][info][gc,marking    ] GC(100) Concurrent Clear Claimed Marks 0.158ms
[10.534s][info][gc,marking    ] GC(100) Concurrent Scan Root Regions
[10.535s][info][gc,marking    ] GC(100) Concurrent Scan Root Regions 0.228ms
[10.535s][info][gc,marking    ] GC(100) Concurrent Mark (10.535s)
[10.535s][info][gc,marking    ] GC(100) Concurrent Mark From Roots
[10.535s][info][gc,task       ] GC(100) Using 5 workers of 5 for marking
[10.536s][info][gc,marking    ] GC(100) Concurrent Mark From Roots 1.292ms
[10.536s][info][gc,marking    ] GC(100) Concurrent Preclean
[10.537s][info][gc,marking    ] GC(100) Concurrent Preclean 0.199ms
[10.537s][info][gc,marking    ] GC(100) Concurrent Mark (10.535s, 10.537s) 1.997ms
[10.537s][info][gc,start      ] GC(100) Pause Remark
[10.538s][info][gc,stringtable] GC(100) Cleaned string and symbol table, strings: 45 processed, 0 removed, symbols: 95 processed, 0 removed
[10.542s][info][gc            ] GC(100) Pause Remark 203M->149M(512M) 4.924ms
[10.544s][info][gc,cpu        ] GC(100) User=0.00s Sys=0.00s Real=0.01s
[10.544s][info][gc,marking    ] GC(100) Concurrent Rebuild Remembered Sets
[10.544s][info][gc,marking    ] GC(100) Concurrent Rebuild Remembered Sets 0.338ms
[10.545s][info][gc,start      ] GC(100) Pause Cleanup
[10.545s][info][gc            ] GC(100) Pause Cleanup 149M->149M(512M) 0.199ms
[10.545s][info][gc,cpu        ] GC(100) User=0.00s Sys=0.00s Real=0.00s
[10.545s][info][gc,marking    ] GC(100) Concurrent Cleanup for Next Mark
[10.545s][info][gc,marking    ] GC(100) Concurrent Cleanup for Next Mark 0.214ms
[10.545s][info][gc            ] GC(100) Concurrent Cycle 11.590ms
[10.739s][info][gc,start      ] GC(101) Pause Young (Normal) (G1 Evacuation Pause)
[10.740s][info][gc,task       ] GC(101) Using 12 workers of 18 for evacuation
[10.741s][info][gc,phases     ] GC(101)   Pre Evacuate Collection Set: 0.1ms
[10.742s][info][gc,phases     ] GC(101)   Evacuate Collection Set: 0.8ms
[10.742s][info][gc,phases     ] GC(101)   Post Evacuate Collection Set: 0.4ms
[10.742s][info][gc,phases     ] GC(101)   Other: 1.1ms
[10.743s][info][gc,heap       ] GC(101) Eden regions: 21->0(21)
[10.743s][info][gc,heap       ] GC(101) Survivor regions: 4->4(4)
[10.743s][info][gc,heap       ] GC(101) Old regions: 43->57
[10.743s][info][gc,heap       ] GC(101) Humongous regions: 163->77
[10.743s][info][gc,metaspace  ] GC(101) Metaspace: 463K(4864K)->463K(4864K) NonClass: 426K(4352K)->426K(4352K) Class: 37K(512K)->37K(512K)
[10.744s][info][gc            ] GC(101) Pause Young (Normal) (G1 Evacuation Pause) 229M->136M(512M) 4.651ms
[10.744s][info][gc,cpu        ] GC(101) User=0.00s Sys=0.00s Real=0.01s
[10.850s][info][gc,start      ] GC(102) Pause Young (Normal) (G1 Evacuation Pause)
[10.851s][info][gc,task       ] GC(102) Using 12 workers of 18 for evacuation
[10.854s][info][gc,phases     ] GC(102)   Pre Evacuate Collection Set: 0.0ms
[10.855s][info][gc,phases     ] GC(102)   Evacuate Collection Set: 2.7ms
[10.855s][info][gc,phases     ] GC(102)   Post Evacuate Collection Set: 0.8ms
[10.855s][info][gc,phases     ] GC(102)   Other: 0.6ms
[10.855s][info][gc,heap       ] GC(102) Eden regions: 21->0(21)
[10.855s][info][gc,heap       ] GC(102) Survivor regions: 4->4(4)
[10.855s][info][gc,heap       ] GC(102) Old regions: 57->78
[10.855s][info][gc,heap       ] GC(102) Humongous regions: 158->115
[10.856s][info][gc,metaspace  ] GC(102) Metaspace: 463K(4864K)->463K(4864K) NonClass: 426K(4352K)->426K(4352K) Class: 37K(512K)->37K(512K)
[10.856s][info][gc            ] GC(102) Pause Young (Normal) (G1 Evacuation Pause) 238M->195M(512M) 5.433ms
[10.856s][info][gc,cpu        ] GC(102) User=0.02s Sys=0.00s Real=0.01s
[10.957s][info][gc,heap,exit  ] Heap
[10.959s][info][gc,heap,exit  ]  garbage-first heap   total 524288K, used 207286K [0x00000000e0000000, 0x0000000100000000)
[10.968s][info][gc,heap,exit  ]   region size 1024K, 6 young (6144K), 4 survivors (4096K)
[10.969s][info][gc,heap,exit  ]  Metaspace       used 464K, capacity 4553K, committed 4864K, reserved 1056768K
[10.969s][info][gc,heap,exit  ]   class space    used 37K, capacity 410K, committed 512K, reserved 1048576K
```
这个 GC 日志输出详细记录了 JVM 在进行垃圾回收过程中的多个步骤和内存状态。下面逐项分析这段日志：

1. GC 事件概述
```plaintext
[10.533s][info][gc            ] GC(99) Pause Young (Concurrent Start) (G1 Humongous Allocation) 245M->186M(512M) 5.629ms
```
- **GC 事件编号**：第 99 次 GC。
- **类型**：年轻代的暂停（Pause Young），这是一次由于 "G1 Humongous Allocation" 触发的 GC。
- **内存变化**：堆内存从 245M 降到 186M，当前堆的总大小为 512M。
- **暂停时间**：5.629 毫秒。

2. CPU 时间
```plaintext
[10.534s][info][gc,cpu        ] GC(99) User=0.01s Sys=0.00s Real=0.01s
```
- **用户 CPU 时间**：0.01 秒。
- **系统 CPU 时间**：0.00 秒。
- **实际经过时间**：0.01 秒，表示总的 GC 时间。

3. 并发周期
```plaintext
[10.534s][info][gc            ] GC(100) Concurrent Cycle
```
- 表示开始第 100 次的并发 GC 周期。

4. 标记过程
```plaintext
[10.534s][info][gc,marking    ] GC(100) Concurrent Clear Claimed Marks
[10.534s][info][gc,marking    ] GC(100) Concurrent Clear Claimed Marks 0.158ms
```
- **清除标记**：清除已声明的标记，时间为 0.158 毫秒。

```plaintext
[10.534s][info][gc,marking    ] GC(100) Concurrent Scan Root Regions
[10.535s][info][gc,marking    ] GC(100) Concurrent Scan Root Regions 0.228ms
```
- **扫描根区域**：扫描根对象，时间为 0.228 毫秒。

5. 并发标记过程
```plaintext
[10.535s][info][gc,marking    ] GC(100) Concurrent Mark (10.535s)
[10.535s][info][gc,marking    ] GC(100) Concurrent Mark From Roots
[10.535s][info][gc,task       ] GC(100) Using 5 workers of 5 for marking
[10.536s][info][gc,marking    ] GC(100) Concurrent Mark From Roots 1.292ms
```
- **并发标记**：这个阶段使用了 5 个工作线程进行标记，标记根对象的时间为 1.292 毫秒。

6. 预清理和标记时间
```plaintext
[10.536s][info][gc,marking    ] GC(100) Concurrent Preclean
[10.537s][info][gc,marking    ] GC(100) Concurrent Preclean 0.199ms
```
- **预清理**：为下一步标记做准备，时间为 0.199 毫秒。

```plaintext
[10.537s][info][gc,marking    ] GC(100) Concurrent Mark (10.535s, 10.537s) 1.997ms
```
- **整个并发标记时间**：2 毫秒。

7. 暂停备注
```plaintext
[10.537s][info][gc,start      ] GC(100) Pause Remark
[10.538s][info][gc,stringtable] GC(100) Cleaned string and symbol table, strings: 45 processed, 0 removed, symbols: 95 processed, 0 removed
```
- 这部分表示开始了备注阶段，并且清除了字符串和符号表中的一些数据。

```plaintext
[10.542s][info][gc            ] GC(100) Pause Remark 203M->149M(512M) 4.924ms
```
- **备注内存变化**：从 203M 降到 149M，堆的总大小为 512M，暂停时间为 4.924 毫秒。

8. 并发重建
```plaintext
[10.544s][info][gc,marking    ] GC(100) Concurrent Rebuild Remembered Sets
[10.544s][info][gc,marking    ] GC(100) Concurrent Rebuild Remembered Sets 0.338ms
```
- **重建记住的集合**：时间为 0.338 毫秒。

9. 清理阶段
```plaintext
[10.545s][info][gc,start      ] GC(100) Pause Cleanup
[10.545s][info][gc            ] GC(100) Pause Cleanup 149M->149M(512M) 0.199ms
```
- **清理内存变化**：总内存没有变化，暂存从 149M 到 149M，暂停时间为 0.199 毫秒。

10. 并发清理
```plaintext
[10.545s][info][gc,marking    ] GC(100) Concurrent Cleanup for Next Mark
[10.545s][info][gc,marking    ] GC(100) Concurrent Cleanup for Next Mark 0.214ms
```
- **并发清理**：为下一次标记做清理，时间为 0.214 毫秒。

11. 并发周期结束
```plaintext
[10.545s][info][gc            ] GC(100) Concurrent Cycle 11.590ms
```
- **并发周期总时间**：11.590 毫秒。

12. 第二次年轻代 GC
```plaintext
[10.739s][info][gc,start      ] GC(101) Pause Young (Normal) (G1 Evacuation Pause)
[10.740s][info][gc,task       ] GC(101) Using 12 workers of 18 for evacuation
```
- **第 101 次 GC**：正常年轻代的暂停，使用了 12 个工作线程进行回收。

13. 各个阶段的时间
```plaintext
[10.741s][info][gc,phases     ] GC(101)   Pre Evacuate Collection Set: 0.1ms
[10.742s][info][gc,phases     ] GC(101)   Evacuate Collection Set: 0.8ms
[10.742s][info][gc,phases     ] GC(101)   Post Evacuate Collection Set: 0.4ms
[10.742s][info][gc,phases     ] GC(101)   Other: 1.1ms
```
- **各阶段时间**：
  - **预回收**：0.1 毫秒
  - **回收集合**：0.8 毫秒
  - **后回收**：0.4 毫秒
  - **其他**：1.1 毫秒

14. 堆内存状态
```plaintext
[10.743s][info][gc,heap       ] GC(101) Eden regions: 21->0(21)
[10.743s][info][gc,heap       ] GC(101) Survivor regions: 4->4(4)
[10.743s][info][gc,heap       ] GC(101) Old regions: 43->57
[10.743s][info][gc,heap       ] GC(101) Humongous regions: 163->77
```
- **Eden 区域**：从 21 个区域降到 0，表示所有对象都被清理。
- **Survivor 区域**：保持不变，仍为 4 个区域。
- **Old 区域**：从 43 增加到 57，表示存活对象转移到老年代。
- **Humongous 区域**：从 163 降到 77，表明较大的对象数量减少。

15. Metaspace 状态
```plaintext
[10.743s][info][gc,metaspace  ] GC(101) Metaspace: 463K(4864K)->463K(4864K) NonClass: 426K(4352K)->426K(4352K) Class: 37K(512K)->37K(512K)
```
- **Metaspace 使用**：未变化，保持在 463K。
- **类的元数据**：非类部分和类部分的使用量未变，分别为 426K 和 37K。

16. GC 结果
```plaintext
[10.744s][info][gc            ] GC(101) Pause Young (Normal) (G1 Evacuation Pause) 229M->136M(512M) 4.651ms
```
- **GC 结果**：经过这次 GC，堆内存从 229M 降到 136M，暂停时间为 4.651 毫秒。

17. 第三次年轻代 GC
```plaintext
[10.850s][info][gc,start      ] GC(102) Pause Young (Normal) (G1 Evacuation Pause)
[10.851s][info][gc,task       ] GC(102) Using 12 workers of 18 for evacuation
```
- **第 102 次 GC**：再次进行年轻代的正常暂停，使用了 12 个工作线程。

18. 各个阶段的时间
```plaintext
[10.854s][info][gc,phases     ] GC(102)   Pre Evacuate Collection Set: 0.0ms
[10.855s][info][gc,phases     ] GC(102)   Evacuate Collection Set: 2.7ms
[10.855s][info][gc,phases     ] GC(102)   Post Evacuate Collection Set: 0.8ms
[10.855s][info][gc,phases     ] GC(102)   Other: 0.6ms
```
- **各阶段时间**：
  - **预回收**：0.0 毫秒
  - **回收集合**：2.7 毫秒
  - **后回收**：0.8 毫秒
  - **其他**：0.6 毫秒

19. 堆内存状态
```plaintext
[10.855s][info][gc,heap       ] GC(102) Eden regions: 21->0(21)
[10.855s][info][gc,heap       ] GC(102) Survivor regions: 4->4(4)
[10.855s][info][gc,heap       ] GC(102) Old regions: 57->78
[10.855s][info][gc,heap       ] GC(102) Humongous regions: 158->115
```
- **Eden 区域**：从 21 降到 0，所有对象被清理。
- **Survivor 区域**：保持不变，4 个区域。
- **Old 区域**：从 57 增加到 78。
- **Humongous 区域**：从 158 降到 115。

20. Metaspace 状态
```plaintext
[10.856s][info][gc,metaspace  ] GC(102) Metaspace: 463K(4864K)->463K(4864K) NonClass: 426K(4352K)->426K(4352K) Class: 37K(512K)->37K(512K)
```
- **Metaspace 使用**：未变化，保持在 463K。

21. GC 结果
```plaintext
[10.856s][info][gc            ] GC(102) Pause Young (Normal) (G1 Evacuation Pause) 238M->195M(512M) 5.433ms
```
- **GC 结果**：堆内存从 238M 降到 195M，暂停时间为 5.433 毫秒。

22. 堆内存总结
```plaintext
[10.957s][info][gc,heap,exit  ] Heap
[10.959s][info][gc,heap,exit  ]  garbage-first heap   total 524288K, used 207286K [0x00000000e0000000, 0x0000000100000000)
```
- **堆总大小**：524288K（512MB），使用 207286K。

23. 堆区域大小
```plaintext
[10.968s][info][gc,heap,exit  ]   region size 1024K, 6 young (6144K), 4 survivors (4096K)
```
- **区域大小**：每个区域 1024K。
- **年轻代区域数**：6 个，总计 6144K。
- **生存区域数**：4 个，总计 4096K。

24. Metaspace 状态总结
```plaintext
[10.969s][info][gc,heap,exit  ]  Metaspace       used 464K, capacity 4553K, committed 4864K, reserved 1056768K
[10.969s][info][gc,heap,exit  ]   class space    used 37K, capacity 410K, committed 512K, reserved 1048576K
```
- **Metaspace 使用**：464K，容量 4553K，已提交 4864K，保留 1056768K。
- **类空间**：使用 37K，容量 410K，已提交 512K，保留 1048576K。


#### 5.5.3 三种收集器对比

| 特性 | Parallel GC | CMS | G1 |
|------|-------------|-----|------|
| 内存布局 | 传统分代 | 传统分代 | Region化分区 |
| 工作模式 | 全线程STW | 并发标记 | 混合式回收 |
| 停顿时间 | 较长(~10ms) | 较短(~1ms) | 可预测(~5ms) |
| 吞吐量 | 最高 | 中等 | 中等 |
| CPU开销 | 低 | 较高 | 中等 |
| 内存碎片 | 少 | 较多 | 少 |
| 可预测性 | 差 | 一般 | 好 |

#### 5.5.4 适用场景

1. **Parallel GC**：
   - 批处理应用
   - 后台计算任务
   - 对吞吐量要求高的场景

2. **CMS**：
   - 老年代对象较多的Web应用
   - 对响应时间要求高的系统
   - 具有足够CPU资源的服务器

3. **G1**：
   - 大内存服务器(>4GB)
   - 需要严格控制停顿时间
   - 对内存碎片敏感的应用

#### 5.5.5 选择建议

1. **堆内存<4GB**：
   - 优先使用Parallel GC
   - 对延迟要求高可选CMS

2. **堆内存4-8GB**：
   - 建议使用G1
   - 对吞吐量要求极高可选Parallel GC

3. **堆内存>8GB**：
   - 首选G1
   - 特殊场景可考虑ZGC



### 5.6 ZGC收集器分析
>极低延迟，大内存堆
#### 5.6.1 运行参数
```bash
java -XX:+UnlockExperimentalVMOptions -XX:+UseZGC -Xms512m -Xmx512m -Xlog:gc*=info:file=gc_zgc.log GCCollectorDemo
```

#### 5.6.2 GC日志分析
日志概览：
```
[0.013s][info][gc,init] Initializing The Z Garbage Collector
[0.013s][info][gc,init] Version: 11.0.25+9-post-Ubuntu-1ubuntu122.04 (release)
[0.014s][info][gc,init] NUMA Support: Disabled
[0.014s][info][gc,init] CPUs: 24 total, 24 available
[0.014s][info][gc,init] Memory: 32019M
[0.014s][info][gc,init] Large Page Support: Disabled
[0.014s][info][gc,init] Workers: 15 parallel, 3 concurrent
[0.015s][info][gc,init] Heap backed by file: /memfd:java_heap
[0.015s][info][gc,init] Available space on backing filesystem: N/A
[0.034s][info][gc,init] Pre-touching: Disabled
[0.034s][info][gc,init] Pre-mapping: 450M
[0.040s][info][gc,init] Runtime Workers: 15 parallel
[0.041s][info][gc     ] Using The Z Garbage Collector
[0.154s][info][gc,start] GC(0) Garbage Collection (Warmup)
[0.154s][info][gc,phases] GC(0) Pause Mark Start 0.141ms
[0.156s][info][gc,phases] GC(0) Concurrent Mark 1.549ms
[0.156s][info][gc,phases] GC(0) Pause Mark End 0.252ms
[0.156s][info][gc,phases] GC(0) Concurrent Process Non-Strong References 0.060ms
[0.157s][info][gc,phases] GC(0) Concurrent Reset Relocation Set 0.000ms
[0.157s][info][gc,phases] GC(0) Concurrent Destroy Detached Pages 0.000ms
[0.158s][info][gc,phases] GC(0) Concurrent Select Relocation Set 0.850ms
[0.158s][info][gc,phases] GC(0) Concurrent Prepare Relocation Set 0.074ms
[0.159s][info][gc,phases] GC(0) Pause Relocate Start 0.927ms
[0.164s][info][gc,phases] GC(0) Concurrent Relocate 4.967ms
[0.164s][info][gc,load  ] GC(0) Load: 0.10/0.09/0.07
[0.164s][info][gc,mmu   ] GC(0) MMU: 2ms/53.6%, 5ms/73.6%, 10ms/86.8%, 20ms/93.4%, 50ms/97.4%, 100ms/98.7%
[0.164s][info][gc,marking] GC(0) Mark: 2 stripe(s), 1 proactive flush(es), 1 terminate flush(es), 0 completion(s), 0 continuation(s) 
[0.164s][info][gc,reloc  ] GC(0) Relocation: Successful, 44M relocated
[0.164s][info][gc,nmethod] GC(0) NMethods: 40 registered, 0 unregistered
[0.164s][info][gc,metaspace] GC(0) Metaspace: 4M used, 4M capacity, 4M committed, 8M reserved
[0.165s][info][gc,ref      ] GC(0) Soft: 117 encountered, 0 discovered, 0 enqueued
[0.165s][info][gc,ref      ] GC(0) Weak: 236 encountered, 198 discovered, 59 enqueued
[0.165s][info][gc,ref      ] GC(0) Final: 0 encountered, 0 discovered, 0 enqueued
[0.165s][info][gc,ref      ] GC(0) Phantom: 3 encountered, 3 discovered, 2 enqueued
[0.165s][info][gc,heap     ] GC(0)                Mark Start          Mark End        Relocate Start      Relocate End           High               Low         
[0.165s][info][gc,heap     ] GC(0)  Capacity:      512M (100%)        512M (100%)        512M (100%)        512M (100%)        512M (100%)        512M (100%)   
[0.165s][info][gc,heap     ] GC(0)   Reserve:       62M (12%)          62M (12%)          62M (12%)          62M (12%)          62M (12%)          62M (12%)    
[0.165s][info][gc,heap     ] GC(0)      Free:      338M (66%)         338M (66%)         338M (66%)         352M (69%)         352M (69%)         244M (48%)    
[0.165s][info][gc,heap     ] GC(0)      Used:      112M (22%)         112M (22%)         112M (22%)          98M (19%)         206M (40%)          98M (19%)    
[0.165s][info][gc,heap     ] GC(0)      Live:         -                46M (9%)           46M (9%)           46M (9%)             -                  -          
[0.165s][info][gc,heap     ] GC(0) Allocated:         -                 0M (0%)            0M (0%)           94M (18%)            -                  -          
[0.165s][info][gc,heap     ] GC(0)   Garbage:         -                65M (13%)          65M (13%)          65M (13%)            -                  -          
[0.165s][info][gc,heap     ] GC(0) Reclaimed:         -                  -                 0M (0%)            0M (0%)             -                  -          
[0.165s][info][gc          ] GC(0) Garbage Collection (Warmup) 112M(22%)->98M(19%)
[0.254s][info][gc,start    ] GC(1) Garbage Collection (Warmup)
[0.254s][info][gc,phases   ] GC(1) Pause Mark Start 0.245ms
[0.257s][info][gc,phases   ] GC(1) Concurrent Mark 2.111ms
[0.258s][info][gc,phases   ] GC(1) Pause Mark End 0.390ms
[0.258s][info][gc,phases   ] GC(1) Concurrent Process Non-Strong References 0.118ms
[0.258s][info][gc,phases   ] GC(1) Concurrent Reset Relocation Set 0.026ms
[0.258s][info][gc,phases   ] GC(1) Concurrent Destroy Detached Pages 0.001ms
[0.260s][info][gc,phases   ] GC(1) Concurrent Select Relocation Set 1.051ms
[0.260s][info][gc,phases   ] GC(1) Concurrent Prepare Relocation Set 0.098ms
...
```

1. **初始化信息**：
```
[0.013s][info][gc,init] Initializing The Z Garbage Collector
[0.014s][info][gc,init] CPUs: 24 total, 24 available
[0.014s][info][gc,init] Memory: 32019M
[0.014s][info][gc,init] Workers: 15 parallel, 3 concurrent
```
- ZGC使用了24个CPU核心
- 配置了15个并行工作线程和3个并发工作线程

2. **GC周期分析**（以GC(0)为例）：
```
[0.154s][info][gc,start] GC(0) Garbage Collection (Warmup)
[0.154s][info][gc,phases] GC(0) Pause Mark Start 0.141ms
[0.156s][info][gc,phases] GC(0) Concurrent Mark 1.549ms
[0.156s][info][gc,phases] GC(0) Pause Mark End 0.252ms
[0.156s][info][gc,phases] GC(0) Concurrent Process Non-Strong References 0.060ms
[0.157s][info][gc,phases] GC(0) Concurrent Reset Relocation Set 0.000ms
[0.158s][info][gc,phases] GC(0) Concurrent Select Relocation Set 0.850ms
[0.158s][info][gc,phases] GC(0) Concurrent Prepare Relocation Set 0.074ms
[0.159s][info][gc,phases] GC(0) Pause Relocate Start 0.927ms
[0.164s][info][gc,phases] GC(0) Concurrent Relocate 4.967ms
```

ZGC的回收过程分为以下几个主要阶段：
- **Mark Start Pause**: 初始标记暂停（0.141ms）
- **Concurrent Mark**: 并发标记（1.549ms）
- **Mark End Pause**: 最终标记暂停（0.252ms）
- **Concurrent Process References**: 并发处理引用（0.060ms）
- **Concurrent Reset/Select/Prepare**: 准备重定位集（~0.924ms）
- **Relocate Start Pause**: 重定位开始暂停（0.927ms）
- **Concurrent Relocate**: 并发重定位（4.967ms）

3. **内存使用情况**：
```
[0.165s][info][gc,heap     ] GC(0)  Capacity:      512M (100%)        512M (100%)   
[0.165s][info][gc,heap     ] GC(0)   Reserve:       62M (12%)          62M (12%)    
[0.165s][info][gc,heap     ] GC(0)      Free:      338M (66%)         352M (69%)    
[0.165s][info][gc,heap     ] GC(0)      Used:      112M (22%)          98M (19%)    
[0.165s][info][gc,heap     ] GC(0)      Live:         -                46M (9%)      
```

```
[9.760s][info][gc,ref      ] GC(21) Soft: 117 encountered, 0 discovered, 0 enqueued
[9.760s][info][gc,ref      ] GC(21) Weak: 177 encountered, 168 discovered, 0 enqueued
[9.760s][info][gc,ref      ] GC(21) Final: 0 encountered, 0 discovered, 0 enqueued
[9.761s][info][gc,ref      ] GC(21) Phantom: 1 encountered, 0 discovered, 0 enqueued
[9.761s][info][gc,heap     ] GC(21)                Mark Start          Mark End        Relocate Start      Relocate End           High               Low         
[9.761s][info][gc,heap     ] GC(21)  Capacity:      512M (100%)        512M (100%)        512M (100%)        512M (100%)        512M (100%)        512M (100%)   
[9.761s][info][gc,heap     ] GC(21)   Reserve:       62M (12%)          62M (12%)          62M (12%)          62M (12%)          62M (12%)          62M (12%)    
[9.761s][info][gc,heap     ] GC(21)      Free:      102M (20%)         102M (20%)         306M (60%)         344M (67%)         344M (67%)         102M (20%)    
[9.761s][info][gc,heap     ] GC(21)      Used:      348M (68%)         348M (68%)         144M (28%)         106M (21%)         348M (68%)         106M (21%)    
[9.761s][info][gc,heap     ] GC(21)      Live:         -                69M (14%)          69M (14%)          69M (14%)            -                  -          
[9.761s][info][gc,heap     ] GC(21) Allocated:         -                 0M (0%)            0M (0%)           38M (7%)             -                  -          
[9.761s][info][gc,heap     ] GC(21)   Garbage:         -               278M (54%)          74M (15%)          36M (7%)             -                  -          
[9.761s][info][gc,heap     ] GC(21) Reclaimed:         -                  -               204M (40%)         242M (47%)            -                  -          
[9.761s][info][gc          ] GC(21) Garbage Collection (Allocation Rate) 348M(68%)->106M(21%)
```
![[Pasted image 20241208145104.png]]
- **Soft, Weak, Final, Phantom**：分别处理了117个软引用、177个弱引用、0个终结引用和1个幽灵引用。这些信息表明GC在处理不同类型的引用方面运行正常。
- **Heap Capacity**：堆容量为512MB，已使用348MB（68%），可用102MB（20%）。
- **Garbage**：回收的垃圾量为278MB（54%），此次GC后，堆使用情况变更为106MB（21%）。

这些信息表明，GC有效地回收了大量的垃圾，降低了堆的使用率。

```
[10.054s][info][gc,stats    ] === Garbage Collection Statistics =======================================================================================================================
[10.054s][info][gc,stats    ]                                                              Last 10s              Last 10m              Last 10h                Total
[10.054s][info][gc,stats    ]                                                              Avg / Max             Avg / Max             Avg / Max             Avg / Max
[10.054s][info][gc,stats    ]   Collector: Garbage Collection Cycle                      9.086 / 19.305        9.086 / 19.305        9.086 / 19.305        9.086 / 19.305      ms
[10.054s][info][gc,stats    ]  Contention: Mark Segment Reset Contention                     0 / 3                 0 / 3                 0 / 3                 0 / 3           ops/s
[10.054s][info][gc,stats    ]  Contention: Mark SeqNum Reset Contention                      0 / 0                 0 / 0                 0 / 0                 0 / 0           ops/s
[10.054s][info][gc,stats    ]  Contention: Relocation Contention                             0 / 2                 0 / 2                 0 / 2                 0 / 2           ops/s
[10.054s][info][gc,stats    ]    Critical: Allocation Stall                              0.000 / 0.000         0.000 / 0.000         0.000 / 0.000         0.000 / 0.000       ms
[10.055s][info][gc,stats    ]    Critical: Allocation Stall                                  0 / 0                 0 / 0                 0 / 0                 0 / 0           ops/s
[10.055s][info][gc,stats    ]    Critical: GC Locker Stall                               0.000 / 0.000         0.000 / 0.000         0.000 / 0.000         0.000 / 0.000       ms
[10.055s][info][gc,stats    ]    Critical: GC Locker Stall                                   0 / 0                 0 / 0                 0 / 0                 0 / 0           ops/s
[10.055s][info][gc,stats    ]      Memory: Allocation Rate                                 518 / 612             518 / 612             518 / 612             518 / 612         MB/s
[10.055s][info][gc,stats    ]      Memory: Heap Used After Mark                            317 / 384             317 / 384             317 / 384             317 / 384         MB
[10.055s][info][gc,stats    ]      Memory: Heap Used After Relocation                       96 / 110              96 / 110              96 / 110              96 / 110         MB
[10.055s][info][gc,stats    ]      Memory: Heap Used Before Mark                           317 / 384             317 / 384             317 / 384             317 / 384         MB
[10.055s][info][gc,stats    ]      Memory: Heap Used Before Relocation                     131 / 148             131 / 148             131 / 148             131 / 148         MB
[10.055s][info][gc,stats    ]      Memory: Out Of Memory                                     0 / 0                 0 / 0                 0 / 0                 0 / 0           ops/s
[10.055s][info][gc,stats    ]      Memory: Page Cache Flush                                  0 / 0                 0 / 0                 0 / 0                 0 / 0           MB/s
[10.055s][info][gc,stats    ]      Memory: Page Cache Hit L1                                41 / 46               41 / 46               41 / 46               41 / 46          ops/s
[10.056s][info][gc,stats    ]      Memory: Page Cache Hit L2                                 0 / 0                 0 / 0                 0 / 0                 0 / 0           ops/s
[10.056s][info][gc,stats    ]      Memory: Page Cache Miss                                   3 / 31                3 / 31                3 / 31                3 / 31          ops/s
[10.056s][info][gc,stats    ]      Memory: Undo Object Allocation Failed                     0 / 0                 0 / 0                 0 / 0                 0 / 0           ops/s
[10.056s][info][gc,stats    ]      Memory: Undo Object Allocation Succeeded                  0 / 2                 0 / 2                 0 / 2                 0 / 2           ops/s
[10.056s][info][gc,stats    ]      Memory: Undo Page Allocation                              0 / 1                 0 / 1                 0 / 1                 0 / 1           ops/s
[10.056s][info][gc,stats    ]       Phase: Concurrent Destroy Detached Pages             0.001 / 0.001         0.001 / 0.001         0.001 / 0.001         0.001 / 0.001       ms
[10.056s][info][gc,stats    ]       Phase: Concurrent Mark                               2.042 / 3.351         2.042 / 3.351         2.042 / 3.351         2.042 / 3.351       ms
[10.056s][info][gc,stats    ]       Phase: Concurrent Mark Continue                      0.000 / 0.000         0.000 / 0.000         0.000 / 0.000         0.000 / 0.000       ms
[10.056s][info][gc,stats    ]       Phase: Concurrent Prepare Relocation Set             0.018 / 0.098         0.018 / 0.098         0.018 / 0.098         0.018 / 0.098       ms
[10.056s][info][gc,stats    ]       Phase: Concurrent Process Non-Strong References      0.125 / 0.229         0.125 / 0.229         0.125 / 0.229         0.125 / 0.229       ms
[10.056s][info][gc,stats    ]       Phase: Concurrent Relocate                           1.412 / 4.967         1.412 / 4.967         1.412 / 4.967         1.412 / 4.967       ms
[10.056s][info][gc,stats    ]       Phase: Concurrent Reset Relocation Set               0.003 / 0.026         0.003 / 0.026         0.003 / 0.026         0.003 / 0.026       ms
[10.056s][info][gc,stats    ]       Phase: Concurrent Select Relocation Set              1.146 / 2.127         1.146 / 2.127         1.146 / 2.127         1.146 / 2.127       ms
[10.056s][info][gc,stats    ]       Phase: Pause Mark End                                0.369 / 0.555         0.369 / 0.555         0.369 / 0.555         0.369 / 0.555       ms
[10.056s][info][gc,stats    ]       Phase: Pause Mark Start                              0.291 / 0.558         0.291 / 0.558         0.291 / 0.558         0.291 / 0.558       ms
[10.056s][info][gc,stats    ]       Phase: Pause Relocate Start                          0.194 / 0.927         0.194 / 0.927         0.194 / 0.927         0.194 / 0.927       ms
[10.056s][info][gc,stats    ]    Subphase: Concurrent Mark                               1.624 / 3.143         1.624 / 3.143         1.624 / 3.143         1.624 / 3.143       ms
[10.057s][info][gc,stats    ]    Subphase: Concurrent Mark Idle                          1.095 / 1.212         1.095 / 1.212         1.095 / 1.212         1.095 / 1.212       ms
[10.057s][info][gc,stats    ]    Subphase: Concurrent Mark Try Flush                     0.088 / 0.316         0.088 / 0.316         0.088 / 0.316         0.088 / 0.316       ms
[10.057s][info][gc,stats    ]    Subphase: Concurrent Mark Try Terminate                 0.564 / 1.214         0.564 / 1.214         0.564 / 1.214         0.564 / 1.214       ms
[10.057s][info][gc,stats    ]    Subphase: Concurrent References Enqueue                 0.001 / 0.009         0.001 / 0.009         0.001 / 0.009         0.001 / 0.009       ms
[10.057s][info][gc,stats    ]    Subphase: Concurrent References Process                 0.048 / 0.112         0.048 / 0.112         0.048 / 0.112         0.048 / 0.112       ms
[10.057s][info][gc,stats    ]    Subphase: Concurrent Weak Roots                         0.025 / 0.058         0.025 / 0.058         0.025 / 0.058         0.025 / 0.058       ms
[10.057s][info][gc,stats    ]    Subphase: Concurrent Weak Roots JNIWeakHandles          0.000 / 0.000         0.000 / 0.000         0.000 / 0.000         0.000 / 0.000       ms
[10.057s][info][gc,stats    ]    Subphase: Concurrent Weak Roots StringTable             0.022 / 0.044         0.022 / 0.044         0.022 / 0.044         0.022 / 0.044       ms
[10.057s][info][gc,stats    ]    Subphase: Concurrent Weak Roots VMWeakHandles           0.008 / 0.015         0.008 / 0.015         0.008 / 0.015         0.008 / 0.015       ms
[10.057s][info][gc,stats    ]    Subphase: Pause Mark Try Complete                       0.000 / 0.000         0.000 / 0.000         0.000 / 0.000         0.000 / 0.000       ms
[10.057s][info][gc,stats    ]    Subphase: Pause Remap TLABS                             0.001 / 0.001         0.001 / 0.001         0.001 / 0.001         0.001 / 0.001       ms
[10.057s][info][gc,stats    ]    Subphase: Pause Retire TLABS                            0.006 / 0.018         0.006 / 0.018         0.006 / 0.018         0.006 / 0.018       ms
[10.057s][info][gc,stats    ]    Subphase: Pause Roots                                   0.015 / 0.851         0.015 / 0.851         0.015 / 0.851         0.015 / 0.851       ms
[10.057s][info][gc,stats    ]    Subphase: Pause Roots ClassLoaderDataGraph              0.094 / 0.851         0.094 / 0.851         0.094 / 0.851         0.094 / 0.851       ms
[10.057s][info][gc,stats    ]    Subphase: Pause Roots CodeCache                         0.011 / 0.037         0.011 / 0.037         0.011 / 0.037         0.011 / 0.037       ms
[10.057s][info][gc,stats    ]    Subphase: Pause Roots JNIHandles                        0.015 / 0.751         0.015 / 0.751         0.015 / 0.751         0.015 / 0.751       ms
[10.058s][info][gc,stats    ]    Subphase: Pause Roots JNIWeakHandles                    0.000 / 0.000         0.000 / 0.000         0.000 / 0.000         0.000 / 0.000       ms
[10.058s][info][gc,stats    ]    Subphase: Pause Roots JRFWeak                           0.000 / 0.000         0.000 / 0.000         0.000 / 0.000         0.000 / 0.000       ms
[10.058s][info][gc,stats    ]    Subphase: Pause Roots JVMTIExport                       0.001 / 0.001         0.001 / 0.001         0.001 / 0.001         0.001 / 0.001       ms
[10.058s][info][gc,stats    ]    Subphase: Pause Roots JVMTIWeakExport                   0.000 / 0.001         0.000 / 0.001         0.000 / 0.001         0.000 / 0.001       ms
[10.058s][info][gc,stats    ]    Subphase: Pause Roots Management                        0.001 / 0.006         0.001 / 0.006         0.001 / 0.006         0.001 / 0.006       ms
[10.058s][info][gc,stats    ]    Subphase: Pause Roots ObjectSynchronizer                0.000 / 0.001         0.000 / 0.001         0.000 / 0.001         0.000 / 0.001       ms
[10.058s][info][gc,stats    ]    Subphase: Pause Roots Setup                             0.002 / 0.010         0.002 / 0.010         0.002 / 0.010         0.002 / 0.010       ms
[10.058s][info][gc,stats    ]    Subphase: Pause Roots StringTable                       0.000 / 0.000         0.000 / 0.000         0.000 / 0.000         0.000 / 0.000       ms
[10.058s][info][gc,stats    ]    Subphase: Pause Roots SystemDictionary                  0.013 / 0.448         0.013 / 0.448         0.013 / 0.448         0.013 / 0.448       ms
[10.058s][info][gc,stats    ]    Subphase: Pause Roots Teardown                          0.001 / 0.002         0.001 / 0.002         0.001 / 0.002         0.001 / 0.002       ms
[10.058s][info][gc,stats    ]    Subphase: Pause Roots Threads                           0.011 / 0.046         0.011 / 0.046         0.011 / 0.046         0.011 / 0.046       ms
[10.058s][info][gc,stats    ]    Subphase: Pause Roots Universe                          0.019 / 0.658         0.019 / 0.658         0.019 / 0.658         0.019 / 0.658       ms
[10.058s][info][gc,stats    ]    Subphase: Pause Roots VMWeakHandles                     0.000 / 0.000         0.000 / 0.000         0.000 / 0.000         0.000 / 0.000       ms
[10.058s][info][gc,stats    ]    Subphase: Pause Weak Roots                              0.000 / 0.002         0.000 / 0.002         0.000 / 0.002         0.000 / 0.002       ms
[10.058s][info][gc,stats    ]    Subphase: Pause Weak Roots JFRWeak                      0.001 / 0.001         0.001 / 0.001         0.001 / 0.001         0.001 / 0.001       ms
[10.059s][info][gc,stats    ]    Subphase: Pause Weak Roots JNIWeakHandles               0.000 / 0.000         0.000 / 0.000         0.000 / 0.000         0.000 / 0.000       ms
[10.059s][info][gc,stats    ]    Subphase: Pause Weak Roots JVMTIWeakExport              0.000 / 0.001         0.000 / 0.001         0.000 / 0.001         0.000 / 0.001       ms
[10.059s][info][gc,stats    ]    Subphase: Pause Weak Roots Setup                        0.000 / 0.000         0.000 / 0.000         0.000 / 0.000         0.000 / 0.000       ms
[10.059s][info][gc,stats    ]    Subphase: Pause Weak Roots StringTable                  0.000 / 0.000         0.000 / 0.000         0.000 / 0.000         0.000 / 0.000       ms
[10.059s][info][gc,stats    ]    Subphase: Pause Weak Roots SymbolTable                  0.000 / 0.000         0.000 / 0.000         0.000 / 0.000         0.000 / 0.000       ms
[10.059s][info][gc,stats    ]    Subphase: Pause Weak Roots Teardown                     0.000 / 0.001         0.000 / 0.001         0.000 / 0.001         0.000 / 0.001       ms
[10.059s][info][gc,stats    ]    Subphase: Pause Weak Roots VMWeakHandles                0.000 / 0.000         0.000 / 0.000         0.000 / 0.000         0.000 / 0.000       ms
[10.059s][info][gc,stats    ]      System: Java Threads                                     10 / 11               10 / 11               10 / 11               10 / 11          threads
[10.059s][info][gc,stats    ] 
```
![[Pasted image 20241208145126.png]]
 1. **GC周期统计**

- **Collector: Garbage Collection Cycle**：显示垃圾回收周期的平均和最大时间。在此示例中，最近10秒内的GC周期平均时间为9.086毫秒，最大时间为19.305毫秒。这表明GC在短时间内完成，显示出低延迟特性。

 2. **争用情况**

- **Contention**：显示在GC过程中可能发生的争用情况。包括：
    
    - **Mark Segment Reset Contention**：标记段重置争用，显示为0/3，表示没有发生争用。
    - **Relocation Contention**：重定位争用，显示为0/2，表示没有争用。
    
    这些信息表明GC在执行时没有显著的性能瓶颈。
    

 3. **关键统计**

- **Critical: Allocation Stall** 和 **GC Locker Stall**：这些指示分配或GC锁的停滞时间，均为0，表明GC没有导致任何显著的延迟或停顿。

4. **内存使用情况**

- **Memory: Allocation Rate**：内存分配速率显示为518 MB/s，显示出应用程序的内存使用情况。
- **Heap Used After Mark/Relocation**：标记后和重定位后堆的使用量。标记后为317MB，重定位后为96MB，说明经过GC后可用内存显著增加。

5. **页面缓存**

- **Memory: Page Cache Hit L1/L2**：指页面缓存命中率，L1命中41次，L2没有命中，表明内存访问效率较高。
- **Memory: Page Cache Miss**：缓存未命中次数为3/31，表示有一些内存访问没有命中缓存。

 6. **GC各阶段的时间**

- **Phase** 部分列出了GC的各个阶段，包括并发标记阶段、准备重定位集、并发重定位等，显示了各个阶段所用的时间：
    - **Concurrent Mark**：2.042毫秒
    - **Pause Mark Start/End**: 标记开始和结束的暂停时间
    - **Pause Relocate Start**: 重定位开始的暂停时间

 7. **系统线程信息**

- **System: Java Threads**：显示Java线程的数量，当前有10个活动线程和11个总线程，表明GC并未对线程的活动造成显著影响。

 总体分析

这一部分的统计信息提供了有关GC性能和内存管理的多方面见解，显示出ZGC在低延迟的同时有效地回收内存，没有显著的停顿或争用情况。

#### 5.6.3 ZGC特点分析

1. **极短的停顿时间**：
   - Mark Start: ~0.141ms
   - Mark End: ~0.252ms
   - Relocate Start: ~0.927ms
   - 所有STW阶段都在1ms级别

2. **并发处理**：
   - 大部分工作在并发阶段完成
   - 并发标记：~1.5ms
   - 并发重定位：~5ms

3. **内存管理**：
   - 预留12%的内存作为储备
   - 存活对象只占用9%左右
   - 内存使用率从22%降至19%

4. **GC效率**：
```
[0.164s][info][gc,load  ] GC(0) Load: 0.10/0.09/0.07
[0.164s][info][gc,mmu   ] GC(0) MMU: 2ms/53.6%, 5ms/73.6%, 10ms/86.8%, 20ms/93.4%
```
- 系统负载保持在较低水平
- MMU(Minimum Mutator Utilization)显示应用线程获得了良好的运行时间

#### 5.6.4 与其他收集器对比

| 特性 | ZGC | G1 | CMS |
|------|-----|----|----|
| 停顿时间 | <1ms | ~5ms | ~1-10ms |
| 内存占用 | 较高 | 中等 | 较低 |
| CPU消耗 | 较高 | 中等 | 较高 |
| 并发能力 | 最强 | 强 | 中等 |
| 内存碎片 | 几乎无 | 少 | 较多 |
| 预测性 | 极好 | 好 | 一般 |

#### 5.6.5 适用场景

1. **最适合场景**：
   - 超大堆内存(>16GB)
	   -  ZGC 适合处理大内存堆（数十 GB 甚至 TB 级别），可以有效地管理大对象的分配和回收。
   - **极低延迟要求**：
    - ZGC 是为了满足极低延迟设计的，适用于对暂停时间敏感的应用，例如实时系统、在线交易系统和大数据分析等。
  -  **高并发和高负载**：
    - 如果应用需要在高并发和高负载下保持性能，ZGC 提供更好的性能保证，因为它的设计目标是避免长时间的停顿。
   - 多核心服务器

2. **不适合场景**：
   - 小内存应用(<4GB)
   - CPU资源受限场景
   - 对吞吐量要求极高的批处理


