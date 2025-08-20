package com.welcomer.welcome.message.service

import com.welcomer.welcome.infrastructure.cursor.CursorKeys
import com.welcomer.welcome.infrastructure.cursor.CursorService
import com.welcomer.welcome.infrastructure.cursor.CursorStatus
import com.welcomer.welcome.message.dto.SearchDTO
import com.welcomer.welcome.message.dto.SearchItemDTO
import com.welcomer.welcome.message.dto.SearchResultDTO
import com.welcomer.welcome.message.dto.SearchType
import com.welcomer.welcome.message.model.Comment
import com.welcomer.welcome.message.model.Message
import kotlinx.coroutines.supervisorScope
import org.jboss.logging.Messages
import org.springframework.stereotype.Service
import java.util.*
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Service
class SearchService(
    private val messageService: MessageService,
    private val commentService: CommentService,
    private val cursorService: CursorService
) {
    suspend fun search(searchDTO: SearchDTO): SearchResultDTO {
        val (query, size, cursorId, types) = searchDTO

        if (query.isBlank() || types.isEmpty()) {
            return SearchResultDTO()
        }

        val cursorStatus: CursorStatus = if (cursorId != null) {
                cursorService.getCursor(types, cursorId) ?: CursorStatus(
                    sources = types.map { type -> type.name to 0u }.toMap(),
                    version = 0L
                )
            } else {
                CursorStatus(
                    sources = types.map { type -> type.name to 0u }.toMap(),
                    version = 0L
                )
            }

        val (messages: List<Message>, comments: List<Comment>) = supervisorScope {
            val messageDeferred = if (SearchType.MESSAGE in types) {
                messageService.search(query, cursorStatus.sources[SearchType.MESSAGE.name] ?: 0u, size)
            } else {
                emptyList<Message>()
            }

            val commentDeferred = if (SearchType.COMMENT in types) {
                commentService.search(query, cursorStatus.sources[SearchType.COMMENT.name] ?: 0u, size)
            } else {
                emptyList<Comment>()
            }

            messageDeferred to commentDeferred
        }

        val pq = PriorityQueue(

            compareByDescending<SearchItemDTO> { it.createdAt }
                .thenByDescending { it.updatedAt }
                .thenByDescending { it.id }
        )

        pq.addAll(
            listOf(
                messages.map {
                    SearchItemDTO(
                        id = it.id!!,
                        type = SearchType.MESSAGE,
                        content = it.content,
                        author = it.author,
                        createdAt = it.createdAt,
                        updatedAt = it.updatedAt
                    )
                },
                comments.map {
                    SearchItemDTO(
                        id = it.id!!,
                        type = SearchType.COMMENT,
                        content = it.content,
                        author = it.author,
                        createdAt = it.createdAt,
                        updatedAt = it.updatedAt
                    )
                }
            ).flatten(),
        )

        var yielded = 0
        val results = mutableListOf<SearchItemDTO>()
        while(pq.isNotEmpty() && yielded < size) {
            val item = pq.poll()
            results.add(item)
            yielded++
        }

        val nextCursorId = cursorId
            ?: cursorService.createCursor(
                types,
                CursorStatus(
                    sources = cursorStatus.sources.toMutableMap().apply {
                        this[results.last().type.name] = results.last().id
                    },
                    version = cursorStatus.version + 1
                ),
                ttl = 300.toDuration(DurationUnit.SECONDS)
            )


        return SearchResultDTO(
            items = results,
            cursorId = nextCursorId
        )
    }
}