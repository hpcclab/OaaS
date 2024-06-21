package org.hpcclab.oprc.cli.command.dev;

import jakarta.inject.Inject;
import org.hpcclab.oprc.cli.conf.ConfigFileManager;
import org.hpcclab.oprc.cli.state.LocalDevManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(
  name = "object-clean",
  aliases = {"ocl", "od"},
  description = "create an object",
  mixinStandardHelpOptions = true
)
public class DevObjectCleanCommand implements Callable<Integer> {
  private static final Logger logger = LoggerFactory.getLogger(DevObjectCleanCommand.class);
  @CommandLine.Parameters(defaultValue = "")
  String cls;

  @Inject
  ConfigFileManager fileManager;
  @Inject
  LocalDevManager devManager;

  @Override
  public Integer call() throws Exception {
    var conf = fileManager.dev();
    if (cls==null) cls = conf.getDefaultClass();
    devManager.getObjRepoManager().clean(cls);
    System.out.println("delete objects of class '" + cls + "'");

    return 0;
  }
}
