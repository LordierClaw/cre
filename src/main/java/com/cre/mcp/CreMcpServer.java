package com.cre.mcp;

import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import java.util.concurrent.CountDownLatch;
import tools.jackson.databind.json.JsonMapper;

/**
 * Stdio MCP bootstrap. Tools are registered in later tasks once the graph stack exists.
 */
public final class CreMcpServer {

  private CreMcpServer() {}

  public static void main(String[] args) throws Exception {
    JsonMapper jackson3 = JsonMapper.builder().build();
    JacksonMcpJsonMapper mcpJson = new JacksonMcpJsonMapper(jackson3);
    StdioServerTransportProvider transport = new StdioServerTransportProvider(mcpJson);

    @SuppressWarnings("unused")
    McpSyncServer server =
        McpServer.sync(transport).serverInfo("cre-mcp", "0.1.0").jsonMapper(mcpJson).build();

    CountDownLatch running = new CountDownLatch(1);
    Runtime.getRuntime().addShutdownHook(new Thread(running::countDown));
    running.await();
  }
}
