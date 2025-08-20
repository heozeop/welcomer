package com.welcomer.welcome.message.controller

import com.welcomer.welcome.message.dto.SearchDTO
import com.welcomer.welcome.message.dto.SearchType
import com.welcomer.welcome.message.service.SearchService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/search")
class SearchController(private val seachService: SearchService) {
    @Operation(summary = "Search messages and comments")
    @ApiResponse(responseCode = "200", description = "Search results retrieved successfully")
    @GetMapping()
    suspend fun search(
        @RequestParam query: String,
        @RequestParam types: List<SearchType>,
        @RequestParam size: Int = 10,
        @RequestParam cursorId: String? = null,
    ): com.welcomer.welcome.message.dto.SearchResultDTO {
        return seachService.search(
            SearchDTO(
                query = query,
                size = size,
                cursorId = cursorId,
                types = types
            )
        )
    }
}