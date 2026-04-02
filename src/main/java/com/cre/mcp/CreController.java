package com.cre.mcp;

import com.cre.core.exception.CreException;
import com.cre.core.exception.ProjectNotFoundException;
import com.cre.core.exception.SymbolNotFoundException;
import com.cre.core.service.ContextOptions;
import com.cre.core.service.CreService;
import java.nio.file.Path;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CreController {

  private final CreService creService;

  public CreController(CreService creService) {
    this.creService = creService;
  }

  @PostMapping("/get_context")
  public String getContext(@RequestBody Map<String, Object> req) throws CreException {
    String projectRoot = String.valueOf(req.get("project_root"));
    String symbol = String.valueOf(req.get("node_id")); // Kept 'node_id' for backward compatibility in JSON
    int depth = 0;
    if (req.get("depth") instanceof Number n) {
      depth = n.intValue();
    }
    Map<String, Object> optionsMap = (req.get("options") instanceof Map m) ? (Map<String, Object>) m : Map.of();
    ContextOptions options = ContextOptions.fromMap(optionsMap);
    
    return creService.getContext(fullPath(projectRoot), symbol, depth, options);
  }

  @PostMapping("/expand")
  public String expand(@RequestBody Map<String, Object> req) throws CreException {
    String projectRoot = String.valueOf(req.get("project_root"));
    String symbol = String.valueOf(req.get("node_id"));
    
    return creService.expand(fullPath(projectRoot), symbol);
  }

  @PostMapping("/get_project_structure")
  public String getProjectStructure(@RequestBody Map<String, Object> req) throws CreException {
    String projectRoot = String.valueOf(req.get("project_root"));
    return creService.getProjectStructure(fullPath(projectRoot));
  }

  @PostMapping("/get_file_structure")
  public String getFileStructure(@RequestBody Map<String, Object> req) throws CreException {
    String projectRoot = String.valueOf(req.get("project_root"));
    String symbol = String.valueOf(req.get("symbol"));
    return creService.getFileStructure(fullPath(projectRoot), symbol);
  }

  @PostMapping("/reset_project")
  public String resetProject(@RequestBody Map<String, Object> req) {
    String projectRoot = String.valueOf(req.get("project_root"));
    creService.resetProject(fullPath(projectRoot));
    return "Project reset: " + projectRoot;
  }

  private Path fullPath(String root) {
    return Path.of(root).toAbsolutePath().normalize();
  }
}
