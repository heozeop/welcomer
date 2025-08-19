package com.welcomer.welcome.messages.service

import com.welcomer.welcome.message.model.Comment
import com.welcomer.welcome.message.model.Comments
import com.welcomer.welcome.message.model.Messages
import com.welcomer.welcome.message.service.CommentService
import com.welcomer.welcome.message.service.MessageService
import com.welcomer.welcome.messages.fixtures.MessageFixture
import io.kotest.core.extensions.Extension
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.extensions.spring.SpringExtension
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional


@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MessageServiceTest(
    private val messageService: MessageService,
    private val commentService: CommentService
): BehaviorSpec({
    beforeSpec{
        transaction {
            SchemaUtils.create(Messages, Comments)
        }
    }

    beforeContainer {
        transaction {
            Messages.deleteAll()
            Comments.deleteAll()
        }
    }

    afterSpec {
        transaction {
            SchemaUtils.drop(Messages, Comments)
        }
    }

    given("MessageService") {
        `when`("a message is saved") {
            then("it should be stored correctly") {
                val message = MessageFixture.createMessage()
                val savedMessage = messageService.save(message)
                savedMessage.author shouldBe message.author
                savedMessage.content shouldBe message.content
                savedMessage.createdAt shouldNotBe null
                savedMessage.updatedAt shouldNotBe null
            }
        }

        `when`("a message is retrieved by ID") {
            then("it should return the correct message") {
                val message = messageService.save(MessageFixture.createMessage())
                val (receivedMessage, commentCount, comments) = messageService.findByIdForDisplay(message.id!!) ?: throw Exception("Message not found")
                receivedMessage.id shouldBe message.id
                receivedMessage.author shouldBe message.author
                receivedMessage.content shouldBe message.content
                commentCount shouldBe 0L // Assuming no comments initially
                comments shouldBe emptyList()
            }
        }

        `when`("messages are retrieved for display") {
            then("it should return messages with comment counts") {
                val message1 = messageService.save(MessageFixture.createMessage("Author 1", "Content 1"))
                val message2 = messageService.save(MessageFixture.createMessage("Author 2", "Content 2"))

                val (messages, totalCount, commentCountMap) = messageService.findForDisplay(10, 0u)

                messages.size shouldBe 2
                messages[0].id shouldBe message1.id
                messages[1].id shouldBe message2.id
                totalCount shouldBe 2L // Assuming two messages were saved
                commentCountMap.size shouldBe 2 // Assuming no comments yet
            }
        }

        `when`("the message count is requested") {
            then("it should return the correct count") {
                val message = MessageFixture.createMessage()
                messageService.save(message)

                val count = messageService.count()
                count shouldBe 1L // Assuming one message was saved
            }
        }

        `when`("a message is updated") {
            then("it should reflect the changes") {
                val message = messageService.save(MessageFixture.createMessage())
                val updatedMessage = message.copy(content = "Updated Content")
                val isUpdated = messageService.update(updatedMessage)

                isUpdated shouldBe true

                val retrievedMessage = messageService.findByIdForDisplay(message.id!!) ?: throw Exception("Message not found")
                retrievedMessage.first.content shouldBe "Updated Content"
            }
        }

        `when`("a message is deleted") {
            then("it should no longer exist in the repository") {
                val message = messageService.save(MessageFixture.createMessage())
                val isDeleted = messageService.delete(message.id!!)

                isDeleted shouldBe true

                val retrievedMessage = messageService.findByIdForDisplay(message.id!!)
                retrievedMessage shouldBe null // Message should not be found
            }
        }

        `when`("comments are added to a message") {
            val message = messageService.save(MessageFixture.createMessage())
            val comment = commentService.save(message.id!!, MessageFixture.createComment())

            then("comment count for message should be 1 when find with message id") {
                val (_, commentCount, comments) = messageService.findByIdForDisplay(message.id!!) ?: throw Exception("Message not found")

                commentCount shouldBe 1
                comments[0].id shouldNotBe null
                comments[0].author shouldBe comment.author
                comments[0].content shouldBe comment.content
            }

            then("comment count for message should be 1 when find with size and cursor id") {
                val (messages, _, commentCountMap) = messageService.findForDisplay(10, 0u)

                messages.size shouldBe 1
                messages[0].id shouldBe message.id
                commentCountMap[messages[0].id] shouldBe 1L
            }
        }
    }
})
