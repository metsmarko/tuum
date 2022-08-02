Account service allowing to:
1. create accounts
2. retrieve accounts
3. create transactions
4. retrieve transactions

### Running the application
1. Build the project, run checkstyle and run unit & integration tests.

   `./gradlew build`
2. Build docker image.

   `./gradlew bootBuildImage`
3. Run application and its dependencies. Database structure is initialized on application start-up using Flyway.

    `docker compose up`
4. Access application's swagger ui at http://localhost:8080/swagger-ui.html
