package com.welcomer.welcome.engagement.event

import com.welcomer.welcome.engagement.model.EngagementEvent
import com.welcomer.welcome.engagement.model.EngagementType
import com.welcomer.welcome.user.service.BehaviorAnalysisService
import com.welcomer.welcome.user.model.BehaviorEvent
import com.welcomer.welcome.user.model.BehaviorEventType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger

/**
 * Event publisher for engagement events to notify downstream systems
 */
@Component
class EngagementEventPublisher(
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val behaviorAnalysisService: BehaviorAnalysisService,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {

    // Event queue for batching
    private val eventQueue = ConcurrentLinkedQueue<EngagementEvent>()
    private val queueSize = AtomicInteger(0)
    
    companion object {
        private const val BATCH_SIZE = 50
        private const val FLUSH_INTERVAL_MS = 5000L
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val RETRY_DELAY_MS = 1000L
    }

    /**
     * Publish an engagement event to downstream systems
     */
    fun publishEngagementEvent(event: EngagementEvent) {
        scope.launch {
            try {
                // Add to queue for batching
                eventQueue.offer(event)
                val currentSize = queueSize.incrementAndGet()
                
                // Process immediately if batch size reached
                if (currentSize >= BATCH_SIZE) {
                    processBatch()
                }
                
                // Publish Spring event for immediate processing
                publishSpringEvent(event)
                
                // Update behavior analysis service
                updateBehaviorAnalysis(event)
                
                // Update recommendation system
                updateRecommendationSystem(event)
                
            } catch (e: Exception) {
                // Log error but don't fail
                println("Error publishing engagement event: ${e.message}")
            }
        }
    }

    /**
     * Process batch of events
     */
    private suspend fun processBatch() {
        val batch = mutableListOf<EngagementEvent>()
        var event = eventQueue.poll()
        
        while (event != null && batch.size < BATCH_SIZE) {
            batch.add(event)
            queueSize.decrementAndGet()
            event = eventQueue.poll()
        }
        
        if (batch.isNotEmpty()) {
            publishBatchToKafka(batch)
            updateMetricsInBatch(batch)
        }
    }

    /**
     * Publish Spring application event
     */
    private fun publishSpringEvent(event: EngagementEvent) {
        val springEvent = EngagementRecordedEvent(
            userId = event.userId,
            contentId = event.contentId,
            engagementType = event.type,
            metadata = event.metadata,
            timestamp = event.timestamp
        )
        
        applicationEventPublisher.publishEvent(springEvent)
    }

    /**
     * Update behavior analysis with engagement data
     */
    private suspend fun updateBehaviorAnalysis(event: EngagementEvent) {
        val behaviorEvent = convertToBehaviorEvent(event)
        behaviorAnalysisService.processBehaviorEvent(behaviorEvent)
    }

    /**
     * Update recommendation system with engagement signal
     */
    private suspend fun updateRecommendationSystem(event: EngagementEvent) {
        // Send to recommendation service
        val recommendationUpdate = RecommendationUpdate(
            userId = event.userId,
            contentId = event.contentId,
            signal = mapEngagementToSignal(event.type),
            strength = calculateSignalStrength(event.type),
            timestamp = event.timestamp
        )
        
        // In production, this would call the recommendation service
        publishRecommendationUpdate(recommendationUpdate)
    }

    /**
     * Publish batch to Kafka or message queue
     */
    private suspend fun publishBatchToKafka(batch: List<EngagementEvent>) {
        // In production, this would publish to Kafka
        println("Publishing batch of ${batch.size} events to Kafka")
        
        val kafkaMessage = EngagementBatchMessage(
            batchId = generateBatchId(),
            events = batch.map { convertToKafkaEvent(it) },
            timestamp = Instant.now()
        )
        
        // Simulate Kafka publish with retry logic
        var attempts = 0
        var success = false
        
        while (attempts < MAX_RETRY_ATTEMPTS && !success) {
            try {
                // kafkaTemplate.send("engagement-events", kafkaMessage)
                simulateKafkaPublish(kafkaMessage)
                success = true
            } catch (e: Exception) {
                attempts++
                if (attempts < MAX_RETRY_ATTEMPTS) {
                    Thread.sleep(RETRY_DELAY_MS * attempts)
                } else {
                    // Log to dead letter queue
                    logToDeadLetterQueue(batch)
                }
            }
        }
    }

    /**
     * Update metrics service with batch
     */
    private suspend fun updateMetricsInBatch(batch: List<EngagementEvent>) {
        val metricsUpdates = batch.groupBy { it.contentId }
            .map { (contentId, events) ->
                ContentMetricsUpdate(
                    contentId = contentId,
                    engagements = events.map { it.type },
                    timestamp = Instant.now()
                )
            }
        
        // In production, would call metrics service
        metricsUpdates.forEach { update ->
            println("Updating metrics for content ${update.contentId}: ${update.engagements.size} events")
        }
    }

    /**
     * Convert engagement event to behavior event
     */
    private fun convertToBehaviorEvent(event: EngagementEvent): BehaviorEvent {
        val behaviorType = when (event.type) {
            EngagementType.VIEW -> BehaviorEventType.CONTENT_VIEW
            EngagementType.LIKE -> BehaviorEventType.CONTENT_LIKE
            EngagementType.SHARE -> BehaviorEventType.CONTENT_SHARE
            EngagementType.COMMENT -> BehaviorEventType.CONTENT_COMMENT
            EngagementType.CLICK -> BehaviorEventType.CONTENT_CLICK
            EngagementType.BOOKMARK -> BehaviorEventType.CONTENT_BOOKMARK
            EngagementType.HIDE -> BehaviorEventType.CONTENT_HIDE
            EngagementType.REPORT -> BehaviorEventType.CONTENT_REPORT
            EngagementType.DWELL_TIME -> BehaviorEventType.DWELL_TIME
            else -> BehaviorEventType.CONTENT_VIEW
        }
        
        return BehaviorEvent(
            id = event.id,
            userId = event.userId,
            eventType = behaviorType,
            contentId = event.contentId,
            duration = event.metadata["duration"]?.let {
                Duration.ofMillis(it as Long)
            },
            metadata = event.metadata,
            timestamp = event.timestamp,
            sessionId = event.sessionId
        )
    }

    /**
     * Map engagement type to recommendation signal
     */
    private fun mapEngagementToSignal(type: EngagementType): RecommendationSignal {
        return when (type) {
            EngagementType.LIKE -> RecommendationSignal.POSITIVE_STRONG
            EngagementType.SHARE -> RecommendationSignal.POSITIVE_STRONG
            EngagementType.COMMENT -> RecommendationSignal.POSITIVE_MEDIUM
            EngagementType.BOOKMARK -> RecommendationSignal.POSITIVE_STRONG
            EngagementType.CLICK -> RecommendationSignal.POSITIVE_WEAK
            EngagementType.VIEW -> RecommendationSignal.NEUTRAL
            EngagementType.HIDE -> RecommendationSignal.NEGATIVE_STRONG
            EngagementType.REPORT -> RecommendationSignal.NEGATIVE_STRONG
            EngagementType.UNLIKE -> RecommendationSignal.NEGATIVE_MEDIUM
            EngagementType.UNBOOKMARK -> RecommendationSignal.NEGATIVE_MEDIUM
            else -> RecommendationSignal.NEUTRAL
        }
    }

    /**
     * Calculate signal strength for recommendation update
     */
    private fun calculateSignalStrength(type: EngagementType): Double {
        return when (type) {
            EngagementType.SHARE -> 1.0
            EngagementType.LIKE -> 0.8
            EngagementType.BOOKMARK -> 0.8
            EngagementType.COMMENT -> 0.7
            EngagementType.CLICK -> 0.3
            EngagementType.VIEW -> 0.1
            EngagementType.HIDE -> -0.8
            EngagementType.REPORT -> -1.0
            EngagementType.UNLIKE -> -0.5
            EngagementType.UNBOOKMARK -> -0.5
            else -> 0.0
        }
    }

    private fun convertToKafkaEvent(event: EngagementEvent): KafkaEngagementEvent {
        return KafkaEngagementEvent(
            id = event.id,
            userId = event.userId,
            contentId = event.contentId,
            type = event.type.name,
            metadata = event.metadata,
            timestamp = event.timestamp.toEpochMilli()
        )
    }

    private fun generateBatchId(): String {
        return "batch_${System.currentTimeMillis()}_${Thread.currentThread().threadId()}"
    }

    private fun simulateKafkaPublish(message: EngagementBatchMessage) {
        // Simulate Kafka publish
        println("Publishing to Kafka: Batch ${message.batchId} with ${message.events.size} events")
    }

    private fun logToDeadLetterQueue(batch: List<EngagementEvent>) {
        println("Logging ${batch.size} events to dead letter queue")
        // In production, would persist to DLQ for later processing
    }

    private fun publishRecommendationUpdate(update: RecommendationUpdate) {
        // In production, would call recommendation service
        println("Updating recommendations: User ${update.userId} -> Content ${update.contentId}: ${update.signal}")
    }
}

/**
 * Spring event for engagement recorded
 */
data class EngagementRecordedEvent(
    val userId: String,
    val contentId: String,
    val engagementType: EngagementType,
    val metadata: Map<String, Any>,
    val timestamp: Instant
)

/**
 * Recommendation update model
 */
data class RecommendationUpdate(
    val userId: String,
    val contentId: String,
    val signal: RecommendationSignal,
    val strength: Double,
    val timestamp: Instant
)

enum class RecommendationSignal {
    POSITIVE_STRONG,
    POSITIVE_MEDIUM,
    POSITIVE_WEAK,
    NEUTRAL,
    NEGATIVE_WEAK,
    NEGATIVE_MEDIUM,
    NEGATIVE_STRONG
}

/**
 * Kafka message models
 */
data class EngagementBatchMessage(
    val batchId: String,
    val events: List<KafkaEngagementEvent>,
    val timestamp: Instant
)

data class KafkaEngagementEvent(
    val id: String,
    val userId: String,
    val contentId: String,
    val type: String,
    val metadata: Map<String, Any>,
    val timestamp: Long
)

data class ContentMetricsUpdate(
    val contentId: String,
    val engagements: List<EngagementType>,
    val timestamp: Instant
)