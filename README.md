Account service allowing to:
1. create accounts
2. retrieve accounts
3. create transactions
4. retrieve transactions

Swagger is available at http://localhost:8080/swagger-ui.html

### Running the application
1. Build docker image

   `./gradlew bootBuildImage`
2. Run application and its dependencies. Database structure is initialized on application start-up using Flyway

    `docker compose up`
3. Access application on http://localhost:8080
