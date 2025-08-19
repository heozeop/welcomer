package com.welcomer.welcome.messages.fixtures

import com.welcomer.welcome.message.model.Comment
import com.welcomer.welcome.message.model.Message

class MessageFixture {
    companion object {
        fun createMessage(author: String = "Test Author", content: String = "Test Content") = Message(author, content)

        fun createComment(author: String = "Test Commenter", content: String = "Test Comment") = Comment(author, content)
    }
}