package com.farmer.mandi.config;

import com.farmer.mandi.dto.MandiPriceDto;
import com.farmer.mandi.service.MandiFilterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MandiMcpTools {

    private final MandiFilterService mandiFilterService;

    @Tool(description = "Search for Mandi (agricultural market) prices. You can filter by state, district, market name, commodity, variety, and grade. Returns a list of matching currently available prices.")
    public List<MandiPriceDto> searchMandiPrices(
            String state,
            String district,
            String market,
            String commodity,
            String variety,
            String grade,
            int offset,
            int limit) {
        log.info("MCP Tool Executing searchMandiPrices for Commodity: {}", commodity);
        int pageOffset = Math.max(0, offset);
        int pageLimit = limit > 0 ? limit : 20;

        return mandiFilterService.searchMarketData(state, district, market, commodity, variety, grade, pageOffset,
                pageLimit);
    }

    @Tool(description = "Get a list of all available commodities in the Mandi (agricultural market) database. Useful for knowing what commodities to search for prices.")
    public List<String> listCommodities() {
        log.info("MCP Tool Executing listCommodities");
        return mandiFilterService.getAllCommodities();
    }

    @Configuration
    static class MandiMcpToolsConfig {
        @Bean
        public MethodToolCallbackProvider mandiToolCallbackProvider(MandiMcpTools mandiMcpTools) {
            return MethodToolCallbackProvider.builder().toolObjects(mandiMcpTools).build();
        }
    }
}
