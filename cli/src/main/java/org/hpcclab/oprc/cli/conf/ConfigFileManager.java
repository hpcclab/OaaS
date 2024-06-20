package org.hpcclab.oprc.cli.conf;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.collections.api.factory.Maps;
import org.hpcclab.oaas.repository.store.DatastoreConf;
import org.hpcclab.oprc.cli.CliConfig;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

/**
 * @author Pawissanutt
 */
@ApplicationScoped
public class ConfigFileManager {
  final CliConfig cliConfig;
  ObjectMapper objectMapper;
  FileCliConfig fileCliConfig;

  public ConfigFileManager(CliConfig cliConfig,
                           ObjectMapper mapper) {
    this.cliConfig = cliConfig;
    objectMapper = mapper.copyWith(new YAMLFactory()
      .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
      .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
    );
  }

  public FileCliConfig.FileCliContext current() throws IOException {
    return getOrCreate().current();
  }

  public FileCliConfig getDefault() {
    var defaultCtx = FileCliConfig.FileCliContext.builder()
      .pmUrl("http://pm.oaas.127.0.0.1.nip.io")
      .invUrl("http://inv.oaas.127.0.0.1.nip.io")
      .defaultClass("example.record")
      .build();
    var localDev = FileCliConfig.LocalDevelopment.builder()
      .port(8888)
      .localStatePath(Path.of(System.getProperty("user.home"), ".oprc", "local"))
      .localPackageFile(Path.of("pkg.yml"))
      .localhost("localhost")
      .dataConf(DatastoreConf.builder()
        .name("S3DEFAULT")
        .user("admin")
        .pass("changethis")
        .options(Map.of(
          "PUBLICURL", "http://localhost:9000",
          "URL", "http://localhost:9000",
            "BUCKET", "oaas-bkt"
        ))
        .build())
      .build();
    return new FileCliConfig(
      Maps.mutable.of("default", defaultCtx),
      "default",
      localDev
    );
  }

  public FileCliConfig getOrCreate() throws IOException {
    if (fileCliConfig != null) return fileCliConfig;
    String homeDir = System.getProperty("user.home");
    Path configFilePath = Path.of(homeDir).resolve(cliConfig.configPath());
    var file = configFilePath.toFile();
    if (file.exists()) {
      fileCliConfig = objectMapper.readValue(file, FileCliConfig.class);
    } else {
      file.getParentFile().mkdirs();
      fileCliConfig = getDefault();
      objectMapper.writeValue(file, fileCliConfig);
    }
    return fileCliConfig;
  }

  public void update(FileCliConfig config) throws IOException {
    String homeDir = System.getProperty("user.home");
    Path configFilePath = Path.of(homeDir).resolve(cliConfig.configPath());
    var file = configFilePath.toFile();
    if (file.exists()) {
      objectMapper.writeValue(file, config);
    } else {
      file.getParentFile().mkdirs();
      objectMapper.writeValue(file, config);
    }
  }

  public void update(FileCliConfig.FileCliContext ctx) throws IOException {
    String homeDir = System.getProperty("user.home");
    Path configFilePath = Path.of(homeDir).resolve(cliConfig.configPath());
    var file = configFilePath.toFile();
    var conf = getOrCreate();
    conf.getContexts().put(conf.getCurrentContext(), ctx);
    objectMapper.writeValue(file, conf);
  }
}
