FROM openjdk:17

# RUN mvn clean package
# Setting up work directory
WORKDIR /app

# Copy the jar file into our app
COPY ./cart_service/target/cart_service-0.0.1-SNAPSHOT.jar /app



# Exposing port 8002
EXPOSE 8002

# Starting the application
CMD ["java", "-jar", "cart_service-0.0.1-SNAPSHOT.jar"]