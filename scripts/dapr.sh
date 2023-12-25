#cart service
dapr run --app-id cart_service --app-port 8080 --dapr-http-port 3500 --dapr-grpc-port 60001 --resources-path ./dapr/components


