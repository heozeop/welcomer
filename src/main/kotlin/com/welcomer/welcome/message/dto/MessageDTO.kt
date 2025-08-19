package com.welcomer.welcome.message.dto

import com.welcomer.welcome.message.model.Comment
import com.welcomer.welcome.message.model.Message

data class MessageDTO(
    val id: UInt? = null,
    val author: String,
    val content: String,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val commentsCount: Long = 0L
)

data class MessageCreateDTO(
    val author: String,
    val content: String
) {
    fun toMessage():Message = Message(
            author = author,
            content = content
        )
}

data class MessageUpdateDTO(
    val author: String? = null,
    val content: String? = null
)

data class MessageListDTO(
    val messages: List<MessageDTO>,
    val nextCursorId: UInt? = null,
    val totalCount: Long = 0L
)

data class MessageDetailDTO (
    val id: UInt,
    val author: String,
    val content: String,
    val createdAt: String,
    val updatedAt: String,
    val commentsCount: Long = 0L,
    val comments: List<Comment> = emptyList()
)
