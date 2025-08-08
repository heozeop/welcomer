-- V3__Create_Engagement_Tables.sql
-- SNS Feed System - Engagement Model
-- Database: MySQL 8.0

-- Reactions table for likes, dislikes, and other reactions
CREATE TABLE reactions (
    id CHAR(36) PRIMARY KEY COMMENT 'UUID as string',
    user_id CHAR(36) NOT NULL COMMENT 'User who made the reaction',
    content_id CHAR(36) NOT NULL COMMENT 'Content being reacted to',
    reaction_type ENUM('like', 'dislike', 'love', 'laugh', 'angry', 'sad', 'wow') NOT NULL DEFAULT 'like' COMMENT 'Type of reaction',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Reaction timestamp',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (content_id) REFERENCES content(id) ON DELETE CASCADE,
    UNIQUE KEY unique_user_content_reaction (user_id, content_id),
    INDEX idx_user_id (user_id),
    INDEX idx_content_id (content_id),
    INDEX idx_reaction_type (reaction_type),
    INDEX idx_created_at (created_at),
    INDEX idx_content_reaction_type (content_id, reaction_type),
    INDEX idx_user_reactions (user_id, created_at DESC)
) ENGINE=InnoDB COMMENT='User reactions to content (likes, dislikes, etc.)';

-- Comments table for threaded discussions
CREATE TABLE comments (
    id CHAR(36) PRIMARY KEY COMMENT 'UUID as string',
    content_id CHAR(36) NOT NULL COMMENT 'Content being commented on',
    user_id CHAR(36) NOT NULL COMMENT 'User who made the comment',
    parent_comment_id CHAR(36) NULL COMMENT 'Parent comment ID for nested comments',
    text TEXT NOT NULL COMMENT 'Comment text content',
    is_edited BOOLEAN DEFAULT FALSE COMMENT 'Whether comment has been edited',
    is_pinned BOOLEAN DEFAULT FALSE COMMENT 'Whether comment is pinned by author',
    is_deleted BOOLEAN DEFAULT FALSE COMMENT 'Soft delete flag',
    language_code VARCHAR(10) COMMENT 'ISO language code of comment',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Comment creation timestamp',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',
    deleted_at TIMESTAMP NULL COMMENT 'Soft delete timestamp',
    FOREIGN KEY (content_id) REFERENCES content(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (parent_comment_id) REFERENCES comments(id) ON DELETE CASCADE,
    INDEX idx_content_id (content_id),
    INDEX idx_user_id (user_id),
    INDEX idx_parent_comment_id (parent_comment_id),
    INDEX idx_created_at (created_at),
    INDEX idx_is_deleted (is_deleted),
    INDEX idx_is_pinned (is_pinned),
    INDEX idx_content_created (content_id, created_at DESC),
    INDEX idx_parent_created (parent_comment_id, created_at ASC),
    FULLTEXT INDEX ft_comment_text (text)
) ENGINE=InnoDB COMMENT='Comments and replies on content';

-- Comment reactions table for reacting to comments
CREATE TABLE comment_reactions (
    id CHAR(36) PRIMARY KEY COMMENT 'UUID as string',
    user_id CHAR(36) NOT NULL COMMENT 'User who made the reaction',
    comment_id CHAR(36) NOT NULL COMMENT 'Comment being reacted to',
    reaction_type ENUM('like', 'dislike', 'love', 'laugh', 'angry', 'sad', 'wow') NOT NULL DEFAULT 'like' COMMENT 'Type of reaction',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Reaction timestamp',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (comment_id) REFERENCES comments(id) ON DELETE CASCADE,
    UNIQUE KEY unique_user_comment_reaction (user_id, comment_id),
    INDEX idx_user_id (user_id),
    INDEX idx_comment_id (comment_id),
    INDEX idx_reaction_type (reaction_type),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB COMMENT='User reactions to comments';

-- Shares/Retweets table
CREATE TABLE shares (
    id CHAR(36) PRIMARY KEY COMMENT 'UUID as string',
    user_id CHAR(36) NOT NULL COMMENT 'User who shared the content',
    content_id CHAR(36) NOT NULL COMMENT 'Content being shared',
    share_type ENUM('share', 'retweet', 'quote_share') NOT NULL DEFAULT 'share' COMMENT 'Type of share',
    quote_text TEXT COMMENT 'Quote text when quote_share type',
    visibility ENUM('public', 'followers', 'private') NOT NULL DEFAULT 'public' COMMENT 'Share visibility level',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Share timestamp',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (content_id) REFERENCES content(id) ON DELETE CASCADE,
    UNIQUE KEY unique_user_content_share (user_id, content_id),
    INDEX idx_user_id (user_id),
    INDEX idx_content_id (content_id),
    INDEX idx_share_type (share_type),
    INDEX idx_created_at (created_at),
    INDEX idx_visibility (visibility),
    INDEX idx_user_shares (user_id, created_at DESC),
    FULLTEXT INDEX ft_quote_text (quote_text)
) ENGINE=InnoDB COMMENT='Content shares and retweets';

-- Content views table for tracking impressions
CREATE TABLE content_views (
    id CHAR(36) PRIMARY KEY COMMENT 'UUID as string',
    user_id CHAR(36) NULL COMMENT 'User who viewed (NULL for anonymous)',
    content_id CHAR(36) NOT NULL COMMENT 'Content being viewed',
    session_id VARCHAR(128) COMMENT 'Session identifier for anonymous views',
    ip_address VARCHAR(45) COMMENT 'IP address of viewer',
    user_agent TEXT COMMENT 'Browser/app user agent',
    referrer_url VARCHAR(500) COMMENT 'Referrer URL',
    view_duration INT UNSIGNED COMMENT 'Time spent viewing in seconds',
    view_depth ENUM('impression', 'click', 'engaged', 'completed') DEFAULT 'impression' COMMENT 'Depth of engagement',
    device_type ENUM('mobile', 'tablet', 'desktop', 'other') COMMENT 'Device type used',
    location_country VARCHAR(2) COMMENT 'Country code from IP geolocation',
    location_city VARCHAR(100) COMMENT 'City from IP geolocation',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'View timestamp',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (content_id) REFERENCES content(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_content_id (content_id),
    INDEX idx_session_id (session_id),
    INDEX idx_created_at (created_at),
    INDEX idx_view_depth (view_depth),
    INDEX idx_device_type (device_type),
    INDEX idx_location_country (location_country),
    INDEX idx_content_views_daily (content_id, DATE(created_at)),
    INDEX idx_user_views (user_id, created_at DESC)
) ENGINE=InnoDB COMMENT='Content view tracking and analytics';

-- Bookmarks/Saves table
CREATE TABLE bookmarks (
    id CHAR(36) PRIMARY KEY COMMENT 'UUID as string',
    user_id CHAR(36) NOT NULL COMMENT 'User who bookmarked',
    content_id CHAR(36) NOT NULL COMMENT 'Content being bookmarked',
    collection_name VARCHAR(100) COMMENT 'Named collection for organizing bookmarks',
    notes TEXT COMMENT 'Private notes about the bookmark',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Bookmark creation timestamp',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (content_id) REFERENCES content(id) ON DELETE CASCADE,
    UNIQUE KEY unique_user_content_bookmark (user_id, content_id),
    INDEX idx_user_id (user_id),
    INDEX idx_content_id (content_id),
    INDEX idx_collection_name (collection_name),
    INDEX idx_created_at (created_at),
    INDEX idx_user_bookmarks (user_id, created_at DESC),
    FULLTEXT INDEX ft_notes (notes)
) ENGINE=InnoDB COMMENT='User bookmarks and saved content';

-- Reports table for content moderation
CREATE TABLE reports (
    id CHAR(36) PRIMARY KEY COMMENT 'UUID as string',
    reporter_user_id CHAR(36) NOT NULL COMMENT 'User who made the report',
    reported_content_id CHAR(36) NULL COMMENT 'Content being reported',
    reported_comment_id CHAR(36) NULL COMMENT 'Comment being reported',
    reported_user_id CHAR(36) NULL COMMENT 'User being reported',
    report_type ENUM('spam', 'harassment', 'hate_speech', 'violence', 'copyright', 'adult_content', 'misinformation', 'other') NOT NULL COMMENT 'Type of report',
    description TEXT COMMENT 'Detailed description of the issue',
    status ENUM('pending', 'reviewing', 'resolved', 'dismissed') DEFAULT 'pending' COMMENT 'Report processing status',
    moderator_id CHAR(36) NULL COMMENT 'Moderator who handled the report',
    moderator_notes TEXT COMMENT 'Internal moderator notes',
    resolution ENUM('no_action', 'content_removed', 'user_warned', 'user_suspended', 'user_banned') COMMENT 'Resolution taken',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Report creation timestamp',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',
    resolved_at TIMESTAMP NULL COMMENT 'Resolution timestamp',
    FOREIGN KEY (reporter_user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (reported_content_id) REFERENCES content(id) ON DELETE SET NULL,
    FOREIGN KEY (reported_comment_id) REFERENCES comments(id) ON DELETE SET NULL,
    FOREIGN KEY (reported_user_id) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (moderator_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_reporter_user_id (reporter_user_id),
    INDEX idx_reported_content_id (reported_content_id),
    INDEX idx_reported_comment_id (reported_comment_id),
    INDEX idx_reported_user_id (reported_user_id),
    INDEX idx_report_type (report_type),
    INDEX idx_status (status),
    INDEX idx_moderator_id (moderator_id),
    INDEX idx_created_at (created_at),
    INDEX idx_pending_reports (status, created_at ASC),
    FULLTEXT INDEX ft_description (description)
) ENGINE=InnoDB COMMENT='Content and user reports for moderation';

-- Comment statistics for caching engagement counts
CREATE TABLE comment_stats (
    comment_id CHAR(36) PRIMARY KEY COMMENT 'Comment ID',
    likes_count INT UNSIGNED DEFAULT 0 COMMENT 'Number of likes on comment',
    dislikes_count INT UNSIGNED DEFAULT 0 COMMENT 'Number of dislikes on comment',
    replies_count INT UNSIGNED DEFAULT 0 COMMENT 'Number of replies to comment',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',
    FOREIGN KEY (comment_id) REFERENCES comments(id) ON DELETE CASCADE,
    INDEX idx_likes_count (likes_count DESC),
    INDEX idx_replies_count (replies_count DESC),
    INDEX idx_updated_at (updated_at)
) ENGINE=InnoDB COMMENT='Cached comment engagement statistics';