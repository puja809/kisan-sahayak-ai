package com.farmer.bandwidth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service for monitoring data usage and notifying farmers.
 * Validates: Requirements 13.4, 13.5
 */
@Slf4j
@Service
public class DataUsageMonitoringService {

    @Value("${data-usage.default-limit-mb:100}")
    private long defaultDataLimitMb;

    @Value("${data-usage.warning-threshold-percent:80}")
    private int warningThresholdPercent;

    /**
     * Tracks data usage for a user session.
     */
    public static class DataUsageTracker {
        private long totalUsedBytes;
        private long limitBytes;
        private long lastResetTime;

        public DataUsageTracker(long limitMb) {
            this.limitBytes = limitMb * 1024 * 1024;
            this.totalUsedBytes = 0;
            this.lastResetTime = System.currentTimeMillis();
        }

        public void addUsage(long bytes) {
            this.totalUsedBytes += bytes;
        }

        public long getTotalUsedBytes() {
            return totalUsedBytes;
        }

        public long getLimitBytes() {
            return limitBytes;
        }

        public double getUsagePercentage() {
            if (limitBytes == 0) return 0;
            return (double) totalUsedBytes / limitBytes * 100;
        }

        public long getRemainingBytes() {
            return Math.max(0, limitBytes - totalUsedBytes);
        }

        public boolean hasExceededLimit() {
            return totalUsedBytes > limitBytes;
        }

        public boolean isNearLimit(int thresholdPercent) {
            return getUsagePercentage() >= thresholdPercent;
        }

        public void reset() {
            this.totalUsedBytes = 0;
            this.lastResetTime = System.currentTimeMillis();
        }
    }

    /**
     * Creates a new data usage tracker with default limit.
     * 
     * @return DataUsageTracker instance
     */
    public DataUsageTracker createTracker() {
        return new DataUsageTracker(defaultDataLimitMb);
    }

    /**
     * Creates a new data usage tracker with custom limit.
     * 
     * @param limitMb data limit in megabytes
     * @return DataUsageTracker instance
     */
    public DataUsageTracker createTracker(long limitMb) {
        return new DataUsageTracker(limitMb);
    }

    /**
     * Checks if data usage exceeds limit.
     * 
     * @param tracker data usage tracker
     * @return true if limit exceeded
     */
    public boolean hasExceededLimit(DataUsageTracker tracker) {
        return tracker.hasExceededLimit();
    }

    /**
     * Checks if data usage is near limit.
     * 
     * @param tracker data usage tracker
     * @return true if usage is near limit
     */
    public boolean isNearLimit(DataUsageTracker tracker) {
        return tracker.isNearLimit(warningThresholdPercent);
    }

    /**
     * Gets data-saving suggestions based on usage.
     * 
     * @param tracker data usage tracker
     * @return array of suggestions
     */
    public String[] getDataSavingSuggestions(DataUsageTracker tracker) {
        if (tracker.hasExceededLimit()) {
            return new String[]{
                "Data limit exceeded. Enable WiFi-only mode for large downloads.",
                "Disable image loading to reduce data usage.",
                "Use text-only mode for browsing."
            };
        } else if (tracker.isNearLimit(warningThresholdPercent)) {
            return new String[]{
                "Data usage is near limit. Consider enabling WiFi-only mode.",
                "Reduce image quality to save data.",
                "Disable auto-refresh features."
            };
        }
        return new String[]{};
    }

    /**
     * Gets the default data limit in MB.
     * 
     * @return default limit in MB
     */
    public long getDefaultDataLimitMb() {
        return defaultDataLimitMb;
    }

    /**
     * Gets the warning threshold percentage.
     * 
     * @return warning threshold
     */
    public int getWarningThresholdPercent() {
        return warningThresholdPercent;
    }
}
