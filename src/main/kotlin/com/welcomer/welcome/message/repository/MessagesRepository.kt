package com.welcomer.welcome.message.repository

import com.welcomer.welcome.message.model.Message
import com.welcomer.welcome.message.model.Messages
import com.welcomer.welcome.utils.toLocalDateTime
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.time.ZoneOffset

@Repository
class MessagesRepository {
    suspend fun save(message: Message):Message {
        val item = transaction {
            Messages.insert {
                it[author] = message.author
                it[content] = message.content
                it[createdAt] = (message.createdAt ?: LocalDateTime.now()).toInstant(ZoneOffset.UTC)
                it[updatedAt] = (message.updatedAt ?: LocalDateTime.now()).toInstant(ZoneOffset.UTC)
            }
        }

        return message.copy(
            id = item[Messages.id],
            createdAt = item[Messages.createdAt].toLocalDateTime(),
            updatedAt = item[Messages.updatedAt].toLocalDateTime()
        )
    }

    suspend fun findById(id: UInt): Message? = transaction {
            Messages.select(Messages.id, Messages.author, Messages.content, Messages.createdAt, Messages.updatedAt)
                .where { Messages.id eq id }
                .mapNotNull { rowToMessage(it) }
                .singleOrNull()
        }

    suspend fun find(size: Int, cursorId: UInt): List<Message> = transaction {
        Messages.selectAll()
            .where { Messages.id greater cursorId }
            .limit(size)
            .orderBy(Messages.id to SortOrder.ASC)
            .map { rowToMessage(it) }
    }

    suspend fun count(): Long = transaction {
        Messages.selectAll().count()
    }

    suspend fun update(message: Message): Boolean =
        transaction {
            val affectedRows: Int = Messages.update({ Messages.id eq message.id!! }) {
                it[author] = message.author
                it[content] = message.content
                it[updatedAt] = LocalDateTime.now().toInstant(ZoneOffset.UTC)
            }

            affectedRows > 0
        }

    suspend fun delete(id: UInt): Boolean = transaction {
        val affectedRows: Int = Messages.deleteWhere { Messages.id eq id }
        affectedRows > 0
    }

    private fun rowToMessage(row: ResultRow): Message {
        return Message(
            id = row[Messages.id],
            author = row[Messages.author],
            content = row[Messages.content],
            createdAt = row[Messages.createdAt].toLocalDateTime(),
            updatedAt = row[Messages.updatedAt].toLocalDateTime()
        )
    }
}