package com.welcomer.welcome.message.model

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.LocalDateTime

object Comments : Table("comments") {
    val id = uinteger("id").autoIncrement()
    val messageId = uinteger("message_id").references(Messages.id)
    val author = varchar("author", 255)
    val content = text("content")
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(id)
}

data class Comment(
    val id: UInt?,
    val author: String,
    val content: String,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?
) {
    constructor(author: String, content: String) : this(
        null,
        author,
        content,
        LocalDateTime.now(),
        LocalDateTime.now()
    )
}
