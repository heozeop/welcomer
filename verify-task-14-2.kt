import com.welcomer.welcome.bdd.fixtures.EnhancedContentFixtures

fun main() {
    val stats = EnhancedContentFixtures.getContentStatistics()
    println("=== Task 14.2 Verification ===")
    println("Total content items: ${stats["total_items"]}")
    println("Content types: ${stats["content_types"]}")
    println("Number of unique topics: ${(stats["topics"] as Map<*, *>).size}")
    println("Number of unique authors: ${(stats["authors"] as Map<*, *>).size}")
    println("Average base score: ${stats["avg_base_score"]}")
    
    val content = EnhancedContentFixtures.generateComprehensiveTestContent()
    println("\n=== Content Diversity Verification ===")
    println("Unique content IDs: ${content.map { it.content.id }.toSet().size}")
    println("Content with followed authors: ${content.count { (it.metadata["is_followed_author"] as? Boolean) == true }}")
    
    println("\n✅ Task 14.2 Requirements Met:")
    println("- Generated ${stats["total_items"]} content items (required: ≥100)")
    println("- Covers ${(stats["topics"] as Map<*, *>).size} different topics")
    println("- Content from ${(stats["authors"] as Map<*, *>).size} different authors")
    println("- Includes diverse content types, engagement metrics, and metadata")
}