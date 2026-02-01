FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar

ENV JAVA_OPTS="\
 -Dspring.profiles.active=bybit \
 -Xms400m \
 -Xmx400m \
 -Xss256k \
 -XX:MaxMetaspaceSize=100m \
 -XX:MaxDirectMemorySize=100m"

EXPOSE 8084

ENTRYPOINT ["java", "-jar", "app.jar"]