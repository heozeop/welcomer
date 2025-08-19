package com.welcomer.welcome.message.controller

import com.welcomer.welcome.message.dto.MessageCreateDTO
import com.welcomer.welcome.message.dto.MessageDTO
import com.welcomer.welcome.message.dto.MessageDetailDTO
import com.welcomer.welcome.message.dto.MessageListDTO
import com.welcomer.welcome.message.model.Comment
import com.welcomer.welcome.message.model.Message
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
import kotlinx.coroutines.runBlocking
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("messages")
class MessageController (
    private val messagesService: com.welcomer.welcome.message.service.MessagesService,
    private val commentService: com.welcomer.welcome.message.service.CommentService
) {
    @Operation(summary = "Save a new message")
    @ApiResponse(responseCode = "200", description = "Message saved successfully")
    @PostMapping("/")
    fun saveMessage(@Valid @RequestBody message: MessageCreateDTO): MessageDTO = runBlocking {
        val response = messagesService.save(message.toMessage())

        MessageDTO(
            id = response.id,
            author = response.author,
            content = response.content,
            createdAt = response.createdAt.toString(),
            updatedAt = response.updatedAt.toString()
        )
    }

    @Operation(summary = "Save a comment for a message")
    @ApiResponse(responseCode = "200", description = "Comment saved successfully")
    @PostMapping("/{id}/comments")
    fun saveComment(
        @PathVariable id: UInt,
        @Valid @RequestBody comment: Comment
    ): Comment = runBlocking { commentService.save(id, comment) }

    @Operation(summary = "Get message by ID")
    @ApiResponse(responseCode = "200", description = "Message retrieved successfully")
    @GetMapping("/{id}")
    fun getMessageById(@PathVariable id: UInt): MessageDetailDTO? = runBlocking {
        val response = messagesService.findByIdForDisplay(id)

        if(response != null) {
            val (message, commentCount, comments) = response

            MessageDetailDTO(
                id = message.id ?: 0u,
                author = message.author,
                content = message.content,
                createdAt = message.createdAt.toString(),
                updatedAt = message.updatedAt.toString(),
                commentsCount = commentCount,
                comments = comments
            )
        } else {
            null
        }
    }

    @Operation(summary = "Get all messages with pagination")
    @ApiResponse(responseCode = "200", description = "Messages retrieved successfully")
    @GetMapping("/")
    fun getMessages(
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(defaultValue = "0") cursorId: UInt
    ): MessageListDTO = runBlocking {
        val (messages, messageCount, commentCountMap ) = messagesService.findForDisplay(size, cursorId)

        MessageListDTO(
            messages = messages.map {
                MessageDTO(
                    id = it.id,
                    author = it.author,
                    content = it.content,
                    createdAt = it.createdAt.toString(),
                    updatedAt = it.updatedAt.toString(),
                    commentsCount = commentCountMap[it.id] ?: 0L
                )
            },
            nextCursorId = if (messages.isNotEmpty()) messages.last().id else null,
            totalCount = messageCount
        )
    }

    @Operation(summary = "Get total count of messages")
    @ApiResponse(responseCode = "200", description = "Total count of messages retrieved successfully")
    @GetMapping("/count")
    fun getMessagesCount(): Long = runBlocking { messagesService.count() }

    @Operation(summary = "Get comments for a message")
    @ApiResponse(responseCode = "200", description = "Comments retrieved successfully")
    @GetMapping("/{id}/comments")
    fun getComments(
        @PathVariable id: UInt,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(defaultValue = "0") cursorId: UInt
    ): List<Comment> = runBlocking { commentService.find(id, size, cursorId) }

    @Operation(summary = "Save a comment for a message")
    @ApiResponse(responseCode = "200", description = "Comment saved successfully")
    @GetMapping("/{id}/comments/count")
    fun getCommentCount(
        @PathVariable id: UInt
    ): Long = runBlocking { commentService.count(id) }

    @Operation(summary = "update a message by ID")
    @ApiResponse(responseCode = "200", description = "update message successfully")
    @PutMapping("/{id}")
    fun updateMessage(
        @PathVariable id: UInt,
        @Valid @RequestBody message: Message
    ): Boolean = runBlocking {
        if (message.id != id) {
            throw IllegalArgumentException("Message ID in path does not match ID in request body")
        }

        messagesService.update(message)
    }

    @Operation(summary = "Update a comment by ID")
    @ApiResponse(responseCode = "200", description = "Comment updated successfully")
    @PutMapping("/{id}/comments/{commentId}")
    fun updateComment(
        @PathVariable id: UInt,
        @PathVariable commentId: UInt,
        @Valid @RequestBody comment: Comment
    ): Boolean = runBlocking {
        if (comment.id != id) {
            throw IllegalArgumentException("Comment ID in path does not match ID in request body")
        }

        commentService.update(id, commentId, comment)
    }

    @Operation(summary = "Delete a message by ID")
    @ApiResponse(responseCode = "200", description = "Message deleted successfully")
    @DeleteMapping("/{id}")
    fun deleteMessage(@PathVariable id: UInt): Boolean = runBlocking { messagesService.delete(id) }

    @Operation(summary = "Delete a comment by ID")
    @ApiResponse(responseCode = "200", description = "Comment deleted successfully")
    @DeleteMapping("/{id}/comments/{commentId}")
    fun deleteComment(
        @PathVariable id: UInt,
        @PathVariable commentId: UInt
    ): Boolean = runBlocking { commentService.delete(id, commentId) }
}