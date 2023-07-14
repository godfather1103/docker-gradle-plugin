# 基础镜像-JDK环境
FROM azul/zulu-openjdk-alpine:11
# 相关标签信息
LABEL Author=JackChu
LABEL Desc=测试代码
LABEL Email=example@example.com
# 相关环境变量
ENV LANG=en_US.UTF-8 LC_CTYPE=en_US.UTF-8 LC_ALL=en_US.UTF-8
# 定义工作目录的参数值
ARG work_dir
ARG GitTag
# 设置环境变量
ENV work_dir=/example/APP/web
ENV GitTag=${GitTag}
# 设置工作目录
WORKDIR ${work_dir}
# 添加介质到工作目录中
COPY build/libs/groovy-dsl-demo-1.0.jar  ${work_dir}
# 需要开放的端口8080
EXPOSE 8080
# 日志挂载的路径
VOLUME ["/example/LOGS/web", "${work_dir}/logs"]
# 启动脚本
CMD ["java", "-jar", "-Xms2g", "-Xmx2G", "-XX:+PrintGCDetails", "-XX:+HeapDumpOnOutOfMemoryError", "-XX:HeapDumpPath=/example/LOGS/web/heapdump_pid_%p.hprof", "-Dfile.encoding=utf-8", "/example/APP/web/groovy-dsl-demo-1.0.jar"]

