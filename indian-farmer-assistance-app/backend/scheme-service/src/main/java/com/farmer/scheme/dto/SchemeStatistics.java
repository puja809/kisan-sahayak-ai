package com.farmer.scheme.dto;

/**
 * DTO for scheme statistics.
 */
public record SchemeStatistics(
    long totalSchemes,
    long activeSchemes,
    long centralSchemes,
    long stateSchemes,
    long cropSpecificSchemes,
    long insuranceSchemes,
    long subsidySchemes,
    long welfareSchemes
) {}