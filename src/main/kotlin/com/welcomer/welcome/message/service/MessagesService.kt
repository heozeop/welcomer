package com.welcomer.welcome.message.service

import com.welcomer.welcome.message.model.Message
import com.welcomer.welcome.message.repository.MessagesRepository
import org.springframework.stereotype.Service

@Service
class MessagesService(
    private val messagesRepository: MessagesRepository
) {
    suspend fun save(message: Message): Message = messagesRepository.save(message)

    suspend fun findById(id: UInt): Message? = messagesRepository.findById(id)

    suspend fun find(size: Int, cursorId: UInt): List<Message> = messagesRepository.find(size, cursorId)

    suspend fun count(): Long = messagesRepository.count()

    suspend fun update(message: Message): Boolean = messagesRepository.update(message)

    suspend fun delete(id: UInt): Boolean = messagesRepository.delete(id)
}