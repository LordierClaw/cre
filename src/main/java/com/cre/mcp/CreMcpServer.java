package com.cre.mcp;

import com.cre.core.bootstrap.CreContext;
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
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.function.BiFunction;
import tools.jackson.databind.json.JsonMapper;

public final class CreMcpServer {

  private static final ObjectMapper JSON = new ObjectMapper();

  private CreMcpServer() {}

  public static void main(String[] args) throws Exception {
    JsonMapper jackson3 = JsonMapper.builder().build();
    JacksonMcpJsonMapper mcpJson = new JacksonMcpJsonMapper(jackson3);

    CreContext ctx = CreContext.defaultFixtureContext();
    GetContextTool getContext = new GetContextTool(ctx.graph());
    FindImplementationsTool findImpl = new FindImplementationsTool(ctx.graph());
    TraceFlowTool trace = new TraceFlowTool(ctx.graph());

    StdioServerTransportProvider transport = new StdioServerTransportProvider(mcpJson);

    @SuppressWarnings("unused")
    McpSyncServer server =
        McpServer.sync(transport)
            .serverInfo("cre-mcp", "0.1.0")
            .jsonMapper(mcpJson)
            .tools(
                tool(
                    mcpJson,
                    "get_context",
                    "Return a normalized context slice for a graph node",
                    """
                    {"type":"object","properties":{"node_id":{"type":"string"},"depth":{"type":"integer"}},"required":["node_id"]}
                    """,
                    (ex, req) -> handleGetContext(getContext, ex, req)),
                tool(
                    mcpJson,
                    "find_implementations",
                    "List implementing type node ids for a Java interface FQN",
                    """
                    {"type":"object","properties":{"interface_fqn":{"type":"string"}},"required":["interface_fqn"]}
                    """,
                    (ex, req) -> handleFindImpl(findImpl, ex, req)),
                tool(
                    mcpJson,
                    "trace_flow",
                    "Trace CALLS edges in deterministic order from an entry method node id",
                    """
                    {"type":"object","properties":{"entry_method_node_id":{"type":"string"}},"required":["entry_method_node_id"]}
                    """,
                    (ex, req) -> handleTrace(trace, ex, req)))
            .build();

    CountDownLatch running = new CountDownLatch(1);
    Runtime.getRuntime().addShutdownHook(new Thread(running::countDown));
    running.await();
  }

  private static CallToolResult handleGetContext(
      GetContextTool tool, McpSyncServerExchange ex, CallToolRequest req) {
    try {
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

  private static CallToolResult handleFindImpl(
      FindImplementationsTool tool, McpSyncServerExchange ex, CallToolRequest req) {
    try {
      String fqn = String.valueOf(req.arguments().get("interface_fqn"));
      String payload = JSON.writeValueAsString(tool.execute(fqn));
      return CallToolResult.builder().addTextContent(payload).build();
    } catch (Exception e) {
      return CallToolResult.builder().isError(true).addTextContent(e.getMessage()).build();
    }
  }

  private static CallToolResult handleTrace(
      TraceFlowTool tool, McpSyncServerExchange ex, CallToolRequest req) {
    try {
      String id = String.valueOf(req.arguments().get("entry_method_node_id"));
      String payload = JSON.writeValueAsString(tool.execute(id));
      return CallToolResult.builder().addTextContent(payload).build();
    } catch (Exception e) {
      return CallToolResult.builder().isError(true).addTextContent(e.getMessage()).build();
    }
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
