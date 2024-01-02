FROM openjdk:17

# Setting up work directory
WORKDIR /app

# Copy the jar file into our app
COPY ./payment-service/target/payment-service-0.0.1-SNAPSHOT.jar /app

# Exposing port 8084
EXPOSE 8084

# Starting the application
CMD ["java", "-jar", "payment-service-0.0.1-SNAPSHOT.jar"]