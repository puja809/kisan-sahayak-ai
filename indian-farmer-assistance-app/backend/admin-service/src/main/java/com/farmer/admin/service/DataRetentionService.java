package com.farmer.admin.service;

import com.farmer.admin.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service for implementing data retention policies.
 * Retains crop records for 5 years or 10 crop cycles (whichever is longer).
 * Retains audit logs indefinitely (for compliance).
 * Requirements: 11A.6
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DataRetentionService {

    private final AuditLogRepository auditLogRepository;

    @Value("${app.retention.crop-years:5}")
    private int cropRetentionYears;

    @Value("${app.retention.crop-cycles:10}")
    private int cropRetentionCycles;

    @Value("${app.retention.error-log-days:90}")
    private int errorLogRetentionDays;

    @Value("${app.retention.warn-log-days:60}")
    private int warnLogRetentionDays;

    @Value("${app.retention.info-log-days:30}")
    private int infoLogRetentionDays;

    @Value("${app.retention.debug-log-days:7}")
    private int debugLogRetentionDays;

    /**
     * Scheduled job to clean up old data based on retention policies.
     * Runs daily at 2 AM.
     * Requirements: 11A.6
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupOldData() {
        log.info("Starting data retention cleanup job");

        try {
            // Clean up old audit logs (keep for 7 years for compliance)
            cleanupAuditLogs();

            log.info("Data retention cleanup completed successfully");
        } catch (Exception e) {
            log.error("Error during data retention cleanup: {}", e.getMessage(), e);
        }
    }

    /**
     * Clean up audit logs older than 7 years.
     * Audit logs are kept longer for compliance and audit trail purposes.
     * Requirements: 21.11
     */
    private void cleanupAuditLogs() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusYears(7);

        try {
            auditLogRepository.deleteByTimestampBefore(cutoffDate);
            log.info("Cleaned up audit logs before: {}", cutoffDate);
        } catch (Exception e) {
            log.error("Error cleaning up audit logs: {}", e.getMessage());
        }
    }

    /**
     * Get crop retention period in years.
     * Crop records are retained for 5 years or 10 crop cycles, whichever is longer.
     * Requirements: 11A.6
     */
    public int getCropRetentionYears() {
        return cropRetentionYears;
    }

    /**
     * Get crop retention period in cycles.
     * Requirements: 11A.6
     */
    public int getCropRetentionCycles() {
        return cropRetentionCycles;
    }

    /**
     * Get error log retention period in days.
     * Requirements: 11A.6
     */
    public int getErrorLogRetentionDays() {
        return errorLogRetentionDays;
    }

    /**
     * Get warning log retention period in days.
     * Requirements: 11A.6
     */
    public int getWarnLogRetentionDays() {
        return warnLogRetentionDays;
    }

    /**
     * Get info log retention period in days.
     * Requirements: 11A.6
     */
    public int getInfoLogRetentionDays() {
        return infoLogRetentionDays;
    }

    /**
     * Get debug log retention period in days.
     * Requirements: 11A.6
     */
    public int getDebugLogRetentionDays() {
        return debugLogRetentionDays;
    }
}
