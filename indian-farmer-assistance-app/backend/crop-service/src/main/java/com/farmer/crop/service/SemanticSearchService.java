package com.farmer.crop.service;

import com.farmer.crop.dto.SearchResultDto;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for semantic search operations.
 * 
 * Handles document search and ranking by similarity.
 */
@Service
public class SemanticSearchService {

    /**
     * Ranks search results by similarity score in descending order.
     * 
     * @param results List of search results to rank
     * @return Sorted list in descending order by similarity
     */
    public List<SearchResultDto> rankBySimilarity(List<SearchResultDto> results) {
        if (results == null || results.isEmpty()) {
            return results;
        }
        
        return results.stream()
                .sorted(Comparator.comparing(SearchResultDto::getSimilarityScore, 
                        Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }

    /**
     * Filters search results by minimum similarity threshold.
     * 
     * @param results List of search results
     * @param threshold Minimum similarity score (0-1)
     * @return Filtered list with similarity >= threshold
     */
    public List<SearchResultDto> filterBySimilarityThreshold(
            List<SearchResultDto> results, BigDecimal threshold) {
        if (results == null || results.isEmpty()) {
            return results;
        }
        
        return results.stream()
                .filter(r -> r.getSimilarityScore() != null && 
                        r.getSimilarityScore().compareTo(threshold) >= 0)
                .collect(Collectors.toList());
    }

    /**
     * Filters search results by category.
     * 
     * @param results List of search results
     * @param category Category to filter by
     * @return Filtered list with matching category
     */
    public List<SearchResultDto> filterByCategory(
            List<SearchResultDto> results, SearchResultDto.DocumentCategory category) {
        if (results == null || results.isEmpty()) {
            return results;
        }
        
        return results.stream()
                .filter(r -> r.getCategory() == category)
                .collect(Collectors.toList());
    }

    /**
     * Filters search results by state.
     * 
     * @param results List of search results
     * @param state State to filter by (null means all states)
     * @return Filtered list with matching state or all if state is null
     */
    public List<SearchResultDto> filterByState(List<SearchResultDto> results, String state) {
        if (results == null || results.isEmpty()) {
            return results;
        }
        
        if (state == null || state.isEmpty()) {
            return results;
        }
        
        return results.stream()
                .filter(r -> r.getState() == null || r.getState().equals(state))
                .collect(Collectors.toList());
    }

    /**
     * Limits search results to top N by similarity.
     * 
     * @param results List of search results
     * @param limit Maximum number of results to return
     * @return Limited list of top results
     */
    public List<SearchResultDto> limitResults(List<SearchResultDto> results, int limit) {
        if (results == null || results.isEmpty()) {
            return results;
        }
        
        return results.stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Calculates average similarity score of results.
     * 
     * @param results List of search results
     * @return Average similarity score, or null if empty
     */
    public BigDecimal calculateAverageSimilarity(List<SearchResultDto> results) {
        if (results == null || results.isEmpty()) {
            return null;
        }
        
        return results.stream()
                .filter(r -> r.getSimilarityScore() != null)
                .map(SearchResultDto::getSimilarityScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(new BigDecimal(results.size()), 4, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Checks if all results are sorted in descending order by similarity.
     * 
     * @param results List of search results
     * @return true if results are in descending order
     */
    public boolean isDescendingOrder(List<SearchResultDto> results) {
        if (results == null || results.size() <= 1) {
            return true;
        }
        
        for (int i = 0; i < results.size() - 1; i++) {
            BigDecimal current = results.get(i).getSimilarityScore();
            BigDecimal next = results.get(i + 1).getSimilarityScore();
            
            if (current == null || next == null) {
                continue;
            }
            
            if (current.compareTo(next) < 0) {
                return false;
            }
        }
        
        return true;
    }
}