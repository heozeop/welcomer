package com.welcomer.welcome.config

import org.jetbrains.exposed.sql.Database
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

@Configuration
class MainDatabaseConfig(
    private val dataSource: DataSource
) {
    @Bean
    fun connectDatabase(): Database = Database.connect(dataSource)
}