package com.cre.mcp;

import com.cre.core.bootstrap.CreContext;
import com.cre.core.bootstrap.ProjectManager;
import com.cre.tools.FindImplementationsTool;
import com.cre.tools.GetContextTool;
import com.cre.tools.TraceFlowTool;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CreController {

  private final ProjectManager projectManager;

  public CreController(ProjectManager projectManager) {
    this.projectManager = projectManager;
  }

  @PostMapping("/get_context")
  public String getContext(@RequestBody Map<String, Object> req) throws Exception {
    String projectRoot = String.valueOf(req.get("project_root"));
    String nodeId = String.valueOf(req.get("node_id"));
    int depth = 0;
    if (req.get("depth") instanceof Number n) {
      depth = n.intValue();
    }
    
    CreContext ctx = projectManager.getContext(Path.of(projectRoot));
    return new GetContextTool(ctx).execute(nodeId, depth);
  }

  @PostMapping("/expand")
  public String expand(@RequestBody Map<String, Object> req) throws Exception {
    String projectRoot = String.valueOf(req.get("project_root"));
    String nodeId = String.valueOf(req.get("node_id"));
    
    CreContext ctx = projectManager.getContext(Path.of(projectRoot));
    return new GetContextTool(ctx).expand(nodeId);
  }

  @PostMapping("/find_implementations")
  public List<String> findImplementations(@RequestBody Map<String, Object> req) throws Exception {
    String projectRoot = String.valueOf(req.get("project_root"));
    String fqn = String.valueOf(req.get("interface_fqn"));
    
    CreContext ctx = projectManager.getContext(Path.of(projectRoot));
    return new FindImplementationsTool(ctx.graph()).execute(fqn);
  }

  @PostMapping("/trace_flow")
  public List<String> traceFlow(@RequestBody Map<String, Object> req) throws Exception {
    String projectRoot = String.valueOf(req.get("project_root"));
    String id = String.valueOf(req.get("entry_method_node_id"));
    
    CreContext ctx = projectManager.getContext(Path.of(projectRoot));
    return new TraceFlowTool(ctx.graph()).execute(id);
  }

  @PostMapping("/reset_project")
  public String resetProject(@RequestBody Map<String, Object> req) {
    String projectRoot = String.valueOf(req.get("project_root"));
    projectManager.resetContext(Path.of(projectRoot));
    return "Project reset: " + projectRoot;
  }
}
