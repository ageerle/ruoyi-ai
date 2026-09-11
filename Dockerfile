# IPD 后端生产镜像（多阶段：maven 构建 → 精简 JRE 运行）
# 构建示例：docker build -t ipd-backend:latest .
# 运行示例（数据源/Redis/密钥全部走环境变量，禁入镜像）：
#   docker run -e SPRING_PROFILES_ACTIVE=prod \
#     -e SPRING_DATASOURCE_DYNAMIC_DATASOURCE_MASTER_URL="jdbc:mysql://<db-host>:3306/ipd?..." \
#     -e SPRING_DATASOURCE_DYNAMIC_DATASOURCE_MASTER_USERNAME=ipd_app \
#     -e SPRING_DATASOURCE_DYNAMIC_DATASOURCE_MASTER_PASSWORD=*** \
#     -e SA_TOKEN_JWT_SECRET_KEY=*** \
#     -p 16039:16039 ipd-backend:latest
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /src
# 先拷 POM 利用 Docker 层缓存拉依赖
COPY pom.xml .
RUN mvn -q -B -DskipTests -Dmaven.main.skip dependency:go-offline || true
# 再拷源码全量构建（IPD 主链：ruoyi-admin 聚合 ruoyi-modules/ruoyi-ipd）
COPY . .
RUN mvn -q -B -DskipTests package -pl ruoyi-admin -am

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN groupadd -r ipd && useradd -r -g ipd ipd && mkdir -p /app/logs && chown -R ipd:ipd /app
USER ipd
COPY --from=build /src/ruoyi-admin/target/ruoyi-admin.jar /app/app.jar
# 生产 JVM 参数与 PERF-P2-3 start.sh 对齐：G1GC + 堆上限 + OOM 时 HeapDump
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0 -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/app/logs"
EXPOSE 16039
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
