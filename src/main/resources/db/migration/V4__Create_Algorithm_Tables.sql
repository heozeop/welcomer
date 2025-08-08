-- V4__Create_Algorithm_Tables.sql
-- SNS Feed System - Algorithm Parameters Model
-- Database: MySQL 8.0

-- Algorithm configurations table for different feed algorithms
CREATE TABLE algorithm_configs (
    id CHAR(36) PRIMARY KEY COMMENT 'UUID as string',
    name VARCHAR(50) NOT NULL UNIQUE COMMENT 'Algorithm name identifier',
    display_name VARCHAR(100) NOT NULL COMMENT 'Human-readable algorithm name',
    description TEXT COMMENT 'Algorithm description and purpose',
    version VARCHAR(20) NOT NULL COMMENT 'Algorithm version (semantic versioning)',
    is_active BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Whether algorithm is currently active',
    is_default BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Whether this is the default algorithm',
    min_user_age_days INT UNSIGNED DEFAULT 0 COMMENT 'Minimum user account age to use this algorithm',
    target_audience ENUM('all', 'new_users', 'active_users', 'premium_users') DEFAULT 'all' COMMENT 'Target user audience',
    algorithm_type ENUM('chronological', 'engagement_based', 'ml_personalized', 'hybrid') NOT NULL COMMENT 'Type of algorithm',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Algorithm creation timestamp',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',
    deprecated_at TIMESTAMP NULL COMMENT 'Algorithm deprecation timestamp',
    INDEX idx_name (name),
    INDEX idx_is_active (is_active),
    INDEX idx_is_default (is_default),
    INDEX idx_algorithm_type (algorithm_type),
    INDEX idx_target_audience (target_audience),
    INDEX idx_created_at (created_at),
    -- Ensure only one default algorithm at a time
    UNIQUE KEY unique_default_algorithm (is_default, deprecated_at)
) ENGINE=InnoDB COMMENT='Feed algorithm configurations';

-- Algorithm parameters table for configurable algorithm settings
CREATE TABLE algorithm_parameters (
    id CHAR(36) PRIMARY KEY COMMENT 'UUID as string',
    algorithm_id CHAR(36) NOT NULL COMMENT 'Algorithm this parameter belongs to',
    parameter_name VARCHAR(50) NOT NULL COMMENT 'Parameter name/key',
    parameter_type ENUM('float', 'integer', 'boolean', 'string', 'json') NOT NULL COMMENT 'Data type of parameter',
    default_value TEXT NOT NULL COMMENT 'Default parameter value as string',
    min_value DECIMAL(10,4) NULL COMMENT 'Minimum allowed value for numeric parameters',
    max_value DECIMAL(10,4) NULL COMMENT 'Maximum allowed value for numeric parameters',
    allowed_values JSON NULL COMMENT 'Allowed values for enum-like parameters',
    description TEXT COMMENT 'Parameter description and usage',
    is_user_configurable BOOLEAN DEFAULT FALSE COMMENT 'Whether users can override this parameter',
    display_order INT UNSIGNED DEFAULT 0 COMMENT 'Order for displaying in UI',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Parameter creation timestamp',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',
    FOREIGN KEY (algorithm_id) REFERENCES algorithm_configs(id) ON DELETE CASCADE,
    UNIQUE KEY unique_algorithm_parameter (algorithm_id, parameter_name),
    INDEX idx_algorithm_id (algorithm_id),
    INDEX idx_parameter_name (parameter_name),
    INDEX idx_parameter_type (parameter_type),
    INDEX idx_is_user_configurable (is_user_configurable),
    INDEX idx_display_order (display_order)
) ENGINE=InnoDB COMMENT='Configurable parameters for algorithms';

-- User algorithm preferences for personalized settings
CREATE TABLE user_algorithm_preferences (
    user_id CHAR(36) NOT NULL COMMENT 'User ID',
    algorithm_id CHAR(36) NOT NULL COMMENT 'Algorithm ID',
    parameter_name VARCHAR(50) NOT NULL COMMENT 'Parameter name being overridden',
    custom_value TEXT NOT NULL COMMENT 'User custom value for this parameter',
    is_active BOOLEAN DEFAULT TRUE COMMENT 'Whether this override is active',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Preference creation timestamp',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',
    PRIMARY KEY (user_id, algorithm_id, parameter_name),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (algorithm_id) REFERENCES algorithm_configs(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_algorithm_id (algorithm_id),
    INDEX idx_is_active (is_active),
    INDEX idx_updated_at (updated_at)
) ENGINE=InnoDB COMMENT='User-specific algorithm parameter overrides';

-- A/B testing experiments table
CREATE TABLE ab_experiments (
    id CHAR(36) PRIMARY KEY COMMENT 'UUID as string',
    name VARCHAR(100) NOT NULL UNIQUE COMMENT 'Experiment name',
    description TEXT COMMENT 'Experiment description and goals',
    experiment_type ENUM('algorithm', 'ui', 'feature', 'content') NOT NULL COMMENT 'Type of experiment',
    status ENUM('draft', 'active', 'paused', 'completed', 'cancelled') DEFAULT 'draft' COMMENT 'Experiment status',
    start_date TIMESTAMP NULL COMMENT 'Experiment start date',
    end_date TIMESTAMP NULL COMMENT 'Experiment end date',
    target_percentage DECIMAL(5,2) DEFAULT 50.00 COMMENT 'Percentage of users to include (0-100)',
    success_metric VARCHAR(100) COMMENT 'Primary success metric to track',
    minimum_sample_size INT UNSIGNED DEFAULT 1000 COMMENT 'Minimum sample size for statistical significance',
    confidence_level DECIMAL(4,2) DEFAULT 95.00 COMMENT 'Statistical confidence level (0-100)',
    created_by CHAR(36) NOT NULL COMMENT 'User who created the experiment',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Experiment creation timestamp',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE RESTRICT,
    INDEX idx_name (name),
    INDEX idx_status (status),
    INDEX idx_experiment_type (experiment_type),
    INDEX idx_start_date (start_date),
    INDEX idx_end_date (end_date),
    INDEX idx_created_by (created_by),
    INDEX idx_active_experiments (status, start_date, end_date)
) ENGINE=InnoDB COMMENT='A/B testing experiment configurations';

-- A/B test variants table
CREATE TABLE ab_experiment_variants (
    id CHAR(36) PRIMARY KEY COMMENT 'UUID as string',
    experiment_id CHAR(36) NOT NULL COMMENT 'Experiment this variant belongs to',
    variant_name VARCHAR(50) NOT NULL COMMENT 'Variant name (control, treatment_a, etc.)',
    description TEXT COMMENT 'Variant description',
    allocation_percentage DECIMAL(5,2) NOT NULL COMMENT 'Percentage allocation for this variant (0-100)',
    algorithm_config JSON COMMENT 'Algorithm configuration for this variant',
    ui_config JSON COMMENT 'UI configuration changes for this variant',
    feature_flags JSON COMMENT 'Feature flags for this variant',
    is_control BOOLEAN DEFAULT FALSE COMMENT 'Whether this is the control variant',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Variant creation timestamp',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',
    FOREIGN KEY (experiment_id) REFERENCES ab_experiments(id) ON DELETE CASCADE,
    UNIQUE KEY unique_experiment_variant (experiment_id, variant_name),
    INDEX idx_experiment_id (experiment_id),
    INDEX idx_variant_name (variant_name),
    INDEX idx_is_control (is_control),
    INDEX idx_allocation_percentage (allocation_percentage)
) ENGINE=InnoDB COMMENT='Variants for A/B testing experiments';

-- User experiment assignments table
CREATE TABLE user_experiment_assignments (
    user_id CHAR(36) NOT NULL COMMENT 'User ID',
    experiment_id CHAR(36) NOT NULL COMMENT 'Experiment ID',
    variant_id CHAR(36) NOT NULL COMMENT 'Assigned variant ID',
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Assignment timestamp',
    is_active BOOLEAN DEFAULT TRUE COMMENT 'Whether assignment is active',
    exclusion_reason VARCHAR(100) COMMENT 'Reason for exclusion if any',
    PRIMARY KEY (user_id, experiment_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (experiment_id) REFERENCES ab_experiments(id) ON DELETE CASCADE,
    FOREIGN KEY (variant_id) REFERENCES ab_experiment_variants(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_experiment_id (experiment_id),
    INDEX idx_variant_id (variant_id),
    INDEX idx_assigned_at (assigned_at),
    INDEX idx_is_active (is_active),
    INDEX idx_active_assignments (user_id, is_active, assigned_at)
) ENGINE=InnoDB COMMENT='User assignments to A/B test variants';

-- Machine learning model configurations
CREATE TABLE ml_model_configs (
    id CHAR(36) PRIMARY KEY COMMENT 'UUID as string',
    model_name VARCHAR(100) NOT NULL UNIQUE COMMENT 'Model identifier name',
    model_type ENUM('recommendation', 'ranking', 'content_understanding', 'user_segmentation') NOT NULL COMMENT 'Type of ML model',
    model_version VARCHAR(20) NOT NULL COMMENT 'Model version',
    model_path VARCHAR(500) COMMENT 'Path to model file or endpoint',
    feature_schema JSON COMMENT 'Expected input feature schema',
    hyperparameters JSON COMMENT 'Model hyperparameters',
    performance_metrics JSON COMMENT 'Model performance metrics',
    training_data_info JSON COMMENT 'Information about training data',
    is_active BOOLEAN DEFAULT FALSE COMMENT 'Whether model is currently active',
    deployment_environment ENUM('development', 'staging', 'production') DEFAULT 'development' COMMENT 'Deployment environment',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Model creation timestamp',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',
    deployed_at TIMESTAMP NULL COMMENT 'Deployment timestamp',
    INDEX idx_model_name (model_name),
    INDEX idx_model_type (model_type),
    INDEX idx_is_active (is_active),
    INDEX idx_deployment_environment (deployment_environment),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB COMMENT='Machine learning model configurations';

-- Content scoring rules for feed ranking
CREATE TABLE content_scoring_rules (
    id CHAR(36) PRIMARY KEY COMMENT 'UUID as string',
    rule_name VARCHAR(100) NOT NULL COMMENT 'Rule identifier name',
    algorithm_id CHAR(36) NOT NULL COMMENT 'Algorithm this rule belongs to',
    rule_type ENUM('boost', 'penalty', 'filter', 'conditional') NOT NULL COMMENT 'Type of scoring rule',
    condition_expression TEXT NOT NULL COMMENT 'Rule condition as SQL-like expression',
    score_adjustment DECIMAL(8,4) NOT NULL COMMENT 'Score adjustment value',
    adjustment_type ENUM('additive', 'multiplicative', 'absolute') DEFAULT 'additive' COMMENT 'How to apply the adjustment',
    priority_order INT UNSIGNED DEFAULT 100 COMMENT 'Order of rule application',
    is_active BOOLEAN DEFAULT TRUE COMMENT 'Whether rule is currently active',
    applies_to_content_types SET('text', 'image', 'video', 'link', 'poll') COMMENT 'Content types this rule applies to',
    max_applications_per_user INT UNSIGNED COMMENT 'Maximum times this rule can apply per user per day',
    description TEXT COMMENT 'Rule description and purpose',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Rule creation timestamp',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',
    FOREIGN KEY (algorithm_id) REFERENCES algorithm_configs(id) ON DELETE CASCADE,
    INDEX idx_rule_name (rule_name),
    INDEX idx_algorithm_id (algorithm_id),
    INDEX idx_rule_type (rule_type),
    INDEX idx_is_active (is_active),
    INDEX idx_priority_order (priority_order),
    INDEX idx_algorithm_priority (algorithm_id, priority_order),
    FULLTEXT INDEX ft_condition_expression (condition_expression)
) ENGINE=InnoDB COMMENT='Content scoring rules for feed algorithms';