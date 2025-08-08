-- V2__Create_Content_And_Media_Tables.sql
-- SNS Feed System - Content and Media Model
-- Database: MySQL 8.0

-- Content table for storing posts and their metadata
CREATE TABLE content (
    id CHAR(36) PRIMARY KEY COMMENT 'UUID as string',
    author_id CHAR(36) NOT NULL COMMENT 'User ID of content author',
    content_type ENUM('text', 'image', 'video', 'link', 'poll') NOT NULL COMMENT 'Type of content',
    text_content TEXT COMMENT 'Main text content of the post',
    link_url VARCHAR(2048) COMMENT 'URL for link posts',
    link_title VARCHAR(255) COMMENT 'Title of linked content',
    link_description TEXT COMMENT 'Description of linked content',
    link_image_url VARCHAR(500) COMMENT 'Preview image URL for links',
    poll_data JSON COMMENT 'Poll configuration and options as JSON',
    visibility ENUM('public', 'followers', 'private', 'unlisted') NOT NULL DEFAULT 'public' COMMENT 'Content visibility level',
    status ENUM('draft', 'published', 'archived', 'deleted') NOT NULL DEFAULT 'published' COMMENT 'Content publication status',
    reply_to_id CHAR(36) NULL COMMENT 'ID of parent post if this is a reply',
    reply_count INT UNSIGNED DEFAULT 0 COMMENT 'Number of replies to this post',
    is_sensitive BOOLEAN DEFAULT FALSE COMMENT 'Content contains sensitive material',
    language_code VARCHAR(10) COMMENT 'ISO language code of content',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Content creation timestamp',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',
    published_at TIMESTAMP NULL COMMENT 'Publication timestamp',
    deleted_at TIMESTAMP NULL COMMENT 'Soft delete timestamp',
    FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (reply_to_id) REFERENCES content(id) ON DELETE SET NULL,
    INDEX idx_author_id (author_id),
    INDEX idx_content_type (content_type),
    INDEX idx_status (status),
    INDEX idx_visibility (visibility),
    INDEX idx_created_at (created_at),
    INDEX idx_published_at (published_at),
    INDEX idx_reply_to_id (reply_to_id),
    INDEX idx_deleted_at (deleted_at),
    INDEX idx_author_published (author_id, published_at DESC),
    INDEX idx_public_published (visibility, status, published_at DESC),
    FULLTEXT INDEX ft_text_content (text_content),
    FULLTEXT INDEX ft_link_content (link_title, link_description)
) ENGINE=InnoDB COMMENT='Content posts and their metadata';

-- Media attachments table for images, videos, etc.
CREATE TABLE media_attachments (
    id CHAR(36) PRIMARY KEY COMMENT 'UUID as string',
    content_id CHAR(36) NOT NULL COMMENT 'Content ID this media belongs to',
    media_type ENUM('image', 'video', 'audio', 'document') NOT NULL COMMENT 'Type of media file',
    original_filename VARCHAR(255) COMMENT 'Original filename when uploaded',
    file_url VARCHAR(500) NOT NULL COMMENT 'URL to the media file',
    thumbnail_url VARCHAR(500) COMMENT 'URL to thumbnail/preview image',
    file_size BIGINT UNSIGNED COMMENT 'File size in bytes',
    mime_type VARCHAR(100) COMMENT 'MIME type of the file',
    width INT UNSIGNED COMMENT 'Image/video width in pixels',
    height INT UNSIGNED COMMENT 'Image/video height in pixels',
    duration INT UNSIGNED COMMENT 'Video/audio duration in seconds',
    alt_text VARCHAR(500) COMMENT 'Alternative text for accessibility',
    processing_status ENUM('pending', 'processing', 'completed', 'failed') DEFAULT 'pending' COMMENT 'Media processing status',
    metadata JSON COMMENT 'Additional media metadata as JSON',
    display_order TINYINT UNSIGNED DEFAULT 0 COMMENT 'Order for displaying multiple attachments',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Upload timestamp',
    processed_at TIMESTAMP NULL COMMENT 'Processing completion timestamp',
    FOREIGN KEY (content_id) REFERENCES content(id) ON DELETE CASCADE,
    INDEX idx_content_id (content_id),
    INDEX idx_media_type (media_type),
    INDEX idx_processing_status (processing_status),
    INDEX idx_display_order (content_id, display_order),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB COMMENT='Media attachments for content';

-- Content tags table for hashtags and topics
CREATE TABLE content_tags (
    content_id CHAR(36) NOT NULL COMMENT 'Content ID',
    tag VARCHAR(100) NOT NULL COMMENT 'Tag name (without # symbol)',
    tag_type ENUM('hashtag', 'topic', 'category') DEFAULT 'hashtag' COMMENT 'Type of tag',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Tag creation timestamp',
    PRIMARY KEY (content_id, tag, tag_type),
    FOREIGN KEY (content_id) REFERENCES content(id) ON DELETE CASCADE,
    INDEX idx_tag (tag),
    INDEX idx_tag_type (tag_type),
    INDEX idx_created_at (created_at),
    FULLTEXT INDEX ft_tag (tag)
) ENGINE=InnoDB COMMENT='Tags and hashtags for content discovery';

-- Content mentions table for @mentions
CREATE TABLE content_mentions (
    content_id CHAR(36) NOT NULL COMMENT 'Content ID',
    mentioned_user_id CHAR(36) NOT NULL COMMENT 'ID of mentioned user',
    mention_start INT UNSIGNED COMMENT 'Start position of mention in text',
    mention_length INT UNSIGNED COMMENT 'Length of mention text',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Mention creation timestamp',
    PRIMARY KEY (content_id, mentioned_user_id),
    FOREIGN KEY (content_id) REFERENCES content(id) ON DELETE CASCADE,
    FOREIGN KEY (mentioned_user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_mentioned_user_id (mentioned_user_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB COMMENT='User mentions in content';

-- Content statistics table for caching engagement counts
CREATE TABLE content_stats (
    content_id CHAR(36) PRIMARY KEY COMMENT 'Content ID',
    likes_count INT UNSIGNED DEFAULT 0 COMMENT 'Number of likes',
    dislikes_count INT UNSIGNED DEFAULT 0 COMMENT 'Number of dislikes',
    comments_count INT UNSIGNED DEFAULT 0 COMMENT 'Number of comments',
    shares_count INT UNSIGNED DEFAULT 0 COMMENT 'Number of shares',
    views_count INT UNSIGNED DEFAULT 0 COMMENT 'Number of views',
    unique_views_count INT UNSIGNED DEFAULT 0 COMMENT 'Number of unique views',
    reach_count INT UNSIGNED DEFAULT 0 COMMENT 'Number of users reached',
    engagement_rate DECIMAL(5,4) DEFAULT 0.0000 COMMENT 'Engagement rate (0-1)',
    viral_score DECIMAL(8,4) DEFAULT 0.0000 COMMENT 'Calculated viral score',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',
    FOREIGN KEY (content_id) REFERENCES content(id) ON DELETE CASCADE,
    INDEX idx_likes_count (likes_count DESC),
    INDEX idx_engagement_rate (engagement_rate DESC),
    INDEX idx_viral_score (viral_score DESC),
    INDEX idx_views_count (views_count DESC),
    INDEX idx_updated_at (updated_at)
) ENGINE=InnoDB COMMENT='Cached content engagement statistics';

-- Trending content table for popular/trending posts
CREATE TABLE trending_content (
    content_id CHAR(36) NOT NULL COMMENT 'Content ID',
    trend_category ENUM('general', 'images', 'videos', 'links', 'location') NOT NULL COMMENT 'Trending category',
    trend_score DECIMAL(10,4) NOT NULL COMMENT 'Calculated trending score',
    trend_rank INT UNSIGNED NOT NULL COMMENT 'Rank within category',
    time_window ENUM('hour', 'day', 'week') NOT NULL COMMENT 'Time window for trending calculation',
    location_code VARCHAR(10) COMMENT 'Location code for location-based trending',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Trending entry timestamp',
    expires_at TIMESTAMP NOT NULL COMMENT 'When this trending entry expires',
    PRIMARY KEY (content_id, trend_category, time_window, COALESCE(location_code, '')),
    FOREIGN KEY (content_id) REFERENCES content(id) ON DELETE CASCADE,
    INDEX idx_trend_category_rank (trend_category, trend_rank),
    INDEX idx_trend_score (trend_score DESC),
    INDEX idx_expires_at (expires_at),
    INDEX idx_location_trending (location_code, trend_category, trend_rank)
) ENGINE=InnoDB COMMENT='Trending content tracking';