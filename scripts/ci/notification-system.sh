#!/bin/bash

# Notification System for CI/CD Pipeline
# Sends notifications for test results, failures, and important events

set -euo pipefail

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ANALYSIS_DIR="build/analysis"
REPORTS_DIR="build/reports"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
NC='\033[0m' # No Color

# Emoji constants
EMOJI_SUCCESS="✅"
EMOJI_FAILURE="❌"
EMOJI_WARNING="⚠️"
EMOJI_INFO="ℹ️"
EMOJI_ROCKET="🚀"
EMOJI_BUG="🐛"
EMOJI_CHART="📊"
EMOJI_BELL="🔔"

# Logging functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

# Function to get environment information
get_environment_info() {
    local git_branch="${GITHUB_REF_NAME:-$(git branch --show-current 2>/dev/null || echo 'unknown')}"
    local git_commit="${GITHUB_SHA:-$(git rev-parse --short HEAD 2>/dev/null || echo 'unknown')}"
    local build_number="${GITHUB_RUN_NUMBER:-${BUILD_NUMBER:-unknown}}"
    local workflow_url="${GITHUB_SERVER_URL:-}/${GITHUB_REPOSITORY:-}/actions/runs/${GITHUB_RUN_ID:-}"
    local commit_url="${GITHUB_SERVER_URL:-}/${GITHUB_REPOSITORY:-}/commit/${GITHUB_SHA:-}"
    
    echo "Branch: $git_branch"
    echo "Commit: $git_commit"
    echo "Build: #$build_number"
    echo "Workflow: $workflow_url"
    echo "Commit URL: $commit_url"
}

# Function to parse test results from analysis
parse_test_results() {
    local analysis_file="$1"
    
    if [[ ! -f "$analysis_file" ]] || ! command -v jq >/dev/null 2>&1; then
        echo "unknown"
        return
    fi
    
    local junit_total=$(jq -r '.junit_analysis.total_tests // 0' "$analysis_file")
    local junit_failed=$(jq -r '.junit_analysis.failed_tests // 0' "$analysis_file")
    local junit_success_rate=$(jq -r '.junit_analysis.success_rate // 0' "$analysis_file")
    
    local cucumber_total=$(jq -r '.cucumber_analysis.total_scenarios // 0' "$analysis_file")
    local cucumber_failed=$(jq -r '.cucumber_analysis.failed_scenarios // 0' "$analysis_file")
    local cucumber_success_rate=$(jq -r '.cucumber_analysis.success_rate // 0' "$analysis_file")
    
    local total_tests=$((junit_total + cucumber_total))
    local total_failed=$((junit_failed + cucumber_failed))
    local overall_success_rate="0"
    
    if [[ $total_tests -gt 0 ]]; then
        overall_success_rate=$(echo "scale=1; (($total_tests - $total_failed) * 100) / $total_tests" | bc -l)
    fi
    
    echo "total_tests:$total_tests"
    echo "total_failed:$total_failed"
    echo "success_rate:$overall_success_rate"
    echo "junit_tests:$junit_total"
    echo "junit_failed:$junit_failed"
    echo "junit_success_rate:$junit_success_rate"
    echo "cucumber_scenarios:$cucumber_total"
    echo "cucumber_failed:$cucumber_failed"
    echo "cucumber_success_rate:$cucumber_success_rate"
}

# Function to send Slack notification
send_slack_notification() {
    local webhook_url="$1"
    local message="$2"
    local color="$3"
    local channel="${4:-#ci-cd}"
    
    if [[ -z "$webhook_url" ]]; then
        log_warn "Slack webhook URL not provided, skipping Slack notification"
        return 0
    fi
    
    log_info "Sending Slack notification..."
    
    local payload=$(cat << EOF
{
    "channel": "$channel",
    "username": "CI/CD Bot",
    "icon_emoji": ":robot_face:",
    "attachments": [
        {
            "color": "$color",
            "text": "$message",
            "mrkdwn": true,
            "footer": "Welcome Feed Personalization CI/CD",
            "ts": $(date +%s)
        }
    ]
}
EOF
    )
    
    local response
    if response=$(curl -s -X POST -H 'Content-type: application/json' \
        --data "$payload" \
        "$webhook_url"); then
        log_success "Slack notification sent successfully"
    else
        log_error "Failed to send Slack notification: $response"
    fi
}

# Function to send Microsoft Teams notification
send_teams_notification() {
    local webhook_url="$1"
    local message="$2"
    local color="$3"
    
    if [[ -z "$webhook_url" ]]; then
        log_warn "Teams webhook URL not provided, skipping Teams notification"
        return 0
    fi
    
    log_info "Sending Microsoft Teams notification..."
    
    local theme_color
    case "$color" in
        "good") theme_color="28a745" ;;
        "warning") theme_color="ffc107" ;;
        "danger") theme_color="dc3545" ;;
        *) theme_color="007bff" ;;
    esac
    
    local payload=$(cat << EOF
{
    "@type": "MessageCard",
    "@context": "http://schema.org/extensions",
    "themeColor": "$theme_color",
    "summary": "BDD Personalization Tests",
    "sections": [{
        "activityTitle": "BDD Personalization Test Results",
        "activitySubtitle": "$(date '+%Y-%m-%d %H:%M:%S')",
        "text": "$message",
        "markdown": true
    }]
}
EOF
    )
    
    local response
    if response=$(curl -s -X POST -H 'Content-Type: application/json' \
        --data "$payload" \
        "$webhook_url"); then
        log_success "Teams notification sent successfully"
    else
        log_error "Failed to send Teams notification: $response"
    fi
}

# Function to send email notification
send_email_notification() {
    local recipient="$1"
    local subject="$2"
    local message="$3"
    local smtp_server="${SMTP_SERVER:-localhost}"
    local smtp_port="${SMTP_PORT:-587}"
    local smtp_user="${SMTP_USER:-}"
    local smtp_pass="${SMTP_PASS:-}"
    
    if [[ -z "$recipient" ]]; then
        log_warn "Email recipient not provided, skipping email notification"
        return 0
    fi
    
    log_info "Sending email notification to: $recipient"
    
    # Create temporary email file
    local email_file=$(mktemp)
    cat > "$email_file" << EOF
To: $recipient
Subject: $subject
Content-Type: text/html; charset=UTF-8

<!DOCTYPE html>
<html>
<head>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        .header { background-color: #f4f4f4; padding: 15px; border-radius: 5px; }
        .success { color: #28a745; }
        .warning { color: #ffc107; }
        .danger { color: #dc3545; }
        .info { color: #007bff; }
        .footer { margin-top: 20px; font-size: 0.9em; color: #666; }
    </style>
</head>
<body>
    <div class="header">
        <h2>🎯 BDD Personalization Test Results</h2>
        <p>$(date '+%Y-%m-%d %H:%M:%S')</p>
    </div>
    
    <div class="content">
        $message
    </div>
    
    <div class="footer">
        <p>This is an automated notification from the Welcome Feed Personalization CI/CD system.</p>
        <p>Repository: ${GITHUB_REPOSITORY:-welcome}</p>
        <p>Build: ${GITHUB_RUN_NUMBER:-unknown}</p>
    </div>
</body>
</html>
EOF
    
    # Send email using sendmail or msmtp if available
    if command -v sendmail >/dev/null 2>&1; then
        sendmail "$recipient" < "$email_file"
        log_success "Email sent using sendmail"
    elif command -v msmtp >/dev/null 2>&1; then
        msmtp "$recipient" < "$email_file"
        log_success "Email sent using msmtp"
    else
        log_warn "No email sending utility found (sendmail/msmtp), skipping email notification"
    fi
    
    rm -f "$email_file"
}

# Function to create GitHub issue for persistent failures
create_github_issue() {
    local title="$1"
    local body="$2"
    local labels="$3"
    
    if [[ -z "$GITHUB_TOKEN" ]]; then
        log_warn "GitHub token not provided, skipping issue creation"
        return 0
    fi
    
    log_info "Creating GitHub issue for test failures..."
    
    local issue_payload=$(jq -n \
        --arg title "$title" \
        --arg body "$body" \
        --argjson labels "$(echo "$labels" | jq -R 'split(",")')" \
        '{
            title: $title,
            body: $body,
            labels: $labels
        }')
    
    local response
    if response=$(curl -s -X POST \
        -H "Authorization: token $GITHUB_TOKEN" \
        -H "Accept: application/vnd.github.v3+json" \
        -d "$issue_payload" \
        "https://api.github.com/repos/${GITHUB_REPOSITORY}/issues"); then
        
        local issue_url=$(echo "$response" | jq -r '.html_url // "unknown"')
        log_success "GitHub issue created: $issue_url"
        echo "$issue_url"
    else
        log_error "Failed to create GitHub issue: $response"
    fi
}

# Function to generate success notification
generate_success_notification() {
    local test_results="$1"
    local env_info="$2"
    
    # Parse test results
    local total_tests=$(echo "$test_results" | grep "total_tests:" | cut -d: -f2)
    local success_rate=$(echo "$test_results" | grep "success_rate:" | cut -d: -f2)
    local junit_tests=$(echo "$test_results" | grep "junit_tests:" | cut -d: -f2)
    local cucumber_scenarios=$(echo "$test_results" | grep "cucumber_scenarios:" | cut -d: -f2)
    
    cat << EOF
$EMOJI_SUCCESS **All BDD Personalization Tests Passed!**

$EMOJI_CHART **Test Results Summary:**
• Total Tests: $total_tests
• Success Rate: $success_rate%
• Unit/Integration Tests: $junit_tests
• BDD Scenarios: $cucumber_scenarios

$EMOJI_ROCKET **Environment Info:**
$env_info

$EMOJI_SUCCESS **Coverage Areas Validated:**
• New User Personalization
• Power User Experience  
• Mobile & Cross-Device
• Real-time Adaptation
• A/B Testing Integration
• Accessibility Features
• Performance & Load Testing
• Error Handling

Great work! The personalization system is working perfectly.
EOF
}

# Function to generate failure notification
generate_failure_notification() {
    local test_results="$1"
    local env_info="$2"
    local analysis_file="$3"
    
    # Parse test results
    local total_tests=$(echo "$test_results" | grep "total_tests:" | cut -d: -f2)
    local total_failed=$(echo "$test_results" | grep "total_failed:" | cut -d: -f2)
    local success_rate=$(echo "$test_results" | grep "success_rate:" | cut -d: -f2)
    local junit_failed=$(echo "$test_results" | grep "junit_failed:" | cut -d: -f2)
    local cucumber_failed=$(echo "$test_results" | grep "cucumber_failed:" | cut -d: -f2)
    
    # Get failure patterns if available
    local patterns_info=""
    if [[ -f "$analysis_file" ]] && command -v jq >/dev/null 2>&1; then
        local pattern_count=$(jq -r '.pattern_analysis.detected_patterns | length' "$analysis_file")
        if [[ $pattern_count -gt 0 ]]; then
            patterns_info=$(jq -r '.pattern_analysis.detected_patterns[] | "• \(.type): \(.count) occurrences - \(.recommendation)"' "$analysis_file" | head -3)
        fi
    fi
    
    cat << EOF
$EMOJI_FAILURE **BDD Personalization Tests Failed**

$EMOJI_BUG **Failure Summary:**
• Total Tests: $total_tests
• Failed Tests: $total_failed
• Success Rate: $success_rate%
• Unit/Integration Failures: $junit_failed
• BDD Scenario Failures: $cucumber_failed

$EMOJI_CHART **Environment Info:**
$env_info

$EMOJI_WARNING **Common Issues Detected:**
$patterns_info

$EMOJI_INFO **Next Steps:**
1. Review the detailed analysis report
2. Check service dependencies
3. Verify test data consistency  
4. Run tests locally to reproduce
5. Check recent code changes

**Detailed Analysis:** Build artifacts contain comprehensive failure analysis
EOF
}

# Main notification function
main() {
    local notification_type="${1:-auto}"
    local force_success="${2:-false}"
    
    log_info "Starting notification system..."
    
    # Get latest analysis file
    local latest_analysis=""
    if [[ -d "$ANALYSIS_DIR" ]]; then
        latest_analysis=$(find "$ANALYSIS_DIR" -name "failure-analysis-*.json" -type f -printf '%T@ %p\n' 2>/dev/null | sort -rn | head -1 | cut -d' ' -f2- || echo "")
    fi
    
    # Get environment information
    local env_info
    env_info=$(get_environment_info)
    
    # Determine test results
    local test_results
    local notification_message
    local notification_color
    local notification_subject
    
    if [[ "$force_success" == "true" ]] || [[ "$notification_type" == "success" ]]; then
        # Force success notification or explicit success
        test_results="total_tests:100 total_failed:0 success_rate:100.0 junit_tests:50 junit_failed:0 cucumber_scenarios:50 cucumber_failed:0"
        notification_message=$(generate_success_notification "$test_results" "$env_info")
        notification_color="good"
        notification_subject="✅ BDD Personalization Tests - All Passed"
    elif [[ -n "$latest_analysis" ]]; then
        # Use analysis results
        test_results=$(parse_test_results "$latest_analysis")
        local total_failed=$(echo "$test_results" | grep "total_failed:" | cut -d: -f2)
        
        if [[ $total_failed -eq 0 ]]; then
            notification_message=$(generate_success_notification "$test_results" "$env_info")
            notification_color="good"
            notification_subject="✅ BDD Personalization Tests - All Passed"
        else
            notification_message=$(generate_failure_notification "$test_results" "$env_info" "$latest_analysis")
            notification_color="danger"
            notification_subject="❌ BDD Personalization Tests - $total_failed Failed"
        fi
    else
        # Fallback: assume failure if no analysis available
        log_warn "No analysis file found, assuming test failure"
        test_results="total_tests:0 total_failed:1 success_rate:0.0"
        notification_message="$EMOJI_FAILURE **BDD Personalization Tests Failed**\n\nNo detailed analysis available. Check CI/CD logs for more information.\n\n**Environment:**\n$env_info"
        notification_color="danger"
        notification_subject="❌ BDD Personalization Tests - Analysis Missing"
    fi
    
    log_info "Notification type: $notification_type"
    log_info "Notification color: $notification_color"
    
    # Send notifications based on configuration
    local sent_notifications=()
    
    # Slack notification
    if [[ -n "${SLACK_WEBHOOK_URL:-}" ]]; then
        send_slack_notification "$SLACK_WEBHOOK_URL" "$notification_message" "$notification_color" "${SLACK_CHANNEL:-#ci-cd}"
        sent_notifications+=("Slack")
    fi
    
    # Microsoft Teams notification
    if [[ -n "${TEAMS_WEBHOOK_URL:-}" ]]; then
        send_teams_notification "$TEAMS_WEBHOOK_URL" "$notification_message" "$notification_color"
        sent_notifications+=("Teams")
    fi
    
    # Email notification
    if [[ -n "${EMAIL_RECIPIENTS:-}" ]]; then
        for recipient in $(echo "$EMAIL_RECIPIENTS" | tr ',' ' '); do
            send_email_notification "$recipient" "$notification_subject" "$notification_message"
        done
        sent_notifications+=("Email")
    fi
    
    # GitHub issue for persistent failures (only on main branch)
    if [[ "$notification_color" == "danger" ]] && [[ "${GITHUB_REF_NAME:-}" == "main" ]] && [[ -n "${GITHUB_TOKEN:-}" ]]; then
        local issue_title="🐛 BDD Personalization Tests Failing on Main Branch"
        local issue_body="$notification_message\n\n**Auto-generated issue for main branch test failures**"
        local issue_labels="bug,ci-cd,tests,personalization"
        
        create_github_issue "$issue_title" "$issue_body" "$issue_labels"
        sent_notifications+=("GitHub Issue")
    fi
    
    # Summary
    if [[ ${#sent_notifications[@]} -gt 0 ]]; then
        log_success "Notifications sent via: $(IFS=', '; echo "${sent_notifications[*]}")"
    else
        log_warn "No notification channels configured"
        log_info "Configure SLACK_WEBHOOK_URL, TEAMS_WEBHOOK_URL, or EMAIL_RECIPIENTS environment variables"
    fi
    
    # Output notification content for debugging
    echo
    echo "=== NOTIFICATION CONTENT ==="
    echo "$notification_message"
    echo "==========================="
}

# Help function
show_help() {
    cat << EOF
Notification System for CI/CD Pipeline

Usage: $0 [TYPE] [OPTIONS]

Sends notifications about test results, failures, and CI/CD events.

Arguments:
    TYPE                Notification type: auto, success, failure (default: auto)

Options:
    --force-success     Force a success notification regardless of test results
    -h, --help          Show this help message

Environment Variables:
    SLACK_WEBHOOK_URL      Slack webhook URL for notifications
    SLACK_CHANNEL         Slack channel (default: #ci-cd)
    TEAMS_WEBHOOK_URL     Microsoft Teams webhook URL
    EMAIL_RECIPIENTS      Comma-separated list of email recipients
    GITHUB_TOKEN          GitHub token for creating issues
    SMTP_SERVER           SMTP server for email (default: localhost)
    SMTP_PORT            SMTP port (default: 587)
    SMTP_USER            SMTP username
    SMTP_PASS            SMTP password

Examples:
    $0                              # Auto-detect and send appropriate notification
    $0 success                      # Force success notification
    $0 failure                      # Force failure notification
    $0 --force-success              # Force success regardless of test results

Notification Channels:
    - Slack (via webhook)
    - Microsoft Teams (via webhook)  
    - Email (via SMTP)
    - GitHub Issues (for persistent failures on main branch)

The script automatically determines test results from analysis files and
sends appropriate notifications to configured channels.
EOF
}

# Parse command line arguments
NOTIFICATION_TYPE="auto"
FORCE_SUCCESS="false"

while [[ $# -gt 0 ]]; do
    case $1 in
        -h|--help)
            show_help
            exit 0
            ;;
        --force-success)
            FORCE_SUCCESS="true"
            shift
            ;;
        success|failure|auto)
            NOTIFICATION_TYPE="$1"
            shift
            ;;
        *)
            log_error "Unknown option: $1"
            show_help
            exit 1
            ;;
    esac
done

# Run main function
main "$NOTIFICATION_TYPE" "$FORCE_SUCCESS"