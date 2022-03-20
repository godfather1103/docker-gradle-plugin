# 基础镜像-JDK环境
FROM mcr.microsoft.com/java/jre:11-zulu-alpine
# 相关标签信息
LABEL Author=JackChu
LABEL Desc=测试代码
LABEL Email=example@example.com
# 相关环境变量
ENV LANG=en_US.UTF-8 LC_CTYPE=en_US.UTF-8 LC_ALL=en_US.UTF-8
# 定义工作目录的参数值
ARG work_dir
# 设置环境变量
ENV work_dir=/example/APP/web
# 设置工作目录
WORKDIR ${work_dir}
# 添加介质到工作目录中
COPY build/libs/docker-gradle-plugin-test-1.0.jar  ${work_dir}
# 需要开放的端口
EXPOSE 8080
# 日志挂载的路径
VOLUME ["/example/LOGS/web", "${work_dir}/logs"]
# 启动脚本
CMD ["java", "-jar", "-Xms2g", "-Xmx2G", "-XX:+PrintGCDetails", "-XX:+HeapDumpOnOutOfMemoryError", "-XX:HeapDumpPath=/example/LOGS/web/heapdump_pid_%p.hprof", "-Dfile.encoding=utf-8", "/example/APP/web/docker-gradle-plugin-test-1.0.jar"]

