
val kotlinVersion: String by System.getProperties()
val jacksonVersion: String by System.getProperties()
val kotlinxDataframeVersion: String by System.getProperties()
val kotlinxCoroutinesVersion: String by System.getProperties()
val graalvmScriptEngineVersion: String by System.getProperties()

plugins {
    `maven-publish`
    `java-library`
    val kotlinVersion by System.getProperties()
    kotlin("jvm") version "$kotlinVersion"
    kotlin("kapt") version "$kotlinVersion"
    kotlin("plugin.allopen") version "$kotlinVersion"
    kotlin("plugin.noarg") version "$kotlinVersion"
    id("io.micronaut.application") version "3.5.1"
}

group = "org.nitb.orchestrator2"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    annotationProcessor("io.micronaut.openapi:micronaut-openapi")

    implementation("io.micronaut:micronaut-jackson-databind")
    implementation("io.micronaut.kotlin:micronaut-kotlin-runtime")
    implementation("jakarta.annotation:jakarta.annotation-api")
    implementation("org.jetbrains.kotlin:kotlin-reflect:${kotlinVersion}")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${kotlinVersion}")
    implementation("io.micronaut:micronaut-validation")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:${jacksonVersion}")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:${jacksonVersion}")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:${jacksonVersion}")
    implementation("org.jetbrains.kotlinx:dataframe:${kotlinxDataframeVersion}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${kotlinxCoroutinesVersion}")
    implementation("io.micronaut.jms:micronaut-jms-activemq-classic")
    implementation("io.micronaut.rabbitmq:micronaut-rabbitmq")
    implementation("org.graalvm.js:js-scriptengine:${graalvmScriptEngineVersion}")
    implementation("io.swagger.core.v3:swagger-annotations")
    implementation("com.fasterxml.jackson.module:jackson-module-jsonSchema:${jacksonVersion}")
    implementation("org.jetbrains.kotlinx:dataframe-excel:${kotlinxDataframeVersion}")

    runtimeOnly("ch.qos.logback:logback-classic")
    runtimeOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.6.4")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.0")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.9.0")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.9.0")
    testImplementation("com.github.fridujo:rabbitmq-mock:1.1.1")
    testImplementation("org.apache.activemq:activemq-broker:5.17.1")
    testImplementation("org.apache.activemq:activemq-kahadb-store:5.17.1")
    testImplementation("org.awaitility:awaitility:4.2.0")
}

java {
    sourceCompatibility = JavaVersion.toVersion("17")
    withSourcesJar()
}

tasks {
    compileKotlin {
        kotlinOptions {
            jvmTarget = "17"
        }
    }
    compileTestKotlin {
        kotlinOptions {
            jvmTarget = "17"
        }
    }

    test {
        useJUnit()
        useJUnitPlatform()
    }

    jar {
        doLast {
            file("build/libs").listFiles()?.firstOrNull { it.name.contains("runner") }?.delete()
        }
    }
}

noArg {
    annotation("org.nitb.orchestrator2.task.annotation.NoArgs")
}

micronaut {
    testRuntime("junit5")
    processing {
        incremental(true)
        annotations("org.nitb.orchestrator2.task.annotation.*")
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
    repositories {
        mavenLocal()
    }
}