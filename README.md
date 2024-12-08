# Java JVM 调优学习项目

这个项目用于学习和实践JVM调优技术，包含内存泄漏分析和内存管理优化的示例代码。

 详细的学习教程请访问：[我的博客](https://blog.csdn.net/qq_43460315?spm=1000.2115.3001.5343)

## 项目特点
- 实际案例演示内存泄漏场景
- G1垃圾收集器的使用和优化
- 软引用（SoftReference）的实践应用
- 详细的性能监控和分析



## 技术栈
- JDK 11
- Maven
- Parallel,CMS,G1,ZGC垃圾收集器
- JVM调优工具：
  - VisualVM
  - Memory Analyzer Tool (MAT)

## 快速开始

### 环境要求
- JDK 11+
- Maven 3.6+

### 运行示例
1. 克隆项目
```bash
git clone [项目地址]
cd java-jvm-tuning
```

2. 编译项目
```bash
mvn clean package
```

3. 运行内存泄漏演示
```bash
java -Xmx512m -XX:+UseG1GC -XX:G1HeapRegionSize=1m -jar target/java-jvm-tuning-1.0-SNAPSHOT.jar
```

## 学习要点
1. 内存泄漏分析
   - 内存泄漏的典型场景
   - 使用工具进行分析
   - GC日志解读

2. 内存管理优化
   - 软引用的使用技巧
   - 集合大小控制
   - GC参数调优

## 学习目录
- [Java内存管理与调优实践--内存泄漏分析](https://blog.csdn.net/qq_43460315/article/details/144319933?spm=1001.2014.3001.5502)
- [Java内存管理与调优实践--内存管理优化](https://blog.csdn.net/qq_43460315/article/details/144320096?spm=1001.2014.3001.5502)
- [Java内存管理与调优实践--垃圾收集器上手实践](https://blog.csdn.net/qq_43460315/article/details/144326228?spm=1001.2014.3001.5502)

