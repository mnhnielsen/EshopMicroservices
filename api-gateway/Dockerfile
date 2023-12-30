FROM openjdk:17

# Path: /app
WORKDIR /app

COPY ./api-gateway/target/api-gateway-0.0.1-SNAPSHOT.jar /app

EXPOSE 8000

CMD ["java", "-jar", "api-gateway-0.0.1-SNAPSHOT.jar"]


