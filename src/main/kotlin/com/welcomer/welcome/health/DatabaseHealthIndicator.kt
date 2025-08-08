package com.welcomer.welcome.health

import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.stereotype.Component
import javax.sql.DataSource

@Component
@ConditionalOnBean(DataSource::class)
class DatabaseHealthIndicator(
    private val dataSource: DataSource
) : HealthIndicator {

    override fun health(): Health {
        return try {
            dataSource.connection.use { connection ->
                val isValid = connection.isValid(5)
                
                if (isValid) {
                    val metaData = connection.metaData
                    Health.up()
                        .withDetail("database", metaData.databaseProductName)
                        .withDetail("version", metaData.databaseProductVersion)
                        .withDetail("driver", metaData.driverName)
                        .withDetail("driverVersion", metaData.driverVersion)
                        .build()
                } else {
                    Health.down()
                        .withDetail("error", "Database connection is not valid")
                        .build()
                }
            }
        } catch (e: Exception) {
            Health.down()
                .withDetail("error", e.message ?: "Unable to connect to database")
                .withException(e)
                .build()
        }
    }
}