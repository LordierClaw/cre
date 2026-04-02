package com.cre.mcp;

import com.cre.core.service.ContextOptions;
import com.cre.core.service.CreService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.server.transport.HttpServletSseServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.BiFunction;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class McpConfig {

  @Value("${mcp.transport:sse}")
  private String transportType;

  @Bean
  public JacksonMcpJsonMapper mcpJsonMapper() {
    JsonMapper jackson3 = JsonMapper.builder().build();
    return new JacksonMcpJsonMapper(jackson3);
  }

  @Bean
  public HttpServletSseServerTransportProvider sseTransport(JacksonMcpJsonMapper mcpJson) {
    return HttpServletSseServerTransportProvider.builder()
        .jsonMapper(mcpJson)
        .sseEndpoint("/mcp/sse")
        .messageEndpoint("/mcp/messages")
        .build();
  }

  @Bean
  public ServletRegistrationBean<HttpServletSseServerTransportProvider> mcpServlet(HttpServletSseServerTransportProvider sseTransport) {
    return new ServletRegistrationBean<>(sseTransport, "/mcp/sse", "/mcp/messages");
  }

  @Bean
  public McpSyncServer mcpServer(JacksonMcpJsonMapper mcpJson, HttpServletSseServerTransportProvider sseTransport, CreService creService) {
    var builder = McpServer.sync(transportType.equalsIgnoreCase("stdio") 
        ? new StdioServerTransportProvider(mcpJson) 
        : sseTransport);

    return builder
        .serverInfo("cre-mcp", "0.1.0")
        .jsonMapper(mcpJson)
        .tools(
            tool(mcpJson, "get_context", "Return a normalized context slice for a symbol",
                """
                {"type":"object","properties":{"project_root":{"type":"string"},"node_id":{"type":"string"},"depth":{"type":"integer"},"options":{"type":"object"}},"required":["project_root","node_id"]}
                """,
                (ex, req) -> handleGetContext(ex, req, creService)),
            tool(mcpJson, "expand", "Expand a symbol into a bounded merged context slice",
                """
                {"type":"object","properties":{"project_root":{"type":"string"},"node_id":{"type":"string"}},"required":["project_root","node_id"]}
                """,
                (ex, req) -> handleExpand(ex, req, creService)),
            tool(mcpJson, "get_project_structure", "Get the project structure as a tree-like string",
                """
                {"type":"object","properties":{"project_root":{"type":"string"}},"required":["project_root"]}
                """,
                (ex, req) -> handleGetProjectStructure(ex, req, creService)),
            tool(mcpJson, "get_file_structure", "Get the structure of a specific file or class as a skeleton",
                """
                {"type":"object","properties":{"project_root":{"type":"string"},"symbol":{"type":"string"}},"required":["project_root","symbol"]}
                """,
                (ex, req) -> handleGetFileStructure(ex, req, creService)),
            tool(mcpJson, "reset_project", "Force re-indexing of a project root",
                """
                {"type":"object","properties":{"project_root":{"type":"string"}},"required":["project_root"]}
                """,
                (ex, req) -> handleReset(ex, req, creService)))
        .build();
  }

  private static CallToolResult handleGetContext(McpSyncServerExchange ex, CallToolRequest req, CreService creService) {
    try {
      Path root = getProjectRoot(req);
      Map<String, Object> a = req.arguments();
      String symbol = String.valueOf(a.get("node_id"));
      int depth = 0;
      Object d = a.get("depth");
      if (d instanceof Number n) {
        depth = n.intValue();
      }
      Map<String, Object> optionsMap = (a.get("options") instanceof Map m) ? (Map<String, Object>) m : Map.of();
      ContextOptions options = ContextOptions.fromMap(optionsMap);
      
      String payload = creService.getContext(root, symbol, depth, options);
      return CallToolResult.builder().addTextContent(payload).build();
    } catch (Exception e) {
      return buildErrorResult(e);
    }
  }

  private static CallToolResult handleExpand(McpSyncServerExchange ex, CallToolRequest req, CreService creService) {
    try {
      Path root = getProjectRoot(req);
      String symbol = String.valueOf(req.arguments().get("node_id"));
      String payload = creService.expand(root, symbol);
      return CallToolResult.builder().addTextContent(payload).build();
    } catch (Exception e) {
      return buildErrorResult(e);
    }
  }

  private static CallToolResult handleGetProjectStructure(McpSyncServerExchange ex, CallToolRequest req, CreService creService) {
    try {
      Path root = getProjectRoot(req);
      String payload = creService.getProjectStructure(root);
      return CallToolResult.builder().addTextContent(payload).build();
    } catch (Exception e) {
      return buildErrorResult(e);
    }
  }

  private static CallToolResult handleGetFileStructure(McpSyncServerExchange ex, CallToolRequest req, CreService creService) {
    try {
      Path root = getProjectRoot(req);
      String symbol = String.valueOf(req.arguments().get("symbol"));
      String payload = creService.getFileStructure(root, symbol);
      return CallToolResult.builder().addTextContent(payload).build();
    } catch (Exception e) {
      return buildErrorResult(e);
    }
  }

  private static CallToolResult handleReset(McpSyncServerExchange ex, CallToolRequest req, CreService creService) {
    try {
      Path root = getProjectRoot(req);
      creService.resetProject(root);
      return CallToolResult.builder().addTextContent("Project reset: " + root).build();
    } catch (Exception e) {
      return buildErrorResult(e);
    }
  }

  private static CallToolResult buildErrorResult(Exception e) {
    String type = e.getClass().getSimpleName();
    String message = e.getMessage();
    return CallToolResult.builder()
        .isError(true)
        .addTextContent("[" + type + "] " + (message != null ? message : "An unexpected error occurred"))
        .build();
  }

  private static Path getProjectRoot(CallToolRequest req) {
    Object root = req.arguments().get("project_root");
    if (root == null) {
      throw new IllegalArgumentException("Missing project_root argument");
    }
    return fullPath(String.valueOf(root));
  }

  private static Path fullPath(String path) {
    return Path.of(path).toAbsolutePath().normalize();
  }

  private static SyncToolSpecification tool(
      JacksonMcpJsonMapper mcpJson,
      String name,
      String description,
      String schemaJson,
      BiFunction<McpSyncServerExchange, CallToolRequest, CallToolResult> handler) {
    return SyncToolSpecification.builder()
        .tool(
            McpSchema.Tool.builder()
                .name(name)
                .description(description)
                .inputSchema(mcpJson, schemaJson)
                .build())
        .callHandler(handler)
        .build();
  }
}
