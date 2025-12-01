# 1단계: 빌드 환경 (Java 21/Maven 환경에서 JAR 파일 빌드)
FROM maven:3.8.8-eclipse-temurin-21 AS build
WORKDIR /app
# 프로젝트 파일 복사
COPY pom.xml .
COPY src ./src
# JAR 파일 빌드
RUN mvn clean package -DskipTests

# 2단계: 실행 환경 (경량화된 JRE 환경에서 JAR 파일 실행)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar /app/synergy-order101-backend.jar

ENTRYPOINT ["java", "-jar", "synergy-order101-backend.jar"]