package org.hpcclab.oprc.cli.command.dev;

import jakarta.inject.Inject;
import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.repository.MapEntityRepository;
import org.hpcclab.oprc.cli.conf.ConfigFileManager;
import org.hpcclab.oprc.cli.mixin.CommonOutputMixin;
import org.hpcclab.oprc.cli.service.OutputFormatter;
import org.hpcclab.oprc.cli.state.LocalDevManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.util.Set;
import java.util.concurrent.Callable;

@Command(
  name = "class-delete",
  aliases = {"cd"},
  description = "delete a class",
  mixinStandardHelpOptions = true
)
public class DevClsDeleteCommand implements Callable<Integer> {
  private static final Logger logger = LoggerFactory.getLogger(DevClsDeleteCommand.class);
  @CommandLine.Mixin
  CommonOutputMixin commonOutputMixin;


  @Inject
  LocalDevManager devManager;
  @Inject
  ConfigFileManager fileManager;
  @Inject
  OutputFormatter outputFormatter;

  @CommandLine.Parameters()
  String clsKey;

  @Override
  public Integer call() throws Exception {
    MapEntityRepository.MapClsRepository clsRepo = devManager.getClsRepo();
    OClass removed = clsRepo.remove(clsKey);
    if (removed != null) {
      System.out.printf("class '%s' is deleted%n", clsKey);
      devManager.persistPkg();
    } else {
      System.out.printf("class '%s' does not exist in local state%n", clsKey);
      Set<String> keys = clsRepo.getMap().keySet();
      System.out.printf("found %s%n", keys);
    }
    return 0;
  }
}
