package com.welcomer.welcome.message.service

import com.welcomer.welcome.message.model.Comment
import com.welcomer.welcome.message.repository.CommentRepository
import org.springframework.stereotype.Service

@Service
class CommentService(
    private val commentRepository: CommentRepository
) {
    suspend fun save(comment: Comment) = commentRepository.save(comment)

    suspend fun find(messageId: UInt, size: Int = 10, cursorId: UInt = 0u) =
        commentRepository.find(messageId, size, cursorId)

    suspend fun count(messageId: UInt) = commentRepository.count(messageId)

    suspend fun update(comment: Comment) = commentRepository.update(comment)

    suspend fun delete(id: UInt) = commentRepository.delete(id)
}