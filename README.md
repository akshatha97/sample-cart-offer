# Cart Offer Application - Test Automation Suite

This project is a comprehensive test automation suite for the Zomato Cart Offer application. The system manages customer segments (p1, p2, p3) and applies either FLATX or PERCENTAGE discounts based on user eligibility. The suite includes 26 test cases covering positive, negative, and edge-case scenarios.

## 📋 Prerequisites

* **JDK 8** (The project is configured for Java 8 compatibility)
* **Maven** (via the included `./mvnw` wrapper)

---

Here is a more professional and neutral version of your README. This version focuses on the technical delivery and functionality rather than personal narrative, which is often preferred for technical assignments.

Zomato Cart Offer API - Test Automation Suite
This project is a comprehensive test automation suite for the Zomato Cart Offer application. The system manages customer segments (p1, p2, p3) and applies either FLATX or PERCENTAGE discounts based on user eligibility. The suite includes 26 test cases covering positive, negative, and edge-case scenarios.

📋 Prerequisites
JDK 8: The project is strictly configured for Java 8 compatibility.

Maven: Managed via the included ./mvnw wrapper.

🚀 Execution Guide
Follow the sequence of commands below to prepare the environment, start the services, and execute the tests.

1. Start the Mock Service
The MockServer Maven plugin simulates the /api/v1/user_segment endpoint. This runs as a background process on port 1080.

Bash

./mvnw org.mock-server:mockserver-maven-plugin:5.14.0:runForked "-Dmockserver.serverPort=1080"
2. Build and Start the Application
Build the project using the Maven wrapper and launch the Spring Boot application on port 8080.

Bash

# Build the project and skip tests
./mvnw clean install -DskipTests -U

# Start the Spring Boot application
java -jar target/simple-springboot-app-0.0.1-SNAPSHOT.jar
3. Execute Tests
Run the automated test suite (TS_01 to TS_26).

Bash

./mvnw test

```

---

## 📊 Reporting and Evaluation

The project integrates Allure Report to provide a clear visualization of test results, including severity levels and detailed scenario descriptions.

Generate and View Report
Once the test execution is complete, use these commands to generate the dashboard:

Bash

# Generate the report
mvn allure:report

# Launch the report in a default browser
mvn allure:serve

```

---

### 💡 Troubleshooting

Stop Mock Server: To terminate the background mock service: ./mvnw org.mock-server:mockserver-maven-plugin:5.14.0:stopForked

Port Conflict: Ensure ports 1080 (MockServer) and 8080 (App) are free before initialization.
