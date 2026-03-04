package com.farmer.yield.config;

import com.farmer.yield.dto.YieldCalculationRequest;
import com.farmer.yield.dto.YieldCalculationResponse;
import com.farmer.yield.service.YieldCalculatorService;
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
public class YieldMcpTools {

    private final YieldCalculatorService yieldCalculatorService;

    @Tool(description = "Calculate estimated crop yield, expected revenue, and profit margin based on commodity name, farm size in hectares, and initial investment amount.")
    public YieldCalculationResponse calculateYieldEstimate(
            String commodity,
            Double farmSizeHectares,
            Double investmentAmount) {
        log.info("MCP Tool Executing calculateYieldEstimate for commodity: {}, farmSize: {}, investment: {}",
                commodity, farmSizeHectares, investmentAmount);

        YieldCalculationRequest request = new YieldCalculationRequest();
        request.setCommodity(commodity);
        request.setFarmSizeHectares(farmSizeHectares != null ? farmSizeHectares : 1.0);
        request.setInvestmentAmount(investmentAmount != null ? investmentAmount : 0.0);

        return yieldCalculatorService.calculateYield(request);
    }

    @Tool(description = "Get a list of all commodities available for yield calculation.")
    public List<String> getAvailableYieldCommodities() {
        log.info("MCP Tool Executing getAvailableYieldCommodities");
        return yieldCalculatorService.getAvailableCommodities();
    }

    @Configuration
    static class YieldMcpToolsConfig {
        @Bean
        public MethodToolCallbackProvider yieldToolCallbackProvider(YieldMcpTools yieldMcpTools) {
            return MethodToolCallbackProvider.builder().toolObjects(yieldMcpTools).build();
        }
    }
}
