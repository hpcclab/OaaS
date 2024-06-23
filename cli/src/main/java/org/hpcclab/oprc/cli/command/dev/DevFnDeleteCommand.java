package org.hpcclab.oprc.cli.command.dev;

import jakarta.inject.Inject;
import org.hpcclab.oaas.repository.MapEntityRepository;
import org.hpcclab.oprc.cli.conf.ConfigFileManager;
import org.hpcclab.oprc.cli.mixin.CommonOutputMixin;
import org.hpcclab.oprc.cli.service.OutputFormatter;
import org.hpcclab.oprc.cli.state.LocalDevManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

@Command(
  name = "function-delete",
  aliases = {"fd"},
  description = "delete a function",
  mixinStandardHelpOptions = true
)
public class DevFnDeleteCommand implements Callable<Integer> {
  private static final Logger logger = LoggerFactory.getLogger(DevFnDeleteCommand.class);
  @CommandLine.Mixin
  CommonOutputMixin commonOutputMixin;


  @Inject
  LocalDevManager devManager;
  @Inject
  ConfigFileManager fileManager;
  @Inject
  OutputFormatter outputFormatter;

  @CommandLine.Parameters()
  String fnKey;

  @Override
  public Integer call() throws Exception {
    MapEntityRepository.MapFnRepository fnRepo = devManager.getFnRepo();
    if (fnRepo.getMap().contains(fnKey)) {
      fnRepo.remove(fnKey);
      System.out.printf("function '%s' is deleted%n", fnKey);
      devManager.persistPkg();
    } else {
      System.out.printf("function '%s' does not exist in local state%n", fnKey);
    }
    return 0;
  }
}
