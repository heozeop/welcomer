package com.welcomer.welcome.message.service

import com.welcomer.welcome.message.model.Comment
import com.welcomer.welcome.message.repository.CommentRepository
import org.springframework.stereotype.Service

@Service
class CommentService(
    private val commentRepository: CommentRepository
) {
    suspend fun save(messageId: UInt, comment: Comment) = commentRepository.save(messageId, comment)

    suspend fun find(messageId: UInt, size: Int = 10, cursorId: UInt = 0u) =
        commentRepository.find(messageId, size, cursorId)

    suspend fun count(messageId: UInt): Long {
        val list = commentRepository.countMap(listOf(messageId))
        return list[messageId] ?: 0L
    }

    suspend fun search(query: String, cursorId: UInt = 0u, size: Int = 10) =
        commentRepository.search(query, cursorId, size)

    suspend fun update(messageId: UInt, commentId: UInt, comment: Comment) = commentRepository.update(messageId, commentId, comment)

    suspend fun delete(messageId: UInt, commentId: UInt) = commentRepository.delete(messageId, commentId)
}