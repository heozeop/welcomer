package com.welcomer.welcome.engagement.service

import com.welcomer.welcome.engagement.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.flow.*
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Real-time pipeline for processing high volumes of engagement events
 */
@Service
class RealTimeProcessingPipeline(
    private val trackingService: EngagementTrackingService,
    private val metricsService: MetricsAggregationService,
    private val config: PipelineConfig = PipelineConfig()
) {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val eventChannel = Channel<EngagementEvent>(Channel.UNLIMITED)
    private val metricsChannel = Channel<MetricsUpdate>(Channel.BUFFERED)
    
    // Pipeline metrics
    private val eventsProcessed = AtomicLong(0)
    private val eventsDropped = AtomicLong(0)
    private val pipelineHealthy = AtomicBoolean(true)
    
    // Circuit breaker
    private val circuitBreaker = CircuitBreaker(
        failureThreshold = config.circuitBreakerThreshold,
        resetTimeout = Duration.ofSeconds(config.circuitBreakerResetSeconds)
    )

    init {
        // Start the pipeline workers
        startPipeline()
    }

    /**
     * Submit an event to the pipeline
     */
    suspend fun submitEvent(event: EngagementEvent): Boolean {
        return try {
            if (!circuitBreaker.isOpen()) {
                eventChannel.send(event)
                true
            } else {
                eventsDropped.incrementAndGet()
                false
            }
        } catch (e: Exception) {
            eventsDropped.incrementAndGet()
            false
        }
    }

    /**
     * Start the processing pipeline
     */
    private fun startPipeline() {
        // Start event processors
        repeat(config.processingWorkers) { workerId ->
            scope.launch {
                processEvents(workerId)
            }
        }
        
        // Start metrics aggregator
        scope.launch {
            aggregateMetrics()
        }
        
        // Start monitoring
        scope.launch {
            monitorPipelineHealth()
        }
    }

    /**
     * Process events from the channel
     */
    private suspend fun processEvents(workerId: Int) {
        eventChannel.consumeAsFlow()
            .buffer(config.eventBufferSize)
            .collect { event ->
                processSingleEvent(event, workerId)
            }
    }

    /**
     * Process a single event
     */
    private suspend fun processSingleEvent(event: EngagementEvent, workerId: Int) {
        try {
            circuitBreaker.recordSuccess()
            
            // Process the event
            processEvent(event)
            
            eventsProcessed.incrementAndGet()
            
            // Send metrics update
            val metricsUpdate = MetricsUpdate(
                contentIds = listOf(event.contentId),
                eventTypes = listOf(event.type),
                count = 1,
                timestamp = Instant.now()
            )
            metricsChannel.send(metricsUpdate)
            
        } catch (e: Exception) {
            circuitBreaker.recordFailure()
            handleProcessingError(listOf(event), e)
        }
    }

    /**
     * Process a batch of events
     */
    private suspend fun processBatch(batch: List<EngagementEvent>, workerId: Int) {
        try {
            circuitBreaker.recordSuccess()
            
            // Process in parallel within batch
            coroutineScope {
                batch.map { event ->
                    async {
                        processEvent(event)
                    }
                }.awaitAll()
            }
            
            eventsProcessed.addAndGet(batch.size.toLong())
            
            // Send metrics update
            val metricsUpdate = MetricsUpdate(
                contentIds = batch.map { it.contentId }.distinct(),
                eventTypes = batch.map { it.type }.distinct(),
                count = batch.size,
                timestamp = Instant.now()
            )
            metricsChannel.send(metricsUpdate)
            
        } catch (e: Exception) {
            circuitBreaker.recordFailure()
            handleProcessingError(batch, e)
        }
    }

    /**
     * Process individual event
     */
    private suspend fun processEvent(event: EngagementEvent) {
        // Apply transformations
        val transformedEvent = applyTransformations(event)
        
        // Validate event
        if (!validateEvent(transformedEvent)) {
            eventsDropped.incrementAndGet()
            return
        }
        
        // Update real-time metrics
        updateRealTimeMetrics(transformedEvent)
        
        // Apply business rules
        applyBusinessRules(transformedEvent)
    }

    /**
     * Aggregate metrics from processed events
     */
    private suspend fun aggregateMetrics() {
        val metricsBuffer = mutableListOf<MetricsUpdate>()
        
        metricsChannel.consumeAsFlow()
            .buffer(config.metricsBufferSize)
            .conflate() // Keep only latest if consumer is slow
            .collect { update ->
                metricsBuffer.add(update)
                
                // Process when buffer is full
                if (metricsBuffer.size >= config.aggregationWindowSize) {
                    processMetricsWindow(metricsBuffer.toList())
                    metricsBuffer.clear()
                }
            }
    }

    /**
     * Process a window of metrics updates
     */
    private suspend fun processMetricsWindow(window: List<MetricsUpdate>) {
        if (window.isEmpty()) return
        
        val aggregated = window.groupBy { it.contentIds }
            .mapValues { (_, updates) ->
                updates.sumOf { it.count }
            }
        
        // Update metrics service
        aggregated.forEach { (contentIds, count) ->
            contentIds.forEach { contentId ->
                // Update metrics for each content
                updateContentMetrics(contentId, count)
            }
        }
    }

    /**
     * Monitor pipeline health
     */
    private suspend fun monitorPipelineHealth() {
        while (scope.isActive) {
            delay(config.healthCheckIntervalMs)
            
            val health = PipelineHealth(
                eventsProcessed = eventsProcessed.get(),
                eventsDropped = eventsDropped.get(),
                channelSize = 0, // Approximation - actual size not easily available
                circuitBreakerState = circuitBreaker.getState(),
                isHealthy = pipelineHealthy.get(),
                timestamp = Instant.now()
            )
            
            // Check health thresholds
            checkHealthThresholds(health)
            
            // Log health metrics
            logHealthMetrics(health)
        }
    }

    /**
     * Apply transformations to event
     */
    private fun applyTransformations(event: EngagementEvent): EngagementEvent {
        return event.copy(
            metadata = event.metadata + mapOf(
                "processed_at" to Instant.now().toEpochMilli(),
                "pipeline_version" to config.pipelineVersion
            )
        )
    }

    /**
     * Validate event
     */
    private fun validateEvent(event: EngagementEvent): Boolean {
        // Check if event is too old
        val eventAge = Duration.between(event.timestamp, Instant.now())
        if (eventAge.toMinutes() > config.maxEventAgeMinutes) {
            return false
        }
        
        // Validate required fields
        if (event.userId.isBlank() || event.contentId.isBlank()) {
            return false
        }
        
        return true
    }

    /**
     * Update real-time metrics
     */
    private suspend fun updateRealTimeMetrics(event: EngagementEvent) {
        // Update in-memory counters for real-time dashboards
        metricsService.updateMetrics(event.contentId, event.type)
    }

    /**
     * Apply business rules to events
     */
    private suspend fun applyBusinessRules(event: EngagementEvent) {
        // Example: Flag suspicious engagement patterns
        if (isSuspiciousEngagement(event)) {
            flagSuspiciousActivity(event)
        }
        
        // Example: Trigger notifications for significant events
        if (isSignificantEvent(event)) {
            triggerNotification(event)
        }
    }

    /**
     * Check if engagement is suspicious
     */
    private fun isSuspiciousEngagement(event: EngagementEvent): Boolean {
        // Simple heuristic - in production would use ML models
        return when {
            event.type == EngagementType.LIKE && event.metadata["duration"] as? Long ?: 0 < 100 -> true
            event.type == EngagementType.SHARE && event.metadata["platform"] == "unknown" -> true
            else -> false
        }
    }

    /**
     * Check if event is significant
     */
    private fun isSignificantEvent(event: EngagementEvent): Boolean {
        return event.type in setOf(
            EngagementType.SHARE,
            EngagementType.REPORT
        )
    }

    /**
     * Handle processing errors
     */
    private fun handleProcessingError(batch: List<EngagementEvent>, error: Exception) {
        println("Error processing batch of ${batch.size} events: ${error.message}")
        eventsDropped.addAndGet(batch.size.toLong())
        
        // In production, would send to dead letter queue
        batch.forEach { event ->
            logToDeadLetterQueue(event, error)
        }
    }

    /**
     * Check health thresholds
     */
    private fun checkHealthThresholds(health: PipelineHealth) {
        val dropRate = if (health.eventsProcessed > 0) {
            health.eventsDropped.toDouble() / health.eventsProcessed
        } else 0.0
        
        pipelineHealthy.set(
            dropRate < config.maxDropRate &&
            health.circuitBreakerState != CircuitBreakerState.OPEN
        )
        
        // Alert if unhealthy
        if (!pipelineHealthy.get()) {
            sendHealthAlert(health)
        }
    }

    /**
     * Update content metrics
     */
    private suspend fun updateContentMetrics(contentId: String, count: Int) {
        // Batch update to metrics service
        println("Updating metrics for content $contentId: $count events")
    }

    /**
     * Flag suspicious activity
     */
    private fun flagSuspiciousActivity(event: EngagementEvent) {
        println("Suspicious activity detected: User ${event.userId} -> Content ${event.contentId}")
        // In production, would log to security monitoring system
    }

    /**
     * Trigger notification for significant events
     */
    private fun triggerNotification(event: EngagementEvent) {
        println("Significant event: ${event.type} on content ${event.contentId}")
        // In production, would trigger actual notifications
    }

    /**
     * Log to dead letter queue
     */
    private fun logToDeadLetterQueue(event: EngagementEvent, error: Exception) {
        println("DLQ: Event ${event.id} failed with error: ${error.message}")
        // In production, would persist to DLQ
    }

    /**
     * Log health metrics
     */
    private fun logHealthMetrics(health: PipelineHealth) {
        println("Pipeline health: Processed=${health.eventsProcessed}, Dropped=${health.eventsDropped}, Healthy=${health.isHealthy}")
    }

    /**
     * Send health alert
     */
    private fun sendHealthAlert(health: PipelineHealth) {
        println("ALERT: Pipeline unhealthy! Drop rate too high or circuit breaker open")
        // In production, would send actual alerts
    }

    /**
     * Shutdown the pipeline gracefully
     */
    fun shutdown() {
        scope.cancel()
        eventChannel.close()
        metricsChannel.close()
    }

    /**
     * Get pipeline statistics
     */
    fun getStatistics(): PipelineStatistics {
        return PipelineStatistics(
            eventsProcessed = eventsProcessed.get(),
            eventsDropped = eventsDropped.get(),
            dropRate = if (eventsProcessed.get() > 0) {
                eventsDropped.get().toDouble() / eventsProcessed.get()
            } else 0.0,
            isHealthy = pipelineHealthy.get(),
            circuitBreakerState = circuitBreaker.getState()
        )
    }
}

/**
 * Pipeline configuration
 */
data class PipelineConfig(
    val processingWorkers: Int = 4,
    val batchSize: Int = 50,
    val eventBufferSize: Int = 1000,
    val metricsBufferSize: Int = 100,
    val aggregationWindowSize: Int = 10,
    val healthCheckIntervalMs: Long = 5000,
    val maxEventAgeMinutes: Long = 60,
    val maxDropRate: Double = 0.01,
    val circuitBreakerThreshold: Int = 5,
    val circuitBreakerResetSeconds: Long = 30,
    val pipelineVersion: String = "1.0.0"
)

/**
 * Metrics update message
 */
data class MetricsUpdate(
    val contentIds: List<String>,
    val eventTypes: List<EngagementType>,
    val count: Int,
    val timestamp: Instant
)

/**
 * Pipeline health status
 */
data class PipelineHealth(
    val eventsProcessed: Long,
    val eventsDropped: Long,
    val channelSize: Int,
    val circuitBreakerState: CircuitBreakerState,
    val isHealthy: Boolean,
    val timestamp: Instant
)

/**
 * Pipeline statistics
 */
data class PipelineStatistics(
    val eventsProcessed: Long,
    val eventsDropped: Long,
    val dropRate: Double,
    val isHealthy: Boolean,
    val circuitBreakerState: CircuitBreakerState
)

/**
 * Simple circuit breaker implementation
 */
class CircuitBreaker(
    private val failureThreshold: Int,
    private val resetTimeout: Duration
) {
    private var failureCount = 0
    private var lastFailureTime: Instant? = null
    private var state = CircuitBreakerState.CLOSED

    fun isOpen(): Boolean = state == CircuitBreakerState.OPEN

    fun recordSuccess() {
        if (state == CircuitBreakerState.HALF_OPEN) {
            state = CircuitBreakerState.CLOSED
            failureCount = 0
        }
    }

    fun recordFailure() {
        failureCount++
        lastFailureTime = Instant.now()
        
        if (failureCount >= failureThreshold) {
            state = CircuitBreakerState.OPEN
        }
    }

    fun getState(): CircuitBreakerState {
        if (state == CircuitBreakerState.OPEN) {
            lastFailureTime?.let {
                if (Duration.between(it, Instant.now()) > resetTimeout) {
                    state = CircuitBreakerState.HALF_OPEN
                }
            }
        }
        return state
    }
}

enum class CircuitBreakerState {
    CLOSED,
    OPEN,
    HALF_OPEN
}