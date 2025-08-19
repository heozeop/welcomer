package com.welcomer.welcome.message.repository

import com.welcomer.welcome.message.model.Comment
import com.welcomer.welcome.message.model.Comments
import com.welcomer.welcome.utils.toLocalDateTime
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.time.ZoneOffset

@Repository
class CommentRepository {
    suspend fun save(messageId: UInt, comment: Comment): Comment {
        val item = transaction {
            Comments.insert {
                it[this.messageId] = messageId
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

    suspend fun find(messageId: UInt, size: Int = 10, cursorId: UInt = 0u): List<Comment> = transaction {
        Comments.select(Comments.id, Comments.author, Comments.content, Comments.createdAt, Comments.updatedAt)
            .where {
                (Comments.id greater  cursorId) and
                (Comments.messageId eq messageId)
            }
            .limit(size)
            .mapNotNull { rowToComment(it) }
    }

    suspend fun countMap(messageIds: List<UInt>): Map<UInt, Long> = transaction {
        Comments.select(Comments.messageId, Comments.id.count())
            .where { Comments.messageId inList messageIds }
            .groupBy(Comments.messageId)
            .fold(mutableMapOf()) { acc, row ->
                val messageId = row[Comments.messageId]
                val count = row[Comments.id.count()]
                acc[messageId] = count

                acc
            }

    }

    suspend fun update(messageId: UInt, commentId: UInt, comment: Comment): Boolean = transaction {
        val affectedRows: Int = Comments.update({ (Comments.id eq commentId) and (Comments.messageId eq messageId) }) {
            it[author] = comment.author
            it[content] = comment.content
            it[updatedAt] = LocalDateTime.now().toInstant(ZoneOffset.UTC)
        }

        affectedRows > 0
    }

    suspend fun delete(messageId: UInt, commentId: UInt): Boolean = transaction {
        val affectedRows: Int = Comments.deleteWhere {
            (id eq commentId) and (Comments.messageId eq messageId)
        }

        affectedRows > 0
    }

    private fun rowToComment(row: org.jetbrains.exposed.sql.ResultRow): Comment {
        return Comment(
            id = row[Comments.id],
            author = row[Comments.author],
            content = row[Comments.content],
            createdAt = row[Comments.createdAt].toLocalDateTime(),
            updatedAt = row[Comments.updatedAt].toLocalDateTime()
        )
    }
}