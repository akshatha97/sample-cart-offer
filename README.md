# Cart Offer Application - Test Automation Suite

I have developed this solution to test the Zomato Cart Offer application. The system manages customer segments (`p1`, `p2`, `p3`) and applies either `FLATX` or `PERCENTAGE` discounts based on the user's eligibility. I have implemented a suite of 26 test cases covering positive, negative, and edge-case scenarios.

## 📋 Prerequisites

* **JDK 8** (The project is configured for Java 8 compatibility)
* **Maven** (via the included `./mvnw` wrapper)

---

## 🚀 How I Run the Project

I use the following sequence of commands to prepare the environment, start the services, and execute the tests.

### 1. Start the Mock Service

I use the MockServer Maven plugin to simulate the `/api/v1/user_segment` endpoint. This runs as a background process on port `1080`.

```bash
./mvnw org.mock-server:mockserver-maven-plugin:5.14.0:runForked "-Dmockserver.serverPort=1080"

```

### 2. Build and Start the Application

I build the project using the Maven wrapper and then launch the Spring Boot application on port `8080`.

```bash
# I build the project and skip tests during this phase
./mvnw clean install -DskipTests -U

# I start the Spring Boot application
java -jar target/simple-springboot-app-0.0.1-SNAPSHOT.jar

```

### 3. Run the Test Suite

I execute the 26 automated test cases (TS_01 to TS_26) which are integrated with Allure for detailed reporting.

```bash
./mvnw test

```

---

## 📊 Reporting and Evaluation

I have integrated **Allure Report** to provide a clear view of the test results, including the importance (Severity) and descriptions of each scenario.

### How I generate and view the report

Once the test execution is complete, I use these commands to generate the dashboard:

```bash
# Generate the report
mvn allure:report

# Launch the report in a browser
mvn allure:serve

```

---

### 💡 Troubleshooting

* **Stopping the Mock Server:** When I am done, I stop the background service using:
`./mvnw org.mock-server:mockserver-maven-plugin:5.14.0:stopForked`
* **Port Conflict:** I ensure port `1080` (MockServer) and `8080` (App) are not occupied before starting.
