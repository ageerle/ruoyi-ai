#基础镜像
FROM findepi/graalvm:java17-native

# 设置环境变量
ENV LANG C.UTF-8
ENV LANGUAGE C.UTF-8
ENV LC_ALL C.UTF-8
ENV SERVER_PORT=6039

MAINTAINER ageerle

RUN mkdir -p /ruoyi/server/logs \
    /ruoyi/server/temp \
    /ruoyi/skywalking/agent


#工作空间
WORKDIR /ruoyi/server



EXPOSE ${SERVER_PORT}

ADD ./target/ruoyi-admin.jar ./app.jar


ENTRYPOINT ["java", \
            "-Djava.security.egd=file:/dev/./urandom", \
            "-Dserver.port=${SERVER_PORT}", \
            # 应用名称 如果想区分集群节点监控 改成不同的名称即可
#            "-Dskywalking.agent.service_name=ruoyi-server", \
#            "-javaagent:/ruoyi/skywalking/agent/skywalking-agent.jar", \
            "-jar", "app.jar"]


