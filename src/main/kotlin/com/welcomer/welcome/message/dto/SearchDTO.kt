package com.welcomer.welcome.message.dto

import com.welcomer.welcome.message.model.Comment
import com.welcomer.welcome.message.model.Message
import java.time.LocalDate
import java.time.LocalDateTime

enum class SearchType {
    MESSAGE,
    COMMENT
}

data class SearchDTO(
    val query: String,
    val size: Int = 10,
    val cursorId: String? = null,
    val types: List<SearchType> = listOf(SearchType.MESSAGE, SearchType.COMMENT)
)

data class SearchItemDTO (
    val id: UInt,
    val type: SearchType,
    val content: String,
    val author: String? = null,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null
)

data class SearchResultDTO(
    val items: List<SearchItemDTO> = emptyList(),
    val cursorId: String? = null
)