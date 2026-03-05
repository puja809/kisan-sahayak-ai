package com.farmer.location.config;

import com.farmer.location.entity.GovernmentBody;
import com.farmer.location.service.GovernmentBodyService;
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
public class LocationMcpTools {

    private final GovernmentBodyService governmentBodyService;

    @Tool(description = "Get a list of government bodies and contact officers for a specific state and/or district. Useful for finding the KVK phone number, district officer contact info, and agricultural department emails.")
    public List<GovernmentBody> getGovernmentBodies(String state, String district) {
        log.info("MCP Tool Executing getGovernmentBodies for State: {}, District: {}", state, district);

        if (state != null && !state.isEmpty() && district != null && !district.isEmpty()) {
            return governmentBodyService.findByStateAndDistrict(state, district);
        } else if (state != null && !state.isEmpty()) {
            return governmentBodyService.findByState(state);
        } else if (district != null && !district.isEmpty()) {
            return governmentBodyService.findByDistrict(district);
        } else {
            return List.of();
        }
    }

    @Configuration
    static class LocationMcpToolsConfig {
        @Bean
        public MethodToolCallbackProvider locationToolCallbackProvider(LocationMcpTools locationMcpTools) {
            return MethodToolCallbackProvider.builder().toolObjects(locationMcpTools).build();
        }
    }
}
