package com.farmer.scheme.config;

import com.farmer.scheme.dto.SchemeDTO;
import com.farmer.scheme.service.SchemeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SchemeMcpTools {

    private final SchemeService schemeService;

    @Tool(description = "Search for government agricultural schemes. You can filter by commodity name (e.g. Rice, Wheat), state (e.g. Maharashtra), or center. Returns a list of matching schemes with their details and links.")
    public List<SchemeDTO> searchSchemes(
            String commodity,
            String state,
            String center,
            int page,
            int size) {
        log.info("MCP Tool Executing searchSchemes for Commodity: {}, State: {}", commodity, state);
        int pageIndex = Math.max(0, page);
        int pageSize = size > 0 ? size : 20;
        Pageable pageable = PageRequest.of(pageIndex, pageSize);

        Page<SchemeDTO> resultPage = schemeService.searchSchemes(commodity, state, center, pageable);
        return resultPage.getContent();
    }

    @Configuration
    static class SchemeMcpToolsConfig {
        @Bean
        public MethodToolCallbackProvider schemeToolCallbackProvider(SchemeMcpTools schemeMcpTools) {
            return MethodToolCallbackProvider.builder().toolObjects(schemeMcpTools).build();
        }
    }
}
