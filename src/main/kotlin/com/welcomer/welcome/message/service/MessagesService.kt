package com.welcomer.welcome.message.service

import com.welcomer.welcome.message.model.Message
import com.welcomer.welcome.message.repository.MessagesRepository
import org.springframework.stereotype.Service

@Service
class MessagesService(
    private val messagesRepository: MessagesRepository
) {
    fun save(message: Message): Message = messagesRepository.save(message)

    fun findById(id: UInt): Message? = messagesRepository.findById(id)

    fun find(size: Int, cursorId: UInt): List<Message> = messagesRepository.find(size, cursorId)

    fun count(): Long = messagesRepository.count()

    fun update(message: Message): Boolean = messagesRepository.update(message)

    fun delete(id: UInt): Boolean = messagesRepository.delete(id)
}