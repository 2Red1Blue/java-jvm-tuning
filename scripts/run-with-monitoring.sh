#!/bin/bash

# 运行Java程序并开启各种监控选项的脚本

# 设置Java程序的主类
MAIN_CLASS="com.example.MemoryLeakDemo"

# JVM参数配置
JVM_OPTS="-Xmx256m -Xms256m \
    -XX:+PrintGCDetails \
    -XX:+PrintGCDateStamps \
    -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath=./dump.hprof \
    -Xloggc:./gc.log"

# 运行程序
java $JVM_OPTS -cp target/java-jvm-tuning-1.0-SNAPSHOT.jar $MAIN_CLASS
