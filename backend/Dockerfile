FROM registry.cn-hangzhou.aliyuncs.com/zhengqing/openjdk:17
WORKDIR /app
COPY target/platform-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Xms256m", "-Xmx384m", "-jar", "app.jar"]
