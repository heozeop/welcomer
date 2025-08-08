-- V1__Create_Users_And_Relationships_Tables.sql
-- SNS Feed System - User Profile and Social Graph Model
-- Database: MySQL 8.0

-- Users table for storing user profiles and authentication data
CREATE TABLE users (
    id CHAR(36) PRIMARY KEY COMMENT 'UUID as string',
    username VARCHAR(50) UNIQUE NOT NULL COMMENT 'Unique username for login and display',
    email VARCHAR(255) UNIQUE NOT NULL COMMENT 'User email address for authentication',
    password_hash VARCHAR(255) NOT NULL COMMENT 'Bcrypt hashed password',
    full_name VARCHAR(100) NOT NULL COMMENT 'User full display name',
    bio TEXT COMMENT 'User biography/description',
    profile_pic_url VARCHAR(500) COMMENT 'URL to profile picture',
    location VARCHAR(100) COMMENT 'User location/city',
    website VARCHAR(255) COMMENT 'Personal website URL',
    birthday DATE COMMENT 'User birthday',
    verified BOOLEAN DEFAULT FALSE COMMENT 'Account verification status',
    preferences JSON COMMENT 'User preferences as JSON document',
    privacy_settings JSON COMMENT 'Privacy settings as JSON document',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Account creation timestamp',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',
    deleted_at TIMESTAMP NULL COMMENT 'Soft delete timestamp',
    INDEX idx_username (username),
    INDEX idx_email (email),
    INDEX idx_created_at (created_at),
    INDEX idx_deleted_at (deleted_at),
    FULLTEXT INDEX ft_username_fullname (username, full_name)
) ENGINE=InnoDB COMMENT='User profiles and authentication data';

-- User relationships table for social graph (follows, blocks, etc.)
CREATE TABLE user_relationships (
    id CHAR(36) PRIMARY KEY COMMENT 'UUID as string',
    follower_id CHAR(36) NOT NULL COMMENT 'ID of user who is following',
    following_id CHAR(36) NOT NULL COMMENT 'ID of user being followed',
    relationship_type ENUM('follow', 'block', 'mute') NOT NULL DEFAULT 'follow' COMMENT 'Type of relationship',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Relationship creation timestamp',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',
    FOREIGN KEY (follower_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (following_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY unique_relationship (follower_id, following_id, relationship_type),
    INDEX idx_follower_id (follower_id),
    INDEX idx_following_id (following_id),
    INDEX idx_relationship_type (relationship_type),
    INDEX idx_created_at (created_at),
    -- Prevent users from following themselves
    CONSTRAINT chk_no_self_follow CHECK (follower_id != following_id)
) ENGINE=InnoDB COMMENT='User social relationships (follows, blocks, mutes)';

-- User statistics table for caching follower/following counts
CREATE TABLE user_stats (
    user_id CHAR(36) PRIMARY KEY COMMENT 'User ID',
    followers_count INT UNSIGNED DEFAULT 0 COMMENT 'Number of followers',
    following_count INT UNSIGNED DEFAULT 0 COMMENT 'Number of users being followed',
    posts_count INT UNSIGNED DEFAULT 0 COMMENT 'Number of posts created',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='Cached user statistics for performance';

-- User sessions table for managing active sessions
CREATE TABLE user_sessions (
    id CHAR(36) PRIMARY KEY COMMENT 'Session UUID',
    user_id CHAR(36) NOT NULL COMMENT 'User ID',
    device_info JSON COMMENT 'Device information as JSON',
    ip_address VARCHAR(45) COMMENT 'User IP address (supports IPv6)',
    user_agent TEXT COMMENT 'Browser/app user agent string',
    is_active BOOLEAN DEFAULT TRUE COMMENT 'Session active status',
    expires_at TIMESTAMP NOT NULL COMMENT 'Session expiration timestamp',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Session creation timestamp',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last activity timestamp',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_expires_at (expires_at),
    INDEX idx_is_active (is_active),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB COMMENT='User session management';