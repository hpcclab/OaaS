package org.hpcclab.oprc.cli.conf;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * @author Pawissanutt
 */
@Data
@AllArgsConstructor
public class FileCliConfig {
  Map<String, FileCliContext> contexts;
  String currentContext;

  public FileCliContext current(){
    return contexts.get(currentContext);
  }

  @Data
  @AllArgsConstructor
  @Builder(toBuilder = true)
  public static class FileCliContext {
    String pmUrl;
    String invUrl;
    String proxy;
    String pmVirtualHost;
    String invVirtualHost;
    String defaultClass;
    String defaultObject;
  }
}
