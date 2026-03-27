package com.cre.mcp;

import com.cre.core.bootstrap.CreContext;
import com.cre.core.bootstrap.ProjectManager;
import com.cre.tools.FindImplementationsTool;
import com.cre.tools.GetContextTool;
import com.cre.tools.TraceFlowTool;
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

  private static final ObjectMapper JSON = new ObjectMapper();

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
  public McpSyncServer mcpServer(JacksonMcpJsonMapper mcpJson, HttpServletSseServerTransportProvider sseTransport) {
    var builder = McpServer.sync(transportType.equalsIgnoreCase("stdio") 
        ? new StdioServerTransportProvider(mcpJson) 
        : sseTransport);

    return builder
        .serverInfo("cre-mcp", "0.1.0")
        .jsonMapper(mcpJson)
        .tools(
            tool(mcpJson, "get_context", "Return a normalized context slice for a graph node",
                """
                {"type":"object","properties":{"project_root":{"type":"string"},"node_id":{"type":"string"},"depth":{"type":"integer"}},"required":["project_root","node_id"]}
                """,
                McpConfig::handleGetContext),
            tool(mcpJson, "expand", "Expand a node id into a bounded merged context slice",
                """
                {"type":"object","properties":{"project_root":{"type":"string"},"node_id":{"type":"string"}},"required":["project_root","node_id"]}
                """,
                McpConfig::handleExpand),
            tool(mcpJson, "find_implementations", "List implementing type node ids for a Java interface FQN",
                """
                {"type":"object","properties":{"project_root":{"type":"string"},"interface_fqn":{"type":"string"}},"required":["project_root","interface_fqn"]}
                """,
                McpConfig::handleFindImpl),
            tool(mcpJson, "trace_flow", "Trace CALLS edges in deterministic order from an entry method node id",
                """
                {"type":"object","properties":{"project_root":{"type":"string"},"entry_method_node_id":{"type":"string"}},"required":["project_root","entry_method_node_id"]}
                """,
                McpConfig::handleTrace),
            tool(mcpJson, "reset_project", "Force re-indexing of a project root",
                """
                {"type":"object","properties":{"project_root":{"type":"string"}},"required":["project_root"]}
                """,
                McpConfig::handleReset))
        .build();
  }

  private static CallToolResult handleGetContext(McpSyncServerExchange ex, CallToolRequest req) {
    try {
      CreContext ctx = getContext(req);
      GetContextTool tool = new GetContextTool(ctx.graph());
      Map<String, Object> a = req.arguments();
      String nodeId = String.valueOf(a.get("node_id"));
      int depth = 0;
      Object d = a.get("depth");
      if (d instanceof Number n) {
        depth = n.intValue();
      }
      String payload = JSON.writeValueAsString(tool.execute(nodeId, depth));
      return CallToolResult.builder().addTextContent(payload).build();
    } catch (Exception e) {
      return CallToolResult.builder().isError(true).addTextContent(e.getMessage()).build();
    }
  }

  private static CallToolResult handleExpand(McpSyncServerExchange ex, CallToolRequest req) {
    try {
      CreContext ctx = getContext(req);
      GetContextTool tool = new GetContextTool(ctx.graph());
      String nodeId = String.valueOf(req.arguments().get("node_id"));
      String payload = JSON.writeValueAsString(tool.expand(nodeId));
      return CallToolResult.builder().addTextContent(payload).build();
    } catch (Exception e) {
      return CallToolResult.builder().isError(true).addTextContent(e.getMessage()).build();
    }
  }

  private static CallToolResult handleFindImpl(McpSyncServerExchange ex, CallToolRequest req) {
    try {
      CreContext ctx = getContext(req);
      FindImplementationsTool tool = new FindImplementationsTool(ctx.graph());
      String fqn = String.valueOf(req.arguments().get("interface_fqn"));
      String payload = JSON.writeValueAsString(tool.execute(fqn));
      return CallToolResult.builder().addTextContent(payload).build();
    } catch (Exception e) {
      return CallToolResult.builder().isError(true).addTextContent(e.getMessage()).build();
    }
  }

  private static CallToolResult handleTrace(McpSyncServerExchange ex, CallToolRequest req) {
    try {
      CreContext ctx = getContext(req);
      TraceFlowTool tool = new TraceFlowTool(ctx.graph());
      String id = String.valueOf(req.arguments().get("entry_method_node_id"));
      String payload = JSON.writeValueAsString(tool.execute(id));
      return CallToolResult.builder().addTextContent(payload).build();
    } catch (Exception e) {
      return CallToolResult.builder().isError(true).addTextContent(e.getMessage()).build();
    }
  }

  private static CallToolResult handleReset(McpSyncServerExchange ex, CallToolRequest req) {
    try {
      Path root = getProjectRoot(req);
      ProjectManager.getInstance().resetContext(root);
      return CallToolResult.builder().addTextContent("Project reset: " + root).build();
    } catch (Exception e) {
      return CallToolResult.builder().isError(true).addTextContent(e.getMessage()).build();
    }
  }

  private static CreContext getContext(CallToolRequest req) throws Exception {
    return ProjectManager.getInstance().getContext(getProjectRoot(req));
  }

  private static Path getProjectRoot(CallToolRequest req) {
    Object root = req.arguments().get("project_root");
    if (root == null) {
      throw new IllegalArgumentException("Missing project_root argument");
    }
    return Path.of(String.valueOf(root));
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
