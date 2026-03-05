import os
import logging
from typing import List, Dict, Any, Optional
from langchain_aws import ChatBedrock
from langchain_core.messages import SystemMessage, HumanMessage
from langgraph.prebuilt import create_react_agent
from mcp import ClientSession, StdioServerParameters
from mcp.client.stdio import stdio_client
from langchain_mcp_adapters.tools import load_mcp_tools
import asyncio

logger = logging.getLogger(__name__)

# List of Spring Boot MCP Servers to connect to
MCP_SERVERS = {
    "scheme-service": {"command": "java", "args": ["-jar", "../../scheme-service/target/scheme-service-1.0.0-SNAPSHOT.jar"]}, # Fallback assuming jarring, better to just run via maven or hit SSE if available. Wait, Spring Boot MCP starters usually just expose HTTP/SSE endpoints. But standard `mcp.client.stdio` needs a command. Let's use SSE.
}

# The Spring Boot MCP starter (Spring AI 1.1.1) exposes an SSE endpoint at /sse by default
# We can use the MCP SSE client from `mcp.client.sse`
from mcp.client.sse import sse_client

MCP_ENDPOINTS_ENV = os.getenv(
    "MCP_ENDPOINTS", 
    "http://localhost:8093/sse,http://localhost:8095/sse,http://localhost:8096/sse,http://localhost:8097/sse,http://localhost:8100/sse,http://localhost:8101/sse"
)

MCP_ENDPOINTS = [url.strip() for url in MCP_ENDPOINTS_ENV.split(",") if url.strip()]

class BedrockAgentWithMCP:
    def __init__(self, region_name="us-east-1"):
        self.llm = ChatBedrock(
            model_id="amazon.nova-pro-v1:0", # Using Nova or Llama3 depending on region. Nova pro supports tool calling well.
            region_name=region_name,
            model_kwargs={"temperature": 0.1},
        )
        self.tools = []
        self.clients = []
        self._connected = False

    async def initialize(self):
        """Connects to all MCP servers via SSE and loads their tools."""
        if self._connected:
            return
            
        logger.info("Initializing MCP Agent and loading tools...")
        
        for endpoint in MCP_ENDPOINTS:
            try:
                # Add a timeout to avoid hanging on down services, but increase it
                # to 60s because some MCP tools (like weather) take time to respond
                sse_ctx = sse_client(endpoint, timeout=60.0, sse_read_timeout=300.0)
                read_stream, write_stream = await sse_ctx.__aenter__()
                session = ClientSession(read_stream, write_stream)
                await session.__aenter__()
                await session.initialize()
                
                # Use langchain_mcp_adapters to convert MCP tools to Langchain tools
                server_tools = await load_mcp_tools(session)
                self.tools.extend(server_tools)
                
                # Keep references to prevent cleanup
                self.clients.append((sse_ctx, session))
                logger.info(f"Successfully loaded tools from {endpoint}")
            except Exception as e:
                logger.warning(f"Failed to connect to MCP server at {endpoint}: {e}")

        logger.info(f"Loaded a total of {len(self.tools)} tools.")
        self._connected = True

    async def cleanup(self):
        """Closes all MCP connections."""
        for sse_ctx, session in self.clients:
            try:
                await session.__aexit__(None, None, None)
                await sse_ctx.__aexit__(None, None, None)
            except Exception as e:
                logger.error(f"Error closing MCP connection: {e}")
        self.clients.clear()
        self.tools.clear()
        self._connected = False

    async def _reverse_geocode(self, latitude: float, longitude: float) -> Optional[Dict[str, str]]:
        """
        Reverse geocode lat/lng to city, district, state using Nominatim (OpenStreetMap).
        Free, no API key required. Rate limit: 1 req/sec.
        """
        try:
            import httpx
            url = "https://nominatim.openstreetmap.org/reverse"
            params = {
                "lat": latitude,
                "lon": longitude,
                "format": "json",
                "addressdetails": 1,
                "zoom": 10,  # district-level detail
            }
            headers = {
                "User-Agent": "KisanSahayakAI/1.0 (indian-farmer-assistance-app)"
            }
            async with httpx.AsyncClient() as client:
                response = await client.get(url, params=params, headers=headers, timeout=5)
                data = response.json()
            
            address = data.get("address", {})
            
            # Extract city/village - Nominatim uses different keys depending on area
            city = (
                address.get("city") or 
                address.get("town") or 
                address.get("village") or 
                address.get("hamlet") or 
                address.get("suburb", "")
            )
            
            # District - in India, usually 'county' or 'state_district' in Nominatim
            district = (
                address.get("state_district") or 
                address.get("county") or 
                address.get("city_district", "")
            )
            
            state = address.get("state", "")
            
            logger.info(f"Reverse geocoded ({latitude}, {longitude}) -> City: {city}, District: {district}, State: {state}")
            
            return {"city": city, "district": district, "state": state}
        except Exception as e:
            logger.warning(f"Reverse geocoding failed: {e}")
            return None

    async def invoke(self, question: str, chat_history: List[Dict[str, str]] = None, latitude: float = None, longitude: float = None, city_name: str = None) -> str:
        """Invokes the agent to answer the question, using loaded tools."""
        if not self._connected:
            await self.initialize()

        if not self.tools:
            return "Error: No tools are available. Ensure microservices are running."

        # Setup prompt
        system_prompt = (
            "You are Krishi Sahayak, an AI assistant for Indian farmers.\n"
            "You have access to several specialized tools for checking crop prices, schemes, weather, location, and crop details.\n"
            "If a user asks a question, intelligently determine which tools to use to find the answer.\n"
            "Answer directly and helpfully based on the tool results. Do not mention the tools to the user.\n"
            "Do NOT include any <thinking> tags or internal reasoning in your response. Only output the final answer.\n"
            "Translate the response into the same language as the user's prompt if requested implicitly.\n"
        )

        # Inject location context if available
        if latitude is not None and longitude is not None:
            # Priority 1: Use coordinates + reverse geocoding for best accuracy
            location_info = await self._reverse_geocode(latitude, longitude)
            if location_info:
                city = location_info.get("city", "")
                district = location_info.get("district", "")
                state = location_info.get("state", "")
                location_context = (
                    f"\n[User's current location: latitude={latitude}, longitude={longitude}."
                    f" City/Village: {city}, District: {district}, State: {state}."
                    f" Use this location automatically when tools require coordinates, district, or state information.]"
                )
            else:
                location_context = (
                    f"\n[User's current location: latitude={latitude}, longitude={longitude}."
                    f" Use this location automatically when tools require coordinates, district, or state information.]"
                )
            system_prompt += location_context
        elif city_name:
            # Priority 2: Fallback to user-provided city/district name
            location_context = (
                f"\n[User's location (self-reported): {city_name}."
                f" Use this city/district name when tools require location, district, or state information.]"
            )
            system_prompt += location_context
        
        # Create Agent using LangGraph
        try:
            agent = create_react_agent(self.llm, tools=self.tools, prompt=system_prompt)
            
            logger.info(f"Invoking Bedrock LangGraph agent with question: {question}")
            
            messages = [HumanMessage(content=question)]
            # Ensure it runs async
            response = await agent.ainvoke({"messages": messages})
            
            # Extract final message from LangGraph response
            if response and "messages" in response and len(response["messages"]) > 0:
                final_msg = response["messages"][-1]
                return self._clean_response(final_msg.content)
            return "Sorry, I couldn't generate an answer."
        except Exception as e:
            import traceback
            error_trace = traceback.format_exc()
            logger.error(f"Error invoking Bedrock agent: {repr(e)}\n{error_trace}")
            return f"I ran into an issue while processing your request: {repr(e)}"

    @staticmethod
    def _clean_response(text: str) -> str:
        """Strip <thinking>...</thinking> tags and clean up the response."""
        import re
        # Remove <thinking>...</thinking> blocks (greedy, multiline)
        cleaned = re.sub(r'<thinking>.*?</thinking>', '', text, flags=re.DOTALL)
        # Remove any stray opening/closing thinking tags
        cleaned = re.sub(r'</?thinking>', '', cleaned)
        return cleaned.strip()

# Singleton agent instance
bedrock_mcp_agent = BedrockAgentWithMCP()
