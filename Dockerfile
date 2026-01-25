FROM eclipse-temurin:21-jdk-alpine

ADD ./target/crypto-analysis-tech-provider.jar /app/

CMD sh -c "java -Dspring.profiles.active=bybit \
  -Xmx400m -Xms400m -Xss256k -XX:MaxMetaspaceSize=100m -XX:MaxDirectMemorySize=100m \
  -jar /app/crypto-analysis-tech-provider.jar"