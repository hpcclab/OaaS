package org.hpcclab.oprc.cli.command.dev;

import jakarta.inject.Inject;
import org.hpcclab.oprc.cli.conf.ConfigFileManager;
import org.hpcclab.oprc.cli.conf.FileCliConfig;
import org.hpcclab.oprc.cli.service.DevServerService;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.util.Optional;
import java.util.concurrent.Callable;

@Command(
  name = "server",
  aliases = "s",
  description = "Start dev server",
  mixinStandardHelpOptions = true
)
public class DevServerCommand implements Callable<Integer> {
  @Inject
  DevServerService devServerService;
  @Inject
  ConfigFileManager fileManager;

  @CommandLine.Option(names = {"-p", "--port"})
  Optional<Integer> optionalPort;

  @Override
  public Integer call() throws Exception {
    FileCliConfig config = fileManager.getOrCreate();
    int port;
    FileCliConfig.LocalDevelopment localDev = config.getLocalDev();
    if (optionalPort.isPresent()) {
      port = optionalPort.get();
      localDev = localDev.toBuilder().port(port).build();
      config.setLocalDev(localDev);
      fileManager.update(config);
    } else {
      port = localDev.port();
    }
    FileCliConfig.FileCliContext dev = fileManager.dev();
    dev.setInvUrl("http://%s:%d".formatted(localDev.localhost(), port));
    fileManager.updateDev(dev);
    devServerService.start(port);
    System.out.println("Start dev server on port: " + port);
    Thread.sleep(Long.MAX_VALUE);
    return 0;
  }
}
