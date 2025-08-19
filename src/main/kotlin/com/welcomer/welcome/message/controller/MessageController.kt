package com.welcomer.welcome.message.controller

import com.welcomer.welcome.message.model.Message
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("messages")
class MessageController (
    private val messagesService: com.welcomer.welcome.message.service.MessagesService
) {
    @Operation(summary = "Save a new message")
    @ApiResponse(responseCode = "200", description = "Message saved successfully")
    @PostMapping("/")
    fun saveMessage(@Valid @RequestBody message: Message): Message = messagesService.save(message)

    @Operation(summary = "Get message by ID")
    @ApiResponse(responseCode = "200", description = "Message retrieved successfully")
    @GetMapping("/{id}")
    fun getMessageById(@PathVariable id: UInt): Message? = messagesService.findById(id)

    @Operation(summary = "Get all messages with pagination")
    @ApiResponse(responseCode = "200", description = "Messages retrieved successfully")
    @GetMapping("/")
    fun getMessages(
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(defaultValue = "0") cursorId: UInt
    ): List<Message> = messagesService.find(size, cursorId)

    @Operation(summary = "Get total count of messages")
    @ApiResponse(responseCode = "200", description = "Total count of messages retrieved successfully")
    @GetMapping("/count")
    fun getMessagesCount(): Long = messagesService.count()

    @Operation(summary = "update a message by ID")
    @ApiResponse(responseCode = "200", description = "update message successfully")
    @PutMapping("/{id}")
    fun updateMessage(
        @PathVariable id: UInt,
        @Valid @RequestBody message: Message
    ): Boolean {
        if (message.id != id) {
            throw IllegalArgumentException("Message ID in path does not match ID in request body")
        }

        return messagesService.update(message)
    }

    @Operation(summary = "Delete a message by ID")
    @ApiResponse(responseCode = "200", description = "Message deleted successfully")
    @DeleteMapping("/{id}")
    fun deleteMessage(@PathVariable id: UInt): Boolean = messagesService.delete(id)
}