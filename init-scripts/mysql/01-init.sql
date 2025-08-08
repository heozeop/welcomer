-- MySQL initialization script for Welcomer SNS Feed System
-- This script creates additional users and sets up initial configuration

-- Create application user with appropriate permissions
CREATE USER IF NOT EXISTS 'welcomer_app'@'%' IDENTIFIED BY 'apppassword';
GRANT SELECT, INSERT, UPDATE, DELETE ON welcomer_db.* TO 'welcomer_app'@'%';

-- Create read-only user for reporting/analytics
CREATE USER IF NOT EXISTS 'welcomer_readonly'@'%' IDENTIFIED BY 'readonlypassword';
GRANT SELECT ON welcomer_db.* TO 'welcomer_readonly'@'%';

-- Flush privileges to apply changes
FLUSH PRIVILEGES;

-- Set MySQL configuration for better performance in development
SET GLOBAL innodb_buffer_pool_size = 134217728; -- 128MB
SET GLOBAL max_connections = 200;
SET GLOBAL query_cache_type = ON;
SET GLOBAL query_cache_size = 16777216; -- 16MB

-- Enable general log for debugging (optional in development)
-- SET GLOBAL general_log = ON;
-- SET GLOBAL general_log_file = '/var/lib/mysql/mysql-general.log';

SELECT 'MySQL initialization completed successfully' as status;