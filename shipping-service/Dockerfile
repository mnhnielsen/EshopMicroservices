FROM openjdk:17

# Setting up work directory
WORKDIR /app

# Copy the jar file into our app
COPY shipping-service/target/Shipping-service-0.0.1-SNAPSHOT.jar /app

# Exposing port 8085
EXPOSE 8085

# Starting the application
CMD ["java", "-jar", "Shipping-service-0.0.1-SNAPSHOT.jar"]