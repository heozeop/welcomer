-- V6__Add_Performance_Indexes_And_Constraints.sql
-- SNS Feed System - Additional Performance Indexes and Constraints
-- Database: MySQL 8.0

-- Additional composite indexes for common query patterns

-- User timeline queries (user's own content + followed users content)
ALTER TABLE content ADD INDEX idx_timeline_query (visibility, status, published_at DESC, author_id);

-- Content discovery queries with engagement metrics
ALTER TABLE content_stats ADD INDEX idx_trending_content (viral_score DESC, engagement_rate DESC, likes_count DESC);

-- User feed generation optimization
ALTER TABLE user_relationships ADD INDEX idx_active_following (follower_id, relationship_type, created_at DESC) 
    WHERE relationship_type = 'follow';

-- Content moderation workflow optimization
ALTER TABLE reports ADD INDEX idx_moderation_workflow (status, report_type, created_at ASC);

-- Media processing workflow
ALTER TABLE media_attachments ADD INDEX idx_processing_workflow (processing_status, created_at ASC);

-- User engagement analysis
ALTER TABLE reactions ADD INDEX idx_user_engagement_analysis (user_id, reaction_type, created_at DESC);
ALTER TABLE comments ADD INDEX idx_user_comment_activity (user_id, is_deleted, created_at DESC);

-- Content performance tracking
ALTER TABLE content_views ADD INDEX idx_content_performance (content_id, view_depth, created_at DESC);

-- Search optimization indexes
ALTER TABLE content ADD INDEX idx_content_search (status, visibility, language_code, created_at DESC);
ALTER TABLE users ADD INDEX idx_user_search (deleted_at, verified, created_at DESC);

-- A/B testing optimization
ALTER TABLE user_experiment_assignments ADD INDEX idx_experiment_analysis (experiment_id, variant_id, is_active, assigned_at);

-- Feed caching optimization
ALTER TABLE feed_entries ADD INDEX idx_feed_cache_lookup (user_id, feed_type, expires_at, rank_position ASC);

-- Algorithm performance tracking
ALTER TABLE feed_performance_metrics ADD INDEX idx_algorithm_performance (algorithm_id, metric_date DESC, satisfaction_score DESC);

-- Session management optimization
ALTER TABLE user_sessions ADD INDEX idx_active_sessions (user_id, is_active, expires_at);

-- Content recommendation system indexes
ALTER TABLE content ADD INDEX idx_recommendation_system (content_type, language_code, created_at DESC, author_id);
ALTER TABLE content_stats ADD INDEX idx_engagement_patterns (engagement_rate DESC, views_count DESC, updated_at DESC);

-- Social graph analysis
ALTER TABLE user_relationships ADD INDEX idx_social_graph (following_id, relationship_type, created_at DESC);

-- Trending analysis
ALTER TABLE trending_content ADD INDEX idx_trending_analysis (trend_category, time_window, location_code, trend_rank ASC);

-- Comment threading optimization  
ALTER TABLE comments ADD INDEX idx_comment_threading (parent_comment_id, created_at ASC) WHERE parent_comment_id IS NOT NULL;

-- Content tagging and discovery
ALTER TABLE content_tags ADD INDEX idx_tag_discovery (tag, tag_type, created_at DESC);

-- User mention notifications
ALTER TABLE content_mentions ADD INDEX idx_mention_notifications (mentioned_user_id, created_at DESC);

-- Bookmarks organization
ALTER TABLE bookmarks ADD INDEX idx_bookmark_collections (user_id, collection_name, created_at DESC);

-- Feed generation queue optimization
ALTER TABLE feed_generation_queue ADD INDEX idx_queue_processing (status, priority DESC, created_at ASC, assigned_worker);

-- Performance constraints and checks

-- Ensure realistic engagement rates (0-1)
ALTER TABLE content_stats ADD CONSTRAINT chk_engagement_rate 
    CHECK (engagement_rate >= 0.0000 AND engagement_rate <= 1.0000);

-- Ensure positive viral scores
ALTER TABLE content_stats ADD CONSTRAINT chk_viral_score 
    CHECK (viral_score >= 0.0000);

-- Ensure valid trend scores
ALTER TABLE trending_content ADD CONSTRAINT chk_trend_score 
    CHECK (trend_score >= 0.0000);

-- Ensure valid trend ranks (positive integers)
ALTER TABLE trending_content ADD CONSTRAINT chk_trend_rank 
    CHECK (trend_rank > 0);

-- Ensure feed scores are reasonable
ALTER TABLE feed_entries ADD CONSTRAINT chk_feed_score 
    CHECK (score >= 0.000000 AND score <= 1000.000000);

-- Ensure valid feed rank positions
ALTER TABLE feed_entries ADD CONSTRAINT chk_rank_position 
    CHECK (rank_position IS NULL OR rank_position > 0);

-- Ensure valid user stats (non-negative counts)
ALTER TABLE user_stats ADD CONSTRAINT chk_followers_count 
    CHECK (followers_count >= 0);
ALTER TABLE user_stats ADD CONSTRAINT chk_following_count 
    CHECK (following_count >= 0);
ALTER TABLE user_stats ADD CONSTRAINT chk_posts_count 
    CHECK (posts_count >= 0);

-- Ensure valid content stats (non-negative counts)
ALTER TABLE content_stats ADD CONSTRAINT chk_likes_count 
    CHECK (likes_count >= 0);
ALTER TABLE content_stats ADD CONSTRAINT chk_comments_count 
    CHECK (comments_count >= 0);
ALTER TABLE content_stats ADD CONSTRAINT chk_shares_count 
    CHECK (shares_count >= 0);
ALTER TABLE content_stats ADD CONSTRAINT chk_views_count 
    CHECK (views_count >= 0);

-- Ensure valid media file sizes
ALTER TABLE media_attachments ADD CONSTRAINT chk_file_size 
    CHECK (file_size IS NULL OR file_size >= 0);

-- Ensure valid media dimensions
ALTER TABLE media_attachments ADD CONSTRAINT chk_width 
    CHECK (width IS NULL OR width > 0);
ALTER TABLE media_attachments ADD CONSTRAINT chk_height 
    CHECK (height IS NULL OR height > 0);

-- Ensure valid duration for media
ALTER TABLE media_attachments ADD CONSTRAINT chk_duration 
    CHECK (duration IS NULL OR duration > 0);

-- Ensure valid allocation percentages for A/B tests
ALTER TABLE ab_experiment_variants ADD CONSTRAINT chk_allocation_percentage 
    CHECK (allocation_percentage >= 0.00 AND allocation_percentage <= 100.00);

-- Ensure valid confidence levels for A/B tests
ALTER TABLE ab_experiments ADD CONSTRAINT chk_confidence_level 
    CHECK (confidence_level >= 50.00 AND confidence_level <= 99.99);

-- Ensure valid target percentages for A/B tests
ALTER TABLE ab_experiments ADD CONSTRAINT chk_target_percentage 
    CHECK (target_percentage >= 0.00 AND target_percentage <= 100.00);

-- Ensure feed cache TTL is reasonable (1 minute to 24 hours)
ALTER TABLE feed_cache_configs ADD CONSTRAINT chk_cache_ttl 
    CHECK (cache_ttl_minutes >= 1 AND cache_ttl_minutes <= 1440);

-- Ensure reasonable max entries for feeds
ALTER TABLE feed_cache_configs ADD CONSTRAINT chk_max_entries 
    CHECK (max_entries >= 10 AND max_entries <= 1000);

-- Ensure reasonable batch sizes
ALTER TABLE feed_cache_configs ADD CONSTRAINT chk_batch_size 
    CHECK (batch_size >= 5 AND batch_size <= 100);

-- Performance monitoring views for common queries

-- Create view for active user feed status
CREATE VIEW v_active_user_feeds AS
SELECT 
    u.id as user_id,
    u.username,
    fm.feed_type,
    fm.last_generated_at,
    fm.content_count,
    fm.next_refresh_at,
    CASE 
        WHEN fm.next_refresh_at < NOW() THEN 'expired'
        WHEN fm.next_refresh_at < DATE_ADD(NOW(), INTERVAL 5 MINUTE) THEN 'expiring_soon'
        ELSE 'fresh'
    END as freshness_status
FROM users u
LEFT JOIN feed_metadata fm ON u.id = fm.user_id
WHERE u.deleted_at IS NULL;

-- Create view for content engagement summary
CREATE VIEW v_content_engagement_summary AS
SELECT 
    c.id as content_id,
    c.author_id,
    c.content_type,
    c.created_at,
    cs.likes_count,
    cs.comments_count,
    cs.shares_count,
    cs.views_count,
    cs.engagement_rate,
    cs.viral_score,
    CASE
        WHEN cs.viral_score > 100 THEN 'viral'
        WHEN cs.engagement_rate > 0.1 THEN 'high_engagement'
        WHEN cs.engagement_rate > 0.05 THEN 'medium_engagement'
        ELSE 'low_engagement'
    END as engagement_category
FROM content c
JOIN content_stats cs ON c.id = cs.content_id
WHERE c.status = 'published' AND c.deleted_at IS NULL;

-- Create view for user activity summary
CREATE VIEW v_user_activity_summary AS
SELECT 
    u.id as user_id,
    u.username,
    us.followers_count,
    us.following_count,
    us.posts_count,
    COUNT(DISTINCT r.id) as total_reactions_given,
    COUNT(DISTINCT cm.id) as total_comments_made,
    COUNT(DISTINCT s.id) as total_shares_made,
    u.created_at as joined_at,
    DATEDIFF(NOW(), u.created_at) as days_since_joining
FROM users u
LEFT JOIN user_stats us ON u.id = us.user_id
LEFT JOIN reactions r ON u.id = r.user_id
LEFT JOIN comments cm ON u.id = cm.user_id
LEFT JOIN shares s ON u.id = s.user_id
WHERE u.deleted_at IS NULL
GROUP BY u.id, u.username, us.followers_count, us.following_count, us.posts_count, u.created_at;