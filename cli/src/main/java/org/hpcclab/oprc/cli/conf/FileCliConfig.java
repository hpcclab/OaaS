package org.hpcclab.oprc.cli.conf;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.hpcclab.oaas.repository.store.DatastoreConf;

import java.nio.file.Path;
import java.util.Map;

/**
 * @author Pawissanutt
 */
@Data
@AllArgsConstructor
@Builder(toBuilder = true)
public class FileCliConfig {
  Map<String, FileCliContext> contexts;
  String currentContext;
  LocalDevelopment localDev;

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

  @Builder(toBuilder = true)
  public record LocalDevelopment(
    String localhost,
    int port,
    Path localPackageFile,
    Path localStatePath,
    DatastoreConf dataConf
  ) {}
}
