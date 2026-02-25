package com.farmer.admin.dto;

import lombok.*;
import java.util.Map;

/**
 * DTO for analytics response.
 * Requirements: 21.8
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalyticsResponse {

    private UserAnalytics userAnalytics;
    private UsageAnalytics usageAnalytics;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserAnalytics {
        private Long totalUsers;
        private Long activeUsers;
        private Long newUsersThisMonth;
        private Long usersByState;
        private Map<String, Long> usersByLanguage;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UsageAnalytics {
        private Long totalDocumentViews;
        private Long totalSearchQueries;
        private Long totalSchemeApplications;
        private Map<String, Long> topSearchedDocuments;
        private Map<String, Long> mostViewedSchemes;
    }
}