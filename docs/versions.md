# Dependencies for the MailScale API (Maven).
# Keep this generated from the pom so it cannot drift.
#
# Verified with: ./backend-java/mvnw -f backend-java/pom.xml dependency:tree

# Core Spring Boot (managed by spring-boot-starter-parent 3.5.16)
org.springframework.boot:spring-boot-starter-web
org.springframework.boot:spring-boot-starter-validation
org.springframework.boot:spring-boot-starter-data-jpa
org.springframework.boot:spring-boot-starter-security
org.springframework.boot:spring-boot-starter-actuator
org.springframework.boot:spring-boot-starter-mail
org.springframework.boot:spring-boot-starter-data-redis
org.springframework.boot:spring-boot-starter-webflux
org.springframework.kafka:spring-kafka

# Database / migrations
org.flywaydb:flyway-core
org.flywaydb:flyway-database-postgresql
org.postgresql:postgresql
com.h2database:h2 (test scope)

# API docs / security / parsing
org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.17
io.jsonwebtoken:jjwt-api / jjwt-impl / jjwt-jackson:0.12.6
org.apache.commons:commons-csv:1.14.1
com.googlecode.owasp-java-html-sanitizer:owasp-java-html-sanitizer:20261011.1

# Test tooling
org.springframework.boot:spring-boot-starter-test
org.springframework.security:spring-security-test
com.github.spotbugs:spotbugs-maven-plugin:4.10.5.0
org.apache.maven.plugins:maven-checkstyle-plugin:3.6.0 (Checkstyle 10.26.1)
org.testcontainers:testcontainers-bom:2.0.5 -> testcontainers-postgresql,
  testcontainers-kafka

# ----------------------------------------------------------------------
# Kotlin worker (Gradle) - worker-kotlin/build.gradle.kts
# ----------------------------------------------------------------------
# org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.21
# org.apache.kafka:kafka-clients:3.9.1
# org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2
# org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1
# com.zaxxer:HikariCP:6.2.1
# org.postgresql:postgresql:42.7.5
# org.eclipse.angus:jakarta.mail:2.0.1
# io.micrometer:micrometer-registry-prometheus:1.15.1
#   NOTE: Micrometer 1.15 moved the Prometheus registry package to
#   io.micrometer.prometheusmetrics.PrometheusMeterRegistry.
# com.fasterxml.jackson.core:jackson-databind:2.19.0
# io.gitlab.arturbosch.detekt:1.23.8
# com.diffplug.spotless:7.0.2

# ----------------------------------------------------------------------
# Frontend (npm) - frontend/package.json
# ----------------------------------------------------------------------
# react 18.3.x, react-dom 18.3.x
# @tanstack/react-query ^5.75.0
# react-hook-form ^7.62.0, @hookform/resolvers ^3.10.0, zod ^3.25.76
# react-router-dom ^6.30.0
# recharts ^2.15.4 (v3 migration documented separately)
# axios ^1.11.0
# vite ^6.3.4, typescript ^5.8.3
# vitest ^3.1.2, @testing-library/react ^16.1.0, jsdom ^26
# tailwindcss ^3.4.17, postcss ^8.5.3, autoprefixer ^10.4.21
# eslint ^8.57.1
