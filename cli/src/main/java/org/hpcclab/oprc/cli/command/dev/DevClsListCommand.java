package org.hpcclab.oprc.cli.command.dev;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jakarta.inject.Inject;
import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oprc.cli.conf.ConfigFileManager;
import org.hpcclab.oprc.cli.mixin.CommonOutputMixin;
import org.hpcclab.oprc.cli.service.OutputFormatter;
import org.hpcclab.oprc.cli.state.LocalDevManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.util.List;
import java.util.concurrent.Callable;

@Command(
  name = "class-list",
  aliases = {"cl"},
  description = "List classes",
  mixinStandardHelpOptions = true
)
public class DevClsListCommand implements Callable<Integer> {
  private static final Logger logger = LoggerFactory.getLogger(DevClsListCommand.class);
  @CommandLine.Mixin
  CommonOutputMixin commonOutputMixin;


  @Inject
  LocalDevManager devManager;
  @Inject
  ConfigFileManager fileManager;
  @Inject
  OutputFormatter outputFormatter;

  @CommandLine.Parameters(defaultValue = "")
  String cls;

  @Override
  public Integer call() throws Exception {
    List<OClass> clsList;
    if (cls.isEmpty()) {
      clsList = devManager.getClsRepo().getMap().stream().toList();
    } else {
      clsList = List.of(devManager.getClsRepo().get(cls));
    }
    Object[] objects = clsList.stream().map(JsonObject::mapFrom).toArray();
    JsonArray jsonArray = JsonArray.of(objects);
    outputFormatter.print(commonOutputMixin.getOutputFormat(), jsonArray);
    return 0;
  }
}
