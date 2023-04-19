import org.jetbrains.kotlin.konan.properties.loadProperties

plugins {
    application
    kotlin("jvm") version "1.8.0"
    kotlin("plugin.serialization") version "1.8.10"

    id("org.flywaydb.flyway") version "9.16.3"
}

val dbProperties = loadProperties("${projectDir}/db.properties")

group = "com.andreih"
version = "1.0-SNAPSHOT"

application {
    mainClass.set("com.andreih.ApplicationKt")
}

repositories {
    mavenCentral()
}

tasks.test {
    useJUnitPlatform()
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

flyway {
    url = dbProperties["jdbcUrl"].toString()
    user = dbProperties["dataSource.user"].toString()
    password = dbProperties["dataSource.password"].toString()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("io.ktor:ktor-server-core:2.2.4")
    implementation("io.ktor:ktor-server-netty:2.2.4")
    implementation("io.ktor:ktor-server-call-logging:2.2.4")
    implementation("io.ktor:ktor-server-resources:2.2.4")
    implementation("io.ktor:ktor-server-content-negotiation:2.2.4")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.2.4")
    implementation("org.slf4j:slf4j-simple:2.0.7")
    implementation("io.arrow-kt:suspendapp:0.4.0")
    implementation("io.arrow-kt:suspendapp-ktor:0.4.0")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")

    // Flyway
    implementation("org.postgresql:postgresql:42.5.1")

    // Vertx postgreSQL client
    implementation("io.vertx:vertx-pg-client:4.4.1")
    implementation("io.vertx:vertx-lang-kotlin-coroutines:4.4.1")
    implementation("io.vertx:vertx-sql-client-templates:4.4.1")
}
