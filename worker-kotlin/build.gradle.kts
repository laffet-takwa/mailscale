plugins {
    kotlin("jvm") version "2.1.21"
    kotlin("plugin.serialization") version "2.1.21"
    application
    id("com.diffplug.spotless") version "7.0.2"
    id("io.gitlab.arturbosch.detekt") version "1.23.8"
}

group = "com.mailscale"
version = "1.0.0"

repositories {
    mavenCentral()
}

val kafkaVersion = "3.9.1"
val kotlinxCoroutinesVersion = "1.10.2"
val kotlinxSerializationVersion = "1.8.1"
val logbackVersion = "1.5.18"
val slf4jVersion = "2.0.17"
val hikariVersion = "6.2.1"
val postgresDriverVersion = "42.7.5"
val jakartaMailVersion = "2.0.1"
val micrometerVersion = "1.15.1"
val jacksonVersion = "2.19.0"
val flywayVersion = "11.5.0"
val junitVersion = "5.12.2"
val mockkVersion = "1.14.2"
val testcontainersVersion = "1.20.4"

dependencies {
    // Kafka
    implementation("org.apache.kafka:kafka-clients:$kafkaVersion")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxCoroutinesVersion")

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")

    // Logging
    implementation("org.slf4j:slf4j-api:$slf4jVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")

    // Database
    implementation("com.zaxxer:HikariCP:$hikariVersion")
    implementation("org.postgresql:postgresql:$postgresDriverVersion")
    implementation("org.flywaydb:flyway-database-postgresql:$flywayVersion")

    // Email
    implementation("org.eclipse.angus:jakarta.mail:$jakartaMailVersion")
    implementation("jakarta.activation:jakarta.activation-api:2.1.3")

    // Metrics
    implementation("io.micrometer:micrometer-core:$micrometerVersion")
    implementation("io.micrometer:micrometer-registry-prometheus:$micrometerVersion")

    // JSON
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")

    // Testing
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:$junitVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$kotlinxCoroutinesVersion")
    testImplementation("org.testcontainers:testcontainers:$testcontainersVersion")
    testImplementation("org.testcontainers:kafka:$testcontainersVersion")
    testImplementation("org.testcontainers:postgresql:$testcontainersVersion")
}

kotlin {
    jvmToolchain(21)
}

// Toolchain discovery: in addition to auto-detected JDKs, these explicit
// locations are checked. Gradle resolves Java 21 from here on this machine.
kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

application {
    mainClass.set("com.mailscale.worker.MainKt")
}

spotless {
    kotlin {
        target("src/**/*.kt")
        ktlint("1.5.0")
    }
    kotlinGradle {
        target("*.gradle.kts")
        ktlint("1.5.0")
    }
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom(files("$projectDir/config/detekt.yml"))
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

tasks.named<JavaExec>("run") {
    environment(System.getenv())
    workingDir = projectDir
}

// Shadow jar for a self-contained deployable worker
plugins.withId("com.github.johnrengelman.shadow") { }
