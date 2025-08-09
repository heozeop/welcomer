#!/bin/bash

# Test Failure Analysis Script for CI/CD Pipeline
# Analyzes test failures and provides detailed insights for debugging

set -euo pipefail

# Configuration
REPORTS_DIR="build/reports"
ANALYSIS_DIR="build/analysis"
TIMESTAMP=$(date '+%Y-%m-%d_%H-%M-%S')
ANALYSIS_FILE="$ANALYSIS_DIR/failure-analysis-$TIMESTAMP.json"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

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

# Create analysis directory
mkdir -p "$ANALYSIS_DIR"

# Function to analyze JUnit XML reports
analyze_junit_reports() {
    log_info "Analyzing JUnit test reports..."
    
    local junit_failures=()
    local total_tests=0
    local failed_tests=0
    
    if [[ -d "$REPORTS_DIR/test-results" ]]; then
        while IFS= read -r -d '' xml_file; do
            if [[ -f "$xml_file" ]]; then
                # Extract test information using grep and sed
                local test_suite=$(grep -o 'name="[^"]*"' "$xml_file" | head -1 | sed 's/name="//;s/"//')
                local tests=$(grep -o 'tests="[0-9]*"' "$xml_file" | sed 's/tests="//;s/"//' || echo "0")
                local failures=$(grep -o 'failures="[0-9]*"' "$xml_file" | sed 's/failures="//;s/"//' || echo "0")
                local errors=$(grep -o 'errors="[0-9]*"' "$xml_file" | sed 's/errors="//;s/"//' || echo "0")
                local time=$(grep -o 'time="[0-9.]*"' "$xml_file" | sed 's/time="//;s/"//' || echo "0")
                
                total_tests=$((total_tests + tests))
                failed_tests=$((failed_tests + failures + errors))
                
                if [[ $((failures + errors)) -gt 0 ]]; then
                    # Extract failure messages
                    local failure_info=$(grep -A 10 '<failure\|<error' "$xml_file" | head -20 || echo "No detailed failure information available")
                    
                    junit_failures+=("{
                        \"test_suite\": \"$test_suite\",
                        \"total_tests\": $tests,
                        \"failures\": $failures,
                        \"errors\": $errors,
                        \"duration\": \"${time}s\",
                        \"failure_details\": \"$(echo "$failure_info" | sed 's/"/\\"/g' | tr '\n' ' ')\"
                    }")
                fi
            fi
        done < <(find "$REPORTS_DIR/test-results" -name "*.xml" -print0)
    fi
    
    echo "{
        \"junit_analysis\": {
            \"total_tests\": $total_tests,
            \"failed_tests\": $failed_tests,
            \"success_rate\": $(echo "scale=2; (($total_tests - $failed_tests) * 100) / $total_tests" | bc -l 2>/dev/null || echo "0"),
            \"failed_suites\": [$(IFS=,; echo "${junit_failures[*]}")]
        }
    }"
}

# Function to analyze Cucumber BDD reports
analyze_cucumber_reports() {
    log_info "Analyzing Cucumber BDD reports..."
    
    local cucumber_failures=()
    local total_scenarios=0
    local failed_scenarios=0
    
    if [[ -d "$REPORTS_DIR/cucumber" ]]; then
        while IFS= read -r -d '' json_file; do
            if [[ -f "$json_file" ]] && command -v jq >/dev/null 2>&1; then
                # Use jq for JSON parsing if available
                local feature_name=$(jq -r '.[0].name // "Unknown Feature"' "$json_file" 2>/dev/null || echo "Unknown Feature")
                local scenarios=$(jq '[.[].elements[] | select(.type == "scenario")] | length' "$json_file" 2>/dev/null || echo "0")
                local failed=$(jq '[.[].elements[] | select(.type == "scenario") | select(.steps[]?.result.status == "failed")] | length' "$json_file" 2>/dev/null || echo "0")
                
                total_scenarios=$((total_scenarios + scenarios))
                failed_scenarios=$((failed_scenarios + failed))
                
                if [[ $failed -gt 0 ]]; then
                    # Extract failed scenario details
                    local failed_details=$(jq -c '[.[].elements[] | select(.type == "scenario") | select(.steps[]?.result.status == "failed") | {name: .name, failed_step: (.steps[] | select(.result.status == "failed") | .name)}]' "$json_file" 2>/dev/null || echo "[]")
                    
                    cucumber_failures+=("{
                        \"feature\": \"$feature_name\",
                        \"total_scenarios\": $scenarios,
                        \"failed_scenarios\": $failed,
                        \"failed_details\": $failed_details
                    }")
                fi
            elif [[ -f "$json_file" ]]; then
                # Fallback: Simple grep-based analysis without jq
                local scenarios_count=$(grep -c '"type": "scenario"' "$json_file" || echo "0")
                local failed_count=$(grep -c '"status": "failed"' "$json_file" || echo "0")
                
                total_scenarios=$((total_scenarios + scenarios_count))
                failed_scenarios=$((failed_scenarios + failed_count))
                
                if [[ $failed_count -gt 0 ]]; then
                    cucumber_failures+=("{
                        \"feature\": \"$(basename "$json_file" .json)\",
                        \"total_scenarios\": $scenarios_count,
                        \"failed_scenarios\": $failed_count,
                        \"failed_details\": \"Detailed analysis requires jq tool\"
                    }")
                fi
            fi
        done < <(find "$REPORTS_DIR/cucumber" -name "*.json" -print0)
    fi
    
    echo "{
        \"cucumber_analysis\": {
            \"total_scenarios\": $total_scenarios,
            \"failed_scenarios\": $failed_scenarios,
            \"success_rate\": $(echo "scale=2; (($total_scenarios - $failed_scenarios) * 100) / $total_scenarios" | bc -l 2>/dev/null || echo "0"),
            \"failed_features\": [$(IFS=,; echo "${cucumber_failures[*]}")]
        }
    }"
}

# Function to analyze common failure patterns
analyze_failure_patterns() {
    log_info "Analyzing common failure patterns..."
    
    local patterns=()
    
    # Check for common patterns in test outputs
    if [[ -d "$REPORTS_DIR" ]]; then
        # Connection/timeout issues
        local connection_failures=$(find "$REPORTS_DIR" -name "*.xml" -exec grep -l "Connection\|timeout\|refused\|unreachable" {} \; | wc -l)
        if [[ $connection_failures -gt 0 ]]; then
            patterns+=("{\"type\": \"connection_issues\", \"count\": $connection_failures, \"recommendation\": \"Check service dependencies and network connectivity\"}")
        fi
        
        # Memory issues
        local memory_failures=$(find "$REPORTS_DIR" -name "*.xml" -exec grep -l "OutOfMemoryError\|heap space" {} \; | wc -l)
        if [[ $memory_failures -gt 0 ]]; then
            patterns+=("{\"type\": \"memory_issues\", \"count\": $memory_failures, \"recommendation\": \"Increase JVM heap size or optimize memory usage\"}")
        fi
        
        # Assertion failures
        local assertion_failures=$(find "$REPORTS_DIR" -name "*.xml" -exec grep -l "AssertionError\|assertion failed" {} \; | wc -l)
        if [[ $assertion_failures -gt 0 ]]; then
            patterns+=("{\"type\": \"assertion_failures\", \"count\": $assertion_failures, \"recommendation\": \"Review test expectations and actual behavior\"}")
        fi
        
        # Null pointer exceptions
        local null_pointer_failures=$(find "$REPORTS_DIR" -name "*.xml" -exec grep -l "NullPointerException" {} \; | wc -l)
        if [[ $null_pointer_failures -gt 0 ]]; then
            patterns+=("{\"type\": \"null_pointer_exceptions\", \"count\": $null_pointer_failures, \"recommendation\": \"Add null checks and proper initialization\"}")
        fi
        
        # Cucumber step definition issues
        local cucumber_step_failures=$(find "$REPORTS_DIR" -name "*.json" -exec grep -l "StepDefinitionException\|UndefinedStepException" {} \; 2>/dev/null | wc -l)
        if [[ $cucumber_step_failures -gt 0 ]]; then
            patterns+=("{\"type\": \"cucumber_step_issues\", \"count\": $cucumber_step_failures, \"recommendation\": \"Ensure all BDD steps have corresponding step definitions\"}")
        fi
    fi
    
    echo "{
        \"pattern_analysis\": {
            \"detected_patterns\": [$(IFS=,; echo "${patterns[*]}")],
            \"analysis_timestamp\": \"$(date -u +%Y-%m-%dT%H:%M:%SZ)\"
        }
    }"
}

# Function to generate recommendations
generate_recommendations() {
    log_info "Generating failure resolution recommendations..."
    
    local recommendations=()
    
    # Generic recommendations based on CI/CD best practices
    recommendations+=("{\"category\": \"test_stability\", \"recommendation\": \"Run tests multiple times to identify flaky tests\", \"priority\": \"medium\"}")
    recommendations+=("{\"category\": \"environment\", \"recommendation\": \"Ensure consistent test environment configuration\", \"priority\": \"high\"}")
    recommendations+=("{\"category\": \"dependencies\", \"recommendation\": \"Verify all service dependencies are available and properly configured\", \"priority\": \"high\"}")
    recommendations+=("{\"category\": \"data\", \"recommendation\": \"Use isolated test data to prevent test interference\", \"priority\": \"medium\"}")
    recommendations+=("{\"category\": \"parallelization\", \"recommendation\": \"Review parallel test execution for race conditions\", \"priority\": \"low\"}")
    
    # BDD-specific recommendations
    recommendations+=("{\"category\": \"bdd_scenarios\", \"recommendation\": \"Ensure BDD scenarios reflect current application behavior\", \"priority\": \"medium\"}")
    recommendations+=("{\"category\": \"step_definitions\", \"recommendation\": \"Keep step definitions simple and focused on single actions\", \"priority\": \"low\"}")
    recommendations+=("{\"category\": \"test_data\", \"recommendation\": \"Use factories for consistent test data generation\", \"priority\": \"medium\"}")
    
    echo "{
        \"recommendations\": [$(IFS=,; echo "${recommendations[*]}")]
    }"
}

# Function to calculate test metrics
calculate_test_metrics() {
    log_info "Calculating comprehensive test metrics..."
    
    local start_time="$1"
    local end_time=$(date +%s)
    local duration=$((end_time - start_time))
    
    echo "{
        \"test_metrics\": {
            \"analysis_duration_seconds\": $duration,
            \"analysis_timestamp\": \"$(date -u +%Y-%m-%dT%H:%M:%SZ)\",
            \"reports_analyzed\": {
                \"junit_xml_files\": $(find "$REPORTS_DIR" -name "*.xml" 2>/dev/null | wc -l || echo "0"),
                \"cucumber_json_files\": $(find "$REPORTS_DIR" -name "*.json" 2>/dev/null | wc -l || echo "0"),
                \"total_report_files\": $(find "$REPORTS_DIR" -type f 2>/dev/null | wc -l || echo "0")
            },
            \"environment_info\": {
                \"ci_system\": \"${CI:-local}\",
                \"build_number\": \"${BUILD_NUMBER:-unknown}\",
                \"git_commit\": \"${GITHUB_SHA:-$(git rev-parse HEAD 2>/dev/null || echo 'unknown')}\",
                \"git_branch\": \"${GITHUB_REF_NAME:-$(git branch --show-current 2>/dev/null || echo 'unknown')}\"
            }
        }
    }"
}

# Main analysis function
main() {
    local start_time=$(date +%s)
    
    log_info "Starting comprehensive test failure analysis..."
    log_info "Analysis will be saved to: $ANALYSIS_FILE"
    
    # Check if reports directory exists
    if [[ ! -d "$REPORTS_DIR" ]]; then
        log_error "Reports directory not found: $REPORTS_DIR"
        log_error "Please run tests first to generate reports"
        exit 1
    fi
    
    # Perform analysis
    local junit_analysis=$(analyze_junit_reports)
    local cucumber_analysis=$(analyze_cucumber_reports)
    local pattern_analysis=$(analyze_failure_patterns)
    local recommendations=$(generate_recommendations)
    local test_metrics=$(calculate_test_metrics "$start_time")
    
    # Combine all analyses into a single JSON report
    cat > "$ANALYSIS_FILE" << EOF
{
    $(echo "$junit_analysis" | sed '1d;$d'),
    $(echo "$cucumber_analysis" | sed '1d;$d'),
    $(echo "$pattern_analysis" | sed '1d;$d'),
    $(echo "$recommendations" | sed '1d;$d'),
    $(echo "$test_metrics" | sed '1d;$d')
}
EOF
    
    # Display summary
    log_success "Analysis completed successfully!"
    echo
    echo "📊 Test Failure Analysis Summary"
    echo "================================="
    
    # Extract and display key metrics
    if command -v jq >/dev/null 2>&1; then
        local junit_total=$(jq -r '.junit_analysis.total_tests // 0' "$ANALYSIS_FILE")
        local junit_failed=$(jq -r '.junit_analysis.failed_tests // 0' "$ANALYSIS_FILE")
        local junit_success_rate=$(jq -r '.junit_analysis.success_rate // 0' "$ANALYSIS_FILE")
        
        local cucumber_total=$(jq -r '.cucumber_analysis.total_scenarios // 0' "$ANALYSIS_FILE")
        local cucumber_failed=$(jq -r '.cucumber_analysis.failed_scenarios // 0' "$ANALYSIS_FILE")
        local cucumber_success_rate=$(jq -r '.cucumber_analysis.success_rate // 0' "$ANALYSIS_FILE")
        
        echo "🧪 Unit/Integration Tests: $junit_total total, $junit_failed failed ($junit_success_rate% success rate)"
        echo "🎯 BDD Scenarios: $cucumber_total total, $cucumber_failed failed ($cucumber_success_rate% success rate)"
        
        local pattern_count=$(jq -r '.pattern_analysis.detected_patterns | length' "$ANALYSIS_FILE")
        echo "🔍 Failure Patterns Detected: $pattern_count"
    else
        echo "📋 Detailed metrics available in: $ANALYSIS_FILE"
        log_warn "Install 'jq' for enhanced JSON parsing and display"
    fi
    
    echo
    echo "📁 Full analysis report: $ANALYSIS_FILE"
    echo "🔧 Review the recommendations section for resolution guidance"
    
    # Return appropriate exit code
    if [[ -f "$ANALYSIS_FILE" ]]; then
        if command -v jq >/dev/null 2>&1; then
            local total_failures=$(jq -r '(.junit_analysis.failed_tests // 0) + (.cucumber_analysis.failed_scenarios // 0)' "$ANALYSIS_FILE")
            if [[ $total_failures -gt 0 ]]; then
                log_error "Found $total_failures test failures that require attention"
                return 1
            else
                log_success "No test failures detected - all tests passed!"
                return 0
            fi
        else
            log_warn "Cannot determine test status without jq. Check the analysis file manually."
            return 0
        fi
    else
        log_error "Analysis file was not generated successfully"
        return 1
    fi
}

# Help function
show_help() {
    cat << EOF
Test Failure Analysis Script

Usage: $0 [OPTIONS]

This script analyzes test failure reports and provides detailed insights
for debugging and resolution.

Options:
    -h, --help          Show this help message
    -d, --reports-dir   Specify custom reports directory (default: build/reports)
    -o, --output-dir    Specify custom analysis output directory (default: build/analysis)
    -v, --verbose       Enable verbose output

Examples:
    $0                                          # Run with default settings
    $0 -d custom/reports -o custom/analysis     # Use custom directories
    $0 -v                                       # Enable verbose mode

The script analyzes:
    - JUnit XML test reports
    - Cucumber JSON BDD reports  
    - Common failure patterns
    - Environment and metrics data

Output:
    - Comprehensive JSON analysis report
    - Summary statistics and recommendations
    - Pattern-based failure categorization
EOF
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -h|--help)
            show_help
            exit 0
            ;;
        -d|--reports-dir)
            REPORTS_DIR="$2"
            shift 2
            ;;
        -o|--output-dir)
            ANALYSIS_DIR="$2"
            shift 2
            ;;
        -v|--verbose)
            set -x
            shift
            ;;
        *)
            log_error "Unknown option: $1"
            show_help
            exit 1
            ;;
    esac
done

# Run main analysis
main "$@"