package com.welcomer.welcome.message.repository

import com.welcomer.welcome.message.model.Comment
import com.welcomer.welcome.message.model.Comments
import com.welcomer.welcome.utils.toLocalDateTime
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.time.ZoneOffset

@Repository
class CommentRepository {
    suspend fun save(comment: Comment): Comment {
        val item = transaction {
            Comments.insert {
                it[messageId] = comment.messageId
                it[author] = comment.author
                it[content] = comment.content
                it[createdAt] = (comment.createdAt ?: LocalDateTime.now()).toInstant(ZoneOffset.UTC)
                it[updatedAt] = (comment.updatedAt ?: LocalDateTime.now()).toInstant(ZoneOffset.UTC)
            }
        }

        return comment.copy(
            id = item[Comments.id],
            createdAt = item[Comments.createdAt].toLocalDateTime(),
            updatedAt = item[Comments.updatedAt].toLocalDateTime()
        )
    }

    suspend fun find(messageId: UInt, size: Int = 10, cursorId: UInt = 0u): Comment? = transaction {
        Comments.select(Comments.id, Comments.messageId, Comments.author, Comments.content, Comments.createdAt, Comments.updatedAt)
            .where {
                (Comments.id greater  cursorId) and
                (Comments.messageId eq messageId)
            }
            .limit(size)
            .mapNotNull { rowToComment(it) }
            .singleOrNull()
    }

    suspend fun count(messageId: UInt): Long = transaction {
        Comments.select(Comments.id, Comments.author, Comments.content, Comments.createdAt, Comments.updatedAt)
            .where { Comments.messageId eq messageId }
            .count()
    }

    suspend fun update(comment: Comment): Boolean = transaction {
        val affectedRows: Int = Comments.update({ Comments.id eq comment.id!! }) {
            it[messageId] = comment.messageId
            it[author] = comment.author
            it[content] = comment.content
            it[updatedAt] = LocalDateTime.now().toInstant(ZoneOffset.UTC)
        }

        affectedRows > 0
    }

    suspend fun delete(id: UInt): Boolean = transaction {
        val affectedRows: Int = Comments.deleteWhere { Comments.id eq id }

        affectedRows > 0
    }

    private fun rowToComment(row: org.jetbrains.exposed.sql.ResultRow): Comment {
        return Comment(
            id = row[Comments.id],
            messageId = row[Comments.messageId],
            author = row[Comments.author],
            content = row[Comments.content],
            createdAt = row[Comments.createdAt].toLocalDateTime(),
            updatedAt = row[Comments.updatedAt].toLocalDateTime()
        )
    }
}