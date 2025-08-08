-- V5__Create_Feed_Composition_Tables.sql
-- SNS Feed System - Feed Composition Model
-- Database: MySQL 8.0

-- Feed entries table for composed feeds per user
CREATE TABLE feed_entries (
    id CHAR(36) PRIMARY KEY COMMENT 'UUID as string',
    user_id CHAR(36) NOT NULL COMMENT 'User whose feed this entry belongs to',
    content_id CHAR(36) NOT NULL COMMENT 'Content ID in this feed entry',
    feed_type ENUM('home', 'following', 'explore', 'trending', 'personalized') NOT NULL DEFAULT 'home' COMMENT 'Type of feed',
    algorithm_id CHAR(36) NULL COMMENT 'Algorithm used to generate this entry',
    score DECIMAL(10,6) DEFAULT 0.000000 COMMENT 'Relevance/ranking score for this entry',
    rank_position INT UNSIGNED COMMENT 'Position in feed ranking',
    reasons JSON COMMENT 'Reasons why this content was included (JSON array)',
    source_type ENUM('following', 'trending', 'recommendation', 'promoted', 'manual') DEFAULT 'following' COMMENT 'How this content was sourced',
    boosted BOOLEAN DEFAULT FALSE COMMENT 'Whether this entry was algorithmically boosted',
    generated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'When this feed entry was generated',
    expires_at TIMESTAMP NULL COMMENT 'When this feed entry should be refreshed',
    seen_at TIMESTAMP NULL COMMENT 'When user last saw this entry',
    interacted_at TIMESTAMP NULL COMMENT 'When user last interacted with this entry',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (content_id) REFERENCES content(id) ON DELETE CASCADE,
    FOREIGN KEY (algorithm_id) REFERENCES algorithm_configs(id) ON DELETE SET NULL,
    UNIQUE KEY unique_user_content_feed (user_id, content_id, feed_type),
    INDEX idx_user_feed_type (user_id, feed_type),
    INDEX idx_user_score (user_id, feed_type, score DESC),
    INDEX idx_user_rank (user_id, feed_type, rank_position ASC),
    INDEX idx_content_id (content_id),
    INDEX idx_algorithm_id (algorithm_id),
    INDEX idx_generated_at (generated_at),
    INDEX idx_expires_at (expires_at),
    INDEX idx_seen_at (seen_at),
    INDEX idx_source_type (source_type),
    INDEX idx_boosted (boosted),
    INDEX idx_feed_refresh (user_id, feed_type, expires_at)
) ENGINE=InnoDB COMMENT='Composed feed entries for users';

-- Feed metadata table for tracking feed generation info
CREATE TABLE feed_metadata (
    user_id CHAR(36) NOT NULL COMMENT 'User ID',
    feed_type ENUM('home', 'following', 'explore', 'trending', 'personalized') NOT NULL COMMENT 'Type of feed',
    last_generated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'When feed was last generated',
    generation_duration_ms INT UNSIGNED COMMENT 'Time taken to generate feed in milliseconds',
    algorithm_id CHAR(36) NULL COMMENT 'Algorithm used for generation',
    content_count INT UNSIGNED DEFAULT 0 COMMENT 'Number of entries in this feed',
    generation_reason ENUM('manual', 'scheduled', 'user_action', 'content_update') DEFAULT 'scheduled' COMMENT 'Why feed was regenerated',
    parameters JSON COMMENT 'Parameters used during generation',
    performance_metrics JSON COMMENT 'Performance metrics from generation',
    next_refresh_at TIMESTAMP NULL COMMENT 'When feed should be refreshed next',
    version INT UNSIGNED DEFAULT 1 COMMENT 'Feed version number for cache invalidation',
    PRIMARY KEY (user_id, feed_type),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (algorithm_id) REFERENCES algorithm_configs(id) ON DELETE SET NULL,
    INDEX idx_last_generated_at (last_generated_at),
    INDEX idx_next_refresh_at (next_refresh_at),
    INDEX idx_algorithm_id (algorithm_id),
    INDEX idx_generation_reason (generation_reason)
) ENGINE=InnoDB COMMENT='Metadata for feed generation tracking';

-- Feed cache configuration table
CREATE TABLE feed_cache_configs (
    id CHAR(36) PRIMARY KEY COMMENT 'UUID as string',
    feed_type ENUM('home', 'following', 'explore', 'trending', 'personalized') NOT NULL UNIQUE COMMENT 'Type of feed',
    cache_ttl_minutes INT UNSIGNED DEFAULT 30 COMMENT 'Cache time-to-live in minutes',
    max_entries INT UNSIGNED DEFAULT 100 COMMENT 'Maximum entries to cache per user',
    refresh_threshold_minutes INT UNSIGNED DEFAULT 15 COMMENT 'Minutes before expiry to start refresh',
    batch_size INT UNSIGNED DEFAULT 20 COMMENT 'Number of entries to fetch per page',
    enable_prefetch BOOLEAN DEFAULT TRUE COMMENT 'Whether to prefetch next batch',
    enable_real_time_updates BOOLEAN DEFAULT FALSE COMMENT 'Whether to update in real-time',
    algorithm_refresh_triggers JSON COMMENT 'Events that trigger algorithm refresh',
    cache_warming_enabled BOOLEAN DEFAULT TRUE COMMENT 'Whether to warm cache for active users',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Configuration creation timestamp',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',
    INDEX idx_cache_ttl (cache_ttl_minutes),
    INDEX idx_enable_real_time (enable_real_time_updates),
    INDEX idx_cache_warming (cache_warming_enabled)
) ENGINE=InnoDB COMMENT='Feed caching configuration';

-- User feed preferences table
CREATE TABLE user_feed_preferences (
    user_id CHAR(36) NOT NULL COMMENT 'User ID',
    feed_type ENUM('home', 'following', 'explore', 'trending', 'personalized') NOT NULL COMMENT 'Type of feed',
    is_enabled BOOLEAN DEFAULT TRUE COMMENT 'Whether this feed type is enabled for user',
    default_algorithm_id CHAR(36) NULL COMMENT 'User preferred algorithm for this feed',
    refresh_frequency ENUM('real_time', 'high', 'medium', 'low') DEFAULT 'medium' COMMENT 'How often to refresh this feed',
    content_filters JSON COMMENT 'Content filters applied to this feed',
    display_preferences JSON COMMENT 'Display preferences for this feed type',
    notification_settings JSON COMMENT 'Notification settings for feed updates',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Preference creation timestamp',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',
    PRIMARY KEY (user_id, feed_type),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (default_algorithm_id) REFERENCES algorithm_configs(id) ON DELETE SET NULL,
    INDEX idx_is_enabled (is_enabled),
    INDEX idx_default_algorithm_id (default_algorithm_id),
    INDEX idx_refresh_frequency (refresh_frequency),
    INDEX idx_updated_at (updated_at)
) ENGINE=InnoDB COMMENT='User preferences for different feed types';

-- Feed generation queue for async processing
CREATE TABLE feed_generation_queue (
    id CHAR(36) PRIMARY KEY COMMENT 'UUID as string',
    user_id CHAR(36) NOT NULL COMMENT 'User ID for feed generation',
    feed_type ENUM('home', 'following', 'explore', 'trending', 'personalized') NOT NULL COMMENT 'Type of feed to generate',
    priority ENUM('low', 'medium', 'high', 'urgent') DEFAULT 'medium' COMMENT 'Generation priority',
    trigger_reason ENUM('user_login', 'content_update', 'schedule', 'manual', 'algorithm_change') NOT NULL COMMENT 'What triggered this generation',
    algorithm_id CHAR(36) NULL COMMENT 'Specific algorithm to use',
    parameters JSON COMMENT 'Additional parameters for generation',
    status ENUM('pending', 'processing', 'completed', 'failed', 'cancelled') DEFAULT 'pending' COMMENT 'Queue entry status',
    attempts INT UNSIGNED DEFAULT 0 COMMENT 'Number of processing attempts',
    max_attempts INT UNSIGNED DEFAULT 3 COMMENT 'Maximum retry attempts',
    error_message TEXT COMMENT 'Error message if processing failed',
    assigned_worker VARCHAR(100) COMMENT 'Worker node assigned to process this',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Queue entry creation timestamp',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',
    started_at TIMESTAMP NULL COMMENT 'Processing start timestamp',
    completed_at TIMESTAMP NULL COMMENT 'Processing completion timestamp',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (algorithm_id) REFERENCES algorithm_configs(id) ON DELETE SET NULL,
    INDEX idx_user_feed_type (user_id, feed_type),
    INDEX idx_status (status),
    INDEX idx_priority (priority),
    INDEX idx_created_at (created_at),
    INDEX idx_trigger_reason (trigger_reason),
    INDEX idx_pending_jobs (status, priority DESC, created_at ASC),
    INDEX idx_worker_jobs (assigned_worker, status),
    INDEX idx_failed_retries (status, attempts, max_attempts)
) ENGINE=InnoDB COMMENT='Queue for asynchronous feed generation';

-- Feed performance metrics table
CREATE TABLE feed_performance_metrics (
    id CHAR(36) PRIMARY KEY COMMENT 'UUID as string',
    user_id CHAR(36) NOT NULL COMMENT 'User ID',
    feed_type ENUM('home', 'following', 'explore', 'trending', 'personalized') NOT NULL COMMENT 'Type of feed',
    metric_date DATE NOT NULL COMMENT 'Date for these metrics',
    algorithm_id CHAR(36) NULL COMMENT 'Algorithm used',
    total_impressions INT UNSIGNED DEFAULT 0 COMMENT 'Total content impressions',
    unique_impressions INT UNSIGNED DEFAULT 0 COMMENT 'Unique content pieces shown',
    total_clicks INT UNSIGNED DEFAULT 0 COMMENT 'Total clicks on content',
    engagement_actions INT UNSIGNED DEFAULT 0 COMMENT 'Total engagement actions (likes, shares, etc)',
    time_spent_seconds INT UNSIGNED DEFAULT 0 COMMENT 'Total time spent in feed',
    scroll_depth_avg DECIMAL(5,2) DEFAULT 0.00 COMMENT 'Average scroll depth percentage',
    bounce_rate DECIMAL(5,4) DEFAULT 0.0000 COMMENT 'Bounce rate (0-1)',
    return_rate DECIMAL(5,4) DEFAULT 0.0000 COMMENT 'Return rate within 24 hours (0-1)',
    satisfaction_score DECIMAL(3,2) COMMENT 'User satisfaction score (1-5) if available',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Metrics creation timestamp',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (algorithm_id) REFERENCES algorithm_configs(id) ON DELETE SET NULL,
    UNIQUE KEY unique_user_feed_date (user_id, feed_type, metric_date),
    INDEX idx_user_feed_type (user_id, feed_type),
    INDEX idx_metric_date (metric_date),
    INDEX idx_algorithm_id (algorithm_id),
    INDEX idx_engagement_actions (engagement_actions DESC),
    INDEX idx_satisfaction_score (satisfaction_score DESC),
    INDEX idx_return_rate (return_rate DESC)
) ENGINE=InnoDB COMMENT='Daily performance metrics for user feeds';