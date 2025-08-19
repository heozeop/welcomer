package com.welcomer.welcome.message.service

import com.welcomer.welcome.message.model.Comment
import com.welcomer.welcome.message.model.Message
import com.welcomer.welcome.message.repository.CommentRepository
import com.welcomer.welcome.message.repository.MessagesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.springframework.stereotype.Service

@Service
class MessageService(
    private val messagesRepository: MessagesRepository,
    private val commentRepository: CommentRepository
) {
    suspend fun save(message: Message): Message = messagesRepository.save(message)

    suspend fun findByIdForDisplay(id: UInt): Triple<Message, Long,List<Comment>>? {
        val message = messagesRepository.findById(id) ?: return null
        val (commentCount, comments) = coroutineScope {
            val countDef = async(Dispatchers.IO) { commentRepository.countMap(listOf(id))[id] ?: 0L }
            val commentsDef = async(Dispatchers.IO) { commentRepository.find(id) }

            Pair(countDef.await(), commentsDef.await())
        }

        return Triple(
            message,
            commentCount,
            comments
        )
    }

    suspend fun findForDisplay(size: Int, cursorId: UInt): Triple<List<Message>, Long, Map<UInt, Long>> {
        val (messages, messageCount) = coroutineScope {
            val messageRequest = async (Dispatchers.IO) { messagesRepository.find(size, cursorId) }
            val countRequest = async (Dispatchers.IO) { count() }

            Pair(messageRequest.await(), countRequest.await())
        }

        val commentCountMap = commentRepository.countMap(messages.mapNotNull { it.id })

        return Triple(messages, messageCount, commentCountMap)
    }

    suspend fun count(): Long = messagesRepository.count()

    suspend fun update(message: Message): Boolean = messagesRepository.update(message)

    suspend fun delete(id: UInt): Boolean = messagesRepository.delete(id)
}