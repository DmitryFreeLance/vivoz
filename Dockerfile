FROM maven:3.9.8-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
ENV BOT_TOKEN=""
ENV BOT_USERNAME=""
ENV ADMIN_IDS=""
ENV DB_PATH="/data/bot.db"
COPY --from=build /app/target/app.jar /app/app.jar
VOLUME ["/data"]
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
