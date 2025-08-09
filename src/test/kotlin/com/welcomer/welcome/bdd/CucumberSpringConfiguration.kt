package com.welcomer.welcome.bdd

import io.cucumber.spring.CucumberContextConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource

/**
 * Spring configuration for Cucumber BDD tests
 */
@CucumberContextConfiguration
@SpringBootTest(
    classes = [BddTestConfiguration::class],
    webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@ActiveProfiles("bdd-test")
@TestPropertySource(properties = [
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa", 
    "spring.datasource.password=",
    "spring.flyway.enabled=false",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.data.redis.port=16379",
    "logging.level.org.springframework.web=WARN"
])
class CucumberSpringConfiguration