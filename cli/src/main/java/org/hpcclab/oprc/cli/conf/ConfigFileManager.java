package org.hpcclab.oprc.cli.conf;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import jakarta.enterprise.context.ApplicationScoped;
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
    var fileCliConfig = new FileCliConfig.FileCliContext(
      "http://pm.oaas.127.0.0.1.nip.io",
      "http://inv.oaas.127.0.0.1.nip.io",
      null,
      "builtin.example.record"
    );
    return new FileCliConfig(
      Map.of("default", fileCliConfig),
      "default"
    );
  }

  public FileCliConfig getOrCreate() throws IOException {
    String homeDir = System.getProperty("user.home");
    Path configFilePath = Path.of(homeDir).resolve(cliConfig.configPath());
    var file = configFilePath.toFile();
    if (file.exists()) {
      return objectMapper.readValue(file, FileCliConfig.class);
    } else {
      file.getParentFile().mkdirs();
      var conf = getDefault();
      objectMapper.writeValue(file, conf);
      return conf;
    }
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
}
