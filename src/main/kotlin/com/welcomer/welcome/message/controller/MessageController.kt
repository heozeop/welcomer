package com.welcomer.welcome.message.controller

import com.welcomer.welcome.message.model.Message
import jakarta.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam

@Controller
class MessageController (
    private val messagesService: com.welcomer.welcome.message.service.MessagesService
) {
    @PostMapping("/messages")
    fun saveMessage(@Valid @RequestBody message: Message): Message = messagesService.save(message)

    @GetMapping("/messages/{id}")
    fun getMessageById(@PathVariable id: UInt): Message? = messagesService.findById(id)

    @GetMapping("/messages")
    fun getMessages(
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(defaultValue = "0") cursorId: UInt
    ): List<Message> = messagesService.find(size, cursorId)

    @PutMapping("/messages/{id}")
    fun updateMessage(
        @PathVariable id: UInt,
        @Valid @RequestBody message: Message
    ): Boolean {
        if (message.id != id) {
            throw IllegalArgumentException("Message ID in path does not match ID in request body")
        }

        return messagesService.update(message)
    }

    @DeleteMapping("/messages/{id}")
    fun deleteMessage(@PathVariable id: UInt): Boolean = messagesService.delete(id)
}