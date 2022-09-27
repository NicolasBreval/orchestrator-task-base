package org.nitb.orchestrator2.tests.task

import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@MicronautTest(propertySources = ["classpath:application-rabbitmq.yml"])
class RabbitMQTaskTests {

    @Test
    @Order(1)
    fun jsonConsumerTest() {
        commonTaskTests.jsonConsumerTest()
    }

    @Test
    @Order(2)
    fun scriptableConsumerTest() {
        commonTaskTests.scriptableConsumerTest()
    }

    @Test
    @Order(3)
    fun scriptableCyclicalTest() {
        commonTaskTests.scriptableCyclicalTest()
    }

    @Test
    @Order(4)
    fun scriptableCyclicalFailedTest() {
        commonTaskTests.scriptableCyclicalFailedTest()
    }

    @Test
    @Order(5)
    fun csvConsumerTest() {
        commonTaskTests.csvConsumerTest()
    }

    @Test
    @Order(6)
    fun csvCompressedConsumerTest() {
        commonTaskTests.csvConsumerTest(true)
    }

    @Test
    @Order(7)
    fun multiJsonConsumerTest() {
        commonTaskTests.multiJsonConsumerTest()
    }

    @Test
    @Order(8)
    fun excelConsumerTest() {
        commonTaskTests.excelConsumerTest()
    }

    @Inject
    private lateinit var commonTaskTests: CommonTaskTests
}